require "webrick"
require "json"

require_relative "onescanhmac"
require_relative "onescanconfig"

module Onescan   #:nodoc:

  class OnescanCallback < WEBrick::HTTPServlet::AbstractServlet

    def do_POST request, response

      # Read the contents of the post
      jsonContent = request.body

      #validate that the signature is correct
      onescanSignature = request["x-onescan-signature"]
      Onescan::HMAC.validateSignature(jsonContent,Onescan::SECRET,onescanSignature)

      # Parse the payload json into a hash
      purchaseMessage = JSON.parse(request.body)

      # to allow correlation with the original payment request the
      # callback includes SessionId property that was set
      # on the original payment request purchaseMessage["SessionId"].
      # SessionData can also be set with other information the merchant wants
      if purchaseMessage["MessageType"] == "StartPayment"
        startPayment(purchaseMessage)

      elsif purchaseMessage["MessageType"] == "AdditionalCharges"
        # Optionally include delivery options
        # Needs surcharges and deliveroptions set to true in start payment callback
        additionalCharges(purchaseMessage)

      elsif purchaseMessage["MessageType"] == "PaymentTaken"
        # For 1-step payment processes (see your payment gateway set up)
        purchaseDone(purchaseMessage)

      elsif purchaseMessage["MessageType"] == "PaymentConfirmed"
        # For 2-step payment processes (see your payment gateway set up)
        purchaseDone(purchaseMessage)

      elsif purchaseMessage["MessageType"] == "PaymentCaptured"
        # TODO Handle capture (if 2-step transaction used).

      elsif purchaseMessage["MessageType"] == "PaymentFailed"
        # TODO: Handle a payment failure (check errors)

      elsif purchaseMessage["MessageType"] == "PaymentCancelled"
        # TODO: Handle cancelled event

      else
        raise "Unexpected Message Type " + purchaseMessage["MessageType"] + ". See documentation on how to respond to other message types."
      end

      jsonMessage = JSON.generate(purchaseMessage)

      # Create and sign the headers
      hmac = Onescan::HMAC.encode(jsonMessage,Onescan::SECRET)

      response["Content-Type"] = "application/json"
      response["x-onescan-account"] = Onescan::ACCOUNT_KEY
      response["x-onescan-signature"] = hmac
      response.status = 200

      response.body = jsonMessage

    end

    def startPayment purchaseMessage
      purchasePayload = {
        :MerchantName => "eZone",
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
      purchaseMessage[:PurchasePayload] = purchasePayload

      # TODO: Optionally add purchase variants
      #addPaymentVariants(purchaseMessage)

      # TODO: Optionally add merchant field options
      #addMerchantFields(purchaseMessage)

      # TODO: Check out the API docs from the portal for other features

    end

    def addPaymentVariants purchaseMessage
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
            :ImageData = "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png",
            :SelectByDefault => true
          },
          {
            :Id => "Id2",
            :PurchaseDescription => "Item 2",
            :Tax => 4.0,
            :PaymentAmount => 24.0,
            :ProductAmount => 20.0,
            :ImageData = "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png",
            :SelectByDefault => false
          }
        ]
      }
      purchaseMessage[:PaymentVariants] = paymentVariants
    end

    def addMerchantFields purchaseMessage
      merchantFields = {
        :FieldGroups => [
          {
            :GroupHeading => "Group 1",
            :IsMandatory => true,
            :Code => "G1",
            # GroupType can be Informational | ExclusiveChoice | MultipleChoice
            :GroupType => "ExclusiveChoice",
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
      purchaseMessage[:MerchantFields] = merchantFields
    end

    def additionalCharges purchaseMessage
      additionalCharges = {
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
      purchaseMessage[:AdditionalCharges] = additionalCharges

    end

    def purchaseDone purchaseMessage
      orderAccepted = {
        :OrderId => "YOUR_ORDER_NUMBER",
        :Name => "Onescan.OrderAccepted"
      }
      purchaseMessage[:OrderAccepted] = orderAccepted
      purchaseMessage[:MessageType] = "OrderAccepted"
      purchaseMessage[:Success] = true
    end

  end
end
