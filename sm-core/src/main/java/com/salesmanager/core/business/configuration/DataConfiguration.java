package com.salesmanager.core.business.configuration;

import java.util.Properties;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;


@Configuration
@EnableCaching
public class DataConfiguration {

    private final String driverClassName;
    private final String url;
    private final String user;
    private final String password;
    private final String hbm2ddl;
    private final String dialect;
    private final String showSql;
    private final String schema;
    private final String testQuery;
    private final int minPoolSize;
    private final int maxPoolSize;

    public DataConfiguration(
        @Value("${db.driverClass}") String driverClassName,
        @Value("${db.jdbcUrl}") String url,
        @Value("${db.user}") String user,
        @Value("${db.password}") String password,
        @Value("${hibernate.hbm2ddl.auto}") String hbm2ddl,
        @Value("${hibernate.dialect}") String dialect,
        @Value("${db.show.sql}") String showSql,
        @Value("${db.schema}") String schema,
        @Value("${db.preferredTestQuery}") String testQuery,
        @Value("${db.minPoolSize}") int minPoolSize,
        @Value("${db.maxPoolSize}") int maxPoolSize) {
        this.driverClassName = driverClassName;
        this.url = url;
        this.user = user;
        this.password = password;
        this.hbm2ddl = hbm2ddl;
        this.dialect = dialect;
        this.showSql = showSql;
        this.schema = schema;
        this.testQuery = testQuery;
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
    }

    @Bean
    public HikariDataSource dataSource() {
    	HikariDataSource dataSource = DataSourceBuilder.create().type(HikariDataSource.class)
    	.driverClassName(driverClassName)
    	.url(url)
    	.username(user)
    	.password(password)
    	.build();
    	
    	/** Datasource config **/
    	dataSource.setMinimumIdle(minPoolSize);
    	dataSource.setMaximumPoolSize(maxPoolSize);
    	dataSource.setConnectionTestQuery(testQuery);
    	
    	return dataSource;
    }

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		vendorAdapter.setGenerateDdl(true);

		LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
		factory.setJpaVendorAdapter(vendorAdapter);
		factory.setPackagesToScan("com.salesmanager.core.model");
		factory.setJpaProperties(additionalProperties());
		factory.setDataSource(dataSource());
		return factory;
	}
	
    final Properties additionalProperties() {
        final Properties hibernateProperties = new Properties();
        
        hibernateProperties.setProperty("hibernate.hbm2ddl.auto", hbm2ddl);
        hibernateProperties.setProperty("hibernate.default_schema", schema);
        hibernateProperties.setProperty("hibernate.dialect", dialect);
        hibernateProperties.setProperty("hibernate.show_sql", showSql);
        // Legacy Hibernate Ehcache integration is not available on Hibernate 7.
        hibernateProperties.setProperty("hibernate.cache.use_second_level_cache", "false");
        hibernateProperties.setProperty("hibernate.cache.use_query_cache", "false");
        hibernateProperties.setProperty("hibernate.connection.CharSet", "utf8");
        hibernateProperties.setProperty("hibernate.connection.characterEncoding", "utf8");
        hibernateProperties.setProperty("hibernate.connection.useUnicode", "true");
        hibernateProperties.setProperty("hibernate.id.new_generator_mappings", "false"); //unless you run on a new schema
        hibernateProperties.setProperty("hibernate.generate_statistics", "false");
        // hibernateProperties.setProperty("hibernate.globally_quoted_identifiers", "true");
        return hibernateProperties;
    }

	@Bean
	public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {

		JpaTransactionManager txManager = new JpaTransactionManager();
		txManager.setEntityManagerFactory(entityManagerFactory);
		return txManager;
	}

	@Bean
	@Primary
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager();
	}

}
