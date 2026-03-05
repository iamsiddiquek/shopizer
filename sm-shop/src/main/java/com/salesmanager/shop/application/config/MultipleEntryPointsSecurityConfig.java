package com.salesmanager.shop.application.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

import com.salesmanager.shop.admin.security.UserAuthenticationSuccessHandler;
import com.salesmanager.shop.admin.security.WebUserServices;
import com.salesmanager.shop.store.security.AuthenticationTokenFilter;
import com.salesmanager.shop.store.security.ServicesAuthenticationSuccessHandler;
import com.salesmanager.shop.store.security.admin.JWTAdminAuthenticationProvider;
import com.salesmanager.shop.store.security.admin.JWTAdminServicesImpl;
import com.salesmanager.shop.store.security.customer.JWTCustomerAuthenticationProvider;
import com.salesmanager.shop.store.security.services.CredentialsService;
import com.salesmanager.shop.store.security.services.CredentialsServiceImpl;

/**
 * Main entry point for security - admin - customer - auth - private - services
 *
 * @author dur9213
 */
@Configuration
@EnableWebSecurity
public class MultipleEntryPointsSecurityConfig {

	private static final String API_VERSION = "/api/v*";

	@Bean
	public AuthenticationTokenFilter authenticationTokenFilter() {
		return new AuthenticationTokenFilter();
	}

	@Bean
	public CredentialsService credentialsService() {
		return new CredentialsServiceImpl();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public UserAuthenticationSuccessHandler userAuthenticationSuccessHandler() {
		return new UserAuthenticationSuccessHandler();
	}

	@Bean
	public ServicesAuthenticationSuccessHandler servicesAuthenticationSuccessHandler() {
		return new ServicesAuthenticationSuccessHandler();
	}

	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
		PathPatternRequestMatcher.Builder matcher = PathPatternRequestMatcher.withDefaults();
		return web -> web.ignoring().requestMatchers(
				// MIGRATION NOTE: Spring Security 7 replaces AntPathRequestMatcher with PathPatternRequestMatcher; the ignored public/static path list itself is unchanged.
				new OrRequestMatcher(matcher.matcher("/"), matcher.matcher("/error"), matcher.matcher("/resources/**"),
						matcher.matcher("/static/**"), matcher.matcher("/services/public/**"),
						matcher.matcher("/swagger-ui.html"), matcher.matcher("/swagger-ui/**"),
						matcher.matcher("/v3/api-docs/**"), matcher.matcher("/webjars/**")));
	}

	@Bean("customerAuthenticationManager")
	public AuthenticationManager customerAuthenticationManager(
			@Qualifier("customerDetailsService") UserDetailsService customerDetailsService, PasswordEncoder passwordEncoder) {
		return new ProviderManager(List.of(daoAuthenticationProvider(customerDetailsService, passwordEncoder)));
	}

	@Bean
	@Primary
	public AuthenticationManager authenticationManager(
			@Qualifier("customerAuthenticationManager") AuthenticationManager customerAuthenticationManager) {
		// MIGRATION NOTE: Spring Security 6 requires a single default AuthenticationManager bean for shared HttpSecurity setup.
		return customerAuthenticationManager;
	}

