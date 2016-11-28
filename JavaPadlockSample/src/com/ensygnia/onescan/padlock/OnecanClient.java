package com.ensygnia.onescan.padlock;

import org.json.JSONObject;

public class OnecanClient 
{
	OnescanClientSettings _settings;
	OnescanHTTPClient _httpClient;
	
	public OnecanClient(OnescanHTTPClient httpClient, OnescanClientSettings settings)
	{
		this._httpClient = httpClient;
		this._settings = settings;
	}
	
	
	/// Request a new padlock.  This request is sent to the partner's server which then uses the relevent Onescan SDK to 
	/// forward an authenticated request for a new padlock session.  The response to this server request is returned unmodified
	/// to the calling client - normally the javascript but in this case java client
	public OnescanPadlockState RequestPadlock()
	{
		String response = this._httpClient.Post(this._settings.serverURL);
		JSONObject jsonObject = new JSONObject(response);
		OnescanPadlockState padlock = new OnescanPadlockState().LoadFromJSONObject(jsonObject);
		return padlock;
	}
	
	/// Given a set of OnescanPadlockState make a request to the OnescanServer to determine it's current status
	public OnescanPadlockState PollStatus(OnescanPadlockState padlock)
	{
		String getRequest = this._settings.onescanURL + "?OnescanSessionID=" + padlock.SessionID; 
		String response = this._httpClient.Get(getRequest);
		JSONObject jsonObject = new JSONObject(response);
		return padlock.LoadStatusFromJSONObject(jsonObject);		
	}
	
	public static OnecanClient Create(OnescanClientSettings settings)
	{
		return new OnecanClient(new OnescanHTTPClient(settings), settings);
	}
	
	public static OnecanClient Create()
	{
		OnescanClientSettings settings = new OnescanClientSettings();
		return OnecanClient.Create(settings);
	}	

	public OnescanClientSettings getSettings()
	{
		return this._settings;
	}
}
