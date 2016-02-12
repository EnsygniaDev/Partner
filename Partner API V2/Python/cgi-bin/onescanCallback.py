import sys
import os
import json
import onescanConfig
from onescanHMAC import onescanHMAC
from onescanObject import onescanObject

def processMessage():
    # Read the contents of the post
    jsonContent = sys.stdin.read()

    # get signature from header (stored in environ from handler)
    onescanSignature = os.environ.get("X-ONESCAN-SIGNATURE")

    # validate that the signature is correct
    onescanHMAC.validateSignature(jsonContent.encode("utf-8"),onescanConfig.SECRET, onescanSignature)

    # Parse the payload json into an object
    purchaseMessage = onescanObject(jsonContent)

    # to allow correlation with the original payment request,
    # this callback includes SessionId property that was set
    # on the original payment request purchaseMessageSessionId;
    # SessionData can also be set with other information the merchant wants
    if purchaseMessage.MessageType == "StartPayment":
        startPayment(purchaseMessage)

    elif purchaseMessage.MessageType == "AdditionalCharges":
        # Optionally include delivery options
        # Needs surcharges and deliveroptions set to true in start payment callback
        additionalCharges(purchaseMessage)

    elif purchaseMessage.MessageType == "PaymentTaken":
        # For 1-step payment processes (see your payment gateway set up)
        paymentDone(purchaseMessage)

    elif purchaseMessage.MessageType == "PaymentConfirmed":
        # For 2-step payment processes (see your payment gateway set up)
        paymentDone(purchaseMessage)

    elif purchaseMessage.MessageType == "PaymentCaptured":
        # TODO Handle capture (if 2 phase transaction used).
        pass
    elif purchaseMessage.MessageType == "PaymentFailed":
        # TODO: Handle a payment failure (check errors)
        pass
    elif purchaseMessage.MessageType == "PaymentCancelled":
        # TODO: Handle cancelled event
        pass
    else:
        raise Exception("Unexpected Message Type " + purchaseMessage.MessageType + ". See documentation on how to respond to other message types.")

    jsonMessage = json.dumps(purchaseMessage, default=lambda o: o.__dict__)
    response = jsonMessage.encode("utf-8")
    # Create and sign the headers
    hmac = onescanHMAC.encode(response,onescanConfig.SECRET)


    import logging
    logging.basicConfig(filename="debug.log",level=logging.DEBUG)
    logging.info(jsonMessage)

    print("content-type: application/json")
    print("x-onescan-account: " + onescanConfig.ACCOUNT_KEY)
    print("x-onescan-signature: " + hmac)
    print()
    #Ensure payload is sent as a binary stream
    sys.stdout.buffer.write(response)
    sys.stdout.flush()

def startPayment(purchaseMessage):
    purchasePayload = onescanObject()
    purchasePayload.MerchantName = "eZone"
    purchasePayload.Currency = "GBP"
    # turn on or off - requirement for delivery address
    purchasePayload.RequiresDeliveryAddress = True
    # Displayed on orderdetails screen for single item purchases and
    # also used by payment variants on the orderaccepted screen too
    purchasePayload.ImageData = "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png"

    # TODO: include following when including AdditionalCharges callback
    # ensure RequiresDeliveryAddress is true
    #requires = onescanObject()
    #requires.Surcharges = True
    #requires.DeliveryOptions = True
    #purchasePayload.Requires = requires

    # Following group not required when using payment variants
    purchasePayload.PurchaseDescription = "Toaster"
    purchasePayload.ProductAmount = 20.00
    purchasePayload.Tax = 2.00
    purchasePayload.PaymentAmount = 22.00

    # TODO: Used when Payment Variants are enabled
    #purchasePayload.CallToAction = "Buy me!"

    purchaseMessage.PurchasePayload = purchasePayload

    # TODO: Optionally add purchase variants
    #addPaymentVariants(purchaseMessage)

    # TODO: Optionally add merchant field options
    #addMerchantFields(purchaseMessage)

    # TODO: Check out the API docs from the portal for other features

