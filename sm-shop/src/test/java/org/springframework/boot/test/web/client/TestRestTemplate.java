package org.springframework.boot.test.web.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Minimal Boot 4 test compatibility shim for legacy Shopizer tests that still use the removed
 * Boot 2/3 TestRestTemplate type.
 */
public class TestRestTemplate {

  private final RestTemplate restTemplate;
  private final String rootUri;

  public TestRestTemplate() {
    this(null);
  }

  public TestRestTemplate(String rootUri) {
    this.restTemplate = new RestTemplate();
    this.restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    this.restTemplate.setErrorHandler(
        new ResponseErrorHandler() {
          @Override
          public boolean hasError(ClientHttpResponse response) {
            return false;
          }
        });
    this.rootUri = normalizeRoot(rootUri);
  }

  public RestTemplate getRestTemplate() {
    return this.restTemplate;
  }

  public <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType) {
    return this.restTemplate.postForEntity(resolve(url), request, responseType);
  }

  public <T> ResponseEntity<T> exchange(
      String url, HttpMethod method, HttpEntity<?> requestEntity, Class<T> responseType) {
    return this.restTemplate.exchange(resolve(url), method, requestEntity, responseType);
  }

  private String resolve(String url) {
    if (url == null || this.rootUri == null || url.startsWith("http://") || url.startsWith("https://")) {
      return url;
    }
    if (url.startsWith("/")) {
      return this.rootUri + url;
    }
    return this.rootUri + "/" + url;
  }

  private String normalizeRoot(String root) {
    if (root == null || root.isBlank()) {
      return null;
    }
    if (root.endsWith("/")) {
      return root.substring(0, root.length() - 1);
    }
    return root;
  }
}
