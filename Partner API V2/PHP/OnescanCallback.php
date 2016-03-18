<?php
include("OnescanConfig.php");
include("HMAC.php");

// Read the contents of the post
$jsonContent = file_get_contents("php://input");

// validate that the signature is correct
validateSignature($jsonContent,$config->OnescanSecret);

# Parse the payload json into an object
$onescanMessage = json_decode($jsonContent);

// the responseMessage to be returned to Onescan
$responseMessage = NULL;

// to allow correlation with the original payment request,
// this callback includes SessionData property that can be set
// on the original payment request $onescanMessage->SessionData;
switch ($onescanMessage->MessageType) {
  case 'StartPayment':
    $responseMessage = startPayment($onescanMessage);
    break;

  case 'AdditionalCharges':
    // Optionally include delivery options
    // Needs surcharges and deliveroptions set to true in start payment callback
    $responseMessage = additionalCharges($onescanMessage);
    break;

  case "PaymentTaken":
    // For 1-step payment processes (see your payment gateway set up)
    $responseMessage = purchaseDone($onescanMessage);
    break;

  case 'PaymentConfirmed':
    // For 2-step payment processes (see your payment gateway set up)
    $responseMessage = purchaseDone($onescanMessage);
    break;

  case 'PaymentCaptured':
    // TODO Handle capture (if 2-step transaction used).
    $responseMessage = createSuccessMessage($onescanMessage);
    break;

  case "PaymentFailed":
    // TODO: Handle a payment failure (check errors)
    $responseMessage = createSuccessMessage($onescanMessage);
    break;

  case "PaymentCancelled":
    // TODO: Handle cancelled event
    $responseMessage = createSuccessMessage($onescanMessage);
    break;

  // Web Content Process
  case "WebContent":
    $responseMessage = createWebContentMessage($onescanMessage, $config->WebContentUrl);
    break;

  case "StartLogin":
    // Called with LoginModes: TokenOrCredentials and UserToken
    $responseMessage = createStartLoginMessage($onescanMessage);
    break;

  case "Login":
    // Called with LoginModes: TokenOrCredentials and UserToken (if first request did not complete)
    // and for "UsernamePassword", "Register"
    $responseMessage = createLoginMessage($onescanMessage);
    break;

  default:
    throw new Exception("Unexpected Message Type ". $onescanMessage->MessageType .
    ". See documentation on how to respond to other message types.");
}

$response = json_encode($responseMessage);

// Create and sign the headers.
$hmac = hashit($response,$config->OnescanSecret);

header("Content-Type: application/json");
header("x-onescan-account: " . $config->OnescanAccountKey);
header("x-onescan-signature: " . $hmac);

echo $response;


function startPayment($onescanMessage) {
  $responseMessage = NULL;
  $purchasePayload = NULL;
  $purchasePayload->MerchantName = "eZone";
  // This is passed to the payment gateway service as your reference.
  // It could be OrderId for purchases for example.
  $purchasePayload->MerchantTransactionId = "YOUR UNIQUE TRANSACTION ID";
  $purchasePayload->Currency = "GBP";
  // turn on or off - requirement for delivery address
  $purchasePayload->RequiresDeliveryAddress = true;
  // Displayed on orderdetails screen for single item purchases and
  // also used by payment variants on the orderaccepted screen too
  $purchasePayload->ImageData = "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png";

  // TODO: include following when including AdditionalCharges callback
  // ensure RequiresDeliveryAddress is true
  //$purchasePayload->requires->Surcharges = true;
  //$purchasePayload->requires->DeliveryOptions = true;

  // Following group not required when using payment variants
  $purchasePayload->PurchaseDescription = "Toaster";
  $purchasePayload->ProductAmount = 20.00;
  $purchasePayload->Tax = 2.00;
  $purchasePayload->PaymentAmount = 22.00;

  // TODO: Used when Payment Variants are enabled
  //$purchasePayload->CallToAction = "Buy me!";

  $responseMessage->PurchasePayload = $purchasePayload;

  // TODO: Optionally add purchase variants
  $responseMessage->PaymentVariants = addPaymentVariants($onescanMessage);

  // TODO: Optionally add merchant field options
  $responseMessage->MerchantFields = addMerchantFields($onescanMessage);

  // TODO: Check out the API docs from the portal for other features

  return $responseMessage;
}

function addPaymentVariants($onescanMessage) {
  // VariantSelectionType can be SingleChoice | MultipleChoice
  $paymentVariants = NULL;
  $paymentVariants->VariantSelectionType = "SingleChoice";
  $variant1 = NULL;
  $variant1->Id = "Id1";
  $variant1->PurchaseDescription = "Item 1";
  $variant1->Tax = 2.0;
  $variant1->ProductAmount = 10.0;
  $variant1->PaymentAmount = 12.0;
  $variant1->SelectByDefault = true;
  $variant1->ImageData = "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png";

  $variant2 = NULL;
  $variant2->Id = "Id2";
  $variant2->PurchaseDescription = "Item 2";
  $variant2->Tax = 4.0;
  $variant2->ProductAmount = 20.0;
  $variant2->PaymentAmount = 24.0;
  $variant2->SelectByDefault = false;
  $variant2->ImageData = "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png";

  $paymentVariants->Variants = [
    $variant1,
    $variant2
  ];

  return $paymentVariants;
}

