package com.ensygnia.onescan.padlock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import sun.security.ssl.SSLContextImpl;

/// A simple HTTP request wrapper
public class OnescanHTTPClient 
{
	public String serverBaseURL;
    public int RequestTimeout; 

    public OnescanHTTPClient(OnescanClientSettings settings)
    {
        this.RequestTimeout = settings.requestTimeoutMilliSeconds;
        this.serverBaseURL = settings.serverURL;
    }

    private String MakeRequest(String method, String urlAction, String data, boolean UseGZip, String contentType ) 
    {
    	// Default value for contentType
    	if (contentType == null || contentType == "")
    		contentType = "application/json";
    			
        URL uri = null;
        
		try {
			uri = new URL(urlAction);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        HttpURLConnection Http = null;
        
		try {
			Http = (HttpURLConnection)uri.openConnection();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        if (UseGZip)
        	Http.setRequestProperty("Accept-Encoding", "gzip");

        if (contentType != null)
        	Http.setRequestProperty("Content-Type", contentType);

        try {
			Http.setRequestMethod(method);
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        if (data != null)
        {
            //byte[] lbPostBuffer = System.Text.Encoding.UTF8.GetBytes(data);
        	byte[] lbPostBuffer = null;
        	
			try {
				lbPostBuffer = data.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        	 Http.setRequestProperty("Content-Length", String.valueOf(lbPostBuffer.length));
        	 Http.setDoOutput(true);
        	 

            if (method == "POST")
            {
                //Stream PostStream = Http.GetRequestStream();
            	OutputStream PostStream = null;
            	
				try {
					PostStream = Http.getOutputStream();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	
                try {
					PostStream.write(lbPostBuffer, 0, lbPostBuffer.length);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
                try {
					PostStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        }
            
       BufferedReader reader = null;
       
		try {
			reader = new BufferedReader(new InputStreamReader(Http.getInputStream()));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
       StringBuilder stringBuilder = new StringBuilder();
       
       String line = null;
       
       try {
		while ((line = reader.readLine()) != null)
		     stringBuilder.append(line + "\n");
		} catch (IOException e) 
       	{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

       
       return stringBuilder.toString();
    }
    
    private String MakeRequest(String method, String urlAction, String data, boolean UseGZip ) 
    {
    	return MakeRequest(method, urlAction, data, UseGZip, "" ); 
    }

    public String Post(String urlAction, String data, boolean UseGZip) 
    {
        return MakeRequest("POST", urlAction, data, UseGZip );
    }
    
    public String Post(String urlAction) 
    {
        return MakeRequest("POST", urlAction, "", false);
    }

    public String Post(String urlAction, String data) 
    {
        return MakeRequest("POST", urlAction, data, false);
    }
    
    public String Get(String urlAction, boolean UseGZip) 
    {
        return MakeRequest("GET", urlAction, null, UseGZip );
    }
    
    public String Get(String urlAction) 
    {
        return MakeRequest("GET", urlAction, null, false );
    }
}
