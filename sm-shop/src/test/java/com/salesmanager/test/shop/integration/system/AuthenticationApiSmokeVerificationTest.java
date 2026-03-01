package com.salesmanager.test.shop.integration.system;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.salesmanager.shop.application.ShopApplication;

import jakarta.inject.Inject;

@SpringBootTest(classes = ShopApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthenticationApiSmokeVerificationTest {

  @Inject private ServletWebServerApplicationContext webServerApplicationContext;
  @Inject private RequestMappingHandlerMapping requestMappingHandlerMapping;

  private TestRestTemplate testRestTemplate;

  @BeforeEach
  void setUp() {
    this.testRestTemplate =
        new TestRestTemplate(
            "http://localhost:" + webServerApplicationContext.getWebServer().getPort());
  }

  @Test
  void invalidAdminLoginReturnsClientError() {
    HttpHeaders headers = jsonHeaders();
    HttpEntity<String> request =
        new HttpEntity<>(
            "{\"username\":\"nobody@example.com\",\"password\":\"bad-password\"}", headers);

    assertClientError(
        () -> testRestTemplate.postForEntity("/api/v1/private/login", request, String.class));
  }

  @Test
  void adminLoginEndpointIsMappedAtRuntime() {
    boolean mapped =
        requestMappingHandlerMapping.getHandlerMethods().entrySet().stream()
            .anyMatch(this::isAdminLoginMapping);
    assertTrue(mapped, "Expected runtime mapping for POST /api/v1/private/login");
  }

  @Test
  void loginEndpointRejectsGetMethod() {
    HttpEntity<String> request = new HttpEntity<>(jsonHeaders());

    assertClientError(
        () -> testRestTemplate.exchange("/api/v1/private/login", HttpMethod.GET, request, String.class));
  }

  @Test
  void malformedLoginPayloadReturnsClientError() {
    HttpHeaders headers = jsonHeaders();
    HttpEntity<String> request = new HttpEntity<>("{not-json}", headers);

    assertClientError(
        () -> testRestTemplate.postForEntity("/api/v1/private/login", request, String.class));
  }

  @Test
  void refreshEndpointWithoutTokenDoesNotReturnServerError() {
    try {
      ResponseEntity<String> response =
          testRestTemplate.exchange("/api/v1/auth/refresh", HttpMethod.GET, new HttpEntity<>(jsonHeaders()),
              String.class);
      assertTrue(
          response.getStatusCode().value() < 500,
          "Expected non-5xx for refresh without token but got " + response.getStatusCode().value());
    } catch (HttpStatusCodeException e) {
      assertTrue(
          e.getStatusCode().value() < 500,
          "Expected non-5xx for refresh without token but got " + e.getStatusCode().value());
    }
  }

  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON, MediaType.ALL));
    return headers;
  }

  private void assertClientError(HttpCall call) {
    try {
      ResponseEntity<String> response = call.invoke();
      int status = response.getStatusCode().value();
      assertTrue(status >= 400 && status < 500, "Expected 4xx but got " + status);
    } catch (HttpStatusCodeException e) {
      int status = e.getStatusCode().value();
      assertTrue(status >= 400 && status < 500, "Expected 4xx but got " + status);
    } catch (Exception e) {
      throw new AssertionError("Expected HTTP 4xx response but request failed unexpectedly", e);
    }
  }

  private boolean isAdminLoginMapping(
      java.util.Map.Entry<RequestMappingInfo, org.springframework.web.method.HandlerMethod> entry) {
    RequestMappingInfo info = entry.getKey();
    boolean hasPath =
        Stream.concat(
                info.getPathPatternsCondition() != null
                    ? info.getPathPatternsCondition().getPatterns().stream().map(Object::toString)
                    : Stream.empty(),
                info.getPatternsCondition() != null
                    ? info.getPatternsCondition().getPatterns().stream()
                    : Stream.empty())
            .anyMatch("/api/v1/private/login"::equals);
    if (!hasPath) {
      return false;
    }
    return info.getMethodsCondition().getMethods().isEmpty()
        || info.getMethodsCondition().getMethods().contains(RequestMethod.POST);
  }

  @FunctionalInterface
  private interface HttpCall {
    ResponseEntity<String> invoke();
  }
}
