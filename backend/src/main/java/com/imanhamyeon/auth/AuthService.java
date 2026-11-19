package com.imanhamyeon.auth;

import com.imanhamyeon.member.Member;
import com.imanhamyeon.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_ATTEMPTS = 5;

    private final MemberRepository members;
    private final PasswordEncoder passwords;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contexts;
    private final HttpSessionCsrfTokenRepository csrfTokens;
    private final ConcurrentHashMap<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    public AuthService(MemberRepository members, PasswordEncoder passwords,
            AuthenticationManager authenticationManager, SecurityContextRepository contexts,
            HttpSessionCsrfTokenRepository csrfTokens) {
        this.members = members;
        this.passwords = passwords;
        this.authenticationManager = authenticationManager;
        this.contexts = contexts;
        this.csrfTokens = csrfTokens;
    }

    @Transactional
    public Member register(String loginId, String password,
            Boolean privacyConsent, Boolean aiConsent) {
        if (loginId == null || !loginId.matches("[a-z0-9_]{4,20}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "아이디는 영문 소문자, 숫자, 밑줄 4~20자로 입력해 주세요.");
        }
        if (password == null || !password.matches("[\\x21-\\x7E]{8,64}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "비밀번호는 공백 없는 영문, 숫자, 특수문자 8~64자로 입력해 주세요.");
        }
        if (!Boolean.TRUE.equals(privacyConsent) || !Boolean.TRUE.equals(aiConsent)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "개인정보 및 외부 AI 전송 동의가 필요합니다.");
        }
        if (members.existsByLoginId(loginId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }
        return members.saveAndFlush(new Member(loginId, passwords.encode(password)));
    }

    public Member login(String loginId, String password,
            HttpServletRequest request, HttpServletResponse response) {
        limitAttempts(request.getRemoteAddr());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginId, password));
        } catch (AuthenticationException error) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        Member member = members.findByLoginId(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "아이디 또는 비밀번호가 올바르지 않습니다."));

        request.getSession(true);
        request.changeSessionId();
        new CsrfAuthenticationStrategy(csrfTokens).onAuthentication(
                authentication, request, response);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return member;
    }

    private void limitAttempts(String ip) {
        Instant now = Instant.now();
        if (loginAttempts.size() > 1000) {
            loginAttempts.entrySet().removeIf(entry ->
                    !now.isBefore(entry.getValue().startedAt().plus(LOGIN_WINDOW)));
        }
        if (loginAttempts.size() > 1000) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "로그인 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
        }
        LoginAttempt current = loginAttempts.compute(ip, (key, previous) -> {
            if (previous == null ||
                    !now.isBefore(previous.startedAt().plus(LOGIN_WINDOW))) {
                return new LoginAttempt(1, now);
            }
            return new LoginAttempt(previous.count() + 1, previous.startedAt());
        });
        if (current.count() > MAX_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "로그인 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private record LoginAttempt(int count, Instant startedAt) {}
}