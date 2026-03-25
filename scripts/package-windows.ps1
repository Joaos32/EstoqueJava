param(
    [string]$AppVersion = "1.0.0"
)

$ErrorActionPreference = "Stop"

$mavenPath = Join-Path $PSScriptRoot "..\.tools\apache-maven-3.9.6\bin\mvn.cmd"
if (-not (Test-Path $mavenPath)) {
    $mavenPath = "mvn"
}

$jpackageCommand = Get-Command jpackage -ErrorAction SilentlyContinue
if ($null -eq $jpackageCommand) {
    throw "O comando jpackage nao foi encontrado no PATH. Instale ou configure o JDK 17 com jpackage."
}

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$inputDir = Join-Path $projectRoot "target\jpackage\input"
$distDir = Join-Path $projectRoot "target\dist"
$mainJar = Join-Path $projectRoot "target\estoqueti-desktop-1.0.0-SNAPSHOT.jar"

if (Test-Path $inputDir) {
    Remove-Item $inputDir -Recurse -Force
}
if (Test-Path $distDir) {
    Remove-Item $distDir -Recurse -Force
}
New-Item -ItemType Directory -Path $inputDir -Force | Out-Null
New-Item -ItemType Directory -Path $distDir -Force | Out-Null

Push-Location $projectRoot
try {
    & $mavenPath clean package dependency:copy-dependencies "-DincludeScope=runtime" "-DoutputDirectory=target/jpackage/input"
    if (-not (Test-Path $mainJar)) {
        throw "Jar principal nao encontrado em $mainJar"
    }

    Copy-Item $mainJar $inputDir -Force

    & $jpackageCommand.Source `
        --type app-image `
        --dest $distDir `
        --input $inputDir `
        --name "EstoqueTI Desktop" `
        --main-jar "estoqueti-desktop-1.0.0-SNAPSHOT.jar" `
        --main-class "br.com.estoqueti.AppLauncher" `
        --app-version $AppVersion `
        --vendor "Empresa de TI" `
        --description "Sistema desktop para controle de estoque de equipamentos de TI" `
        --java-options "-Dfile.encoding=UTF-8" `
        --java-options "--module-path" `
        --java-options "`$APPDIR" `
        --java-options "--add-modules" `
        --java-options "javafx.controls,javafx.fxml"
} finally {
    Pop-Location
}

$launcherPath = Join-Path $distDir "EstoqueTI Desktop\EstoqueTI Desktop.exe"
if (-not (Test-Path $launcherPath)) {
    throw "Executavel nao encontrado em $launcherPath"
}

Write-Host "Executavel gerado com sucesso em: $launcherPath"