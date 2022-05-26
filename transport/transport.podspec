Pod::Spec.new do |spec|
    spec.name                     = 'transport'
    spec.version                  = '1.2.1'
    spec.homepage                 = 'https://github.com/MyPureCloud/genesys-messenger-transport-mobile-sdk'
    spec.source                   = { :git => "Not Published", :tag => "Cocoapods/#{spec.name}/#{spec.version}" }
    spec.authors                  = 'Genesys Cloud Services, Inc.'
    spec.license                  = 'MIT'
    spec.summary                  = 'Genesys Cloud Messenger Transport Framework - Development podspec for use with local testbed app.'

    spec.vendored_frameworks      = "build/cocoapods/framework/MessengerTransport.framework"
    spec.libraries                = "c++"
    spec.module_name              = "#{spec.name}_umbrella"

    spec.ios.deployment_target = '13.0'

                

    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':transport',
        'PRODUCT_MODULE_NAME' => 'transport',
    }

    spec.script_phases = [
        {
            :name => 'Build transport',
            :execution_position => :before_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                if [ "YES" = "$COCOAPODS_SKIP_KOTLIN_BUILD" ]; then
                  echo "Skipping Gradle build task invocation due to COCOAPODS_SKIP_KOTLIN_BUILD environment variable set to \"YES\""
                  exit 0
                fi
                set -ev
                REPO_ROOT="$PODS_TARGET_SRCROOT"
                "$REPO_ROOT/../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:syncFramework \
                    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
                    -Pkotlin.native.cocoapods.archs="$ARCHS" \
                    -Pkotlin.native.cocoapods.configuration=$CONFIGURATION
            SCRIPT
        }
    ]
end