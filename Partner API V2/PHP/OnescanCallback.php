<?php
include("OnescanConfig.php");
include("HMAC.php");

// Read the contents of the post
$jsonContent = file_get_contents("php://input");

// validate that the signature is correct
validateSignature($jsonContent,$config->OnescanSecret);

# Parse the payload json into an object
$purchaseMessage = json_decode($jsonContent);

// to allow correlation with the original payment request,
// this callback includes SessionId property that was set
// on the original payment request $purchaseMessage->SessionId;
// SessionData can also be set with other information the merchant wants
switch ($purchaseMessage->MessageType) {
    case 'StartPayment':
      startPayment($purchaseMessage);
      break;

    case 'AdditionalCharges':
      // Optionally include delivery options
      // Needs surcharges and deliveroptions set to true in start payment callback
      additionalCharges($purchaseMessage);
      break;

    case "PaymentTaken":
      // For 1-step payment processes (see your payment gateway set up)
      purchaseDone($purchaseMessage);
      break;

    case 'PaymentConfirmed':
      // For 2-step payment processes (see your payment gateway set up)
      purchaseDone($purchaseMessage);
      break;

    case 'PaymentCaptured':
      // TODO Handle capture (if 2-step transaction used).
      break;

    case "PaymentFailed":
      // TODO: Handle a payment failure (check errors)
      break;

    case "PaymentCancelled":
      // TODO: Handle cancelled event
      break;

    default:
      throw new Exception("Unexpected Message Type ". $purchaseMessage->MessageType .
      ". See documentation on how to respond to other message types.");
}

$response = json_encode($purchaseMessage);

// Create and sign the headers.
$hmac = hashit($response,$config->OnescanSecret);

header("Content-Type: application/json");
header("x-onescan-account: " . $config->OnescanAccountKey);
header("x-onescan-signature: " . $hmac);

echo $response;

function startPayment($purchaseMessage) {
  $purchasePayload->MerchantName = "eZone";
  $purchasePayload->Currency = "GBP";
  // turn on or off - requirement for delivery address
  $purchasePayload->RequiresDeliveryAddress = true;
  // Displayed on orderdetails screen for single item purchases and
  // also used by payment variants on the orderaccepted screen too
  $purchasePayload->ImageData = "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png";

  // TODO: include following when including AdditionalCharges callback
  // ensure RequiresDeliveryAddress is true
  #$purchasePayload->requires->Surcharges = true;
  #$purchasePayload->requires->DeliveryOptions = true;

  // Following group not required when using payment variants
  $purchasePayload->PurchaseDescription = "Toaster";
  $purchasePayload->ProductAmount = 20.00;
  $purchasePayload->Tax = 2.00;
  $purchasePayload->PaymentAmount = 22.00;

  // TODO: Used when Payment Variants are enabled
  #$purchasePayload->CallToAction = "Buy me!";

  $purchaseMessage->PurchasePayload = $purchasePayload;

  // TODO: Optionally add purchase variants
  #addPaymentVariants($purchaseMessage);

  // TODO: Optionally add merchant field options
  #addMerchantFields($purchaseMessage);

  // TODO: Check out the API docs from the portal for other features

}

function addPaymentVariants($purchaseMessage) {
    // VariantSelectionType can be SingleChoice | MultipleChoice
    $paymentVariants->VariantSelectionType = "SingleChoice";
    $variant1->Id = "Id1";
    $variant1->PurchaseDescription = "Item 1";
    $variant1->Tax = 2.0;
    $variant1->ProductAmount = 10.0;
    $variant1->PaymentAmount = 12.0;
    $variant1->SelectByDefault = true;
    $variant1->ImageData = "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png";

    $variant2->Id = "Id2";
    $variant2->PurchaseDescription = "Item 2";
    $variant2->Tax = 4.0;
    $variant2->ProductAmount = 20.0;
    $variant2->PaymentAmount = 24.0;
    $variant2->SelectByDefault = false;
    $variant2->ImageData = "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png";

    $paymentVariants->Variants[0] = $variant1;
    $paymentVariants->Variants[1] = $variant2;

    $purchaseMessage->PaymentVariants = $paymentVariants;
}

function addMerchantFields($purchaseMessage) {

    $fieldGroup1->GroupHeading = "Group 1";
    $fieldGroup1->IsMandatory = true;
    $fieldGroup1->Code = "G1";
    // GroupType can be Informational | ExclusiveChoice
    $fieldGroup1->GroupType = "ExclusiveChoice";
    // Only applies when used with Payment Variants
    $fieldGroup1->AppliesToEachItem = true;

    $field1->Label = "Option 1";
    $field1->Code = "F1";
    $field1->PriceDelta = 10.0;
    $field1->TaxDelta = 2.0;
    $field1->SelectByDefault = true;

    $field2->Label = "Option 2";
    $field2->Code = "F2";
    $field2->PriceDelta = 20.0;
    $field1->TaxDelta = 4.0;
    $field2->SelectByDefault = false;
    $fieldGroup1->Fields[0] = $field1;
    $fieldGroup1->Fields[1] = $field2;

    $merchantFields->FieldGroups[0] = $fieldGroup1;

    $purchaseMessage->MerchantFields = $merchantFields;
}

function additionalCharges($purchaseMessage) {

    # TODO: Include if the address supplied cant be delivered to
    //$additionalCharges->AddressNotSupported = true;
    //$additionalCharges->AddressNotSupportedReason = "Reason message";

    $deliveryOption1->Code = "FIRST";
    $deliveryOption1->Description = "Royal Mail First Class";
    $deliveryOption1->IsDefault = true;
    $deliveryOption1->Label = "First class";

    $charge1->BaseAmount = 2.99;
    $charge1->Tax = 0.0;
    $charge1->TotalAmount = 2.99;
    $deliveryOption1->Charge = $charge1;

    $additionalCharges->DeliveryOptions[0] = $deliveryOption1;

    $deliveryOption2->Code = "SECOND";
    $deliveryOption2->Description = "Royal Mail Second Class";
    $deliveryOption2->IsDefault = false;
    $deliveryOption2->Label = "Second class";

    $charge2->BaseAmount = 1.99;
    $charge2->Tax = 0.0;
    $charge2->TotalAmount = 1.99;
    $deliveryOption2->Charge = $charge2;

    $additionalCharges->DeliveryOptions[1] = $deliveryOption2;

    // Surcharges
    $paymentMethodCharge->Code = "PLAY";
    $paymentMethodCharge->Description = "This attracts a surcharge";

    $charge3->BaseAmount = 1.00;
    $charge3->Tax = 0.0;
    $charge3->TotalAmount = 1.00;
    $paymentMethodCharge->Charge = $charge3;

    $additionalCharges->PaymentMethodCharge = $paymentMethodCharge;

    $purchaseMessage->AdditionalCharges = $additionalCharges;
}

function purchaseDone($purchaseMessage) {
  $orderAccepted->OrderId = "YOUR_ORDER_NUMBER";
  $orderAccepted->Name = "Onescan.OrderAccepted";
  $purchaseMessage->OrderAccepted = $orderAccepted;
  $purchaseMessage->MessageType = "OrderAccepted";
  $purchaseMessage->Success = true;
}

?>
