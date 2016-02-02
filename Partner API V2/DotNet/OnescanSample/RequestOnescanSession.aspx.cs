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
            JObject purchaseRequest = new JObject();
            purchaseRequest.Add("ProcessType", "Payment");
            purchaseRequest.Add("MessageType", "StartPayment");
            // The Session properties are set by you to represent a hook to the users
            // session in some way and will be passed back with each callback
            // so you can locate the session (eg Order number) that the request belongs to.
            purchaseRequest.Add("SessionId", Guid.NewGuid().ToString());
            purchaseRequest.Add("SessionData", "CUSTOM SESSION DATA");
            purchaseRequest.Add("Version", (int)2);
            JObject metaData = new JObject();
            metaData.Add("EndpointURL", ConfigurationManager.AppSettings["OnescanCallbackURL"]);
            purchaseRequest.Add("MetaData", metaData);

            // TODO: You can set the PurchasePayload early if you wish (see StartPayment callback)

            var purchaseRequestMessage = JsonConvert.SerializeObject(purchaseRequest);

            HttpWebRequest http = (HttpWebRequest)WebRequest.Create(ConfigurationManager.AppSettings["OnescanServerURL"]);
            http.ContentType = "application/json";
            string acckey = ConfigurationManager.AppSettings["OnescanAccountKey"];
            http.Headers.Add("x-onescan-account", acckey);
            string hmac = HMAC.Hash(purchaseRequestMessage, ConfigurationManager.AppSettings["OnescanSecret"]);
            http.Headers.Add("x-onescan-signature", hmac);
            http.Method = "POST";

            byte[] lbPostBuffer = System.Text.Encoding.UTF8.GetBytes(purchaseRequestMessage);
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
    }
}
