import uuid
import json
import urllib.request
import urllib.error
import onescanConfig
from onescanHMAC import onescanHMAC
from onescanObject import onescanObject

onescanRequest = onescanObject()
onescanRequest.ProcessType = "Payment"
onescanRequest.MessageType = "StartPayment"
# TODO: For WebContent Processes change the process and messagetypes:
#onescanRequest.ProcessType = "WebContent"
#onescanRequest.MessageType = "WebContent"
# TODO: For Login Processes:
#onescanRequest.ProcessType = "Login"
#onescanRequest.MessageType = "StartLogin"
#loginPayload = onescanObject()
#loginPayload.FriendlyName = "Demo site"
#loginPayload.SiteIdentifier = "YOUR UNIQUE SITE ID"
##TODO: Other LoginModes are: TokenOrCredentials, UserToken, TokenOrCredentials, Register
#loginPayload.LoginMode = "UsernamePassword"
##TODO: Profiles are required for Register (and with UserToken when the response MessageType "RegisterUser" is used.
##loginPayload.Profiles = [ "basic" ];
#onescanRequest.LoginPayload = loginPayload

# The Session Data property can be set by you to represent a hold data relating
# to the users session in some way and will be passed back with each callback
# so you can locate the session (eg Order number) that the request belongs to.
onescanRequest.SessionData = "CUSTOM SESSION DATA"
onescanRequest.Version = 2
MetaData = onescanObject()
MetaData.EndpointURL = onescanConfig.CALLBACK_URL
onescanRequest.MetaData = MetaData

# TODO: You can set the PurchasePayload early if you wish (see StartPayment callback)

jsonMessage = json.dumps(onescanRequest, default=lambda o: o.__dict__)
onescanRequestMessage = jsonMessage.encode('utf-8')

headers={}
headers['content-type'] = 'application/json'
headers['x-onescan-account'] = onescanConfig.ACCOUNT_KEY
# Sign the header
hmac = onescanHMAC.encode(onescanRequestMessage,onescanConfig.SECRET)
headers['x-onescan-signature'] = hmac

try:
    req=urllib.request.Request(onescanConfig.SERVER_URL,onescanRequestMessage,headers)
    response=urllib.request.urlopen(req)
except urllib.error.URLError as e:
    if e.code != 200:
        raise Exception("Http Error: " + str(e.code) + " " + e.reason)
except:
	raise

responseData = response.read()

# validate the response header signature
onescanSignature = response.getheader('x-onescan-Signature')
onescanHMAC.validateSignature(responseData,onescanConfig.SECRET,onescanSignature)

print('content-type: application/json')
print()
print(responseData.decode('utf-8'))
