package com.salesmanager.test.shop.integration.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.salesmanager.shop.application.ShopApplication;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.RequestMapping;

@SpringBootTest(classes = ShopApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class ApiControllerRegistrationVerificationTest {

  private static final String API_BASE_PACKAGE = "com.salesmanager.shop.store.api";
  private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{([^}/:]+)(?::[^}]+)?}");

  private static final Set<String> ALLOWLIST_WITHOUT_ACTIVE_ENDPOINTS =
      Set.of("com.salesmanager.shop.store.api.v1.system.ModulesApi");

  @Autowired private ApplicationContext applicationContext;

  private List<Class<?>> apiControllerTypes;

  @BeforeAll
  void discoverApiControllers() {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

    List<Class<?>> discoveredControllers = new ArrayList<>();
    scanner.findCandidateComponents(API_BASE_PACKAGE).forEach(
        candidate -> discoveredControllers.add(resolveClass(candidate.getBeanClassName())));

    this.apiControllerTypes =
        discoveredControllers.stream()
            .filter(type -> type.getSimpleName().endsWith("Api"))
            .filter(type -> !Modifier.isAbstract(type.getModifiers()))
            .sorted(Comparator.comparing(Class::getName))
            .toList();

    assertTrue(
        this.apiControllerTypes.size() >= 40,
        "Unexpectedly low API controller count discovered: " + this.apiControllerTypes.size());
  }

  @TestFactory
  Stream<DynamicTest> eachApiControllerHasBeanAndValidMappings() {
    return this.apiControllerTypes.stream()
        .map(
            controllerType ->
                DynamicTest.dynamicTest(
                    controllerType.getName(),
                    () -> {
                      assertTrue(
                          applicationContext.getBeanNamesForType(controllerType).length > 0,
                          "Controller bean not registered: " + controllerType.getName());

                      List<Method> mappedMethods = requestMappedMethods(controllerType);

                      if (ALLOWLIST_WITHOUT_ACTIVE_ENDPOINTS.contains(controllerType.getName())) {
                        assertEquals(
                            0,
                            mappedMethods.size(),
                            "Placeholder controller now has mapped methods; remove allowlist entry: "
                                + controllerType.getName());
                        return;
                      }

                      assertTrue(
                          !mappedMethods.isEmpty(),
                          "No request-mapped methods found on controller " + controllerType.getName());

                      for (Method method : mappedMethods) {
                        validateNoDuplicatePathVariables(controllerType, method);
                      }
                    }));
  }

  private List<Method> requestMappedMethods(Class<?> controllerType) {
    return Arrays.stream(controllerType.getDeclaredMethods())
        .filter(method -> AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) != null)
        .toList();
  }

  private void validateNoDuplicatePathVariables(Class<?> controllerType, Method method) {
    RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
    if (requestMapping == null) {
      return;
    }

    String[] patterns = requestMapping.path().length > 0 ? requestMapping.path() : requestMapping.value();
    if (patterns.length == 0) {
      return;
    }

    for (String pattern : patterns) {
      Set<String> pathVariables = new HashSet<>();
      Matcher matcher = PATH_VARIABLE_PATTERN.matcher(pattern);
      while (matcher.find()) {
        String variableName = matcher.group(1);
        assertTrue(
            pathVariables.add(variableName),
            "Duplicate path variable '"
                + variableName
                + "' in mapping '"
                + pattern
                + "' for "
                + controllerType.getName()
                + "#"
                + method.getName());
      }
    }
  }

  private Class<?> resolveClass(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Unable to resolve controller class " + className, e);
    }
  }
}
