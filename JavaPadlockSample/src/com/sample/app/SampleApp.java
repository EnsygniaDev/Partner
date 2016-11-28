package com.sample.app

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import com.ensygnia.onescan.padlock.OnecanClient;
import com.ensygnia.onescan.padlock.OnescanClientSettings;
import com.ensygnia.onescan.padlock.OnescanPadlockState;

public class SampleApp {
    public static void main(String[] args) {
        System.out.println("Requesting Padlock"); 

		try {
			SSLContext context = SSLContext.getInstance("TLSv1.2");
			context.init(null, null, null);
			SSLContext.setDefault(context);
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        OnescanClientSettings settings = new OnescanClientSettings();
        settings.serverURL = "http://play.onescan.me/demo/RequestOneScanPurchaseSession/ezone";
        settings.onescanURL = "https://liveservice.ensygnia.net/api/PartnerGateway/1/CheckOnescanSessionStatus";
        
        OnecanClient onescanClient = OnecanClient.Create(settings);
        OnescanPadlockState padlock = onescanClient.RequestPadlock();

        System.out.print("SessionID: "); 
        System.out.println(padlock.SessionID); 

        System.out.print("QRData: "); 
        System.out.println(padlock.QRData); 
        
        while (padlock.Status < OnescanPadlockState.CONST_PADLOCK_TRANSACTION_COMPLETE)
        {
        	onescanClient.PollStatus(padlock);
            System.out.print("Poll status: "); 
            String statusDescription = "";
            switch (padlock.Status)
            {
	            case 0:
	            	statusDescription = "Waiting for scan";
	            	break;
	            case 1:
	            	statusDescription = "Scanned: waiting for outcome";
	            	break;
	            case 2:
	            	statusDescription = "Transaction complete";
	            	break;
            }
            System.out.println(statusDescription); 
        	try {
				Thread.sleep(500);
			} 
        	catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
    }
}