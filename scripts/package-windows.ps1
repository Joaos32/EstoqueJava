param(
    [string]$AppVersion = "1.0.0",
    [ValidateSet("exe", "msi", "app-image")]
    [string]$PackageType = "exe",
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

function Invoke-ExternalCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Executable,
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [string]$FailureMessage
    )

    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) {
        if ([string]::IsNullOrWhiteSpace($FailureMessage)) {
            throw "Falha ao executar: $Executable $($Arguments -join ' ')"
        }

        throw $FailureMessage
    }
}

function Resolve-MavenPath {
    $bundledMaven = Join-Path $PSScriptRoot "..\.tools\apache-maven-3.9.6\bin\mvn.cmd"
    if (Test-Path $bundledMaven) {
        return (Resolve-Path $bundledMaven).Path
    }

    $mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue
    if ($null -eq $mavenCommand) {
        throw "O Maven nao foi encontrado. Instale o Maven ou use a pasta .tools do projeto."
    }

    return $mavenCommand.Source
}

function Resolve-JPackagePath {
    $jpackageCommand = Get-Command jpackage -ErrorAction SilentlyContinue
    if ($null -eq $jpackageCommand) {
        throw "O comando jpackage nao foi encontrado no PATH. Instale ou configure o JDK 17 com jpackage."
    }

    return $jpackageCommand.Source
}

function Resolve-CscPath {
    $runtimeDirectory = [System.Runtime.InteropServices.RuntimeEnvironment]::GetRuntimeDirectory()
    $cscPath = Join-Path $runtimeDirectory "csc.exe"
    if (-not (Test-Path $cscPath)) {
        throw "O compilador C# nao foi encontrado em $cscPath"
    }

    return $cscPath
}

function Escape-CSharpString {
    param([string]$Value)

    return $Value.Replace('\\', '\\\\').Replace('"', '\\"')
}

function Resolve-MavenRepositoryArgument {
    param([string]$ProjectRoot)

    $defaultRepository = Join-Path $env:USERPROFILE ".m2\repository"
    try {
        if (-not (Test-Path $defaultRepository)) {
            New-Item -ItemType Directory -Path $defaultRepository -Force | Out-Null
        }

        $probeFile = Join-Path $defaultRepository ".codex-write-test"
        Set-Content -Path $probeFile -Value "ok" -Encoding ASCII -ErrorAction Stop
        Remove-Item $probeFile -Force -ErrorAction SilentlyContinue
        return $null
    } catch {
        $fallbackRepository = Join-Path $ProjectRoot ".m2\repository"
        New-Item -ItemType Directory -Path $fallbackRepository -Force | Out-Null
        return "-Dmaven.repo.local=$fallbackRepository"
    }
}

function Invoke-MavenBuild {
    param(
        [string]$MavenPath,
        [string]$ProjectRoot,
        [bool]$SkipTestsFlag,
        [string]$RepositoryArgument
    )

    $mavenArguments = @(
        "clean",
        "package",
        "dependency:copy-dependencies",
        "-DincludeScope=runtime",
        "-DoutputDirectory=target/jpackage/input"
    )

    if (-not [string]::IsNullOrWhiteSpace($RepositoryArgument)) {
        $mavenArguments += $RepositoryArgument
    }

    if ($SkipTestsFlag) {
        $mavenArguments += "-DskipTests"
    }

    Push-Location $ProjectRoot
    try {
        Invoke-ExternalCommand -Executable $MavenPath -Arguments $mavenArguments -FailureMessage "Falha ao executar o build Maven."
    } finally {
        Pop-Location
    }
}

function New-AppImage {
    param(
        [string]$JPackagePath,
        [string]$ProjectRoot,
        [string]$InputDirectory,
        [string]$AppImageOutput,
        [string]$AppName,
        [string]$AppVersion,
        [string]$MainJarName,
        [string]$MainClass,
        [string]$Vendor,
        [string]$Description,
        [string]$IconPath
    )

    if (Test-Path $AppImageOutput) {
        Remove-Item $AppImageOutput -Recurse -Force
    }
    New-Item -ItemType Directory -Path $AppImageOutput -Force | Out-Null

    $jpackageArguments = @(
        "--type", "app-image",
        "--dest", $AppImageOutput,
        "--input", $InputDirectory,
        "--name", $AppName,
        "--main-jar", $MainJarName,
        "--main-class", $MainClass,
        "--app-version", $AppVersion,
        "--vendor", $Vendor,
        "--description", $Description,
        "--icon", $IconPath,
        "--java-options", "-Dfile.encoding=UTF-8",
        "--java-options", "-Dprism.order=d3d,sw",
        "--java-options", "--module-path",
        "--java-options", '$APPDIR',
        "--java-options", "--add-modules",
        "--java-options", "javafx.controls,javafx.fxml"
    )

    Push-Location $ProjectRoot
    try {
        Invoke-ExternalCommand -Executable $JPackagePath -Arguments $jpackageArguments -FailureMessage "Falha ao gerar o app-image com jpackage."
    } finally {
        Pop-Location
    }

    $artifactPath = Join-Path $AppImageOutput "$AppName\$AppName.exe"
    if (-not (Test-Path $artifactPath)) {
        throw "Executavel do app-image nao encontrado em $artifactPath"
    }

    return $artifactPath
}

