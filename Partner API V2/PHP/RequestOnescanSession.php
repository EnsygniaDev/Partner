<?php
  include("OnescanConfig.php");
  include("HMAC.php");

  $onescanRequest = NULL;
  // Build the Request Onescan Session payload
  $onescanRequest->ProcessType = "Payment";
  $onescanRequest->MessageType = "StartPayment";
  // TODO: For WebContent Processes change the process and messagetypes:
  //$onescanRequest->ProcessType = "WebContent";
  //$onescanRequest->MessageType = "WebContent";
  // TODO: For Login Processes:
  //$onescanRequest->ProcessType = "Login";
  //$onescanRequest->MessageType = "StartLogin";
  //$onescanRequest->LoginPayload = createLoginRequest();

  // The Session Data property can be set by you to represent a hold data relating
  // to the users session in some way and will be passed back with each callback
  // so you can locate the session (eg Order number) that the request belongs to.
  $onescanRequest->SessionData = "YOUR_SESSION_DATA";
  $onescanRequest->Version = 2;
  $onescanRequest->MetaData->EndpointURL = $config->OnescanCallbackURL;

  // TODO: For payment processes you can set the PurchasePayload early if you wish (see StartPayment callback)

  // JSon Encode
  $onescanRequestMessage = json_encode($onescanRequest);

  // Sign the header
  $hmac = hashit($onescanRequestMessage,$config->OnescanSecret);
  $header = array(
      "content-type: application/json",
      "x-onescan-account: " . $config->OnescanAccountKey,
      "x-onescan-signature: " . $hmac
  );
  $uri = curl_init($config->OnescanServerURL);
  curl_setopt($uri, CURLOPT_HTTPHEADER, $header);
  curl_setopt($uri,CURLOPT_POST,true);
  curl_setopt($uri,CURLOPT_RETURNTRANSFER,true);
  curl_setopt($uri, CURLOPT_POSTFIELDS, $onescanRequestMessage);
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


  function createLoginRequest()
  {
      $loginPayload = NULL;
      $loginPayload->FriendlyName = "Demo site";
      $loginPayload->SiteIdentifier = "YOUR UNIQUE SITE ID";
      //TODO: Other LoginModes are: TokenOrCredentials, UserToken, TokenOrCredentials, Register
      $loginPayload->LoginMode = "UsernamePassword";

      //TODO: Profiles are required for Register (and with UserToken when the response MessageType "RegisterUser" is used.
      $loginPayload->Profiles = [ "basic" ];
      return $loginPayload;
  }
?>
