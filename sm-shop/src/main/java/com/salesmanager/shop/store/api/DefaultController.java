package com.salesmanager.shop.store.api;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class DefaultController {
	
	private final Environment environment;

	public DefaultController(Environment environment) {
		this.environment = environment;
	}
	
	@GetMapping(value = "/")
	public @ResponseBody String version() {
		String version = environment.getProperty("application-version", "unknown");
		String buildTimestamp = environment.getProperty("build.timestamp", "unknown");
		return "{\"version\":\"" + version + "\", \"build\":\"" + buildTimestamp + "\"}";
	}

}
