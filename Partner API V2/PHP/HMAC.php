<?php

function hashit($message, $secret) {
  return hash_hmac('sha1',$message,$secret);
}

// validates the Signature supplied in the header against the message / secret
function validateSignature($message, $secret) {
  foreach ($_SERVER["HTTP_*"] as $name => $value) {
    if (strtoupper(substr($name, 0, 5)) == 'HTTP_') {
      $header = str_replace(' ', '-', strtolower(str_replace('_', ' ', substr($name, 5))));
      if ($header == "x-onescan-signature") {
        $hmac = hashit($message,$secret);
        if ($hmac == $value) {
          return;
        }
        throw new Exception("HTTP Error: Header signature is invalid");
      }
    }
    throw new Exception("HTTP Error: No signature found in the header");
  }
}
?>
