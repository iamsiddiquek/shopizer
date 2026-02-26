package com.salesmanager.shop.application.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import com.salesmanager.shop.admin.security.UserAuthenticationSuccessHandler;
import com.salesmanager.shop.admin.security.WebUserServices;
import com.salesmanager.shop.store.controller.customer.facade.CustomerFacade;
import com.salesmanager.shop.store.security.AuthenticationTokenFilter;
import com.salesmanager.shop.store.security.ServicesAuthenticationSuccessHandler;
import com.salesmanager.shop.store.security.admin.JWTAdminAuthenticationProvider;
import com.salesmanager.shop.store.security.admin.JWTAdminServicesImpl;
import com.salesmanager.shop.store.security.customer.JWTCustomerAuthenticationProvider;
import com.salesmanager.shop.store.security.services.CredentialsService;
import com.salesmanager.shop.store.security.services.CredentialsServiceImpl;

/**
 * Spring Security 7 / Boot 4 migration replacement for legacy WebSecurityConfigurerAdapter setup.
 * Preserves named AuthenticationManager beans used by existing controllers/facades.
 */
@Configuration
@EnableWebSecurity
public class MultipleEntryPointsSecurityConfig {

  @Bean
  public AuthenticationTokenFilter authenticationTokenFilter() {
    return new AuthenticationTokenFilter();
  }

  @Bean
  public CredentialsService credentialsService() {
    return new CredentialsServiceImpl();
  }

  @Bean("passwordEncoder")
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
  public CustomerFacade customerFacade() {
    return new com.salesmanager.shop.store.controller.customer.facade.CustomerFacadeImpl();
  }

  @Bean
  public AuthenticationProvider servicesAuthenticationProvider(
      WebUserServices userDetailsService,
      @Qualifier("passwordEncoder") PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
  }

  @Bean
  public AuthenticationProvider customerAuthenticationProvider(
      @Qualifier("customerDetailsService") UserDetailsService customerDetailsService,
      @Qualifier("passwordEncoder") PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customerDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
  }

  @Bean("customerAuthenticationManager")
  @Primary
  public AuthenticationManager customerAuthenticationManager(
      @Qualifier("customerAuthenticationProvider") AuthenticationProvider customerAuthenticationProvider) {
    return new ProviderManager(List.of(customerAuthenticationProvider));
  }

  @Bean("jwtAdminAuthenticationManager")
  public AuthenticationManager jwtAdminAuthenticationManager(
      AutowireCapableBeanFactory beanFactory,
      JWTAdminServicesImpl jwtAdminDetailsService) {
    JWTAdminAuthenticationProvider provider = beanFactory.createBean(JWTAdminAuthenticationProvider.class);
    provider.setJwtAdminDetailsService(jwtAdminDetailsService);
    return new ProviderManager(List.of(provider));
  }

  @Bean("jwtCustomerAuthenticationManager")
  public AuthenticationManager jwtCustomerAuthenticationManager(
      AutowireCapableBeanFactory beanFactory,
      @Qualifier("jwtCustomerDetailsService") UserDetailsService jwtCustomerDetailsService) {
    JWTCustomerAuthenticationProvider provider = beanFactory.createBean(JWTCustomerAuthenticationProvider.class);
    provider.setJwtCustomerDetailsService(jwtCustomerDetailsService);
    return new ProviderManager(List.of(provider));
  }

  @Bean
  public SecurityFilterChain applicationSecurityFilterChain(
      HttpSecurity http,
      AuthenticationTokenFilter authenticationTokenFilter,
      ServicesAuthenticationSuccessHandler servicesAuthenticationSuccessHandler,
      @Qualifier("servicesAuthenticationProvider") AuthenticationProvider servicesAuthenticationProvider,
      @Qualifier("customerAuthenticationProvider") AuthenticationProvider customerAuthenticationProvider) throws Exception {

    http.authenticationProvider(servicesAuthenticationProvider);
    http.authenticationProvider(customerAuthenticationProvider);

    http.csrf(AbstractHttpConfigurer::disable);

    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/", "/error", "/resources/**", "/static/**").permitAll()
        .requestMatchers("/shop/**").permitAll()
        .requestMatchers("/services/public/**").permitAll()
        .requestMatchers(HttpMethod.OPTIONS, "/api/**", "/services/**").permitAll()
        .requestMatchers("/api/v*/private/login*", "/api/v*/private/refresh").permitAll()
        .requestMatchers("/api/v*/auth/login", "/api/v*/auth/register", "/api/v*/auth/refresh").permitAll()
        .requestMatchers(
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs.yaml",
            "/v3/api-docs/swagger-config",
            "/v3/api-docs/**")
        .permitAll()
        .requestMatchers("/api/v*/private/**").hasRole("AUTH")
        .requestMatchers("/api/v*/auth/**").hasRole("AUTH_CUSTOMER")
        .requestMatchers("/services/private/**").hasRole("AUTH")
        .anyRequest().permitAll());

    http.httpBasic(Customizer.withDefaults());
    http.formLogin(form -> form.successHandler(servicesAuthenticationSuccessHandler));
    http.addFilterAfter(authenticationTokenFilter, BasicAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public AuthenticationEntryPoint shopAuthenticationEntryPoint() {
    return basicEntryPoint("shop-realm");
  }

  @Bean
  public AuthenticationEntryPoint servicesAuthenticationEntryPoint() {
    return basicEntryPoint("rest-customer-realm");
  }

  @Bean
  public AuthenticationEntryPoint apiAdminAuthenticationEntryPoint() {
    return basicEntryPoint("api-admin-realm");
  }

  @Bean
  public AuthenticationEntryPoint apiCustomerAuthenticationEntryPoint() {
    return basicEntryPoint("api-customer-realm");
  }

  private AuthenticationEntryPoint basicEntryPoint(String realm) {
    BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
    entryPoint.setRealmName(realm);
    return entryPoint;
  }
}
