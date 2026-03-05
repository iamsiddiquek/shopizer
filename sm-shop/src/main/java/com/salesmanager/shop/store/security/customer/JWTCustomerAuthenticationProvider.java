package com.salesmanager.shop.store.security.customer;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Custom authautentication provider for customer api
 * @author carlsamson
 *
 */
public class JWTCustomerAuthenticationProvider extends DaoAuthenticationProvider {
	
    private UserDetailsService jwtCustomerDetailsService;

	public JWTCustomerAuthenticationProvider(UserDetailsService jwtCustomerDetailsService) {
		super(jwtCustomerDetailsService);
		// MIGRATION NOTE: Spring Security 7 requires DaoAuthenticationProvider subclasses to receive the UserDetailsService in the constructor; provider behavior is otherwise unchanged.
		this.jwtCustomerDetailsService = jwtCustomerDetailsService;
	}


	@Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) authentication;
        String name = auth.getName();
        Object credentials = auth.getCredentials();
        UserDetails customer = jwtCustomerDetailsService.loadUserByUsername(name);
        if (customer == null) {
            throw new BadCredentialsException("Username/Password does not match for " + auth.getPrincipal());
        }
        
        String pass = credentials.toString();
        String usr = name;
        
        if(!passwordMatch(pass, usr)) {
        	throw new BadCredentialsException("Username/Password does not match for " + auth.getPrincipal());
        }
        
        
        /**
         * username password auth
         */

        
        return new UsernamePasswordAuthenticationToken(customer, credentials, customer.getAuthorities());
    }
	
	
    private boolean passwordMatch(String rawPassword, String user) {
		    return getPasswordEncoder().matches(rawPassword, user);
	}
	
    @Override
    public boolean supports(Class<?> authentication) {
        return true;
    }


	public UserDetailsService getJwtCustomerDetailsService() {
		return jwtCustomerDetailsService;
	}


	public void setJwtCustomerDetailsService(UserDetailsService jwtCustomerDetailsService) {
		this.jwtCustomerDetailsService = jwtCustomerDetailsService;
	}

}
