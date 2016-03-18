using System;
using System.Configuration;
using System.IO;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;


namespace OnescanSample
{
    public partial class OnescanCallback : System.Web.UI.Page
    {
        protected void Page_Load(object sender, EventArgs e)
        {

            if (Request.HttpMethod != "POST")
                return;

            JObject onescanMessage = null;

            Stream httpBodyStream = Request.InputStream;
      		if (httpBodyStream.Length > int.MaxValue)
            {
      			throw new ArgumentException("HTTP InputStream too large.");
      		}
      		int streamLength = Convert.ToInt32(httpBodyStream.Length);
      		httpBodyStream.Position = 0;
      		using (StreamReader sr = new StreamReader(httpBodyStream))
            {
      			string jsonContent = sr.ReadToEnd();
                    HMAC.ValidateSignature(jsonContent);
      			onescanMessage = JsonConvert.DeserializeObject<JObject>(jsonContent);
      		}

            // the responseMessage to be returned to Onescan
            JObject responseMessage = null;

            string messageType = (string)onescanMessage["MessageType"];
            // to allow correlation with the original payment request,
            // this callback includes SessionData property that can be set
            // on the original payment request onescanMessage["SessionData"];
            switch (messageType)
            {
                case "StartPayment":
                    responseMessage = startPayment(onescanMessage);
                    break;

                case "AdditionalCharges":
                    // Optionally include delivery options
                    // Needs surcharges and deliveroptions set to true in start payment callback
                    responseMessage = additionalCharges(onescanMessage);
                    break;

                case "PaymentTaken":
                    // For 1-step payment processes (see your payment gateway set up)
                case "PaymentConfirmed":
                    // For 2-step payment processes (see your payment gateway set up)
                    responseMessage = purchaseDone(onescanMessage);
                    break;

                case "PaymentCaptured":
                    // TODO Handle capture (if 2-step transaction used).
                    responseMessage = createSuccessMessage(onescanMessage);
                    break;
                case "PaymentFailed":
                    // TODO: Handle a payment failure (check errors)
                    responseMessage = createSuccessMessage(onescanMessage);
                    break;
                case "PaymentCancelled":
                    // TODO Handle cancelled event
                    responseMessage = createSuccessMessage(onescanMessage);
                    break;

                // Web Content Process
                case "WebContent":
                    responseMessage = createWebContentMessage(onescanMessage);
                    break;

                case "StartLogin":
                    // Called with LoginModes: TokenOrCredentials and UserToken
                    responseMessage = createStartLoginMessage(onescanMessage);
                    break;

                case "Login":
                    // Called with LoginModes: TokenOrCredentials and UserToken (if first request did not complete)
                    // and for "UsernamePassword", "Register"
                    responseMessage = createLoginMessage(onescanMessage);
                    break;

                default:
                    throw new Exception("Unexpected Message Type " + onescanMessage["MessageType"] +
                    ". See documentation on how to respond to other message types.");
            }

            var jsonResponse = JsonConvert.SerializeObject(responseMessage);
            // Create and sign the headers.
            string acckey = ConfigurationManager.AppSettings["OnescanAccountKey"];
            Response.Headers.Add("x-onescan-account", acckey);
            string hmac = HMAC.Hash(jsonResponse, ConfigurationManager.AppSettings["OnescanSecret"]);
            Response.Headers.Add("x-onescan-signature", hmac);
            Response.ContentType = "application/json";

            //  need to send the response back
            Stream httpResponseStream = Response.OutputStream;
            using (StreamWriter sw = new StreamWriter(httpResponseStream))
            {
                sw.Write(jsonResponse);
                Response.Flush();
            }
        }

