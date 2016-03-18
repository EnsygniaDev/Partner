require "webrick"
require "json"
require "net/http"
require "securerandom"

require_relative "onescanhmac"
require_relative "onescanconfig"

module Onescan

  class RequestOnescanSession < WEBrick::HTTPServlet::AbstractServlet

    def do_POST request, response

      onescanRequest = {
        :ProcessType => "Payment",
        :MessageType => "StartPayment",
        # TODO: For WebContent Processes change the process and messagetypes:
        #:ProcessType => "WebContent",
        #:MessageType => "WebContent",
        # TODO: For Login Processes:
        #:ProcessType => "Login",
        #:MessageType => "StartLogin",
        #:LoginPayload => {
        #  :FriendlyName => "Demo site",
        #  :SiteIdentifier => "YOUR UNIQUE SITE ID",
        #  # TODO: Other LoginModes are: TokenOrCredentials, UserToken, TokenOrCredentials, Register
        #  :LoginMode => "UsernamePassword",
        #  # TODO: Profiles are required for Register (and with UserToken when the response MessageType "RegisterUser" is used.
        #  :Profiles => [ "basic" ]
        #},
        # The Session Data property can be set by you to represent a hold data relating
        # to the users session in some way and will be passed back with each callback
        # so you can locate the session (eg Order number) that the request belongs to.
        :SessionData => "CUSTOM SESSION DATA",
        :Version => 2,
        :MetaData => {
          :EndpointURL => Onescan::CALLBACK_URL
        }
      }

      # TODO: You can set the PurchasePayload early if you wish (see StartPayment callback)

      onescanRequestMessage = JSON.generate(onescanRequest)

      uri = URI(Onescan::SERVER_URL)
      req = Net::HTTP::Post.new(uri)

      req.body = onescanRequestMessage
      req.content_type = "application/json"
      req["x-onescan-account"] = Onescan::ACCOUNT_KEY
      # Sign the header
      hmac = Onescan::HMAC.encode(onescanRequestMessage, Onescan::SECRET)
      req["x-onescan-signature"] = hmac

      http = Net::HTTP.new(uri.host, uri.port)
      http.use_ssl = true
      # ca_file may be required if certs are not present on server
      http.ca_file = "./curl/curl-ca-bundle.crt"

      res =  http.request(req)
      responseData = res.body

      # validate the response header signature
      onescanSignature = res["x-onescan-signature"]
      Onescan::HMAC.validateSignature(responseData,Onescan::SECRET,onescanSignature)

      response.status = 200
      response["Content-Type"] = "application/json"
      response.body = responseData
    end
  end

end
