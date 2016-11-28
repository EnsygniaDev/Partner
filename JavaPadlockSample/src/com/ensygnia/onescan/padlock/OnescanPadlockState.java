package com.ensygnia.onescan.padlock;


import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/*
 * OnescanPadlockState: responsible for holding the state associated with a session
 */
public class OnescanPadlockState {
	
	public static final int CONST_PADLOCK_PENDING = 0;
	public static final int CONST_PADLOCK_SCANNED = 1;
	public static final int CONST_PADLOCK_TRANSACTION_COMPLETE = 2;
	
	
	/// A protocol version
	public String Protocol;

	/// The raw data encoded into the QRCode
	public String QRData;
	
	/// The imagebytes of the QRcode - as a bitmap
	public byte[] ImageBytes;
	
	///  The server session ID
	public String SessionID;
	
	///  The current status
	///  Values:  	0: awaiting interaction
	//				1: code has been scanned by the mobile device - animate padlock closed
	//				2: the transaction is complete - animate tick
	public int Status;
	
	public OnescanPadlockState LoadFromJSONObject(JSONObject jsonObject)
	{
		JSONObject jsonPayloadItem = this.findPayloadItemData(jsonObject, "Onescan.qrImageData");
		this.deserialisePadlockData(jsonPayloadItem);
		return this;
	}
	
	public OnescanPadlockState LoadStatusFromJSONObject(JSONObject jsonObject)
	{
		if (jsonObject.has("Status")) {
			this.Status = jsonObject.getInt("Status");			
		}
		return this;
	}
		

	///  The main interaction uses a standard Onescan message structure that supports multiple payloads
	///  This funtion extracts a specific payload by name
	private JSONObject findPayloadItemData(JSONObject jsonObject,String payloadName)
	{
		JSONObject payloadItem = null;
		JSONArray payloads = jsonObject.getJSONArray("Payloads");
		if (payloads != null) {
			for (int idx = 0; idx < payloads.length(); idx++) {
				JSONObject payload = payloads.getJSONObject(idx);
				String localPayloadName = payload.getString("PayloadName");
				if (localPayloadName.equals(payloadName)) {
					payloadItem = payload;
					break;
				}
			}			
		}
		JSONObject jsonData = null;
		if (payloadItem != null)
		{
			//  we've found the payloadItem - now deserialise the actual json data
			String rawJsonData = payloadItem.getString("JsonPayload"); 
			jsonData = new JSONObject(rawJsonData);
		}
		return jsonData;
	}
	
	private void deserialisePadlockData(JSONObject jsonObject)
	{
		if (jsonObject.has("Protocol")) {
			this.Protocol = jsonObject.getString("Protocol");
		}
		if (jsonObject.has("QRData")) {
			this.QRData = jsonObject.getString("QRData");
		}
		if (jsonObject.has("ImageBytes")) {
			String imageBytesB64 = jsonObject.getString("ImageBytes");
			this.ImageBytes = Base64.decode(imageBytesB64);
		}
		if (jsonObject.has("Session")) {
			JSONObject jsonSession = jsonObject.getJSONObject("Session");
			this.SessionID = jsonSession.getString("SessionID");
			this.Status = jsonSession.getInt("Status");
		}
	}
}
