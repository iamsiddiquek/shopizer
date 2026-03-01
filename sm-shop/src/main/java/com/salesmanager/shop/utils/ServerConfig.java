package com.salesmanager.shop.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.salesmanager.shop.application.config.ServerHostProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;


@Component
public class ServerConfig implements ApplicationListener<WebServerInitializedEvent> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerConfig.class);
	
	private final ServerHostProperties serverHostProperties;
	private volatile String applicationHost = null;

	public ServerConfig(ServerHostProperties serverHostProperties) {
		this.serverHostProperties = serverHostProperties;
	}
	
	@Override
	public void onApplicationEvent(final WebServerInitializedEvent event) {
	    int port = event.getWebServer().getPort();
	    final String host = getHost();
	    setApplicationHost(String.join(":", host, String.valueOf(port)));

	}
	
    private String getHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
        	LOGGER.warn("Falling back to configured host {}", serverHostProperties.getFallback(), e);
            return serverHostProperties.getFallback();
        }
    }

	public String getApplicationHost() {
		return applicationHost;
	}

	public void setApplicationHost(String applicationHost) {
		this.applicationHost = applicationHost;
	}

}
