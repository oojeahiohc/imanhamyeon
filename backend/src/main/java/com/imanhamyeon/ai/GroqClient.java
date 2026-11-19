package com.imanhamyeon.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GroqClient {

    private static final String ENDPOINT =
            "https://api.groq.com/openai/v1/chat/completions";

    private final Gson gson = new Gson();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ApiUsageStore usage;
    private final String apiKey;
    private final String model;

    public GroqClient(ApiUsageStore usage,
            @Value("${GROQ_API_KEY}") String apiKey,
            @Value("${GROQ_MODEL:openai/gpt-oss-20b}") String model) {
        this.usage = usage;
        this.apiKey = apiKey;
        this.model = model;
    }

    public boolean hasSeriousRisk(String concern) {
        JsonObject props = new JsonObject();
        props.add("highRisk", scalar("boolean"));
        JsonObject schema = objectSchema(props, List.of("highRisk"));

        JsonObject reply = complete(
                "사용자의 글에서 자살 또는 자해 위험, 타인에 대한 심각한 위해 의도, "
                + "현실 판단이 크게 손상된 상태 또는 급성 정신건강 위기 등 "
                + "심각한 위험 징후를 판단하세요. 의심되면 highRisk=true. "
                + "진단하지 말고 JSON만 반환하세요.",
                concern, "risk_check", schema);
        try {
            return reply.get("highRisk").getAsBoolean();
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "위험 여부를 확인할 수 없습니다.");
        }
    }

    public List<SuggestedGoal> suggestGoals(String concern) {
        JsonObject goalProps = new JsonObject();
        goalProps.add("title", scalar("string"));
        goalProps.add("actionText", scalar("string"));
        JsonObject itemSchema = objectSchema(
                goalProps, List.of("title", "actionText"));

        JsonObject arraySchema = scalar("array");
        arraySchema.add("items", itemSchema);
        JsonObject rootProps = new JsonObject();
        rootProps.add("goals", arraySchema);
        JsonObject schema = objectSchema(rootProps, List.of("goals"));

        JsonObject reply = complete(
                "사용자의 고민을 바탕으로 작고 안전하며 서로 다른 하루 실천 목표 "
                + "정확히 3개를 한국어로 제안하세요. 각 목표에 짧은 title과 "
                + "구체적인 actionText를 작성하세요. "
                + "진단, 치료, 위험 행동 조언, 과도한 부담을 주는 제안은 금지합니다.",
                concern, "daily_goals", schema);

        try {
            JsonArray items = reply.getAsJsonArray("goals");
            if (items == null || items.size() != 3) {
                throw new IllegalArgumentException("목표 수 오류");
            }
            Set<String> seen = new HashSet<>();
            List<SuggestedGoal> goals = new java.util.ArrayList<>();
            for (JsonElement item : items) {
                JsonObject goal = item.getAsJsonObject();
                String title = goal.get("title").getAsString().trim();
                String action = goal.get("actionText").getAsString().trim();
                if (title.isBlank() || title.length() > 100
                        || action.isBlank() || action.length() > 500
                        || !seen.add(title + "\n" + action)) {
                    throw new IllegalArgumentException("목표 형식 오류");
                }
                goals.add(new SuggestedGoal(title, action));
            }
            return List.copyOf(goals);
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "목표 생성 응답을 확인할 수 없습니다.");
        }
    }

    private JsonObject complete(String system, String user,
            String schemaName, JsonObject schema) {
        LocalDate usageDay = usage.reserveCall();

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of(
                                "name", schemaName,
                                "strict", true,
                                "schema", schema)),
                "max_completion_tokens", 1000);

        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        try {
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "AI 서비스 호출 제한으로 잠시 이용할 수 없습니다.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "AI 서비스 응답 오류가 발생했습니다.");
            }
            JsonObject result = JsonParser.parseString(response.body())
                    .getAsJsonObject();
            JsonObject tokenUsage = result.getAsJsonObject("usage");
            if (tokenUsage != null && tokenUsage.has("total_tokens")) {
                usage.addTokens(usageDay, tokenUsage.get("total_tokens").getAsInt());
            }
            String content = result.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
            return JsonParser.parseString(content).getAsJsonObject();
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 서비스 연결에 실패했습니다.");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 서비스 요청이 중단되었습니다.");
        } catch (IllegalStateException | NullPointerException
                | IndexOutOfBoundsException | JsonParseException error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI 서비스 응답 형식 오류가 발생했습니다.");
        }
    }

    private static JsonObject scalar(String type) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", type);
        return schema;
    }

    private static JsonObject objectSchema(
            JsonObject properties, List<String> required) {
        JsonObject schema = scalar("object");
        schema.add("properties", properties);
        schema.add("required", new Gson().toJsonTree(required));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    public record SuggestedGoal(String title, String actionText) {}
}