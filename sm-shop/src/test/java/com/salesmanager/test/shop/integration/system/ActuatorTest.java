package com.salesmanager.test.shop.integration.system;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import com.salesmanager.shop.application.ShopApplication;
import com.salesmanager.test.shop.common.ShopTestConfiguration;



@SpringBootTest(classes = ShopApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
// MIGRATION NOTE: Spring Boot 4 requires explicit AutoConfigureTestRestTemplate on tests that do not inherit the shared ServicesTestSupport base.
@AutoConfigureTestRestTemplate
@Import(ShopTestConfiguration.class)
@RunWith(SpringRunner.class)
// MIGRATION NOTE: Suppress SpringRunner deprecation warnings because this integration test intentionally stays on the JUnit 4 Spring runner during the Boot 4 migration.
@SuppressWarnings("deprecation")
public class ActuatorTest {
	
	  @Inject
	  // MIGRATION NOTE: Spring Boot 4 relocated TestRestTemplate to spring-boot-resttestclient without changing test HTTP behavior.
	  private TestRestTemplate testRestTemplate;
	
	
	  @Test
	  public void testPing() throws Exception {
		  HttpHeaders headers = new HttpHeaders();
		  headers.setContentType(MediaType.APPLICATION_JSON);
		  
		  HttpEntity<Object> entity = new HttpEntity<Object>(headers);

	      final ResponseEntity<Void> response = testRestTemplate.
	    		  exchange("/actuator/health/ping/", HttpMethod.GET, entity, Void.class);
	      if (response.getStatusCode() != HttpStatus.OK) {
	          throw new Exception(response.toString());
	      }
	  }

}
