Pod::Spec.new do |spec|
    spec.name                     = 'transport'
    spec.version                  = '2.3.2'
    spec.homepage                 = 'https://github.com/MyPureCloud/genesys-messenger-transport-mobile-sdk'
    spec.source                   = { :http=> ''}
    spec.authors                  = 'Genesys Cloud Services, Inc.'
    spec.license                  = 'MIT'
    spec.summary                  = 'Genesys Cloud Messenger Transport Framework - Development podspec for use with local testbed app.'
    spec.vendored_frameworks      = 'build/cocoapods/framework/MessengerTransport.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target = '13.0'
                
                
    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':transport',
        'PRODUCT_MODULE_NAME' => 'MessengerTransport',
    }
                
    spec.script_phases = [
        {
            :name => 'Build transport',
            :execution_position => :before_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                  exit 0
                fi
                set -ev
                REPO_ROOT="$PODS_TARGET_SRCROOT"
                "$REPO_ROOT/../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:syncFramework \
                    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
                    -Pkotlin.native.cocoapods.archs="$ARCHS" \
                    -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
            SCRIPT
        }
    ]
                
end