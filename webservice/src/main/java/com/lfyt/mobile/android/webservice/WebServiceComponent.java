package com.lfyt.mobile.android.webservice;

public class WebServiceComponent {
	
	
	private final String baseUrl;
	private final WebServiceConfiguration webServiceConfiguration;
	
	public WebServiceComponent(String baseUrl, WebServiceConfiguration webServiceConfiguration) {
		this.baseUrl = baseUrl;
		this.webServiceConfiguration = webServiceConfiguration;
	}
	
	public String getBaseUrl() {
		return baseUrl;
	}

	public WebServiceConfiguration getWebServiceConfiguration() {
		return webServiceConfiguration;
	}
	
	
}
