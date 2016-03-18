package OnescanSample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;


@WebServlet("/RequestOnescanSession")
public class RequestOnescanSession extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private ServletContext appSettings;

	public void init() throws ServletException {
		appSettings = getServletConfig().getServletContext();
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		// Build the Request Onescan Session payload
		JSONObject onescanRequest = new JSONObject() {{
			put("ProcessType", "Payment");
			put("MessageType", "StartPayment");
			// TODO: For WebContent Processes change the process and messagetypes:
			//put("ProcessType", "WebContent");
			//put("MessageType", "WebContent");
            // TODO: For Login Processes:
	        //put("ProcessType", "Login");
	        //put("MessageType", "StartLogin");
	        //put("LoginPayload", createLoginRequest());

			// The Session Data property can be set by you to represent a hold data relating
			// to the users session in some way and will be passed back with each callback
			// so you can locate the session (eg Order number) that the request belongs to.
			put("SessionData", "CUSTOM SESSION DATA");
			put("Version", (int)2);
			put("MetaData", new JSONObject() {{
				put("EndpointURL", appSettings.getInitParameter("OnescanCallbackURL") );
			}});

			// TODO: You can set the PurchasePayload early if you wish (see StartPayment callback)
		}};

	    String onescanRequestMessage = onescanRequest.toString();

	    OutputStream postStream = null;
	    BufferedReader reader = null;
	    String jsonResponse = null;

	    try {

	    	URL uri = new URL(appSettings.getInitParameter("OnescanServerURL"));
	    	HttpURLConnection http = (HttpURLConnection)uri.openConnection();

	    	http.setRequestProperty("Content-Type", "application/json");
	    	String acckey = appSettings.getInitParameter("OnescanAccountKey");
	    	http.setRequestProperty("x-onescan-account", acckey);
	    	String hmac = HMAC.Hash(onescanRequestMessage, appSettings.getInitParameter("OnescanSecret"));
	    	http.setRequestProperty(HMAC.HmacHeaderName, hmac);

	    	http.setRequestMethod("POST");

	    	byte[] lbPostBuffer = onescanRequestMessage.getBytes("UTF-8");
    		http.setRequestProperty("Content-Length", String.valueOf(lbPostBuffer.length));

    		http.setDoOutput(true);

		    postStream = http.getOutputStream();
		    postStream.write(lbPostBuffer, 0, lbPostBuffer.length);

		    reader = new BufferedReader(new InputStreamReader(http.getInputStream()));

		    StringBuilder stringBuilder = new StringBuilder();
		    String line = null;
		    while ((line = reader.readLine()) != null){
				stringBuilder.append(line);
		    }
		    jsonResponse = stringBuilder.toString();

    		// Check that the signature of the header matches the payload
		    HMAC.ValidateSignature(jsonResponse,appSettings.getInitParameter("OnescanSecret"), http.getHeaderField(HMAC.HmacHeaderName));
	    }
	    catch (Exception ex) {
	    	throw new ServletException(ex);
	    }
	    finally {
			postStream.close();
		    reader.close();
	    }

    	PrintWriter out = response.getWriter();
    	out.print(jsonResponse);

	}

    private JSONObject createLoginRequest()
    {
        return new JSONObject() {{
        	put("FriendlyName", "Demo site");
        	put("SiteIdentifier", "YOUR UNIQUE SITE ID");
        	//TODO: Other LoginModes are: TokenOrCredentials, UserToken, TokenOrCredentials, Register
        	put("LoginMode", "UsernamePassword");

        	//TODO: Profiles are required for Register (and with UserToken when the response MessageType "RegisterUser" is used.
        	put("Profiles", new JSONArray() {{
        		put("basic");
        	}});
        }};
    }
}