        private JObject startPayment(JObject purchaseMessage)
        {
            JObject purchasePayload = new JObject();
            purchasePayload.Add("MerchantName", "eZone");
            purchasePayload.Add("Currency", "GBP");
            // turn on or off - requirement for delivery address
            purchasePayload.Add("RequiresDeliveryAddress", true);
            // Displayed on orderdetails screen for single item purchases and
            // also used by payment variants on the orderaccepted screen too
            purchasePayload.Add("ImageData", "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png");

            // TODO: include following when including AdditionalCharges callback
            // ensure RequiresDeliveryAddress is true
            //JObject requires = new JObject();
            //requires.Add("DeliveryOptions", true);
            //requires.Add("Surcharges", true);
            //purchasePayload.Add("Requires", requires);

            // Following group not required when using payment variants
            purchasePayload.Add("PurchaseDescription", "Toaster");
            purchasePayload.Add("ProductAmount", 20.00);
            purchasePayload.Add("Tax", 2.0);
            purchasePayload.Add("PaymentAmount", 22.00);

            // TODO: Used when Payment Variants are enabled
            purchasePayload.Add("CallToAction", "Buy me!");

            var responseMessage = new JObject();

            responseMessage.Add("PurchasePayload", purchasePayload);

            // TODO: Optionally add purchase variants
            //responseMessage.Add("PaymentVariants", addPaymentVariants(purchaseMessage));

            // TODO: Optionally add merchant field options
            //responseMessage.Add("MerchantFields", addMerchantFields(purchaseMessage));

            // TODO: Check out the API docs from the portal for other features

            return responseMessage;
        }

        private JObject addMerchantFields(JObject purchaseMessage)
        {
            JObject merchantFields = new JObject();
            JArray fieldGroups = new JArray();
            JObject fieldGroup1 = new JObject();
            fieldGroup1.Add("GroupHeading", "Group 1");
            fieldGroup1.Add("IsMandatory", true);
            fieldGroup1.Add("Code", "G1");
            // GroupType can be Informational | ExclusiveChoice
            fieldGroup1.Add("GroupType", "ExclusiveChoice");
            // Only applies when used with Payment Variants
            fieldGroup1.Add("AppliesToEachItem", true);
            JObject field1 = new JObject();
            field1.Add("Label", "Option 1");
            field1.Add("Code", "F1");
            field1.Add("PriceDelta", 10.0);
            field1.Add("TaxDelta", 2.0);
            field1.Add("SelectByDefault", true);
            JObject field2 = new JObject();
            field2.Add("Label", "Option 2");
            field2.Add("Code", "F2");
            field2.Add("PriceDelta", 20.0);
            field2.Add("TaxDelta", 4.0);
            field2.Add("SelectByDefault", true);
            JArray fields = new JArray();
            fields.Add(field1);
            fields.Add(field2);
            fieldGroup1.Add("Fields", fields);
            fieldGroups.Add(fieldGroup1);
            merchantFields.Add("FieldGroups", fieldGroups);
            return merchantFields;
        }

        private JObject addPaymentVariants(JObject purchaseMessage)
        {
            JObject paymentVariants = new JObject();
            // VariantSelectionType can be SingleChoice | MultipleChoice
            paymentVariants.Add("VariantSelectionType", "SingleChoice");
            JArray variants = new JArray();
            JObject v1 = new JObject();
            v1.Add("Id", "Id1");
            v1.Add("PurchaseDescription", "Item 1");
            v1.Add("Tax", 2.0);
            v1.Add("PaymentAmount", 12.0);
            v1.Add("ProductAmount", 10.0);
            v1.Add("ImageData", "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png");
            v1.Add("SelectByDefault", true);
            variants.Add(v1);
            JObject v2 = new JObject();
            v2.Add("Id", "Id2");
            v2.Add("PurchaseDescription", "Item 2");
            v2.Add("Tax", 4.0);
            v2.Add("PaymentAmount", 24.0);
            v2.Add("ProductAmount", 20.0);
            v2.Add("ImageData", "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png");
            v2.Add("SelectByDefault", false);
            variants.Add(v2);
            paymentVariants.Add("Variants", variants);
            return paymentVariants;
        }

