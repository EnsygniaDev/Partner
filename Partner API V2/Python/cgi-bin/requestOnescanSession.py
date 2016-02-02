import uuid
import json
import urllib.request
import urllib.error
import onescanConfig
from onescanHMAC import onescanHMAC
from onescanObject import onescanObject

purchaseRequest = onescanObject()
purchaseRequest.ProcessType = "Payment"
purchaseRequest.MessageType = "StartPayment"
# The Session properties are set by you to represent a hook to the users session in some way and
# will be  passed back with each callback so you can locate the session
# (eg Order number) that the request belongs to.
purchaseRequest.SessionId = str(uuid.uuid4())
purchaseRequest.SessionData = "CUSTOM SESSION DATA"
purchaseRequest.Version = 2
MetaData = onescanObject()
MetaData.EndpointURL = onescanConfig.CALLBACK_URL
purchaseRequest.MetaData = MetaData

# TODO: You can set the PurchasePayload early if you wish (see StartPayment callback)

jsonMessage = json.dumps(purchaseRequest, default=lambda o: o.__dict__)
purchaseRequestMessage = jsonMessage.encode('utf-8')

headers={}
headers['content-type'] = 'application/json'
headers['x-onescan-account'] = onescanConfig.ACCOUNT_KEY
# Sign the header
hmac = onescanHMAC.encode(purchaseRequestMessage,onescanConfig.SECRET)
headers['x-onescan-signature'] = hmac

try:
    req=urllib.request.Request(onescanConfig.SERVER_URL,purchaseRequestMessage,headers)
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
