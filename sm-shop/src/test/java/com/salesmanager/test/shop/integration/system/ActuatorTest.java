package com.salesmanager.test.shop.integration.system;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import com.salesmanager.shop.application.ShopApplication;



@SpringBootTest(classes = ShopApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class ActuatorTest {
	  @Autowired
	  private ServletWebServerApplicationContext webServerApplicationContext;

	  private TestRestTemplate testRestTemplate;

	  @Before
	  public void setUp() {
		  this.testRestTemplate = new TestRestTemplate(
				  "http://localhost:" + webServerApplicationContext.getWebServer().getPort());
	  }
	
	
	  @Test
	  public void testPing() throws Exception {
	      final ResponseEntity<String> response = testRestTemplate.
	    		  exchange("/actuator/health", HttpMethod.GET, null, String.class);
	      if (!response.getStatusCode().is2xxSuccessful()) {
	          throw new Exception(response.toString());
	      }
	  }

}
