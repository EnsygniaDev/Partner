<?php
  include("OnescanConfig.php");
  include("HMAC.php");

  // Build the Request Onescan Session payload
  $purchaseRequest->ProcessType = "Payment";
  $purchaseRequest->MessageType = "StartPayment";
  // The Session properties are set by you to represent a hook to the users
  // session in some way and will be passed back with each callback
  // so you can locate the session (eg Order number) that the request belongs to.
  $purchaseRequest->SessionId = "YOUR_SESSION_ID";
  $purchaseRequest->SessionData = "YOUR_SESSION_DATA";
  $purchaseRequest->Version = 2;
  $purchaseRequest->MetaData->EndpointURL = $config->OnescanCallbackURL;

  // TODO: You can set the PurchasePayload early if you wish (see StartPayment callback)

  // JSon Encode
  $purchaseRequestMessage = json_encode($purchaseRequest);

  // Sign the header
  $hmac = hashit($purchaseRequestMessage,$config->OnescanSecret);
  $header = array(
      "content-type: application/json",
      "x-onescan-account: " . $config->OnescanAccountKey,
      "x-onescan-signature: " . $hmac
  );
  $uri = curl_init($config->OnescanServerURL);
  curl_setopt($uri, CURLOPT_HTTPHEADER, $header);
  curl_setopt($uri,CURLOPT_POST,true);
  curl_setopt($uri,CURLOPT_RETURNTRANSFER,true);
  curl_setopt($uri, CURLOPT_POSTFIELDS, $purchaseRequestMessage);
  // TODO: For windows you might need to provide the curl cert bundle
  curl_setopt($uri, CURLOPT_CAINFO, dirname(__FILE__) . "/curl/curl-ca-bundle.crt");

  try {
    $response = curl_exec($uri);
    if (curl_errno($uri)) {
        throw new Exception("HTTP Error " . curl_errno($uri) . ". " . curl_error($uri));
    }
    curl_close($uri);
  }
  catch (Exception $e) {
      curl_close($uri);
      file_put_contents("php://stdout", $hmac . "ffff\n");
      throw $e;
  }

  // Check that the signature of the header matches the payload
  validateSignature($response,$config->OnescanSecret);

  echo $response;
?>
