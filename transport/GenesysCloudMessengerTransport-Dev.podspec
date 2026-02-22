Pod::Spec.new do |spec|
    spec.name                     = 'GenesysCloudMessengerTransport'
    spec.version                  = '2.11.0-rc6'
    spec.homepage                 = 'https://github.com/MyPureCloud/genesys-messenger-transport-mobile-sdk'
    spec.source                   = { :http => '' }
    spec.authors                  = 'Genesys Cloud Services, Inc.'
    spec.license                  = { :type => 'MIT' }
    spec.summary                  = 'Genesys Cloud Messenger Transport Framework - Local development podspec.'
    spec.vendored_frameworks      = 'build/cocoapods/framework/MessengerTransport.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target    = '13.0'
    
    # Include Kotlin source files for debugging in Xcode (appears in Development Pods like other local pods)
    # Note: These are Kotlin files, but CocoaPods will still add them to the project navigator
    spec.source_files = [
        'src/commonMain/**/*.kt',
        'src/iosMain/**/*.kt'
    ]
    
    spec.xcconfig = {
        'ENABLE_USER_SCRIPT_SANDBOXING' => 'NO',
    }

    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':transport',
        'PRODUCT_MODULE_NAME' => 'MessengerTransport',
    }

    spec.script_phases = [
        {
            :name => 'Build MessengerTransport (Kotlin/Native)',
            :execution_position => :before_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                # Set JAVA_HOME for Xcode environment
                if [[ "$(uname -m)" == arm64 ]]; then
                    export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
                else
                    export JAVA_HOME="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
                fi
                export PATH="$JAVA_HOME/bin:$PATH"
                if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to YES"
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

