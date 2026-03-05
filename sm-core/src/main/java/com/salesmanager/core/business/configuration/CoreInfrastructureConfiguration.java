package com.salesmanager.core.business.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.repositories.user.UserRepository;
import com.salesmanager.core.business.services.user.UserService;
import com.salesmanager.core.business.services.user.UserServiceImpl;
import com.salesmanager.core.business.utils.CoreConfiguration;

@Configuration
// MIGRATION NOTE: Legacy AOP transaction infrastructure is intentionally preserved in Java configuration during the Boot 4 migration, so compatibility-only deprecation warnings are suppressed here.
@SuppressWarnings("deprecation")
public class CoreInfrastructureConfiguration {

  @Bean(name = "persistenceExceptionTranslationPostProcessor")
  public static PersistenceExceptionTranslationPostProcessor persistenceExceptionTranslationPostProcessor() {
    // MIGRATION NOTE: Replaced the legacy Spring XML infrastructure bean with Java configuration while preserving exception translation behavior.
    return new PersistenceExceptionTranslationPostProcessor();
  }

  @Bean(name = "coreConfiguration")
  public CoreConfiguration coreConfiguration(@Qualifier("shopizer-properties") Properties properties) {
    // MIGRATION NOTE: Replaced the legacy Spring XML bean with Java configuration while preserving the CoreConfiguration property source.
    CoreConfiguration configuration = new CoreConfiguration();
    configuration.setProperties(properties);
    return configuration;
  }

  @Bean(name = "secretKey")
  public String secretKey(org.springframework.core.env.Environment environment) {
    // MIGRATION NOTE: Replaced the legacy Spring XML bean with Java configuration while preserving the secretKey placeholder lookup.
    return environment.getRequiredProperty("secretKey");
  }

  @Bean(name = "userService")
  public UserService userService(UserRepository userRepository) {
    // MIGRATION NOTE: Replaced the legacy Spring XML bean with Java configuration while preserving the explicit UserServiceImpl registration.
    return new UserServiceImpl(userRepository);
  }

  @Bean(name = "txAdvice")
  public TransactionInterceptor txAdvice(
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager) {
    // MIGRATION NOTE: Replaced the XML transaction advice with an equivalent Java AOP interceptor so generic service transaction boundaries remain unchanged.
    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    Map<String, TransactionAttribute> transactionAttributes = new HashMap<>();
    transactionAttributes.put("get*", readOnlyTransactionAttribute());
    transactionAttributes.put("list*", readOnlyTransactionAttribute());
    transactionAttributes.put("search*", readOnlyTransactionAttribute());
    transactionAttributes.put("*", writeTransactionAttribute());
    source.setNameMap(transactionAttributes);
    return new TransactionInterceptor(transactionManager, source);
  }

  @Bean(name = "txAdvisor")
  public Advisor txAdvisor(@Qualifier("txAdvice") TransactionInterceptor txAdvice) {
    // MIGRATION NOTE: Replaced the XML pointcut advisor with Java AOP while preserving the TransactionalAspectAwareService pointcut.
    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
    pointcut.setExpression(
        "this(com.salesmanager.core.business.services.common.generic.TransactionalAspectAwareService)");
    return new DefaultPointcutAdvisor(pointcut, txAdvice);
  }

  private TransactionAttribute readOnlyTransactionAttribute() {
    RuleBasedTransactionAttribute attribute = new RuleBasedTransactionAttribute();
    attribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    attribute.setReadOnly(true);
    return attribute;
  }

  private TransactionAttribute writeTransactionAttribute() {
    RuleBasedTransactionAttribute attribute = new RuleBasedTransactionAttribute();
    attribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    attribute.setRollbackRules(List.of(new RollbackRuleAttribute(ServiceException.class)));
    return attribute;
  }
}
