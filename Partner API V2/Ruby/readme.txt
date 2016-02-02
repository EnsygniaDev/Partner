Edit onescanconfig.rb with your account key and Secret and server URL
Try using ngrok.io for secure tunnelling to localhost for testing.
A copy of curl ca-bundle is included and may be required for certain
environments (windows). Either add or remove line in requestonescansession.rb
http.ca_file = "./curl/curl-ca-bundle.crt"
To start, open the command prompt and type
ruby onescanserver.rb
