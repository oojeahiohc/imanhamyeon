package com.imanhamyeon.auth;

import com.imanhamyeon.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.stereotype.Component;

@Component
public class GoogleLoginHandler
        implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private final MemberRepository members;
    private final GoogleAuthService googleAuthService;
    private final SecurityContextRepository contexts;
    private final HttpSessionCsrfTokenRepository csrfTokens;
    private final String frontendUrl;

    public GoogleLoginHandler(MemberRepository members,
            GoogleAuthService googleAuthService,
            SecurityContextRepository contexts,
            HttpSessionCsrfTokenRepository csrfTokens,
            @Value("${FRONTEND_URL:http://localhost:5173}") String frontendUrl) {
        this.members = members;
        this.googleAuthService = googleAuthService;
        this.contexts = contexts;
        this.csrfTokens = csrfTokens;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response, Authentication authentication)
            throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken google)
                || !"google".equals(google.getAuthorizedClientRegistrationId())) {
            fail(request, response);
            return;
        }

        String sub = google.getPrincipal().getAttribute("sub");
        if (sub == null || sub.isBlank()) {
            fail(request, response);
            return;
        }

        try {
            if (members.findByGoogleSub(sub).isPresent()) {
                Authentication memberAuth = UsernamePasswordAuthenticationToken
                        .authenticated("google:" + sub, null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));

                new CsrfAuthenticationStrategy(csrfTokens)
                        .onAuthentication(memberAuth, request, response);

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(memberAuth);
                SecurityContextHolder.setContext(context);
                contexts.saveContext(context, request, response);
                response.sendRedirect(frontendUrl + "/today");
            } else {
                SecurityContextHolder.clearContext();
                googleAuthService.hold(sub, request);
                response.sendRedirect(frontendUrl + "/google/consent");
            }
        } catch (RuntimeException error) {
            fail(request, response);
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        fail(request, response);
    }

    private void fail(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(frontendUrl + "/login?googleError=failed");
    }
}