function addMerchantFields($onescanMessage) {
  $fieldGroup1 = NULL;
  $fieldGroup1->GroupHeading = "Group 1";
  $fieldGroup1->IsMandatory = true;
  $fieldGroup1->Code = "G1";
  // GroupType can be Informational | ExclusiveChoice
  $fieldGroup1->GroupType = "ExclusiveChoice";
  // Only applies when used with Payment Variants
  $fieldGroup1->AppliesToEachItem = true;

  $field1 = NULL;
  $field1->Label = "Option 1";
  $field1->Code = "F1";
  $field1->PriceDelta = 10.0;
  $field1->TaxDelta = 2.0;
  $field1->SelectByDefault = true;

  $field2 = NULL;
  $field2->Label = "Option 2";
  $field2->Code = "F2";
  $field2->PriceDelta = 20.0;
  $field1->TaxDelta = 4.0;
  $field2->SelectByDefault = false;
  $fieldGroup1->Fields = [
    $field1,
    $field2
  ];

  $merchantFields = NULL;
  $merchantFields->FieldGroups = [ $fieldGroup1 ];
  return $merchantFields;
}

function additionalCharges($onescanMessage) {

  # TODO: Include if the address supplied cant be delivered to
  //$additionalCharges->AddressNotSupported = true;
  //$additionalCharges->AddressNotSupportedReason = "Reason message";

  $deliveryOption1 = NULL;
  $deliveryOption1->Code = "FIRST";
  $deliveryOption1->Description = "Royal Mail First Class";
  $deliveryOption1->IsDefault = true;
  $deliveryOption1->Label = "First class";

  $charge1 = NULL;
  $charge1->BaseAmount = 2.99;
  $charge1->Tax = 0.0;
  $charge1->TotalAmount = 2.99;
  $deliveryOption1->Charge = $charge1;

  $additionalCharges = NULL;
  $additionalCharges->DeliveryOptions[0] = $deliveryOption1;

  $deliveryOption2 = NULL;
  $deliveryOption2->Code = "SECOND";
  $deliveryOption2->Description = "Royal Mail Second Class";
  $deliveryOption2->IsDefault = false;
  $deliveryOption2->Label = "Second class";

  $charge2 = NULL;
  $charge2->BaseAmount = 1.99;
  $charge2->Tax = 0.0;
  $charge2->TotalAmount = 1.99;
  $deliveryOption2->Charge = $charge2;

  $additionalCharges->DeliveryOptions[1] = $deliveryOption2;

  // Surcharges
  $paymentMethodCharge = NULL;
  $paymentMethodCharge->Code = "PLAY";
  $paymentMethodCharge->Description = "This attracts a surcharge";

  $charge3 = NULL;
  $charge3->BaseAmount = 1.00;
  $charge3->Tax = 0.0;
  $charge3->TotalAmount = 1.00;
  $paymentMethodCharge->Charge = $charge3;

  $additionalCharges->PaymentMethodCharge = $paymentMethodCharge;

  $responseMessage = NULL;
  $responseMessage->AdditionalCharges = $additionalCharges;
  return $responseMessage;
}

function purchaseDone($onescanMessage) {
  $responseMessage = NULL;
  $orderAccepted = NULL;
  $orderAccepted->OrderId = "YOUR_ORDER_NUMBER";
  $responseMessage->OrderAccepted = $orderAccepted;
  $responseMessage->MessageType = "OrderAccepted";
  $responseMessage->Success = true;
  return $responseMessage;
}

function createSuccessMessage($onescanMessage) {
  $responseMessage = NULL;
  $responseMessage->Success = true;
  return $responseMessage;
}

function createWebContentMessage($onescanMessage, $webContentUrl) {
  $responseMessage = NULL;
  $webContent = NULL;
  $webContent->WebUrl =  $webContentUrl;
  $webContent->ProcessCompleteUrl = "http://close-window";
  $field1->DataItem = "FirstName";
  $field1->HtmlId = "html_id_1";
  $webContent->FieldMappings[0] = $field1;
  $field2->DataItem = "LastName";
  $field2->HtmlId = "html_id_2";
  $webContent->FieldMappings[1] = $field2;
  $field3->DataItem = "Email";
  $field3->HtmlId = "html_id_3";
  $webContent->FieldMappings[2] = $field3;

  $responseMessage->WebContent = $webContent;
  return $responseMessage;
}

function createLoginMessage($onescanMessage) {
    // Called with LoginModes: TokenOrCredentials and UserToken
    $responseMessage = NULL;
    $processOutcome = NULL;
    $processOutcome->RedirectURL = "loginresult.html";
    $responseMessage->ProcessOutcome = $processOutcome;

    // TODO: Other return MessageTypes for TokenOrCredentials include "RetryLogin" or "LoginProblem"
    // For UserToken then "RegisterUser" can be used to get the user to release their personal info
    $responseMessage->Success = true;
    $responseMessage->MessageType = "ProcessComplete";

    return $responseMessage;
}

function createStartLoginMessage($onescanMessage) {
    // Called with LoginModes: TokenOrCredentials and UserToken (if first request did not complete)
    // and for "UsernamePassword", "Register"
    $responseMessage = NULL;
    $processOutcome = NULL;
    $processOutcome->RedirectURL = "loginresult.html";
    $responseMessage->ProcessOutcome = $processOutcome;

    // TODO: Other return MessageTypes for UsernamePassword, TokenOrCredentials are "RetryLogin" or "LoginProblem"
    // For UserToken and Register is "LoginProblem"
    $responseMessage->Success = true;
    $responseMessage->MessageType = "ProcessComplete";

    return $responseMessage;
}

?>