        private JObject additionalCharges(JObject purchaseMessage)
        {
            JObject additionalCharges = new JObject();
            //additionalCharges.Add("AddressNotSupported", true);
            //additionalCharges.Add("AddressNotSupportedReason", "We are very sorry but we cannot deliver to this location.");
            JArray deliveryOptions = new JArray();
            JObject deliveryOption1 = new JObject();
            deliveryOption1.Add("Code", "FIRST");
            deliveryOption1.Add("Description", "Royal Mail First Class");
            deliveryOption1.Add("IsDefault", true);
            deliveryOption1.Add("Label", "First class");
            JObject charge1 = new JObject();
            charge1.Add("BaseAmount", 2.99);
            charge1.Add("Tax", 0.00);
            charge1.Add("TotalAmount", 2.99);
            deliveryOption1.Add("Charge", charge1);
            deliveryOptions.Add(deliveryOption1);
            JObject deliveryOption2 = new JObject();
            deliveryOption2.Add("Code", "SECOND");
            deliveryOption2.Add("Description", "Royal Mail Second Class");
            deliveryOption2.Add("IsDefault", false);
            deliveryOption2.Add("Label", "Second class");
            JObject charge2 = new JObject();
            charge2.Add("BaseAmount", 1.99);
            charge2.Add("Tax", 0.00);
            charge2.Add("TotalAmount", 1.99);
            deliveryOption2.Add("Charge", charge2);
            deliveryOptions.Add(deliveryOption2);
            additionalCharges.Add("DeliveryOptions", deliveryOptions);
            // Add Payment Method Charges
            JObject paymentMethodCharge = new JObject();
            paymentMethodCharge.Add("Code", "PLAY");
            paymentMethodCharge.Add("Description", "This attracts a surcharge");
            JObject charge3 = new JObject();
            charge3.Add("BaseAmount", 1.00);
            charge3.Add("Tax", 0.00);
            charge3.Add("TotalAmount", 1.00);
            paymentMethodCharge.Add("Charge", charge3);
            additionalCharges.Add("PaymentMethodCharge", paymentMethodCharge);

            JObject responseMessage = new JObject();
            responseMessage.Add("AdditionalCharges", additionalCharges);
            return responseMessage;
        }

        private JObject purchaseDone(JObject purchaseMessage)
        {
            JObject responseMessage = new JObject();

            JObject orderAccepted = new JObject();
            orderAccepted.Add("OrderId", "YOUR_ORDER_NUMBER");

            responseMessage.Add("OrderAccepted", orderAccepted);
            responseMessage.Add("MessageType","OrderAccepted");
            responseMessage.Add("Success", true);
            return responseMessage;
        }

        private JObject createSuccessMessage(JObject onescanMessage)
        {
            JObject responseMessage = new JObject();
            responseMessage.Add("Success", true);
            return responseMessage;
        }

        private JObject createWebContentMessage(JObject onescanMessage)
        {
            JObject webContent = new JObject();
            webContent.Add("WebUrl", ConfigurationManager.AppSettings["WebContentUrl"]);
            webContent.Add("ProcessCompleteUrl", "http://close-window");
            JArray fieldMappings = new JArray();
            JObject field1 = new JObject();
            field1.Add("DataItem", "FirstName");
            field1.Add("HtmlId", "html_id_1");
            fieldMappings.Add(field1);
            JObject field2 = new JObject();
            field2.Add("DataItem", "LastName");
            field2.Add("HtmlId", "html_id_2");
            fieldMappings.Add(field2);
            JObject field3 = new JObject();
            field3.Add("DataItem", "Email");
            field3.Add("HtmlId", "html_id_3");
            fieldMappings.Add(field3);
            webContent.Add("FieldMappings", fieldMappings);

            JObject responseMessage = new JObject();
            responseMessage.Add("WebContent", webContent);
            return responseMessage;
        }

        private JObject createLoginMessage(JObject onescanMessage)
        {
            // Called with LoginModes: TokenOrCredentials and UserToken
            JObject responseMessage = new JObject();

            JObject processOutcome = new JObject();
            processOutcome.Add("RedirectURL", "/LoginResult.aspx");
            responseMessage.Add("ProcessOutcome",processOutcome);

            // TODO: Other return MessageTypes for TokenOrCredentials include "RetryLogin" or "LoginProblem"
            // For UserToken then "RegisterUser" can be used to get the user to release their personal info
            responseMessage["Success"] = true;
            responseMessage["MessageType"] = "ProcessComplete";

            return responseMessage;
        }

        private JObject createStartLoginMessage(JObject onescanMessage)
        {
            // Called with LoginModes: TokenOrCredentials and UserToken (if first request did not complete)
            // and for "UsernamePassword", "Register"
            JObject responseMessage = new JObject();

            JObject processOutcome = new JObject();
            processOutcome.Add("RedirectURL", "/LoginResult.aspx");
            responseMessage.Add("ProcessOutcome", processOutcome);

            // TODO: Other return MessageTypes for UsernamePassword, TokenOrCredentials are "RetryLogin" or "LoginProblem"
            // For UserToken and Register is "LoginProblem"
            responseMessage["Success"] = true;
            responseMessage["MessageType"] = "ProcessComplete";

            return responseMessage;
        }
    }
}
