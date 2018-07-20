package com.lfyt.mobile.android.webservice;

import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import retrofit2.converter.gson.GsonConverterFactory;

public class WebServiceConfiguration {
	

	public WebServiceConfiguration(){
		setupDefaultComponents();
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	// ALTER CONFIGURATION
	///////////////////////////////////////////////////////////////////////////
	
	protected int provideTotalConnections() {
		return DEFAULT_TOTAL_CONNECTIONS;
	}
	
	protected long provideConnectionDurationSeconds() {
		return DEFAULT_CONNECTION_DURATION_SECONDS;
	}
	
	protected int provideMaxRequestPerHost(){
		return MAX_REQUEST_PER_HOST;
	}
	
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// COMPONENTS
	///////////////////////////////////////////////////////////////////////////
	
	private ConnectionPool connectionPool;
	
	private Dispatcher dispatcher;
	
	private GsonConverterFactory gsonConverterFactory;
	
	
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// GETTERS && SETTERS
	///////////////////////////////////////////////////////////////////////////
	
	public ConnectionPool getConnectionPool() {
		return connectionPool;
	}
	
	public Dispatcher getDispatcher() {
		return dispatcher;
	}
	
	
	public GsonConverterFactory getGsonConverterFactory() {
		return gsonConverterFactory;
	}
	
	public void setConnectionPool(ConnectionPool connectionPool) {
		this.connectionPool = connectionPool;
	}
	
	public void setDispatcher(Dispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}
	
	public void setGsonConverterFactory(GsonConverterFactory gsonConverterFactory) {
		this.gsonConverterFactory = gsonConverterFactory;
	}
	
	///////////////////////////////////////////////////////////////////////////
	// DEFAULT
	///////////////////////////////////////////////////////////////////////////
	
	private static final int DEFAULT_TOTAL_CONNECTIONS = 10;
	private static final long DEFAULT_CONNECTION_DURATION_SECONDS = 120;
	private static final int MAX_REQUEST_PER_HOST = 10000;
	
	
	
	private void setupDefaultComponents() {
		connectionPool = provideDefaultConnectionPool();
		dispatcher = provideDefaultDispatcher();
		gsonConverterFactory = provideDefaultGsonConverterFactory();
	}
	
	
	
	private ConnectionPool provideDefaultConnectionPool(){
		return new ConnectionPool(provideTotalConnections(), provideConnectionDurationSeconds() , TimeUnit.SECONDS);
	}
	
	
	private Dispatcher provideDefaultDispatcher(){
		Dispatcher dispatcher = new Dispatcher();
		dispatcher.setMaxRequestsPerHost(provideMaxRequestPerHost());
		return dispatcher;
	}
	
	
	private GsonConverterFactory provideDefaultGsonConverterFactory(){
		return GsonConverterFactory.create(
				new GsonBuilder()
						.setDateFormat("dd-MM-yyyy")
						.create()
		);
	}
	
}
