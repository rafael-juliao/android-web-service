package com.lfyt.mobile.android.webservice;

import android.support.annotation.CallSuper;

import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;
import com.lfyt.mobile.android.livemodel.Event;
import com.lfyt.mobile.android.livemodel.LiveModel;
import com.lfyt.mobile.android.log.Logger;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public abstract class WebServiceModel<T extends WebServiceResponse> extends LiveModel implements Callback<T>,HttpLogInterceptor.Logger {


	//################################################
	//################################################
	//####
	//####    Web Service Configuration
	//####
	//################################################
	//################################################
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// Constructor
	///////////////////////////////////////////////////////////////////////////
	WebServiceStateAPI mWebServiceStateAPI;

	public WebServiceModel(WebServiceStateAPI mWebServiceStateAPI){
		this.mWebServiceStateAPI = mWebServiceStateAPI;
		Logger.DI(this);
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	// Configuration Atributes
	///////////////////////////////////////////////////////////////////////////
	
	protected boolean handleResponse = true;
	protected HttpLogInterceptor.Level logLevel = HttpLogInterceptor.Level.BODY_ONLY;
	
	protected int connectionTimeout = 7000;
	protected int writeTimeout = 4000;
	protected int readTimeout = 4000;





	/**
	 * Must call this method after instanciate a new WebServiceAPI object to setup the service
	 * @param webServiceComponent
	 * @param service
	 */
	protected<V> V setupWebService(WebServiceComponent webServiceComponent, final Class<V> service){

		OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
				
				//Common Connection Objects
				.dispatcher(webServiceComponent.getWebServiceConfiguration().getDispatcher())

				//TODO: Check if can use current Connection Pool configuration
				//connectionPool(webServiceComponent.getWebServiceConfiguration().getConnectionPool())


				//Timeout
				.connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
				.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
				.readTimeout(readTimeout, TimeUnit.MILLISECONDS)
				
				//Never Retry On Connection Failure
				.retryOnConnectionFailure(false);
		
		
		
		//Logging
		HttpLogInterceptor interceptor = new HttpLogInterceptor(this);
		interceptor.setLevel(logLevel);
		okHttpBuilder.addInterceptor(interceptor);
		
		
		//Give dev opportunity to change OkHttp config
		setupOkHttp(okHttpBuilder);
		
		
		//Create Retrofit
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(webServiceComponent.getBaseUrl())
				.addConverterFactory(webServiceComponent.getWebServiceConfiguration().getGsonConverterFactory())
				.client(okHttpBuilder.build())
				.build();
		
		
		//Create Service Instance
		return retrofit.create(service);
	}
	
	
	protected void setupOkHttp(OkHttpClient.Builder okHttpBuilder){};
	
	
	
	@Override
	public void log(String message) {
		Logger.D(this, message);
	}
	
	
	
	
	
	
	
	
	
	//################################################
	//################################################
	//####
	//####    REQUEST EXECUTION
	//####
	//################################################
	//################################################
	
	
	//Execution Atributes
	public boolean waitingResponse = false;
	public long requestTime = 0;
	
	public boolean isWaitingResponse() {
		return waitingResponse;
	}
	
	
	//Execute async call to the server
	@CallSuper
	protected void executeRequest(Call<T> call) {

		if( handleResponse )
			mWebServiceStateAPI.onRequestExecuted();


		post(new WebServiceRequestExecutedEvent(this));

		
		waitingResponse = true;
		requestTime = new Date().getTime();
		
		call.enqueue(this);
	}





	
	
	
	
	//################################################
	//################################################
	//####
	//####    REQUEST RESPONSE
	//####
	//################################################
	//################################################
	
	
	///////////////////////////////////////////////////////////////////////////
	// Handle response from host or network
	///////////////////////////////////////////////////////////////////////////
	
	@Override
	@CallSuper
	public void onResponse(Call<T> call, Response<T> response) {
		waitingResponse = false;
		requestTime = new Date().getTime() - requestTime;
		
		if( ! handleResponse )
			return;


		int code = response.code();
		
		
		if(  code >= 200 && code <= 207 ) {
			onSuccess(code, response.body());
			post(new WebServiceResponseReceivedEvent(this));

			if( handleResponse ){
				mWebServiceStateAPI.onRequestResponse();
			}

			return;
		}

		
		onRequestError();
		
		
		if( code >= 300 && code <= 307 ){
			onRedirect(code, call, response);
		}
		
		else if( code >= 400 && code <= 424 ){
			onClientError(code, call, response);
		}
		
		else if( code >= 500 && code <= 507 ){
			onServerError(code, call, response);
		}
		else{
			onGenericRequestError();
		}

		post(new WebServiceErrorEvent(this));


		if( handleResponse ){
			mWebServiceStateAPI.onRequestResponse();
		}
	}
	
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// Handle failure to complete the request
	//
	///**
	// * Invoked when a network exception occurred
	// * talking to the network or when an unexpected
	// * exception occurred creating the request or processing the response.
	// */
	///////////////////////////////////////////////////////////////////////////
	
	@Override
	@CallSuper
	public void onFailure(Call<T> call, Throwable error) {
		waitingResponse = false;
		requestTime = new Date().getTime() - requestTime;
		
		if( !handleResponse )
			return;
		
		onRequestError();
		
		
		if( error instanceof ConnectException){
			onNoInternetConnection();
		}
		
		else if( error instanceof SocketTimeoutException){
			onTimeout();
		}
		
		else if( error instanceof UnknownHostException){
			onUnknownHostException(call, error);
		}
		
		else if( error instanceof MalformedJsonException){
			onJsonError(call, error);
		}
		
		else if( error instanceof JsonSyntaxException){
			onJsonError( call, error);
		}
		
		else{
			onRequestFailure(call, error);
		}


		post(new WebServiceErrorEvent(this));


		if( handleResponse ){
			mWebServiceStateAPI.onRequestResponse();
		}
	}
	
	
	
	
	
	
	
	//################################################
	//################################################
	//####
	//####    REQUEST RESPONSE INTERFACE
	//####
	//################################################
	//################################################
	
	
	
	
	
	/**
	 * Method is called on success ( code == 2xx ) of WebServiceAPI call
	 */
	protected void onSuccess(int code, T body){
		post(body);
	}
	
	
	/**
	 * This error is thrown for any case that is successfull( code != 2xx )
	 */
	protected void onRequestError() {
		post( new RequestError(this));
	}
	
	
	
	
	
	
	
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// Main errors
	///////////////////////////////////////////////////////////////////////////
	
	
	
	protected void onNoInternetConnection() {
		post( new NoInternetEvent(this) );
	}
	
	
	
	protected void onTimeout() {
		post( new TimeoutEvent(this) );
	}
	
	
	
	protected void onGenericRequestError() {
		post( new GenericRequestError(this) );
	}
	
	
	
	
	
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// Other errors that could be catch
	///////////////////////////////////////////////////////////////////////////
	
	protected void onRequestFailure(Call<T> call, Throwable error){
		onGenericRequestError();
	}
	
	
	protected void onUnknownHostException(Call<T> call, Throwable error){
		onGenericRequestError();
	}
	
	
	protected void onJsonError(Call<T> call, Throwable error) {
		onGenericRequestError();
	}
	
	
	protected void onServerError(int code, Call<T> call, Response<T> response) {
		onGenericRequestError();
	}
	
	
	protected void onClientError(int code, Call<T> call, Response<T> response) {
		onGenericRequestError();
	}
	
	
	protected void onRedirect(int code, Call<T> call, Response<T> response) {
		onGenericRequestError();
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	// REQUEST RESPONSE EVENTS
	///////////////////////////////////////////////////////////////////////////

	public class WebServiceRequestExecutedEvent extends Event {

		public WebServiceRequestExecutedEvent(Object caller) {
			this.caller = caller;
		}

		private Object caller;

		public Object getCaller() {
			return caller;
		}

	}
	public class WebServiceResponseReceivedEvent extends Event{
		
		public WebServiceResponseReceivedEvent(Object caller) {
			this.caller = caller;
		}
		
		private Object caller;
		
		public Object getCaller() {
			return caller;
		}
		
	}
	public class WebServiceErrorEvent extends Event{
		
		public WebServiceErrorEvent(Object caller) {
			this.caller = caller;
		}
		
		private Object caller;
		
		public Object getCaller() {
			return caller;
		}
		
	}


	/**
	 * This method is called when any request error happen
	 * != then GenericRequestError that is called when the error is unidentified
	 */
	public class RequestError extends Event {
		
		public RequestError(Object caller) {
			this.caller = caller;
		}
		
		private Object caller;
		
		public Object getCaller() {
			return caller;
		}
		
	}
	
	
	public class NoInternetEvent extends Event{
		public NoInternetEvent(Object caller) {
			this.caller = caller;
		}
		
		private Object caller;
		
		public Object getCaller() {
			return caller;
		}
	}
	public class TimeoutEvent extends Event{
		public TimeoutEvent(Object caller) {
			this.caller = caller;
		}
		
		private Object caller;
		
		public Object getCaller() {
			return caller;
		}
	}

	/**
	 * Thrown when the error is unidentified
	 */
	public class GenericRequestError extends Event {
		
		public GenericRequestError(Object caller) {
			this.caller = caller;
		}
		
		private Object caller;
		
		public Object getCaller() {
			return caller;
		}
		
	}




	
	
}
