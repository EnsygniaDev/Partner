<?php
// Get your Onescan AccountKey from https://portal.ensygnia.net
$config->OnescanAccountKey = "YOUR ACCOUNT KEY";
// Get your Onescan Secret from https://portal.ensygnia.net
$config->OnescanSecret = "YOUR SECRET";
// Set your callback URL
$config->OnescanCallbackURL = "http://YOUR SERVER URL/OnescanCallback.php";
// Set location to test the WebContent Process
$config->WebContentUrl = "http://YOUR SERVER URL/webcontent.html";
// URL for Onescan 
$config->OnescanServerURL = "https://liveservice.ensygnia.net/api/partnergateway/2/RequestOnescanSession";
?>
