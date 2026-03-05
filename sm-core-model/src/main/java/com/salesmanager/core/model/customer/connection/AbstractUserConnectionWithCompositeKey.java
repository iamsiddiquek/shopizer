package com.salesmanager.core.model.customer.connection;

import java.io.Serial;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@Deprecated
@MappedSuperclass
// MIGRATION NOTE: Hibernate 6 disallows @Table on @MappedSuperclass; the inherited USERCONNECTION mapping is now declared on the concrete entity without changing table semantics.
public abstract class AbstractUserConnectionWithCompositeKey extends
		AbstractUserConnection<UserConnectionPK> {

	@Id
	private UserConnectionPK primaryKey = new UserConnectionPK();

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;

	@Override
	public String getProviderId() {
		return primaryKey.getProviderId();
	}

	@Override
	public void setProviderId(String providerId) {
		primaryKey.setProviderId(providerId);
	}

	@Override
	public String getProviderUserId() {
		return primaryKey.getProviderUserId();
	}

	@Override
	public void setProviderUserId(String providerUserId) {
		primaryKey.setProviderUserId(providerUserId);
	}

	@Override
	public String getUserId() {
		return primaryKey.getUserId();
	}

	@Override
	public void setUserId(String userId) {
		primaryKey.setUserId(userId);
	}

	@Override
	protected UserConnectionPK getId() {
		return primaryKey;
	}

}
