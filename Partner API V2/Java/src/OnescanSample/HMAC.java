package OnescanSample;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class HMAC {
	public static String HmacHeaderName = "x-onescan-signature";

	public static String Hash( String input, String keystring ) 
		throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException 
	{
		Mac mac = Mac.getInstance("HmacSHA1");

		//get the bytes of the hmac key and data string
		byte[] secretByte = keystring.getBytes("UTF-8");
	
		SecretKeySpec secret = new SecretKeySpec(secretByte, "HmacSHA1");

		mac.init(secret);
	
		byte[] dataBytes = input.getBytes("UTF-8");

		byte[] bytes = mac.doFinal(dataBytes);

		String form = "";
		for (int i = 0; i < bytes.length; i++) {
		    String str = Integer.toHexString(((int)bytes[i]) & 0xff);
		    if (str.length() == 1){
		        str = "0" + str;
		    }
	
		    form = form + str;
		}
		return form;		
	}

    public static void ValidateSignature(String input, String secret, String headerHmac) 
    		throws Exception {
        if (headerHmac == null || headerHmac.isEmpty())
            throw new Exception("HTTP Error: No signature found in the header");
        String calculatedHmac = HMAC.Hash(input, secret);
		if (!calculatedHmac.equals(headerHmac)) {
			throw new Exception("HTTP Error: Header signature is invalid");
		}
	}
}
