package com.imanhamyeon.member;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByLoginId(String loginId);
    Optional<Member> findByGoogleSub(String googleSub);
    boolean existsByLoginId(String loginId);
}