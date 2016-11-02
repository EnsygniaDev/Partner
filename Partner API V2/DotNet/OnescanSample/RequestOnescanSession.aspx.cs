using System;
using System.Configuration;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System.Net;
using System.IO;
using System.Text;

namespace OnescanSample
{
    public partial class RequestOnescanSession : System.Web.UI.Page
    {
        protected void Page_Load(object sender, EventArgs e)
        {

            if (Request.HttpMethod != "POST")
                return;

            // Build the Request Onescan Session payload
            JObject onescanRequest = new JObject();

            onescanRequest.Add("ProcessType", "Payment");
            onescanRequest.Add("MessageType", "StartPayment");
            // TODO: For WebContent Processes change the process and messagetypes:
            //onescanRequest.Add("ProcessType", "WebContent");
            //onescanRequest.Add("MessageType", "WebContent");
            // TODO: For Login Processes:
            //createLoginRequest(onescanRequest);

            // The Session Data property can be set by you to represent a hold data relating
            // to the users session in some way and will be passed back with each callback
            // so you can locate the session (eg Order number) that the request belongs to.
            onescanRequest.Add("SessionData", "CUSTOM SESSION DATA");
            onescanRequest.Add("Version", 2);

            JObject metaData = new JObject();
            metaData.Add("EndpointURL", ConfigurationManager.AppSettings["OnescanCallbackURL"]);
            onescanRequest.Add("MetaData", metaData);

            // TODO: You can set the PurchasePayload early if you wish (see StartPayment callback)

            var onescanRequestMessage = JsonConvert.SerializeObject(onescanRequest);

            // TODO: Onescan no longer supports TLS1.0 and SSL 3. 
            // TLS10 can be the default for many .NET servers so you may need the following command to force a change of protocol
            // This code is just here to higlight the requirement and you should consider moving it to your start up code in global.asax for example
            ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12;

            HttpWebRequest http = (HttpWebRequest)WebRequest.Create(ConfigurationManager.AppSettings["OnescanServerURL"]);
            http.ContentType = "application/json";
            string acckey = ConfigurationManager.AppSettings["OnescanAccountKey"];
            http.Headers.Add("x-onescan-account", acckey);
            string hmac = HMAC.Hash(onescanRequestMessage, ConfigurationManager.AppSettings["OnescanSecret"]);
            http.Headers.Add("x-onescan-signature", hmac);
            http.Method = "POST";

            byte[] lbPostBuffer = System.Text.Encoding.UTF8.GetBytes(onescanRequestMessage);
            http.ContentLength = lbPostBuffer.Length;

            Stream postStream = http.GetRequestStream();
            postStream.Write(lbPostBuffer, 0, lbPostBuffer.Length);
            postStream.Close();

            string jsonResponse = "";
            HttpWebResponse webResponse = (HttpWebResponse)http.GetResponse();
            using (webResponse)
            {
                Stream responseStream = responseStream = webResponse.GetResponseStream();
                try
                {
                    StreamReader reader = new StreamReader(responseStream, Encoding.Default);
                    using (reader)
                    {
                        jsonResponse = reader.ReadToEnd();
                    }
                    // Check that the signature of the header matches the payload
                    HMAC.ValidateSignature(jsonResponse, webResponse.Headers);
                }
                finally
                {
                    webResponse.Close();
                    responseStream.Close();
                }
            }
            Response.Write(jsonResponse) ;
        }

        private void createLoginRequest(JObject onescanRequest)
        {
            onescanRequest.Add("ProcessType", "Login");
            onescanRequest.Add("MessageType", "StartLogin");

            JObject loginPayload = new JObject();
            loginPayload.Add("FriendlyName", "Demo site");
            loginPayload.Add("SiteIdentifier", "YOUR UNIQUE SITE ID");
            //TODO: Other LoginModes are: TokenOrCredentials, UserToken, TokenOrCredentials, Register
            loginPayload.Add("LoginMode", "UsernamePassword");

            //TODO: Profiles are required for Register (and with UserToken when the response MessageType "RegisterUser" is used.
            //JArray profiles = new JArray();
            //profiles.Add("basic");
            //loginPayload.Add("Profiles", profiles);

            onescanRequest.Add("LoginPayload", loginPayload);
        }
    }
}
