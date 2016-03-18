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
    onescanMessage = onescanObject(jsonContent)

    # The responseMessage to be returned to Onescan
    responseMessage = None

    # to allow correlation with the original payment request,
    # this callback includes SessionData property that can be set
    # on the original payment request onescanMessage.SessionData.
    if onescanMessage.MessageType == "StartPayment":
        responseMessage = startPayment(onescanMessage)

    elif onescanMessage.MessageType == "AdditionalCharges":
        # Optionally include delivery options
        # Needs surcharges and deliveroptions set to true in start payment callback
        responseMessage = additionalCharges(onescanMessage)

    elif onescanMessage.MessageType == "PaymentTaken":
        # For 1-step payment processes (see your payment gateway set up)
        responseMessage = paymentDone(onescanMessage)

    elif onescanMessage.MessageType == "PaymentConfirmed":
        # For 2-step payment processes (see your payment gateway set up)
        responseMessage = paymentDone(onescanMessage)

    elif onescanMessage.MessageType == "PaymentCaptured":
        # TODO Handle capture (if 2 phase transaction used).
        responseMessage = createSuccessMessage(onescanMessage)

    elif onescanMessage.MessageType == "PaymentFailed":
        # TODO: Handle a payment failure (check errors)
        responseMessage = createSuccessMessage(onescanMessage)

    elif onescanMessage.MessageType == "PaymentCancelled":
        # TODO: Handle cancelled event
        responseMessage = createSuccessMessage(onescanMessage)

    # Web Content Process
    elif onescanMessage.MessageType == "WebContent":
        responseMessage = createWebContentMessage(onescanMessage)

    elif onescanMessage.MessageType == "StartLogin":
        # Called with LoginModes: TokenOrCredentials and UserToken
        responseMessage = createStartLoginMessage(onescanMessage)

    elif onescanMessage.MessageType == "Login":
        # Called with LoginModes: TokenOrCredentials and UserToken (if first request did not complete)
        # and for "UsernamePassword", "Register"
        responseMessage = createLoginMessage(onescanMessage)

    else:
        raise Exception("Unexpected Message Type " + onescanMessage.MessageType + ". See documentation on how to respond to other message types.")

    jsonMessage = json.dumps(responseMessage, default=lambda o: o.__dict__)
    response = jsonMessage.encode("utf-8")
    # Create and sign the headers
    hmac = onescanHMAC.encode(response,onescanConfig.SECRET)

    print("content-type: application/json")
    print("x-onescan-account: " + onescanConfig.ACCOUNT_KEY)
    print("x-onescan-signature: " + hmac)
    print()
    #Ensure payload is sent as a binary stream
    sys.stdout.buffer.write(response)
    sys.stdout.flush()

def startPayment(onescanMessage):
    responseMessage = onescanObject()
    responseMessage.purchasePayload = addPurchasePayload(onescanMessage)

    # TODO: Optionally add purchase variants
    #responseMessage.PaymentVariants = addPaymentVariants(onescanMessage)

    # TODO: Optionally add merchant field options
    #responseMessage.MerchantFields = addMerchantFields(onescanMessage)

    # TODO: Check out the API docs from the portal for other features

    return responseMessage

def addPurchasePayload(onescanMessage):
    purchasePayload = onescanObject()
    purchasePayload.MerchantName = "eZone"
    # This is passed to the payment gateway service as your reference.
    # It could be OrderId for purchases for example.
    purchasePayload.MerchantTransactionId = "YOUR UNIQUE TRANSACTION ID"
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

    return purchasePayload

def addPaymentVariants(onescanMessage):
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
    return paymentVariants

def addMerchantFields(onescanMessage):
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
    return merchantFields

def additionalCharges(onescanMessage):
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

    responseMessage = onescanObject()
    responseMessage.AdditionalCharges = additionalCharges

    return responseMessage

def paymentDone(onescanMessage):
    responseMessage = onescanObject()

    orderAccepted = onescanObject()
    orderAccepted.OrderId = "YOUR_ORDER_NUMBER"
    responseMessage.OrderAccepted = orderAccepted

    responseMessage.MessageType = "OrderAccepted"
    responseMessage.Success = True

    return responseMessage

def createSuccessMessage(onescanMessage):
    responseMessage = onescanObject()
    responseMessage.Success = True
    return responseMessage

def createWebContentMessage(onescanMessage):
    field1 = onescanObject()
    field1.DataItem = "FirstName"
    field1.HtmlId = "html_id_1"
    field2 = onescanObject()
    field2.DataItem = "LastName"
    field2.HtmlId = "html_id_2"
    field3 = onescanObject()
    field3.DataItem = "Email"
    field3.HtmlId = "html_id_3"

    webContent = onescanObject()
    webContent.WebUrl = onescanConfig.WEBCONTENT_URL
    webContent.ProcessCompleteUrl = "http://close-window"
    webContent.fieldMappings = [
        field1,
        field2,
        field3
    ]

    responseMessage = onescanObject()
    responseMessage.WebContent = webContent

    return responseMessage

def createLoginMessage(onescanMessage):
    # Called with LoginModes: TokenOrCredentials and UserToken
    responseMessage = onescanObject();

    processOutcome = onescanObject();
    processOutcome.RedirectURL = "/loginresult.html"
    responseMessage.ProcessOutcome = processOutcome

    # TODO: Other return MessageTypes for TokenOrCredentials include "RetryLogin" or "LoginProblem"
    # For UserToken then "RegisterUser" can be used to get the user to release their personal info
    responseMessage.Success = True
    responseMessage.MessageType = "ProcessComplete"

    return responseMessage

def createStartLoginMessage(onescanMessage):
    # Called with LoginModes: TokenOrCredentials and UserToken (if first request did not complete)
    # and for "UsernamePassword", "Register"
    responseMessage = onescanObject();

    processOutcome = onescanObject();
    processOutcome.RedirectURL = "/loginresult.html"
    responseMessage.ProcessOutcome = processOutcome

    # TODO: Other return MessageTypes for UsernamePassword, TokenOrCredentials are "RetryLogin" or "LoginProblem"
    # For UserToken and Register is "LoginProblem"
    responseMessage.Success = True
    responseMessage.MessageType = "ProcessComplete"

    return responseMessage


#Initial call to kick the process off
processMessage()
