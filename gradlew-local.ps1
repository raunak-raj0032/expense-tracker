$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$jdk = Join-Path $root ".jdk\jdk-17.0.18+8"
$androidSdk = Join-Path $root ".android-sdk"

$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$env:PATH"
$env:GRADLE_USER_HOME = Join-Path $root ".gradle-user-home"
$env:ANDROID_HOME = $androidSdk
$env:ANDROID_SDK_ROOT = $androidSdk

& (Join-Path $root "gradlew.bat") @args
exit $LASTEXITCODE