function New-CustomInstaller {
    param(
        [string]$ProjectRoot,
        [string]$AppImageOutput,
        [string]$InstallerOutput,
        [string]$AppName,
        [string]$AppVersion,
        [string]$Vendor,
        [string]$IconPath
    )

    $templatePath = Join-Path $ProjectRoot "scripts\windows-installer\InstallerTemplate.cs"
    if (-not (Test-Path $templatePath)) {
        throw "Template do instalador nao encontrado em $templatePath"
    }

    $installerWork = Join-Path $ProjectRoot "target\installer-work"
    if (Test-Path $installerWork) {
        Remove-Item $installerWork -Recurse -Force
    }
    New-Item -ItemType Directory -Path $installerWork -Force | Out-Null

    $payloadDirectory = Join-Path $AppImageOutput $AppName
    if (-not (Test-Path $payloadDirectory)) {
        throw "Diretorio do app-image nao encontrado em $payloadDirectory"
    }

    $payloadZip = Join-Path $installerWork "payload.zip"
    Compress-Archive -Path (Join-Path $payloadDirectory "*") -DestinationPath $payloadZip -Force

    $template = Get-Content $templatePath -Raw
    $appId = ($AppName -replace '[^A-Za-z0-9]', '')
    $source = $template
    $source = $source.Replace("__APP_NAME__", (Escape-CSharpString $AppName))
    $source = $source.Replace("__APP_VERSION__", (Escape-CSharpString $AppVersion))
    $source = $source.Replace("__PUBLISHER__", (Escape-CSharpString $Vendor))
    $source = $source.Replace("__MAIN_EXE__", (Escape-CSharpString ($AppName + ".exe")))
    $source = $source.Replace("__UNINSTALLER_EXE__", (Escape-CSharpString ("Uninstall " + $AppName + ".exe")))
    $source = $source.Replace("__APP_ID__", (Escape-CSharpString $appId))

    $sourcePath = Join-Path $installerWork "Installer.cs"
    Set-Content -Path $sourcePath -Value $source -Encoding UTF8

    if (Test-Path $InstallerOutput) {
        Remove-Item $InstallerOutput -Recurse -Force
    }
    New-Item -ItemType Directory -Path $InstallerOutput -Force | Out-Null

    $installerPath = Join-Path $InstallerOutput ($AppName + ".exe")
    $cscPath = Resolve-CscPath
    $compilerArguments = @(
        "/nologo",
        "/target:winexe",
        "/optimize+",
        "/out:$installerPath",
        "/resource:$payloadZip,EstoqueTIInstaller.Payload.zip",
        "/win32icon:$IconPath",
        "/r:System.Windows.Forms.dll",
        "/r:System.IO.Compression.dll",
        "/r:System.IO.Compression.FileSystem.dll",
        $sourcePath
    )

    Invoke-ExternalCommand -Executable $cscPath -Arguments $compilerArguments -FailureMessage "Falha ao compilar o instalador Windows."
    if (-not (Test-Path $installerPath)) {
        throw "Falha ao localizar o instalador compilado em $installerPath"
    }

    return $installerPath
}

