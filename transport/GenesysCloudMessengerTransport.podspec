Pod::Spec.new do |s|
  s.name                   = 'GenesysCloudMessengerTransport'
  s.version                = '2.9.3-rc1'
  s.summary                = 'Genesys Cloud Messenger Transport SDK'

  s.description            = <<-DESC
The Genesys Cloud Messenger Transport SDK is a framework that provides methods for connecting to Genesys Cloud Messenger chat APIs and WebSockets from iOS.
                             DESC

  s.homepage               = 'https://github.com/MyPureCloud/genesys-messenger-transport-mobile-sdk'
  s.license                = { :type => 'MIT', :text => <<-LICENSE
MIT License

Copyright (c) 2021 Genesys Cloud Services, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
                               LICENSE
                             }
  s.author                 = 'Genesys Cloud Services, Inc.'
  s.source                 = { :http => 'https://github.com/MyPureCloud/genesys-messenger-transport-mobile-sdk/releases/download/v2.9.3-rc1/MessengerTransport.xcframework.zip' }

  s.ios.deployment_target  = '13.0'

  s.vendored_frameworks    = 'MessengerTransport.xcframework'
  s.libraries              = 'c++'
end
