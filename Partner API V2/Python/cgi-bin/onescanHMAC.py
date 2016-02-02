from hashlib import sha1
import hmac

# HMac helper class
class onescanHMAC:
	@staticmethod
	def encode(data, secret):
		response=hmac.new(secret,data,sha1).hexdigest()
		return response

    # Validates that the signature in the header is the same as
    # the one created using our secret key
	@staticmethod
	def validateSignature(data,secret,signature):
		if signature:
			hmac = onescanHMAC.encode(data,secret)
			if hmac == signature:
				return
			raise Exception("HTTP Error: Header signature is invalid")

		raise Exception("HTTP Error: No signature found in the header")