function New-MsiInstaller {
    param(
        [string]$JPackagePath,
        [string]$ProjectRoot,
        [string]$InputDirectory,
        [string]$InstallerOutput,
        [string]$AppName,
        [string]$AppVersion,
        [string]$MainJarName,
        [string]$MainClass,
        [string]$Vendor,
        [string]$Description,
        [string]$UpgradeUuid,
        [string]$IconPath
    )

    $candleCommand = Get-Command candle.exe -ErrorAction SilentlyContinue
    $lightCommand = Get-Command light.exe -ErrorAction SilentlyContinue
    if ($null -eq $candleCommand -or $null -eq $lightCommand) {
        throw "WiX Toolset v3 nao encontrado. Para gerar MSI, deixe candle.exe e light.exe no PATH."
    }

    if (Test-Path $InstallerOutput) {
        Remove-Item $InstallerOutput -Recurse -Force
    }
    New-Item -ItemType Directory -Path $InstallerOutput -Force | Out-Null

    $jpackageArguments = @(
        "--type", "msi",
        "--dest", $InstallerOutput,
        "--input", $InputDirectory,
        "--name", $AppName,
        "--main-jar", $MainJarName,
        "--main-class", $MainClass,
        "--app-version", $AppVersion,
        "--vendor", $Vendor,
        "--description", $Description,
        "--icon", $IconPath,
        "--java-options", "-Dfile.encoding=UTF-8",
        "--java-options", "-Dprism.order=d3d,sw",
        "--java-options", "--module-path",
        "--java-options", '$APPDIR',
        "--java-options", "--add-modules",
        "--java-options", "javafx.controls,javafx.fxml",
        "--install-dir", $AppName,
        "--win-dir-chooser",
        "--win-menu",
        "--win-shortcut",
        "--win-per-user-install",
        "--win-upgrade-uuid", $UpgradeUuid
    )

    Push-Location $ProjectRoot
    try {
        Invoke-ExternalCommand -Executable $JPackagePath -Arguments $jpackageArguments -FailureMessage "Falha ao gerar o instalador MSI com jpackage."
    } finally {
        Pop-Location
    }

    $artifact = Get-ChildItem -Path $InstallerOutput -File | Where-Object { $_.Extension -eq ".msi" } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($null -eq $artifact) {
        throw "Instalador MSI nao encontrado em $InstallerOutput"
    }

    return $artifact.FullName
}

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$inputDir = Join-Path $projectRoot "target\jpackage\input"
$distributionRoot = Join-Path $projectRoot "dist\windows"
$intermediatePackagingRoot = Join-Path $projectRoot "target\packaging"
$appImageOutput = if ($PackageType -eq "exe") { Join-Path $intermediatePackagingRoot "app-image" } else { Join-Path $distributionRoot "app-image" }
$installerOutput = if ($PackageType -eq "exe") { $distributionRoot } else { Join-Path $distributionRoot "installer" }
$appName = "EstoqueTI Desktop"
$mainJarName = "estoqueti-desktop-1.0.0-SNAPSHOT.jar"
$mainJarPath = Join-Path $projectRoot ("target\" + $mainJarName)
$mainClass = "br.com.estoqueti.AppLauncher"
$vendor = "EstoqueTI"
$description = "Sistema desktop para controle de estoque de equipamentos de TI"
$upgradeUuid = "8d8d8f5a-17f6-47b5-bb80-5c9a989c2c46"
$appIconPath = Join-Path $projectRoot "scripts\windows-installer\assets\logo-nitrolux.ico"

if (Test-Path $inputDir) {
    Remove-Item $inputDir -Recurse -Force
}
New-Item -ItemType Directory -Path $inputDir -Force | Out-Null

if ($PackageType -eq "exe") {
    if (Test-Path $distributionRoot) {
        Get-ChildItem -Path $distributionRoot -Force | Remove-Item -Recurse -Force
    }
    if (Test-Path $appImageOutput) {
        Remove-Item $appImageOutput -Recurse -Force
    }
}

New-Item -ItemType Directory -Path $distributionRoot -Force | Out-Null

$mavenPath = Resolve-MavenPath
$jpackagePath = Resolve-JPackagePath
$repositoryArgument = Resolve-MavenRepositoryArgument -ProjectRoot $projectRoot
Invoke-MavenBuild -MavenPath $mavenPath -ProjectRoot $projectRoot -SkipTestsFlag $SkipTests.IsPresent -RepositoryArgument $repositoryArgument
if (-not (Test-Path $appIconPath)) {
    throw "Icone Windows nao encontrado em $appIconPath"
}

$configInputDir = Join-Path $inputDir "config"
$configTemplatePath = Join-Path $projectRoot "src\main\resources\application-local.properties.example"
$configOverrideCandidates = @(
    (Join-Path $projectRoot "config\application-local.properties"),
    (Join-Path $projectRoot "application-local.properties")
)
New-Item -ItemType Directory -Path $configInputDir -Force | Out-Null
$configOverridePath = $configOverrideCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if ($configOverridePath) {
    Copy-Item $configOverridePath (Join-Path $configInputDir "application-local.properties") -Force
}
if (Test-Path $configTemplatePath) {
    Copy-Item $configTemplatePath (Join-Path $configInputDir "application-local.properties.example") -Force
}

if (-not (Test-Path $mainJarPath)) {
    throw "Jar principal nao encontrado em $mainJarPath"
}

Copy-Item $mainJarPath $inputDir -Force
$appImageExecutable = New-AppImage -JPackagePath $jpackagePath -ProjectRoot $projectRoot -InputDirectory $inputDir -AppImageOutput $appImageOutput -AppName $appName -AppVersion $AppVersion -MainJarName $mainJarName -MainClass $mainClass -Vendor $vendor -Description $description -IconPath $appIconPath

if ($PackageType -eq "app-image") {
    Write-Host "App-image gerado com sucesso em: $appImageExecutable"
    return
}

if ($PackageType -eq "msi") {
    $msiPath = New-MsiInstaller -JPackagePath $jpackagePath -ProjectRoot $projectRoot -InputDirectory $inputDir -InstallerOutput $installerOutput -AppName $appName -AppVersion $AppVersion -MainJarName $mainJarName -MainClass $mainClass -Vendor $vendor -Description $description -UpgradeUuid $upgradeUuid -IconPath $appIconPath
    Write-Host "Instalador MSI gerado com sucesso em: $msiPath"
    return
}

$installerPath = New-CustomInstaller -ProjectRoot $projectRoot -AppImageOutput $appImageOutput -InstallerOutput $installerOutput -AppName $appName -AppVersion $AppVersion -Vendor $vendor -IconPath $appIconPath
if (Test-Path $appImageOutput) {
    Remove-Item $appImageOutput -Recurse -Force
}
Write-Host "Instalador EXE gerado com sucesso em: $installerPath"