def addPaymentVariants(purchaseMessage):
    paymentVariants = onescanObject()
    # VariantSelectionType can be SingleChoice | MultipleChoice
    paymentVariants.VariantSelectionType = "SingleChoice"
    variant1 = onescanObject()
    variant1.Id = "Id1"
    variant1.PurchaseDescription = "Item 1"
    variant1.Tax = 2.0
    variant1.PaymentAmount = 12.0
    variant1.ProductAmount = 10.0
    variant1.SelectByDefault = True
    variant1.ImageData = "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png"
    variant2 = onescanObject()
    variant2.Id = "Id2"
    variant2.PurchaseDescription = "Item 2"
    variant2.Tax = 4.0
    variant2.PaymentAmount = 24.0
    variant2.ProductAmount = 20.0
    variant2.SelectByDefault = False
    variant2.ImageData = "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png"
    paymentVariants.Variants = [
        variant1,
        variant2
    ]
    purchaseMessage.PaymentVariants = paymentVariants


def addMerchantFields(purchaseMessage):
    merchantFields = onescanObject()
    fieldGroup1 = onescanObject()
    fieldGroup1.GroupHeading = "Group 1"
    fieldGroup1.IsMandatory = True
    fieldGroup1.Code = "G1"
    # GroupType can be Informational | ExclusiveChoice
    fieldGroup1.GroupType = "ExclusiveChoice"
    # Only applies when used with Payment Variants
    fieldGroup1.AppliesToEachItem = True
    field1 = onescanObject()
    field1.Label = "Option 1"
    field1.Code = "F1"
    field1.PriceDelta = 10.0
    field1.TaxDelta = 2.0
    field1.SelectByDefault = True
    field2 = onescanObject()
    field2.Label = "Option 2"
    field2.Code = "F2"
    field2.PriceDelta = 20.0
    field2.TaxDelta = 4.0
    field2.SelectByDefault = False
    fieldGroup1.Fields = [
        field1,
        field2
    ]
    merchantFields.FieldGroups = [
        fieldGroup1
    ]
    purchaseMessage.MerchantFields = merchantFields

def additionalCharges(purchaseMessage):
    additionalCharges = onescanObject()
    # TODO: Include if the address supplied cant be delivered to
    #additionalCharges.AddressNotSupported = True
    #additionalCharges.AddressNotSupportedReason = "Reason message"

    deliveryOption1 = onescanObject()
    deliveryOption1.Code = "FIRST"
    deliveryOption1.Description = "Royal Mail First Class"
    deliveryOption1.IsDefault = True
    deliveryOption1.Label = "First class"

    charge1 = onescanObject()
    charge1.BaseAmount = 2.99
    charge1.Tax = 0.0
    charge1.TotalAmount = 2.99
    deliveryOption1.Charge = charge1

    deliveryOption2 = onescanObject()
    deliveryOption2.Code = "SECOND"
    deliveryOption2.Description = "Royal Mail Second Class"
    deliveryOption2.IsDefault = False
    deliveryOption2.Label = "Second class"

    charge2 = onescanObject()
    charge2.BaseAmount = 1.99
    charge2.Tax = 0.0
    charge2.TotalAmount = 1.99
    deliveryOption2.Charge = charge2

    additionalCharges.DeliveryOptions = [
        deliveryOption1,
        deliveryOption2
    ]

    # Surcharges
    paymentMethodCharge = onescanObject()
    paymentMethodCharge.Code = "PLAY"
    paymentMethodCharge.Description = "This attracts a surcharge"

    charge3 = onescanObject()
    charge3.BaseAmount = 1.00
    charge3.Tax = 0.0
    charge3.TotalAmount = 1.00
    paymentMethodCharge.Charge = charge3

    additionalCharges.PaymentMethodCharge = paymentMethodCharge

    purchaseMessage.AdditionalCharges = additionalCharges

def paymentDone(purchaseMessage):
    orderAccepted = onescanObject()
    orderAccepted.OrderId = "YOUR_ORDER_NUMBER"
    orderAccepted.Name = "Onescan.OrderAccepted"
    purchaseMessage.OrderAccepted = orderAccepted
    purchaseMessage.MessageType = "OrderAccepted"
    purchaseMessage.Success = True


#Initial call to kick the process off
processMessage()
