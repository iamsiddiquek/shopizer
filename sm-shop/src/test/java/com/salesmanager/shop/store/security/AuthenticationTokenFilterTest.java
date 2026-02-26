package com.salesmanager.shop.store.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.salesmanager.shop.store.security.common.CustomAuthenticationManager;

class AuthenticationTokenFilterTest {

	private AuthenticationTokenFilter filter;
	private CustomAuthenticationManager customerManager;
	private CustomAuthenticationManager adminManager;

	@BeforeEach
	void setUp() {
		filter = new AuthenticationTokenFilter();
		customerManager = mock(CustomAuthenticationManager.class);
		adminManager = mock(CustomAuthenticationManager.class);

		ReflectionTestUtils.setField(filter, "tokenHeader", "Authorization");
		ReflectionTestUtils.setField(filter, "jwtCustomCustomerAuthenticationManager", customerManager);
		ReflectionTestUtils.setField(filter, "jwtCustomAdminAuthenticationManager", adminManager);
	}

	@Test
	void authBearerDelegatesToCustomerManager() throws Exception {
		MockHttpServletRequest request = request("GET", "/api/v1/auth/customer/profile");
		request.addHeader("Authorization", "Bearer token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		verify(customerManager).authenticateRequest(request, response);
		verify(adminManager, never()).authenticateRequest(any(), any());
		verify(chain).doFilter(request, response);
	}

	@Test
	void authFacebookTokenSkipsManagersAndContinues() throws Exception {
		MockHttpServletRequest request = request("GET", "/api/v1/auth/customer/facebook");
		request.addHeader("Authorization", "FB token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		verifyNoInteractions(customerManager, adminManager);
		verify(chain).doFilter(request, response);
	}

	@Test
	void privateRequestWithoutAuthorizationReturnsUnauthorized() throws Exception {
		MockHttpServletRequest request = request("GET", "/api/v1/private/orders");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		assertEquals(401, response.getStatus());
		verifyNoInteractions(adminManager);
		verify(chain, never()).doFilter(any(), any());
	}

	@Test
	void privateOptionsRequestWithoutAuthorizationIsAllowed() throws Exception {
		MockHttpServletRequest request = request("OPTIONS", "/api/v1/private/orders");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		assertEquals(200, response.getStatus());
		verifyNoInteractions(adminManager);
		verify(chain).doFilter(request, response);
	}

	@Test
	void privateLoginRequestWithoutAuthorizationIsAllowed() throws Exception {
		MockHttpServletRequest request = request("POST", "/api/v1/private/login");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		assertEquals(200, response.getStatus());
		verifyNoInteractions(adminManager);
		verify(chain).doFilter(request, response);
	}

	@Test
	void privateRequestInvalidBearerReturnsUnauthorized() throws Exception {
		MockHttpServletRequest request = request("GET", "/api/v1/private/orders");
		request.addHeader("Authorization", "Bearer invalid");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);
		doThrow(new RuntimeException("bad token")).when(adminManager).authenticateRequest(any(), any());

		filter.doFilterInternal(request, response, chain);

		assertEquals(401, response.getStatus());
		verify(adminManager).authenticateRequest(request, response);
		verify(chain, never()).doFilter(any(), any());
	}

	@Test
	void privateLoginInvalidBearerPropagatesAsServletException() throws Exception {
		MockHttpServletRequest request = request("POST", "/api/v1/private/login");
		request.addHeader("Authorization", "Bearer invalid");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);
		doThrow(new RuntimeException("bad token")).when(adminManager).authenticateRequest(any(), any());

		assertThrows(ServletException.class, () -> filter.doFilterInternal(request, response, chain));
		verify(adminManager).authenticateRequest(request, response);
		verify(chain, never()).doFilter(any(), any());
	}

	@Test
	void setsCorsHeadersAndUsesRequestOrigin() throws Exception {
		MockHttpServletRequest request = request("GET", "/health");
		request.addHeader("origin", "https://example.test");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilterInternal(request, response, chain);

		assertEquals("https://example.test", response.getHeader("Access-Control-Allow-Origin"));
		assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
		verify(chain).doFilter(request, response);
	}

	private MockHttpServletRequest request(String method, String uri) {
		MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8080);
		return request;
	}
}
