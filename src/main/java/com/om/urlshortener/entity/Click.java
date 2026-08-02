package com.om.urlshortener.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clicks")
public class Click {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "url_id", nullable = false)
	private Long urlId;

	@Column(name = "clicked_at", nullable = false, updatable = false, insertable = false)
	private OffsetDateTime clickedAt;

	@Column(name = "referrer", columnDefinition = "TEXT")
	private String referrer;

	@Column(name = "ip_address", columnDefinition = "INET")
	private String ipAddress;

	@Column(name = "user_agent", columnDefinition = "TEXT")
	private String userAgent;

	protected Click() {
	}

	public Click(Long urlId, String referrer, String ipAddress, String userAgent) {
		this.urlId = urlId;
		this.referrer = referrer;
		this.ipAddress = ipAddress;
		this.userAgent = userAgent;
	}

	public Long getId() {
		return id;
	}

	public Long getUrlId() {
		return urlId;
	}

	public OffsetDateTime getClickedAt() {
		return clickedAt;
	}

	public String getReferrer() {
		return referrer;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public String getUserAgent() {
		return userAgent;
	}
}
