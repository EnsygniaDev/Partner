from http.server import HTTPServer
from http.server import CGIHTTPRequestHandler

import os

# CGI Request Handler over written in order to ensure
# that the x-onescan-signature is provided to the CGI paython scripts
class OnescanCGIHTTPRequestHandler(CGIHTTPRequestHandler):

    def run_cgi(self):
        onescan_signature = self.headers.get("x-onescan-signature")
        if onescan_signature:
            os.environ['X-ONESCAN-SIGNATURE'] = onescan_signature
        CGIHTTPRequestHandler.run_cgi(self)

# HTTPServer initialisation.
port = 8080
host = '127.0.0.1'
server_address = (host,port)
httpd = HTTPServer(server_address,OnescanCGIHTTPRequestHandler)

print("Starting my web server on port "+str(port))
httpd.serve_forever()
