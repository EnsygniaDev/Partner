<?php
// Get your Onescan AccountKey and secret from https://portal.ensygnia.net
$config->OnescanAccountKey = "YOUR_ACCOUNT_KEY";
$config->OnescanSecret = "YOUR_SECRET";
// Set your callback URL
$config->OnescanCallbackURL = "https://YOUR_SERVER_URL/OnescanCallback.php";
$config->OnescanServerURL = "https://liveservice.ensygnia.net/api/partnergateway/2/RequestOnescanSession";
?>
