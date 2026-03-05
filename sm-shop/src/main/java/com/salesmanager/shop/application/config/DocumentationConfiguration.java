package com.salesmanager.shop.application.config;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class DocumentationConfiguration {

	private static final String HOST = "http://localhost:8080";

	@Bean
	public OpenAPI shopizerOpenApi() {
		// MIGRATION NOTE: Springfox was replaced with Springdoc for Spring Boot 3 compatibility while keeping the same documented controller surface and JWT header contract.
		return new OpenAPI()
				.info(new Info()
						.title("Shopizer REST API")
						.description(
								"API for Shopizer e-commerce. Contains public end points as well as private end points requiring basic authentication and remote authentication based on jwt bearer token. URL patterns containing /private/** use bearer token; those are authorized customer and administrators administration actions.")
						.version("1.0")
						.contact(new Contact().name("Shopizer").url("https://www.shopizer.com")))
				.addServersItem(new Server().url(HOST))
				.components(new Components().addSecuritySchemes("JWT",
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
								.in(SecurityScheme.In.HEADER).name(AUTHORIZATION)))
				.addSecurityItem(new SecurityRequirement().addList("JWT"));
	}

	@Bean
	public GroupedOpenApi shopizerApiV1() {
		// MIGRATION NOTE: Restricts the generated OpenAPI group to the same v1 package scope previously selected in Springfox.
		return GroupedOpenApi.builder().group("v1").packagesToScan("com.salesmanager.shop.store.api.v1").build();
	}

	@Bean
	public GroupedOpenApi shopizerApiV2() {
		// MIGRATION NOTE: Restricts the generated OpenAPI group to the same v2 package scope previously selected in Springfox.
		return GroupedOpenApi.builder().group("v2").packagesToScan("com.salesmanager.shop.store.api.v2").build();
	}

}
