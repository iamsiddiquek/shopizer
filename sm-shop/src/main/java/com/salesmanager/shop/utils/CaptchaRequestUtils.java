package com.salesmanager.shop.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesmanager.core.business.utils.CoreConfiguration;
import com.salesmanager.shop.constants.ApplicationConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Creates a request to reCaptcha 2
 * https://www.google.com/recaptcha/api/siteverify
 * Throws an exception if it can't connect to reCaptcha
 * returns true or false if validation has passed
 * @author carlsamson
 *
 */
@Component
public class CaptchaRequestUtils {
	
	private static final String SUCCESS_INDICATOR = "success";
	private static final TypeReference<Map<String, Object>> CAPTCHA_RESPONSE_TYPE = new TypeReference<>() {};

	private final CoreConfiguration configuration;
	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
	private final String secretKey;

	public CaptchaRequestUtils(
			CoreConfiguration configuration,
			@Value("${config.recaptcha.secretKey:}") String secretKey) {
		this.configuration = configuration;
		this.secretKey = secretKey;
	}
	
	public boolean checkCaptcha(String gRecaptchaResponse) throws Exception {
		if (StringUtils.isBlank(secretKey)) {
			throw new IllegalStateException("reCaptcha secret key is not configured");
		}

	    String url = configuration.getProperty(ApplicationConstants.RECAPTCHA_URL);
	    if (StringUtils.isBlank(url)) {
	    	throw new IllegalStateException("reCaptcha verification URL is not configured");
	    }

        List<NameValuePair> data = new ArrayList<NameValuePair>();
        data.add(new BasicNameValuePair("secret",  secretKey));
        data.add(new BasicNameValuePair("response",  gRecaptchaResponse));

	    
	    // Create a method instance.
        HttpPost post = new HttpPost(url);
	    post.setEntity(new UrlEncodedFormEntity(data,StandardCharsets.UTF_8));

	    try (CloseableHttpClient client = HttpClientBuilder.create().build();
	    		CloseableHttpResponse httpResponse = client.execute(post)) {
	      // Execute the method.
            int statusCode = httpResponse.getStatusLine().getStatusCode();

	      if (statusCode != HttpStatus.SC_OK) {
	    	throw new Exception("Got an invalid response from reCaptcha " + url + " [" + httpResponse.getStatusLine() + "]");
	      }

	      // Read the response body.
            HttpEntity entity = httpResponse.getEntity();
            String json = EntityUtils.toString(entity, StandardCharsets.UTF_8);
	      Map<String, Object> map = objectMapper.readValue(json, CAPTCHA_RESPONSE_TYPE);
	  	  Object successInd = map.get(SUCCESS_INDICATOR);
	  	  
	  	  if(successInd == null) {
	  		  throw new Exception("Unreadable response from reCaptcha " + json);
	  	  }
	  	  
	  	  return Boolean.parseBoolean(String.valueOf(successInd));

	    } finally {
	      // Release the connection.
	      post.releaseConnection();
	    }  
	  }


}
