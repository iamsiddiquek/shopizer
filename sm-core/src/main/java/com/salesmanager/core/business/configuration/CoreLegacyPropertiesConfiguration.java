package com.salesmanager.core.business.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
public class CoreLegacyPropertiesConfiguration {

  @Bean
  public static PropertySourcesPlaceholderConfigurer corePropertySourcesPlaceholderConfigurer(
      Environment environment) {
    // MIGRATION NOTE: Replaced the profile-scoped XML PropertyPlaceholderConfigurer beans with a Java equivalent that preserves the existing property file precedence.
    PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
    configurer.setIgnoreUnresolvablePlaceholders(false);
    configurer.setLocations(resolvePropertyResources(environment));
    return configurer;
  }

  private static Resource[] resolvePropertyResources(Environment environment) {
    Set<String> profiles = new LinkedHashSet<>(Arrays.asList(environment.getActiveProfiles()));
    List<String> locations = new ArrayList<>();

    String databaseLocation = databaseLocation(profiles);
    if (databaseLocation != null) {
      locations.add(databaseLocation);
    }

    locations.add("classpath:email.properties");

    String coreLocation = coreLocation(profiles);
    if (coreLocation != null) {
      locations.add(coreLocation);
    }

    locations.add("classpath:authentication.properties");

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    List<Resource> resources = new ArrayList<>();
    for (String location : locations) {
      Resource resource = resolver.getResource(location);
      if (resource.exists()) {
        resources.add(resource);
      }
    }
    return resources.toArray(Resource[]::new);
  }

  private static String databaseLocation(Set<String> profiles) {
    if (profiles.contains("firebase")) {
      return null;
    }
    if (profiles.contains("local")) {
      return "classpath:profiles/local/database.properties";
    }
    if (profiles.contains("gcp")) {
      return "classpath:profiles/gcp/database.properties";
    }
    if (profiles.contains("cloud")) {
      return "classpath:profiles/cloud/database.properties";
    }
    if (profiles.contains("mysql")) {
      return "classpath:profiles/mysql/database.properties";
    }
    if (profiles.contains("dependency")) {
      return "classpath:profiles/dependency/database.properties";
    }
    if (profiles.contains("docker")) {
      return "classpath:profiles/docker/database.properties";
    }
    return "classpath:database.properties";
  }

  private static String coreLocation(Set<String> profiles) {
    if (profiles.contains("local")) {
      return "classpath:profiles/local/shopizer-core.properties";
    }
    if (profiles.contains("gcp")) {
      return "classpath:profiles/gcp/shopizer-core.properties";
    }
    if (profiles.contains("cloud")) {
      return "classpath:profiles/cloud/shopizer-core.properties";
    }
    if (profiles.contains("mysql")) {
      return "classpath:profiles/mysql/shopizer-core.properties";
    }
    if (profiles.contains("dependency")) {
      return "classpath:profiles/dependency/shopizer-core.properties";
    }
    if (profiles.contains("aws")) {
      return "classpath:profiles/aws/shopizer-core.properties";
    }
    return "classpath:shopizer-core.properties";
  }
}
