$ErrorActionPreference = "Stop"

$work = Split-Path -Parent $MyInvocation.MyCommand.Path
$classes = Join-Path $work "classes"
$staging = Join-Path $work "staging"
$outputJar = Join-Path (Split-Path -Parent $work) "plugin-captcha-1.0.0.jar"
$apiJar = Join-Path $work ".m2repo\run\halo\app\api\2.20.11\api-2.20.11.jar"
$roots = @(
    (Join-Path $work ".m2repo"),
    (Join-Path $env:USERPROFILE ".gradle\caches\modules-2\files-2.1")
)

function Find-Jar([string] $name) {
    foreach ($root in $roots) {
        if (-not (Test-Path $root)) {
            continue
        }
        $jar = Get-ChildItem $root -Recurse -Filter "$name-*.jar" -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notmatch "sources|javadoc" -and $_.Name -match "^$([regex]::Escape($name))-\d" } |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
        if ($jar) {
            return $jar.FullName
        }
    }
    throw "Missing jar: $name"
}

if (-not (Test-Path $apiJar)) {
    mvn "-Dmaven.repo.local=$work\.m2repo" "-Dartifact=run.halo.app:api:2.20.11" "-Dtransitive=false" dependency:get
}

Remove-Item -Recurse -Force $classes, $staging -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $classes, $staging | Out-Null

$jars = @(
    $apiJar,
    (Find-Jar "spring-webflux"),
    (Find-Jar "spring-web"),
    (Find-Jar "spring-core"),
    (Find-Jar "spring-context"),
    (Find-Jar "spring-beans"),
    (Find-Jar "spring-aop"),
    (Find-Jar "spring-expression"),
    (Find-Jar "reactor-core"),
    (Find-Jar "jackson-core"),
    (Find-Jar "jackson-databind"),
    (Find-Jar "jackson-annotations"),
    (Find-Jar "slf4j-api"),
    (Find-Jar "reactive-streams"),
    (Find-Jar "pf4j"),
    (Find-Jar "jakarta.annotation-api")
)

$sources = Get-ChildItem (Join-Path $work "src") -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -proc:none --release 21 -cp ($jars -join ";") -d $classes $sources
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Copy-Item -Recurse (Join-Path $classes "*") $staging
foreach ($item in @("console", "extensions")) {
    Copy-Item -Recurse (Join-Path $work $item) (Join-Path $staging $item)
}
foreach ($item in @("plugin.yaml", "logo.png")) {
    Copy-Item (Join-Path $work $item) (Join-Path $staging $item)
}
New-Item -ItemType Directory -Force (Join-Path $staging "META-INF") | Out-Null
Copy-Item (Join-Path $work "META-INF\plugin-components.idx") (Join-Path $staging "META-INF\plugin-components.idx")

$manifest = Join-Path $staging "META-INF\MANIFEST.MF"
$manifestText = @"
Manifest-Version: 1.0
Plugin-Main-Class: site.zzrbk.plugin.captcha.CaptchaPlugin
Build-Jdk-Spec: 21
Implementation-Title: plugin-captcha
Implementation-Version: 1.0.0

"@
[System.IO.File]::WriteAllText($manifest, $manifestText, [System.Text.Encoding]::ASCII)

Push-Location $staging
try {
    if (Test-Path $outputJar) {
        Remove-Item $outputJar
    }
    jar cfm $outputJar $manifest .
} finally {
    Pop-Location
}

Write-Host "SUCCESS! JAR created: $outputJar ($((Get-Item $outputJar).Length) bytes)"
