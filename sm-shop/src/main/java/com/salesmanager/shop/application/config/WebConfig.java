package com.salesmanager.shop.application.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.UrlHandlerFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig implements WebMvcConfigurer {
	
	
    @Autowired
    private MerchantStoreArgumentResolver merchantStoreArgumentResolver;
    
    @Autowired
    private LanguageArgumentResolver languageArgumentResolver;

	
	    @Override
	    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
	        argumentResolvers.add(merchantStoreArgumentResolver);
	        argumentResolvers.add(languageArgumentResolver);
	    }

	    @Bean
	    public FilterRegistrationBean<UrlHandlerFilter> trailingSlashCompatibilityFilter() {
	        // MIGRATION NOTE: Spring Framework 7 removes configurePathMatch(...).setUseTrailingSlashMatch(true), so this filter preserves the previous trailing-slash request handling by wrapping matching requests.
	        FilterRegistrationBean<UrlHandlerFilter> registrationBean = new FilterRegistrationBean<>();
	        registrationBean.setFilter(UrlHandlerFilter.trailingSlashHandler("/**").wrapRequest().build());
	        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
	        return registrationBean;
	    }
	    

	}
