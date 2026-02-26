package com.salesmanager.core.model.customer.connection;

import java.io.Serial;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Deprecated
@Entity
@Table(name = "USERCONNECTION", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "providerId", "userRank"})
})
public class UserConnection extends AbstractUserConnectionWithCompositeKey {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;


}
