require "webrick"
require "json"

require_relative "onescanhmac"
require_relative "onescanconfig"

module Onescan

  class OnescanCallback < WEBrick::HTTPServlet::AbstractServlet

    def do_POST request, response

      # Read the contents of the post
      jsonContent = request.body

      #validate that the signature is correct
      onescanSignature = request["x-onescan-signature"]
      Onescan::HMAC.validateSignature(jsonContent,Onescan::SECRET,onescanSignature)

      # Parse the payload json into a hash
      onescanMessage = JSON.parse(request.body)

      # The responseMessage to be returned to Onescan
      responseMessage = nil;

      # to allow correlation with the original payment request,
      # this callback includes SessionData property that can be set
      # on the original payment request onescanMessage["SessionData"].
      if onescanMessage["MessageType"] == "StartPayment"
        responseMessage = startPayment(onescanMessage)

      elsif onescanMessage["MessageType"] == "AdditionalCharges"
        # Optionally include delivery options
        # Needs surcharges and deliveroptions set to true in start payment callback
        responseMessage = additionalCharges(onescanMessage)

      elsif onescanMessage["MessageType"] == "PaymentTaken"
        # For 1-step payment processes (see your payment gateway set up)
        responseMessage = purchaseDone(onescanMessage)

      elsif onescanMessage["MessageType"] == "PaymentConfirmed"
        # For 2-step payment processes (see your payment gateway set up)
        responseMessage = purchaseDone(onescanMessage)

      elsif onescanMessage["MessageType"] == "PaymentCaptured"
        # TODO Handle capture (if 2-step transaction used).
        responseMessage = createSuccessMessage(onescanMessage)

      elsif onescanMessage["MessageType"] == "PaymentFailed"
        # TODO: Handle a payment failure (check errors)
        responseMessage = createSuccessMessage(onescanMessage)

      elsif onescanMessage["MessageType"] == "PaymentCancelled"
        # TODO: Handle cancelled event
        responseMessage = createSuccessMessage(onescanMessage)

      # Web Content Process
      elsif onescanMessage["MessageType"] == "WebContent"
        responseMessage = createWebContentMessage(onescanMessage)

      elsif onescanMessage["MessageType"] == "StartLogin"
        # Called with LoginModes: TokenOrCredentials and UserToken
        responseMessage = createStartLoginMessage(onescanMessage)

      elsif onescanMessage["MessageType"] == "Login"
        # Called with LoginModes: TokenOrCredentials and UserToken (if first request did not complete)
        # and for "UsernamePassword", "Register"
        responseMessage = createLoginMessage(onescanMessage)

      else
        raise "Unexpected Message Type " + onescanMessage["MessageType"] + ". See documentation on how to respond to other message types."
      end

      jsonMessage = JSON.generate(responseMessage)

      # Create and sign the headers
      hmac = Onescan::HMAC.encode(jsonMessage,Onescan::SECRET)

      response["Content-Type"] = "application/json"
      response["x-onescan-account"] = Onescan::ACCOUNT_KEY
      response["x-onescan-signature"] = hmac
      response.status = 200

      response.body = jsonMessage

    end

    def startPayment onescanMessage
      responseMessage = {
        :PurchasePayload => addPurchasePayload(onescanMessage),

        # TODO: Optionally add purchase variants
        #:PaymentVariants => addPaymentVariants(onescanMessage),

        # TODO: Optionally add merchant field options
        #:MerchantFields => addMerchantFields(onescanMessage)
      }

      # TODO: Check out the API docs from the portal for other features

      return responseMessage
    end

    def addPurchasePayload onescanMessage
      purchasePayload = {
        :MerchantName => "eZone",
        # This is passed to the payment gateway service as your reference.
        # It could be OrderId for purchases for example.
        :MerchantTransactionId => "YOUR UNIQUE TRANSACTION ID",
        :Currency => "GBP",
        # turn on or off - requirement for delivery address
        :RequiresDeliveryAddress => true,
        # Displayed on orderdetails screen for single item purchases and
        # also used by payment variants on the orderaccepted screen too
        :ImageData => "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png",

        # TODO: include following when including AdditionalCharges callback
        # ensure RequiresDeliveryAddress is true
        #:Requires => {
        #  :Surcharges => true,
        #  :DeliveryOptions => true
        #},

        # Following group not required when using payment variants
        :PurchaseDescription => "Toaster",
        :ProductAmount => 20.00,
        :Tax => 2.00,
        :PaymentAmount => 22.00,

        # TODO: Used when Payment Variants are enabled
        #:CallToAction => "Buy me!"
      }
      return purchasePayload
    end

    def addPaymentVariants onescanMessage
      paymentVariants = {
        # VariantSelectionType can be SingleChoice | MultipleChoice
        :VariantSelectionType => "SingleChoice",
        :Variants => [
          {
            :Id => "Id1",
            :PurchaseDescription => "Item 1",
            :Tax => 2.0,
            :PaymentAmount => 12.0,
            :ProductAmount => 10.0,
            :ImageData => "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png",
            :SelectByDefault => true
          },
          {
            :Id => "Id2",
            :PurchaseDescription => "Item 2",
            :Tax => 4.0,
            :PaymentAmount => 24.0,
            :ProductAmount => 20.0,
            :ImageData => "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png",
            :SelectByDefault => false
          }
        ]
      }
      return paymentVariants
    end

    def addMerchantFields onescanMessage
      responseMessage = {
        :FieldGroups => [
          {
            :GroupHeading => "Group 1",
            :IsMandatory => true,
            :Code => "G1",
            # GroupType can be Informational | ExclusiveChoice
            :GroupType => "ExclusiveChoice",
            # Only applies when used with Payment Variants
            :AppliesToEachItem => true,
            :Fields => [
              {
                :Label => "Option 1",
                :Code => "F1",
                :PriceDelta => 10.0,
                :TaxDelta => 2.0,
                :SelectByDefault => true
              },
              {
                :Label => "Option 2",
                :Code => "F2",
                :PriceDelta => 20.0,
                :TaxDelta => 4.0,
                :SelectByDefault => false
              }
            ]
          }
        ]
      }
      return responseMessage
    end

    def additionalCharges onescanMessage
      responseMessage = {
        :AdditionalCharges => {

          # TODO: Include if the address supplied cant be delivered to
          #:AddressNotSupported => true,
          #:AddressNotSupportedReason => "Reason message",

          :DeliveryOptions => [
            {
              :Code => "FIRST",
              :Description => "Royal Mail First Class",
              :IsDefault => true,
              :Label => "First class",
              :Charge => {
                :BaseAmount => 2.99,
                :Tax => 0.0,
                :TotalAmount => 2.99
              }
            },
            {
              :Code => "SECOND",
              :Description => "Royal Mail Second Class",
              :IsDefault => false,
              :Label => "Second class",
              :Charge => {
                :BaseAmount => 1.99,
                :Tax => 0.0,
                :TotalAmount => 1.99
              }
            }
          ],
          # Payment surcharges
          :PaymentMethodCharge => {
            :Code => "PLAY",
            :Description => "This attracts a surcharge",
            :Charge => {
              :BaseAmount => 1.0,
              :Tax => 0.0,
              :TotalAmount => 1.0
            }
          }
        }
      }
      return responseMessage
    end

    def purchaseDone onescanMessage
      responseMessage = {
        :MessageType => "OrderAccepted",
        :Success => true,
        :OrderAccepted => {
          :OrderId => "YOUR_ORDER_NUMBER"
        }
      }
      return responseMessage
    end

    def createSuccessMessage onescanMessage
      responseMessage = {
        :Success => true
      }
      return responseMessage;
    end

    def createWebContentMessage onescanMessage
      responseMessage = {
        :WebContent => {
          :WebUrl => Onescan::WEBCONTENT_URL,
          :ProcessCompleteUrl => "http://close-window",
          :FieldMappings =>
          [{
              :DataItem => "FirstName",
              :HtmlId => "html_id_1"
            },
            {
              :DataItem => "LastName",
              :HtmlId => "html_id_2"
            },
            {
              :DataItem => "Email",
              :HtmlId => "html_id_3"
            }
          ]
        }
      }

      return responseMessage
    end

    def createLoginMessage onescanMessage
      # Called with LoginModes: TokenOrCredentials and UserToken
      return {
          :ProcessOutcome => {
            :RedirectURL => "loginresult.html"
          },
          # TODO: Other return MessageTypes for TokenOrCredentials include "RetryLogin" or "LoginProblem"
          # For UserToken then "RegisterUser" can be used to get the user to release their personal info
          :Success => true,
          :MessageType => "ProcessComplete"
        }
    end

    def createStartLoginMessage onescanMessage
        # Called with LoginModes: TokenOrCredentials and UserToken (if first request did not complete)
        # and for "UsernamePassword", "Register"
        return {
            :ProcessOutcome => {
              :RedirectURL => "loginresult.html"
            },
            # TODO: Other return MessageTypes for UsernamePassword, TokenOrCredentials are "RetryLogin" or "LoginProblem"
            # For UserToken and Register is "LoginProblem"
            :Success => true,
            :MessageType => "ProcessComplete"
          }
    end

  end

end
