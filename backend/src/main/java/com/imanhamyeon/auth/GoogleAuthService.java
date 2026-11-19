package com.imanhamyeon.auth;

import com.imanhamyeon.member.Member;
import com.imanhamyeon.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GoogleAuthService {

    private static final String PENDING_KEY = "PENDING_GOOGLE_SIGNUP";
    private static final Duration VALID_FOR = Duration.ofMinutes(10);

    private final MemberRepository members;

    public GoogleAuthService(MemberRepository members) {
        this.members = members;
    }

    public void hold(String sub, HttpServletRequest request) {
        if (sub == null || sub.isBlank()) {
            throw new IllegalArgumentException("Google 식별값이 없습니다.");
        }

        HttpSession previous = request.getSession(false);
        if (previous != null) {
            previous.invalidate();
        }
        HttpSession pending = request.getSession(true);
        pending.setAttribute(PENDING_KEY, new PendingGoogle(sub, Instant.now()));
    }

    public boolean isPending(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object saved = session.getAttribute(PENDING_KEY);
        if (!(saved instanceof PendingGoogle pending)) {
            return false;
        }
        if (!Instant.now().isBefore(pending.startedAt().plus(VALID_FOR))) {
            session.removeAttribute(PENDING_KEY);
            return false;
        }
        return true;
    }

    @Transactional
    public Completion complete(HttpServletRequest request,
            Boolean privacyConsent, Boolean aiConsent) {
        HttpSession session = request.getSession(false);
        if (!isPending(session)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Google 인증을 다시 진행해 주세요.");
        }
        if (!Boolean.TRUE.equals(privacyConsent)
                || !Boolean.TRUE.equals(aiConsent)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "두 가지 필수 동의가 필요합니다.");
        }

        PendingGoogle pending = (PendingGoogle) session.getAttribute(PENDING_KEY);
        Member member = members.findByGoogleSub(pending.sub())
                .orElseGet(() -> members.saveAndFlush(Member.google(pending.sub())));
        session.removeAttribute(PENDING_KEY);
        return new Completion(member, pending.sub());
    }

    public void cancel(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(PENDING_KEY) != null) {
            session.invalidate();
        }
    }

    private record PendingGoogle(String sub, Instant startedAt)
            implements Serializable {}

    public record Completion(Member member, String sub) {}
}