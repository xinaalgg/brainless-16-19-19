param(
    [Parameter(Mandatory)][string]$Jdk,
    [Parameter(Mandatory)][string]$ApiJar,
    [string]$RuneLiteCache = (Join-Path $env:USERPROFILE '.runelite\repository2')
)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $deps = @(Get-ChildItem -LiteralPath $RuneLiteCache -Filter '*.jar' | Where-Object {
        $_.Name -notmatch '^(client-|runelite-api-|injected-client-)' -or $_.Name -eq 'client-1.13.8.jar'
    })
    if (-not ($deps.Name -contains 'client-1.13.8.jar')) { throw 'RuneLite 1.13.8 client JAR is required.' }
    $cp = @((Resolve-Path -LiteralPath $ApiJar).Path) + $deps.FullName -join ';'
    $out = Join-Path $PSScriptRoot ('build\compile-' + [guid]::NewGuid().ToString('N'))
    $main = New-Item -ItemType Directory -Force -Path "$out\main"
    $tests = New-Item -ItemType Directory -Force -Path "$out\tests"
    $libs = New-Item -ItemType Directory -Force -Path 'build\libs'
    $sources = @(Get-ChildItem 'src\main\java' -File -Recurse -Filter '*.java').FullName
    & "$Jdk\bin\javac.exe" --release 11 -encoding UTF-8 -classpath $cp -d $main $sources
    if ($LASTEXITCODE) { throw 'Plugin compilation failed.' }
    $testSources = @(Get-ChildItem 'src\test\java' -File -Recurse -Filter '*.java').FullName
    & "$Jdk\bin\javac.exe" --release 11 -encoding UTF-8 -classpath "$main;$cp" -d $tests $testSources
    if ($LASTEXITCODE) { throw 'Test compilation failed.' }
    if (Test-Path "$tests\dev\tempoross\assist\AssistChecks.class") {
        & "$Jdk\bin\java.exe" -ea "-Djava.awt.headless=true" "-Duser.home=$out" -classpath "$tests;$main;$cp" dev.tempoross.assist.AssistChecks
        if ($LASTEXITCODE) { throw 'Verification failed.' }
    }
    & "$Jdk\bin\jar.exe" --create --file "$libs\brainless-16-19-19-0.1.0.jar" -C $main . -C "$PSScriptRoot\src\main\resources" . -C $PSScriptRoot runelite-plugin.properties
    if ($LASTEXITCODE) { throw 'Plugin packaging failed.' }
    & "$Jdk\bin\jar.exe" --create --file "$libs\brainless-16-19-19-dev.jar" -C $tests dev/tempoross/assist/DevelopmentClient.class
    if ($LASTEXITCODE) { throw 'Development launcher packaging failed.' }
} finally { Pop-Location }
