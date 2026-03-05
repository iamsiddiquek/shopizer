package com.salesmanager.core.model.common;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Embeddable
// MIGRATION NOTE: Suppress jakarta.persistence.Temporal deprecation warnings because this legacy Date mapping must stay unchanged during the Boot 4 migration.
@SuppressWarnings("deprecation")
public class CredentialsReset {
	
	@Column (name ="RESET_CREDENTIALS_REQ", length=256)
	private String credentialsRequest;

	@Temporal(TemporalType.DATE)
	@Column(name = "RESET_CREDENTIALS_EXP")
	private Date credentialsRequestExpiry = new Date();

	public String getCredentialsRequest() {
		return credentialsRequest;
	}

	public void setCredentialsRequest(String credentialsRequest) {
		this.credentialsRequest = credentialsRequest;
	}

	public Date getCredentialsRequestExpiry() {
		return credentialsRequestExpiry;
	}

	public void setCredentialsRequestExpiry(Date credentialsRequestExpiry) {
		this.credentialsRequestExpiry = credentialsRequestExpiry;
	}

}
