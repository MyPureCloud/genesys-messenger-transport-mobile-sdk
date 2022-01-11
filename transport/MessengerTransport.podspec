Pod::Spec.new do |spec|
    spec.name                     = 'MessengerTransport'
    spec.version                  = '<VERSION>'
    spec.homepage                 = 'https://github.com/MyPureCloud/genesys-messenger-transport-mobile-sdk'
    spec.summary                  = 'Genesys Cloud Messenger Transport SDK'
    spec.description              = <<-DESC
    The Genesys Cloud Messenger Transport SDK is a framework that provides methods for connecting to Genesys Cloud Messenger chat APIs and WebSockets from iOS.
    DESC
    spec.author                   = 'Genesys Cloud Services, Inc.'
    spec.license                  = 'MIT'

    spec.source                   = { :http => '<SOURCE_HTTP_URL>' }

    spec.vendored_frameworks      = 'MessengerTransport.xcframework'

    spec.ios.deployment_target    = '11.0'
    spec.pod_target_xcconfig      = { 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'arm64' }
    spec.user_target_xcconfig     = { 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'arm64' }

    spec.dependency 'jetfire', '0.1.5'
end
