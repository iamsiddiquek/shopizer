package com.salesmanager.shop.store.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.salesmanager.shop.application.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class JwtTokenUtilConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(TestConfiguration.class);

  @Test
  void createsJwtTokenUtilWhenRequiredPropertiesArePresent() {
    contextRunner
        .withPropertyValues(
            "jwt.secret=test-secret-that-is-long-enough-for-tests",
            "jwt.expiration=604800",
            "jwt.grace-period-seconds=200")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(JwtProperties.class);
              assertThat(context).hasSingleBean(JWTTokenUtil.class);
            });
  }

  @Test
  void failsFastWhenJwtSecretIsMissing() {
    contextRunner
        .withPropertyValues("jwt.expiration=604800", "jwt.grace-period-seconds=200")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasRootCauseInstanceOf(BindValidationException.class)
                  .rootCause()
                  .hasMessageContaining("jwt")
                  .hasMessageContaining("secret");
            });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(JwtProperties.class)
  static class TestConfiguration {

    @Bean
    JWTTokenUtil jwtTokenUtil(JwtProperties jwtProperties) {
      return new JWTTokenUtil(jwtProperties);
    }
  }
}