	@Bean("jwtAdminAuthenticationProvider")
	public AuthenticationProvider jwtAdminAuthenticationProvider(JWTAdminServicesImpl jwtAdminDetailsService,
			PasswordEncoder passwordEncoder) {
		// MIGRATION NOTE: Spring Security 7 requires DaoAuthenticationProvider subclasses to receive the UserDetailsService at construction time; authentication flow is otherwise unchanged.
		JWTAdminAuthenticationProvider provider = new JWTAdminAuthenticationProvider(jwtAdminDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		provider.setJwtAdminDetailsService(jwtAdminDetailsService);
		return provider;
	}

	@Bean("jwtAdminAuthenticationManager")
	public AuthenticationManager jwtAdminAuthenticationManager(JWTAdminServicesImpl jwtAdminDetailsService,
			PasswordEncoder passwordEncoder,
			@Qualifier("jwtAdminAuthenticationProvider") AuthenticationProvider jwtAdminAuthenticationProvider) {
		return new ProviderManager(List.of(daoAuthenticationProvider(jwtAdminDetailsService, passwordEncoder),
				jwtAdminAuthenticationProvider));
	}

	@Bean("jwtCustomerAuthenticationProvider")
	public AuthenticationProvider jwtCustomerAuthenticationProvider(
			@Qualifier("jwtCustomerDetailsService") UserDetailsService jwtCustomerDetailsService,
			PasswordEncoder passwordEncoder) {
		// MIGRATION NOTE: Spring Security 7 requires DaoAuthenticationProvider subclasses to receive the UserDetailsService at construction time; authentication flow is otherwise unchanged.
		JWTCustomerAuthenticationProvider provider = new JWTCustomerAuthenticationProvider(jwtCustomerDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		provider.setJwtCustomerDetailsService(jwtCustomerDetailsService);
		return provider;
	}

	@Bean("jwtCustomerAuthenticationManager")
	public AuthenticationManager jwtCustomerAuthenticationManager(
			@Qualifier("jwtCustomerDetailsService") UserDetailsService jwtCustomerDetailsService,
			PasswordEncoder passwordEncoder,
			@Qualifier("jwtCustomerAuthenticationProvider") AuthenticationProvider jwtCustomerAuthenticationProvider) {
		return new ProviderManager(List.of(daoAuthenticationProvider(jwtCustomerDetailsService, passwordEncoder),
				jwtCustomerAuthenticationProvider));
	}

	@Bean
	@Order(1)
	public SecurityFilterChain customerSecurityFilterChain(HttpSecurity http,
			@Qualifier("customerAuthenticationManager") AuthenticationManager customerAuthenticationManager)
			throws Exception {
		http.securityMatcher("/shop/**");
		http.authenticationManager(customerAuthenticationManager);
		http.csrf(AbstractHttpConfigurer::disable);
		http.authorizeHttpRequests(authorize -> authorize
				// MIGRATION NOTE: Preserves the legacy matcher ordering where /shop/** remains permit-all ahead of narrower customer paths.
				.requestMatchers("/shop/", "/shop/**", "/shop/customer/logon*", "/shop/customer/registration*",
						"/shop/customer/logout*", "/shop/customer/customLogon*", "/shop/customer/denied*")
				.permitAll().anyRequest().authenticated());
		http.httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(basicAuthenticationEntryPoint("shop-realm")));
		http.logout(logout -> logout.logoutUrl("/shop/customer/logout").logoutSuccessUrl("/shop/")
				.deleteCookies("JSESSIONID")
				// MIGRATION NOTE: Preserves the final effective legacy logout setting, where invalidateHttpSession(false) overrides the earlier true flag.
				.invalidateHttpSession(false));
		http.exceptionHandling(exceptionHandling -> exceptionHandling.accessDeniedPage("/shop/"));
		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain servicesSecurityFilterChain(HttpSecurity http, WebUserServices userDetailsService,
			PasswordEncoder passwordEncoder, ServicesAuthenticationSuccessHandler servicesAuthenticationSuccessHandler)
			throws Exception {
		http.securityMatcher("/services/**");
		http.authenticationManager(
				new ProviderManager(List.of(daoAuthenticationProvider(userDetailsService, passwordEncoder))));
		http.csrf(AbstractHttpConfigurer::disable);
		http.authorizeHttpRequests(authorize -> authorize.requestMatchers("/services/public/**").permitAll()
				.requestMatchers("/services/private/**").hasRole("AUTH").anyRequest().authenticated());
		http.httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(basicAuthenticationEntryPoint("rest-customer-realm")));
		http.formLogin(formLogin -> formLogin.successHandler(servicesAuthenticationSuccessHandler));
		return http.build();
	}

	@Bean
	@Order(5)
	public SecurityFilterChain userApiSecurityFilterChain(HttpSecurity http, AuthenticationTokenFilter authenticationTokenFilter,
			@Qualifier("jwtAdminAuthenticationManager") AuthenticationManager jwtAdminAuthenticationManager) throws Exception {
		http.securityMatcher(API_VERSION + "/private/**");
		http.authenticationManager(jwtAdminAuthenticationManager);
		http.authorizeHttpRequests(authorize -> authorize.requestMatchers(API_VERSION + "/private/login*").permitAll()
				.requestMatchers(API_VERSION + "/private/refresh").permitAll()
				.requestMatchers(HttpMethod.OPTIONS, API_VERSION + "/private/**").permitAll()
				.requestMatchers(API_VERSION + "/private/**").hasRole("AUTH").anyRequest().authenticated());
		http.httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(basicAuthenticationEntryPoint("api-admin-realm")));
		http.addFilterAfter(authenticationTokenFilter, BasicAuthenticationFilter.class);
		http.csrf(AbstractHttpConfigurer::disable);
		return http.build();
	}

	@Bean
	@Order(6)
	public SecurityFilterChain customerApiSecurityFilterChain(HttpSecurity http, AuthenticationTokenFilter authenticationTokenFilter,
			@Qualifier("jwtCustomerAuthenticationManager") AuthenticationManager jwtCustomerAuthenticationManager)
			throws Exception {
		http.securityMatcher(API_VERSION + "/auth/**");
		http.authenticationManager(jwtCustomerAuthenticationManager);
		http.authorizeHttpRequests(authorize -> authorize.requestMatchers(API_VERSION + "/auth/refresh").permitAll()
				.requestMatchers(API_VERSION + "/auth/login").permitAll()
				.requestMatchers(API_VERSION + "/auth/register").permitAll()
				.requestMatchers(HttpMethod.OPTIONS, API_VERSION + "/auth/**").permitAll()
				.requestMatchers(API_VERSION + "/auth/**").hasRole("AUTH_CUSTOMER").anyRequest().authenticated());
		http.httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(basicAuthenticationEntryPoint("api-customer-realm")));
		http.csrf(AbstractHttpConfigurer::disable);
		http.addFilterAfter(authenticationTokenFilter, BasicAuthenticationFilter.class);
		return http.build();
	}

	private DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		// MIGRATION NOTE: Spring Security 7 moves DaoAuthenticationProvider to constructor-based UserDetailsService injection; password encoding and authentication boundaries are unchanged.
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	private AuthenticationEntryPoint basicAuthenticationEntryPoint(String realmName) {
		BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
		entryPoint.setRealmName(realmName);
		try {
			entryPoint.afterPropertiesSet();
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to initialize authentication entry point for realm " + realmName,
					exception);
		}
		return entryPoint;
	}

}
