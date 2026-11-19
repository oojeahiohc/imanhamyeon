package com.imanhamyeon.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "members")
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "login_id", length = 20, unique = true)
	private String loginId;

	@Column(name = "password_hash", length = 255)
	private String passwordHash;

	@Column(name = "auth_provider", nullable = false, length = 10)
	private String authProvider;

	@Column(name = "google_sub", length = 255, unique = true)
	private String googleSub;

	@Column(name = "privacy_agreed_at", nullable = false)
	private Instant privacyAgreedAt;

	@Column(name = "ai_agreed_at", nullable = false)
	private Instant aiAgreedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Member() {
	}

	public Member(String loginId, String passwordHash) {
		Instant now = Instant.now();
		this.loginId = loginId;
		this.passwordHash = passwordHash;
		this.authProvider = "LOCAL";
		this.privacyAgreedAt = now;
		this.aiAgreedAt = now;
		this.createdAt = now;
	}

	public static Member google(String googleSub) {
		Member member = new Member();
		Instant now = Instant.now();
		member.googleSub = googleSub;
		member.authProvider = "GOOGLE";
		member.privacyAgreedAt = now;
		member.aiAgreedAt = now;
		member.createdAt = now;
		return member;
	}

	public Long getId() {
		return id;
	}

	public String getLoginId() {
		return loginId;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getAuthProvider() {
		return authProvider;
	}

	public Instant getPrivacyAgreedAt() {
		return privacyAgreedAt;
	}

	public Instant getAiAgreedAt() {
		return aiAgreedAt;
	}
}