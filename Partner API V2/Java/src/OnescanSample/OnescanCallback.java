package OnescanSample;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/OnescanCallback")

public class OnescanCallback extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private ServletContext appSettings;

	public void init() throws ServletException {
		appSettings = getServletConfig().getServletContext();
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		BufferedReader reader = request.getReader();

		StringBuffer sb = new StringBuffer();
		String line = null;
		while((line = reader.readLine()) != null )
			sb.append(line);

		String jsonContent = sb.toString();
		try {
			HMAC.ValidateSignature(jsonContent,appSettings.getInitParameter("OnescanSecret"), request.getHeader(HMAC.HmacHeaderName));
		} catch (Exception ex) {
			throw new ServletException(ex);
		}

		JSONObject onescanMessage = new JSONObject(jsonContent);

		// the responseMessage to be returned to Onescan
		JSONObject responseMessage = null;

		// to allow correlation with the original payment request,
		// this callback includes SessionData property that can be set
		// on the original payment request onescanMessage.getString("SessionData");
		String messageType = onescanMessage.getString("MessageType");
		switch (messageType) {
		case "StartPayment":
			responseMessage = startPayment(onescanMessage);
			break;

		case "AdditionalCharges":
			responseMessage = additionalCharges(onescanMessage);
			break;

		case "PaymentTaken":
			// For 1-step payment processes (see your payment gateway set up)
			responseMessage = purchaseDone(onescanMessage);
			break;

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
			// TODO: Handle cancelled event
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
			throw new ServletException("Unexpected Message Type " + messageType +
					". See documentation on how to respond to other message types.");
		}

		String jsonResponse = responseMessage.toString();
		// Create and sign the headers.
		response.setContentType("application/json");
		String acckey = appSettings.getInitParameter("OnescanAccountKey");
		response.setHeader("x-onescan-account", acckey);
		String hmac = null;
		try {
			hmac = HMAC.Hash(jsonResponse, appSettings.getInitParameter("OnescanSecret"));
		} catch (Exception ex) {
			throw new ServletException(ex);
		}
		response.setHeader("x-onescan-signature", hmac);

		response.getWriter().write(jsonResponse);
	}


	private JSONObject startPayment(JSONObject onescanMessage) {

		JSONObject responseMessage = new JSONObject();

		responseMessage.put("PurchasePayload",  addPurchasePayload(onescanMessage));

		// TODO: Optionally add purchase variants
		//responseMessage.put("PaymentVariants",addPaymentVariants(onescanMessage));

		// TODO: Optionally add merchant field options
		//responseMessage.put("MerchantFields",addMerchantFields(onescanMessage));

		// TODO: Check out the API docs from the portal for other features

		return responseMessage;
	}

	private JSONObject addPurchasePayload(JSONObject onescanMessage)
	{
		return new JSONObject() {{
			put("MerchantName", "eZone");
			// This is passed to the payment gateway service as your reference.
			  // It could be OrderId for purchases for example.
			put("MerchantTransactionId", "YOUR UNIQUE TRANSACTION ID");
			put("Currency", "GBP");
			// turn on or off - requirement for delivery address
			put("RequiresDeliveryAddress", true);
			// Displayed on orderdetails screen for single item purchases and
			// also used by payment variants on the orderaccepted screen too
			put("ImageData", "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png");

			// TODO: include following when including AdditionalCharges callback
			// ensure RequiresDeliveryAddress is true
			//put("Requires", new JSONObject() {{
			//	put("DeliveryOptions",true);
			//	put("Surcharges", true);
			//}});

			// Following group not required when using payment variants
			put("PurchaseDescription", "Toaster");
			put("ProductAmount", 20.00);
			put("Tax", 4.0);
			put("PaymentAmount", 24.00);

			// TODO: Used when Payment Variants are enabled
			put("CallToAction", "Buy me!");
		}};
	}

	private JSONObject addMerchantFields(JSONObject onescanMessage)
	{
		return new JSONObject() {{
			put("FieldGroups", new JSONArray() {{
				put(new JSONObject() {{
					put("GroupHeading", "Group 1");
					put("IsMandatory", true);
					put("Code", "G1");
					// GroupType can be Informational | ExclusiveChoice
					put("GroupType", "ExclusiveChoice");
					// Only applies when used with Payment Variants
					put("AppliesToEachItem", true);
					put("Fields", new JSONArray() {{
						put(new JSONObject() {{
							put("Label", "Option 1");
							put("Code", "F1");
							put("PriceDelta", 10.0);
							put("TaxDelta", 2.0);
							put("SelectByDefault", true);
						}});
						put(new JSONObject() {{
							put("Label", "Option 2");
							put("Code", "F2");
							put("PriceDelta", 20.0);
							put("TaxDelta", 4.0);
							put("SelectByDefault", true);
						}});
					}});
				}});
			}});
		}};

	}

	private JSONObject addPaymentVariants(JSONObject onescanMessage) {

		return new JSONObject() {{
			// VariantSelectionType can be SingleChoice | MultipleChoice
			put("VariantSelectionType", "SingleChoice");
			put("Variants", new JSONArray() {{
				put(new JSONObject() {{
					put("Id", "Id1");
					put("PurchaseDescription", "Item 1");
					put("Tax", 2.0);
					put("PaymentAmount", 12.0);
					put("ProductAmount", 10.0);
					put("ImageData", "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png");
					put("SelectByDefault", true);
				}});
				put(new JSONObject() {{
					put("Id", "Id2");
					put("PurchaseDescription", "Item 2");
					put("Tax", 4.0);
					put("PaymentAmount", 24.0);
					put("ProductAmount", 20.0);
					put("ImageData", "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png");
					put("SelectByDefault", false);
				}});
			}});
		}};
	}

	private JSONObject additionalCharges(JSONObject onescanMessage) {
		return new JSONObject() {{
			put("AdditionalCharges", new JSONObject() {{

				// TODO: Include if the address supplied cant be delivered to
				//put("AddressNotSupported", true);
				//put("AddressNotSupportedReason", "We are very sorry but we cannot deliver to this location.");

				put("DeliveryOptions", new JSONArray() {{
					put(new JSONObject() {{
						put("Code", "FIRST");
						put("Description","Royal Mail First Class");
						put("IsDefault",true);
						put("Label","First class");
						put("Charge", new JSONObject() {{
							put("BaseAmount",2.99);
							put("Tax",0.00);
							put("TotalAmount", 2.99);
						}});
					}});
					put(new JSONObject() {{
						put("Code", "SECOND");
						put("Description","Royal Mail Second Class");
						put("IsDefault",false);
						put("Label","Second class");
						put("Charge", new JSONObject() {{
							put("BaseAmount",1.99);
							put("Tax",0.00);
							put("TotalAmount", 1.99);
						}});
					}});
				}});
				put("PaymentMethodCharge", new JSONObject() {{
					put("Code", "PLAY");
					put("Description", "This attracts a surcharge");
					put("Charge", new JSONObject() {{
						put("BaseAmount",1.00);
						put("Tax",0.00);
						put("TotalAmount", 1.00);
					}});
				}});
			}});
		}};
	}

	private JSONObject purchaseDone(JSONObject onescanMessage)
	{
		return new JSONObject() {{
			put("OrderAccepted", new JSONObject() {{
				put("OrderId", "YOUR_ORDER_NUMBER");
			}});
			put("MessageType","OrderAccepted");
			put("Success",true);
		}};
	}

	private JSONObject createSuccessMessage(JSONObject onescanMessage)
	{
		return new JSONObject() {{
			put("Success",true);
		}};
	}

	private JSONObject createWebContentMessage(JSONObject onescanMessage)
	{
		return new JSONObject() {{
			put("WebContent", new JSONObject() {{
				put("WebUrl", appSettings.getInitParameter("WebContentUrl"));
				put("ProcessCompleteUrl", "http://close-window");
				put("FieldMappings", new JSONArray() {{
					put(new JSONObject() {{
						put("DataItem","FirstName");
						put("HtmlId","html_id_1");
					}});
					put(new JSONObject() {{
						put("DataItem","LastName");
						put("HtmlId","html_id_2");
					}});
					put(new JSONObject() {{
						put("DataItem","Email");
						put("HtmlId","html_id_3");
					}});
				}});
			}});
		}};
	}

    private JSONObject createLoginMessage(JSONObject onescanMessage)
    {
        // Called with LoginModes: TokenOrCredentials and UserToken
        JSONObject responseMessage = new JSONObject();

        JSONObject processOutcome = new JSONObject();
        processOutcome.put("RedirectURL", "loginresult.html");
        responseMessage.put("ProcessOutcome",processOutcome);

        // TODO: Other return MessageTypes for TokenOrCredentials include "RetryLogin" or "LoginProblem"
        // For UserToken then "RegisterUser" can be used to get the user to release their personal info
        responseMessage.put("Success", true);
        responseMessage.put("MessageType","ProcessComplete");

        return responseMessage;
    }

    private JSONObject createStartLoginMessage(JSONObject onescanMessage)
    {
        // Called with LoginModes: TokenOrCredentials and UserToken (if first request did not complete)
        // and for "UsernamePassword", "Register"
        JSONObject responseMessage = new JSONObject();

        JSONObject processOutcome = new JSONObject();
        processOutcome.put("RedirectURL", "loginresult.html");
        responseMessage.put("ProcessOutcome", processOutcome);

        // TODO: Other return MessageTypes for UsernamePassword, TokenOrCredentials are "RetryLogin" or "LoginProblem"
        // For UserToken and Register is "LoginProblem"
        responseMessage.put("Success", true);
        responseMessage.put("MessageType","ProcessComplete");

        return responseMessage;
    }

}
