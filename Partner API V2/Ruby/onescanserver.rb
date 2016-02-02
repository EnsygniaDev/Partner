require "webrick"
require_relative "requestonescansession"
require_relative "onescancallback"

root = File.expand_path "./"
server = WEBrick::HTTPServer.new :Port => 8080, :DocumentRoot => root

trap "INT" do server.shutdown end

server.mount "/onescancallback", Onescan::OnescanCallback
server.mount "/requestonescansession", Onescan::RequestOnescanSession

server.start
