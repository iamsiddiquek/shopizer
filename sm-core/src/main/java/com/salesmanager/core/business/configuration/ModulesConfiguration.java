package com.salesmanager.core.business.configuration;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import freemarker.template.TemplateException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.salesmanager.core.business.modules.integration.payment.impl.BeanStreamPayment;
import com.salesmanager.core.business.modules.integration.payment.impl.BraintreePayment;
import com.salesmanager.core.business.modules.integration.payment.impl.MoneyOrderPayment;
import com.salesmanager.core.business.modules.integration.payment.impl.PayPalExpressCheckoutPayment;
import com.salesmanager.core.business.modules.integration.payment.impl.Stripe3Payment;
import com.salesmanager.core.business.modules.integration.payment.impl.StripePayment;
import com.salesmanager.core.business.modules.integration.shipping.impl.CustomShippingQuoteRules;
import com.salesmanager.core.business.modules.integration.shipping.impl.CustomWeightBasedShippingQuote;
import com.salesmanager.core.business.modules.integration.shipping.impl.PriceByDistanceShippingQuoteRules;
import com.salesmanager.core.business.modules.integration.shipping.impl.StorePickupShippingQuote;
import com.salesmanager.core.business.modules.integration.shipping.impl.UPSShippingQuote;
import com.salesmanager.core.business.modules.integration.shipping.impl.USPSShippingQuote;
import com.salesmanager.core.business.modules.integration.shipping.impl.DefaultPackagingImpl;
import com.salesmanager.core.business.modules.order.InvoiceModule;
import com.salesmanager.core.business.modules.order.ODSInvoiceModule;
import com.salesmanager.core.business.modules.utils.EncryptionImpl;
import com.salesmanager.core.business.modules.utils.GeoLocationImpl;
import com.salesmanager.core.modules.integration.payment.model.PaymentModule;
import com.salesmanager.core.modules.integration.shipping.model.ShippingQuoteModule;

/**
 * Contains injection of external shopizer starter modules
 * @author carlsamson
 * New Way - out of xml config and using spring boot starters
 *
 */
@Configuration
public class ModulesConfiguration {

  @Value("${mailSender.protocol}")
  private String mailProtocol;

  @Value("${mailSender.host}")
  private String mailHost;

  @Value("${mailSender.port}")
  private int mailPort;

  @Value("${mailSender.username}")
  private String mailUsername;

  @Value("${mailSender.password}")
  private String mailPassword;

  @Value("${mailSender.mail.smtp.auth}")
  private String mailSmtpAuth;

  @Value("${mail.smtp.starttls.enable}")
  private String mailStartTls;

  @Bean(name = "usps")
  public ShippingQuoteModule usps() {
    // MIGRATION NOTE: Replaced the legacy Spring XML module bean with Java configuration while preserving the bean id consumed by shippingModules.
    return new USPSShippingQuote();
  }

  @Bean(name = "ups")
  public ShippingQuoteModule ups() {
    // MIGRATION NOTE: Replaced the legacy Spring XML module bean with Java configuration while preserving the bean id consumed by shippingModules.
    return new UPSShippingQuote();
  }

  @Bean(name = "weightBased")
  public ShippingQuoteModule weightBased() {
    // MIGRATION NOTE: Replaced the legacy Spring XML module bean with Java configuration while preserving the bean id consumed by shippingModules.
    return new CustomWeightBasedShippingQuote();
  }

  @Bean(name = "priceByDistance")
  public ShippingQuoteModule priceByDistance() {
    // MIGRATION NOTE: Replaced the legacy Spring XML module bean with Java configuration while preserving the bean id consumed by shippingModules.
    return new PriceByDistanceShippingQuoteRules();
  }

  @Bean(name = "storePickUp")
  public StorePickupShippingQuote storePickUp() {
    // MIGRATION NOTE: Replaced the legacy Spring XML module bean with Java configuration while preserving the bean id consumed by shippingModules.
    return new StorePickupShippingQuote();
  }

  @Bean(name = "customShippingPriceModule")
  public ShippingQuoteModule customShippingPriceModule() {
    // MIGRATION NOTE: Replaced the legacy Spring XML module bean with Java configuration while preserving the bean id consumed by shippingModules.
    return new CustomShippingQuoteRules();
  }

  @Bean(name = "shippingModules")
  public Map<String, ShippingQuoteModule> shippingModules(
      @Qualifier("usps") ShippingQuoteModule usps,
      @Qualifier("ups") ShippingQuoteModule ups,
      @Qualifier("weightBased") ShippingQuoteModule weightBased,
      @Qualifier("storePickUp") ShippingQuoteModule storePickUp,
      @Qualifier("priceByDistance") ShippingQuoteModule priceByDistance,
      @Qualifier("customShippingPriceModule") ShippingQuoteModule customShippingPriceModule,
      @Qualifier("canadapost") ObjectProvider<ShippingQuoteModule> canadapostProvider) {
    // MIGRATION NOTE: Replaced the legacy util:map with a Java Map bean while preserving module keys and optional starter-based canadapost wiring.
    Map<String, ShippingQuoteModule> shippingModules = new LinkedHashMap<>();
    canadapostProvider.ifAvailable(module -> shippingModules.put("canadapost", module));
    shippingModules.put("usps", usps);
    shippingModules.put("ups", ups);
    shippingModules.put("weightBased", weightBased);
    shippingModules.put("storePickUp", storePickUp);
    shippingModules.put("priceByDistance", priceByDistance);
    shippingModules.put("customQuotesRules", customShippingPriceModule);
    return shippingModules;
  }

