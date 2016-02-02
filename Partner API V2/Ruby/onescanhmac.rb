require "openssl"

module Onescan

  class HMAC

    def self.encode(data, secret)
      digest = OpenSSL::Digest.new("sha1")
      return OpenSSL::HMAC.hexdigest(digest, secret, data)
    end

    # Validates that the signature in the header is the same as
    # the one created using our secret key
    def self.validateSignature(data,secret,signature)
      if signature
        hmac = Onescan::HMAC.encode(data,secret)
        if hmac == signature
          return
        end
        raise "HTTP Error: Header signature is invalid"
      else
        raise "HTTP Error: No signature found in the header"
      end
    end

  end

end
