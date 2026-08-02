package com.om.urlshortener.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "urls")
public class Url {

	public static final String PENDING_SHORT_CODE = "";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "short_code", nullable = false, unique = true, length = 10)
	private String shortCode;

	@Column(name = "long_url", nullable = false, columnDefinition = "TEXT")
	private String longUrl;

	@Column(name = "created_at", nullable = false, updatable = false, insertable = false)
	private OffsetDateTime createdAt;

	@Column(name = "expires_at")
	private OffsetDateTime expiresAt;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	protected Url() {
	}

	private Url(String longUrl) {
		this.shortCode = PENDING_SHORT_CODE;
		this.longUrl = longUrl;
	}

	public static Url pending(String longUrl) {
		return new Url(longUrl);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getShortCode() {
		return shortCode;
	}

	public void setShortCode(String shortCode) {
		this.shortCode = shortCode;
	}

	public String getLongUrl() {
		return longUrl;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(OffsetDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