  @Bean(name = "boxPackaging")
  public DefaultPackagingImpl boxPackaging() {
    // MIGRATION NOTE: Replaced the legacy Spring XML module bean with Java configuration while preserving the default packaging implementation.
    return new DefaultPackagingImpl();
  }

  @Bean(name = "beanstream")
  public PaymentModule beanstream() {
    // MIGRATION NOTE: Replaced the legacy Spring XML module bean with Java configuration while preserving the bean id consumed by paymentModules.
    return new BeanStreamPayment();
  }

  @Bean(name = "moneyorder")
  public PaymentModule moneyorder() {
    // MIGRATION NOTE: Replaced the legacy Spring XML module bean with Java configuration while preserving the bean id consumed by paymentModules.
    return new MoneyOrderPayment();
  }

  @Bean(name = "paypal-ec")
  public PaymentModule payPalExpressCheckout() {
    // MIGRATION NOTE: Replaced the legacy Spring XML module bean with Java configuration while preserving the bean id consumed by paymentModules.
    return new PayPalExpressCheckoutPayment();
  }

  @Bean(name = "stripe")
  public PaymentModule stripe() {
    // MIGRATION NOTE: Replaced the legacy Spring XML module bean with Java configuration while preserving the bean id consumed by paymentModules.
    return new StripePayment();
  }

  @Bean(name = "stripe3")
  public PaymentModule stripe3() {
    // MIGRATION NOTE: Replaced the legacy Spring XML module bean with Java configuration while preserving the bean id consumed by paymentModules.
    return new Stripe3Payment();
  }

  @Bean(name = "braintree")
  public PaymentModule braintree() {
    // MIGRATION NOTE: Replaced the legacy Spring XML module bean with Java configuration while preserving the bean id consumed by paymentModules.
    return new BraintreePayment();
  }

  @Bean(name = "paymentModules")
  public Map<String, PaymentModule> paymentModules(
      @Qualifier("beanstream") PaymentModule beanstream,
      @Qualifier("paypal-ec") PaymentModule payPalExpressCheckout,
      @Qualifier("moneyorder") PaymentModule moneyorder,
      @Qualifier("stripe") PaymentModule stripe,
      @Qualifier("stripe3") PaymentModule stripe3,
      @Qualifier("braintree") PaymentModule braintree) {
    // MIGRATION NOTE: Replaced the legacy util:map with a Java Map bean while preserving payment module keys and wiring.
    Map<String, PaymentModule> paymentModules = new LinkedHashMap<>();
    paymentModules.put("beanstream", beanstream);
    paymentModules.put("paypal-express-checkout", payPalExpressCheckout);
    paymentModules.put("moneyorder", moneyorder);
    paymentModules.put("stripe", stripe);
    paymentModules.put("stripe3", stripe3);
    paymentModules.put("braintree", braintree);
    return paymentModules;
  }

  @Bean(name = "encryption")
  public EncryptionImpl encryption(@Qualifier("secretKey") String secretKey) {
    // MIGRATION NOTE: Replaced the legacy Spring XML module bean with Java configuration while preserving setter-based secret key wiring.
    EncryptionImpl encryption = new EncryptionImpl();
    encryption.setSecretKey(secretKey);
    return encryption;
  }

  @Bean(name = "geoLocation")
  public GeoLocationImpl geoLocation() {
    // MIGRATION NOTE: Replaced the legacy Spring XML module bean with Java configuration while preserving the bean identity used by customer services.
    return new GeoLocationImpl();
  }

  @Bean(name = "invoiceModule")
  public InvoiceModule invoiceModule() {
    // MIGRATION NOTE: Replaced the legacy Spring XML module bean with Java configuration while preserving the invoice module type and bean name.
    return new ODSInvoiceModule();
  }

  @Bean(name = "freemarkerMailConfiguration")
  public freemarker.template.Configuration freemarkerMailConfiguration()
      throws IOException, TemplateException {
    // MIGRATION NOTE: Replaced the legacy FreeMarker factory XML with Java configuration while preserving the email template loader path.
    FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
    factoryBean.setTemplateLoaderPath("classpath:/templates/email");
    factoryBean.afterPropertiesSet();
    return factoryBean.getObject();
  }

  @Bean(name = "mailSender")
  public JavaMailSenderImpl mailSender() {
    // MIGRATION NOTE: Replaced the legacy JavaMail XML bean with Java configuration while preserving mail property names and defaults.
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setProtocol(mailProtocol);
    mailSender.setHost(mailHost);
    mailSender.setPort(mailPort);
    mailSender.setUsername(mailUsername);
    mailSender.setPassword(mailPassword);
    Properties properties = new Properties();
    properties.put("mail.smtp.auth", mailSmtpAuth);
    properties.put("mail.smtp.starttls.enable", mailStartTls);
    mailSender.setJavaMailProperties(properties);
    return mailSender;
  }
}
