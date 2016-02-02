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

		JSONObject purchaseMessage = new JSONObject(jsonContent);

		// to allow correlation with the original payment request,
		// on the original payment request purchaseMessage.getString("SessionId");
		// this callback includes SessionId property that was set
		// SessionData can also be set with other information the merchant wants
		String messageType = purchaseMessage.getString("MessageType");
        switch (messageType) {
	    	case "StartPayment":
	    		startPayment(purchaseMessage);
				break;

			case "AdditionalCharges":
	    		additionalCharges(purchaseMessage);
				break;

			case "PaymentTaken":
				// For 1-step payment processes (see your payment gateway set up)
            case "PaymentConfirmed":
				// For 2-step payment processes (see your payment gateway set up)
            	purchaseDone(purchaseMessage);
            	break;

            case "PaymentCaptured":
				// TODO Handle capture (if 2-step transaction used).
                break;

            case "PaymentFailed":
                // TODO: Handle a payment failure (check errors)
                break;

            case "PaymentCancelled":
				// TODO: Handle cancelled event
                break;

            default:
                throw new ServletException("Unexpected Message Type " + messageType +
                ". See documentation on how to respond to other message types.");
        }

        String jsonResponse = purchaseMessage.toString();
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


	void startPayment(JSONObject purchaseMessage) {

		JSONObject purchasePayload = new JSONObject() {{
			put("MerchantName", "eZone");
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
			//put("CallToAction", "Buy me!");
		}};

		purchaseMessage.put("PurchasePayload", purchasePayload);

		// TODO: Optionally add purchase variants
		//addPaymentVariants(purchaseMessage);

		// TODO: Optionally add merchant field options
		//addMerchantFields(purchaseMessage);

		// TODO: Check out the API docs from the portal for other features
	}

	void addMerchantFields(JSONObject purchaseMessage)
	{
		purchaseMessage.put("MerchantFields", new JSONObject() {{
			put("FieldGroups", new JSONArray() {{
				put(new JSONObject() {{
					put("GroupHeading", "Group 1");
					put("IsMandatory", true);
					put("Code", "G1");
					// GroupType can be Informational | ExclusiveChoice | MultipleChoice
					put("GroupType", "ExclusiveChoice");
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
		}});
	}

	private void addPaymentVariants(JSONObject purchaseMessage) {

		purchaseMessage.put("PaymentVariants", new JSONObject() {{
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
		}});
	}

	private void additionalCharges(JSONObject purchaseMessage) {
		purchaseMessage.put("AdditionalCharges", new JSONObject() {{
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
	}

	private void purchaseDone(JSONObject purchaseMessage)
	{
		JSONObject orderAccepted = new JSONObject() {{
			put("OrderId", "YOUR_ORDER_NUMBER");
	    	put("Name","Onescan.OrderAccepted");
		}};
		purchaseMessage.put("OrderAccepted",orderAccepted);
		purchaseMessage.put("MessageType","OrderAccepted");
		purchaseMessage.put("Success",true);
	}

}
