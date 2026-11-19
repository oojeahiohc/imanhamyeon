package com.imanhamyeon.auth;

import com.imanhamyeon.member.Member;
import com.imanhamyeon.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final MemberRepository members;
    private final GoogleAuthService googleAuthService;
    private final SecurityContextRepository contexts;
    private final HttpSessionCsrfTokenRepository csrfTokens;

    public AuthController(AuthService authService, MemberRepository members,
            GoogleAuthService googleAuthService,
            SecurityContextRepository contexts,
            HttpSessionCsrfTokenRepository csrfTokens) {
        this.authService = authService;
        this.members = members;
        this.googleAuthService = googleAuthService;
        this.contexts = contexts;
        this.csrfTokens = csrfTokens;
    }

    @GetMapping("/session")
    public SessionResponse session(Authentication authentication, CsrfToken csrfToken,
            HttpServletRequest request) {
        MemberResponse member = null;

        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            String name = authentication.getName();
            if (name.startsWith("google:")) {
                member = members.findByGoogleSub(name.substring("google:".length()))
                        .map(MemberResponse::from)
                        .orElse(null);
            } else {
                member = members.findByLoginId(name)
                        .map(MemberResponse::from)
                        .orElse(null);
            }
        }

        return new SessionResponse(member != null, member, csrfToken.getToken(),
                googleAuthService.isPending(request.getSession(false)));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse register(@RequestBody RegisterRequest request) {
        return MemberResponse.from(authService.register(request.loginId(),
                request.password(), request.privacyConsent(), request.aiConsent()));
    }

    @PostMapping("/login")
    public MemberResponse login(@RequestBody LoginRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        return MemberResponse.from(authService.login(body.loginId(),
                body.password(), request, response));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    @PostMapping("/google/start")
    public GoogleStartResponse startGoogle() {
        return new GoogleStartResponse("/oauth2/authorization/google");
    }

    @PostMapping("/google/complete")
    public MemberResponse completeGoogle(@RequestBody ConsentRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        GoogleAuthService.Completion completed = googleAuthService.complete(
                request, body.privacyConsent(), body.aiConsent());

        request.changeSessionId();
        Authentication authentication = UsernamePasswordAuthenticationToken
                .authenticated("google:" + completed.sub(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));

        new CsrfAuthenticationStrategy(csrfTokens)
                .onAuthentication(authentication, request, response);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);

        return MemberResponse.from(completed.member());
    }

    @PostMapping("/google/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelGoogle(HttpServletRequest request) {
        googleAuthService.cancel(request);
    }

    public record RegisterRequest(
            String loginId, String password, Boolean privacyConsent, Boolean aiConsent) {}

    public record LoginRequest(String loginId, String password) {}

    public record ConsentRequest(Boolean privacyConsent, Boolean aiConsent) {}

    public record GoogleStartResponse(String authorizationUrl) {}

    public record MemberResponse(Long id, String loginId, String authProvider) {
        static MemberResponse from(Member member) {
            return new MemberResponse(member.getId(),
                    member.getLoginId(), member.getAuthProvider());
        }
    }

    public record SessionResponse(boolean authenticated, MemberResponse member,
            String csrfToken, boolean googleSignupPending) {}
}