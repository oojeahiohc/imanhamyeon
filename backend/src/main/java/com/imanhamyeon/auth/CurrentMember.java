package com.imanhamyeon.auth;

import com.imanhamyeon.member.Member;
import com.imanhamyeon.member.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentMember {

    private final MemberRepository members;

    public CurrentMember(MemberRepository members) {
        this.members = members;
    }

    public Member require(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw unauthorized();
        }
        String name = authentication.getName();
        return (name.startsWith("google:")
                ? members.findByGoogleSub(name.substring("google:".length()))
                : members.findByLoginId(name))
                .orElseThrow(this::unauthorized);
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "로그인이 필요합니다.");
    }
}