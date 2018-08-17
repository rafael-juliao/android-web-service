package com.lfyt.mobile.android.webservice;

import com.lfyt.mobile.android.livemodel.Event;
import com.lfyt.mobile.android.livemodel.LiveModel;
import com.lfyt.mobile.android.log.Logger;

/**
 * Created by rafaeljuliao on 04/05/18.
 */
public abstract class WebServiceStateAPI extends LiveModel {

	/**
	 * Public constructor
	 */
	public WebServiceStateAPI(){
		Logger.DI(this);
	}


	private int executingRequests;

	public class WebServiceStartedRequests extends Event {}
	public class WebServiceFinishedRequests extends Event{}


	
	public final synchronized void onRequestExecuted() {

		if( executingRequests == 0)
		{
			post(new WebServiceStartedRequests());
		}

		executingRequests++;

	}
	
	public final synchronized void onRequestResponse() {

		executingRequests--;
		
		if( executingRequests == 0)
		{
			post(new WebServiceFinishedRequests());
		}
	}
	
}
