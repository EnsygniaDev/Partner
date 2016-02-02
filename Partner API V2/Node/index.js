/**
 * Created by John Metzger on 04/12/2015.
 */
var express = require('express'); // npm install express
var request = require('request'); // npm install request

var CryptoJS = require("crypto-js"); // npm install crypto-js - used for message signing

// Express JS configuration
var app = express();
app.use(express.static('html')); // this means any static html from the "html" can be used
app.use(rawBody); // parse the body as text

// this node server is listening in on port 3000
var server = app.listen(3000, function () {
    var host = server.address().address;
    var port = server.address().port;

    console.log('Simple Partner app listening at http://%s:%s', host, port);
});

// Partner configuration - this comes from the partner portal
var config = {};
config.accountKey = "YOUR_ACCOUNT_KEY";
config.secret = "YOUR_SECRET";
config.callback = "http://[YOUR_SERVER_URL]/onescancallback"; // put your server URL here
config.requestSession = "https://liveservice.ensygnia.net/api/partnergateway/2/requestonescansession";

// make the padlock the default URL
app.get('/', function(req, res){
    res.sendFile('padlock.html', { root: __dirname + "/html" } );
});

// Called from the padlock on padlock.html on the this user interface
app.post('/requestonescansession', function (req, response) {

    var padlockRequestString = JSON.stringify(createPaymentRequest());

    var options = {
        url: config.requestSession,
        headers: {
            'x-onescan-account': config.accountKey,
            'x-onescan-signature': CryptoJS.HmacSHA1(padlockRequestString, config.secret),
            'Content-Type': 'application/json'
        },
        method: 'POST',
        body: padlockRequestString
    };

    request(options, function (error, resp, body) {

        if (!error && resp.statusCode == 200) {
            if(requestIsSignedCorrectly( body, resp )) {
                response.writeHead(200, {"Content-Type": "application/json"});
                response.write(body);
                response.end();
            }
            else {
                console.log("Signatures do not match");
                response.writeHead(400);
                response.write("WARNING SIGNATURES DO NOT MATCH");
                response.end();
            }
        }
    });
});

// Callback from onescan to the partner to allow the partner to respond
// really should test inbound message signature too (see above)
app.post('/onescancallback', function (request, response) {

    if(requestIsSignedCorrectly( request.rawBody, request)) {

        var purchaseMessage = JSON.parse( request.rawBody);
        if (purchaseMessage.MessageType === "PaymentConfirmed") {

            // to allow correlation with the original payment request,
            // this callback includes SessionID property that was set
            // on the original payment request
            console.log("SessionID: %s", purchaseMessage.SessionID);
            createOrderAcceptedPayload(purchaseMessage);
        }
        else {
            console.log("For how to respond to other message types, see documentation");
        }
        var responseString = JSON.stringify(purchaseMessage);
        response.writeHead(200, {
            "Content-Type": "application/json",
            "x-onescan-account": config.accountKey,
            "x-onescan-signature": CryptoJS.HmacSHA1(responseString, config.secret)
        });
        response.write(responseString);
        response.end();
    }
    else {
        console.log("Signatures do not match");
        response.writeHead(400);
        response.write("WARNING SIGNATURES DO NOT MATCH");
        response.end();
    }
});

// Onescan message for payment
function createPaymentRequest() {

    var purchasePayload = {
        MerchantName: "YOUR_MERCHANT_NAME",
        PurchaseDescription: "YOUR_PRODUCT_DESCRIPTION",
        ProductAmount: 20.00,
        Tax: 2.00,
        PaymentAmount: 22.00,
        Currency: "GBP",
        RequiresDeliveryAddress: true,
        ImageData: "http://www.ensygnia.com/wp-content/uploads/onescan-square-image.png"
    };

    var purchaseRequestMessage = {
        MetaData: {
            EndpointURL: config.callback
        },
        Version: 2,
        ProcessType: "Payment",
        MessageType: "StartPayment",
        PurchasePayload: purchasePayload,
        SessionId: "YOUR_SESSION_ID", // Will be passed back to the callback for you to identify the initial session
        SessionData: "YOUR_SESSION_DATA" // Optional session data
    };
    return purchaseRequestMessage;
}

function createOrderAcceptedPayload(purchaseRequestMessage) {

    // use purchaseRequestMessage.SessionID to find your order or product

    var orderAccepted = {
        Name: "Onescan.OrderAccepted",
        OrderId: "YOUR_ORDER_NUMBER"
    };
    purchaseRequestMessage.OrderAccepted = orderAccepted;
    purchaseRequestMessage.MessageType = "OrderAccepted";
    purchaseRequestMessage.Success = true;
}

// Utility functions

function rawBody(req, res, next) {
    req.setEncoding('utf8');
    req.rawBody = '';
    req.on('data', function(chunk) {
        req.rawBody += chunk;
    });
    req.on('end', function(){
        next();
    });
}

function requestIsSignedCorrectly(body, request){
    var headerSignature = request.headers["x-onescan-signature"];
    var calculatedSignature = CryptoJS.HmacSHA1(body, config.secret);
    return headerSignature == calculatedSignature;
}
