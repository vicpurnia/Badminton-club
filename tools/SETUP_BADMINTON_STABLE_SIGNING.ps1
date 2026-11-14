# Badminton Premium v24 - one-time, owner-controlled release signing setup.
# Run on a trusted Windows PC with GitHub CLI and a JDK / Android Studio installed.
# Never put passwords or the private keystore in a Git repository or ChatGPT chat.
[CmdletBinding()]
param(
    [string]$Repository = "vicpurnia/Badminton-club",
    [string]$KeystorePath = "",
    [switch]$CheckJavaOnly
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$aliasName = "badminton-stable"
$pinVariable = "BADMINTON_EXPECTED_CERT_SHA256"

function Fail([string]$Message) { throw $Message }

function NativeCheck([string]$Action) {
    if ($LASTEXITCODE -ne 0) { Fail "$Action failed (exit code $LASTEXITCODE)." }
}

function SecureToText([Security.SecureString]$Value) {
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

function UploadSecret([string]$Name, [string]$Value) {
    # GitHub CLI reads from standard input. No password on the command line.
    # Explicit UTF-8 protects non-ASCII passwords from Windows PowerShell 5's ASCII pipe default.
    $priorEncoding = $OutputEncoding
    try {
        $OutputEncoding = [System.Text.UTF8Encoding]::new($false)
        $Value | & $script:gh secret set $Name -R $Repository -a actions
        NativeCheck "Setting GitHub secret $Name"
    } finally {
        $OutputEncoding = $priorEncoding
    }
    Write-Host "Configured secret: $Name" -ForegroundColor Green
}

Write-Host ""
Write-Host "BADMINTON PREMIUM - STABLE ANDROID SIGNING" -ForegroundColor Cyan
Write-Host "Repository: $Repository"
Write-Host "A permanent key will be created ONLY on this PC. It will not be uploaded to a public repository."
Write-Host ""

function Find-Keytool {
    # Find a JDK even when JAVA_HOME and PATH have not been updated.
    $found = Get-Command keytool.exe -ErrorAction SilentlyContinue
    if ($found -and $found.Source -and (Test-Path -LiteralPath $found.Source)) {
        return $found.Source
    }
    $direct = New-Object 'System.Collections.Generic.List[string]'
    foreach ($home in @(
        $env:JAVA_HOME,
        [Environment]::GetEnvironmentVariable("JAVA_HOME", "Machine"),
        [Environment]::GetEnvironmentVariable("JAVA_HOME", "User"),
        $env:JDK_HOME
    )) {
        if (-not [string]::IsNullOrWhiteSpace($home)) {
            $direct.Add((Join-Path $home "bin\keytool.exe"))
        }
    }
    $roots = New-Object 'System.Collections.Generic.List[string]'
    foreach ($base in @($env:ProgramFiles, [Environment]::GetFolderPath("ProgramFilesX86"), $env:LOCALAPPDATA)) {
        if ([string]::IsNullOrWhiteSpace($base)) { continue }
        foreach ($sub in @("Eclipse Adoptium", "Microsoft", "Java", "Amazon Corretto", "Zulu", "BellSoft", "Programs\Eclipse Adoptium", "Programs\Microsoft")) {
            $roots.Add((Join-Path $base $sub))
        }
        $direct.Add((Join-Path $base "Android\Android Studio\jbr\bin\keytool.exe"))
        $direct.Add((Join-Path $base "Android\Android Studio\jre\bin\keytool.exe"))
    }
    if ($env:USERPROFILE) {
        $roots.Add((Join-Path $env:USERPROFILE ".jdks"))
        $direct.Add((Join-Path $env:USERPROFILE "scoop\apps\temurin17-jdk\current\bin\keytool.exe"))
    }
    if ($env:ChocolateyInstall) {
        $roots.Add((Join-Path $env:ChocolateyInstall "lib"))
    }
    foreach ($root in $roots) {
        if (-not (Test-Path -LiteralPath $root -PathType Container)) { continue }
        foreach ($directory in @(Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue)) {
            $direct.Add((Join-Path $directory.FullName "bin\keytool.exe"))
            $direct.Add((Join-Path $directory.FullName "jbr\bin\keytool.exe"))
            $direct.Add((Join-Path $directory.FullName "tools\bin\keytool.exe"))
        }
    }
    foreach ($candidate in $direct) {
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            return $candidate
        }
    }
    return $null
}

# CI may test Java discovery without contacting GitHub or creating any signing key.
if ($CheckJavaOnly) {
    $located = Find-Keytool
    if (-not $located) { Fail "No JDK keytool found (check mode)." }
    Write-Host "JDK detected: $located" -ForegroundColor Green
    return
}

function Ensure-Keytool {
    $located = Find-Keytool
    if ($located) {
        Write-Host "Java keytool found: $located" -ForegroundColor Green
        return $located
    }
    Write-Host ""
    Write-Host "Java Development Kit (JDK) not found." -ForegroundColor Yellow
    Write-Host "A JDK is required once to create your permanent Android signing certificate."
    Write-Host "Recommended: free Eclipse Temurin 17 from https://adoptium.net/installation/"
    $wingetCommand = Get-Command winget.exe -ErrorAction SilentlyContinue
    if ($wingetCommand) {
        Write-Host ""
        Write-Host "Type INSTALL to approve Temurin 17 JDK installation using Windows Package Manager."
        Write-Host "Type SKIP to stop and install Java yourself. No change occurs unless you type INSTALL."
        $decision = Read-Host "Choice"
        if ($decision -cne "INSTALL") {
            Fail "Java installation skipped. No signing key or GitHub secret was changed."
        }
        Write-Host "Starting Microsoft winget installer. Approve Windows prompts only if you agree." -ForegroundColor Yellow
        & $wingetCommand.Source install --id EclipseAdoptium.Temurin.17.JDK --exact --source winget --accept-package-agreements --accept-source-agreements
        if ($LASTEXITCODE -ne 0) {
            Fail "JDK installation did not finish. Install Eclipse Temurin 17 JDK from https://adoptium.net/installation/, then rerun."
        }
        # An already-open PowerShell window may still have the old PATH.
        # Search common JDK installation directories without requiring a reboot.
        $located = Find-Keytool
        if ($located) {
            Write-Host "Java successfully installed; continuing signing setup." -ForegroundColor Green
            return $located
        }
    }
    Write-Host ""
    Write-Host "Install Eclipse Temurin 17 JDK from https://adoptium.net/installation/" -ForegroundColor Yellow
    Fail "Java keytool was not detected. Close this window, reopen the setup BAT after installation. No signing key has been created."
}

$ghCommand = Get-Command gh -ErrorAction SilentlyContinue
if (-not $ghCommand) {
    Fail "GitHub CLI is missing. Install from https://cli.github.com/ and rerun this script."
}
$gh = $ghCommand.Source
$keytool = Ensure-Keytool

# Owner authentication happens locally in their GitHub account.
$loggedIn = $false
try {
    & $gh auth status -h github.com *> $null
    $loggedIn = ($LASTEXITCODE -eq 0)
} catch {
    # Native stderr can throw under Windows PowerShell ErrorActionPreference=Stop.
    $loggedIn = $false
}
if (-not $loggedIn) {
    Write-Host "Please sign in to your GitHub account in the browser." -ForegroundColor Yellow
    & $gh auth login --hostname github.com --web
    NativeCheck "GitHub login"
}

# Fail closed if the repository already pins a different signing identity.
$variablesJson = & $gh variable list -R $Repository --json name,value
NativeCheck "Reading existing signing-certificate pin"
$variables = @($variablesJson | ConvertFrom-Json)
$currentPin = $variables | Where-Object { $_.name -eq $pinVariable } | Select-Object -First 1

if (-not $KeystorePath) {
    if (-not $env:LOCALAPPDATA) { Fail "LOCALAPPDATA is unavailable; use -KeystorePath." }
    $privateDir = Join-Path $env:LOCALAPPDATA "Badminton_Stable_Signing_Keep_Private"
    $KeystorePath = Join-Path $privateDir "badminton-stable.p12"
}
$KeystorePath = [IO.Path]::GetFullPath($KeystorePath)
$privateDir = Split-Path -Parent $KeystorePath
if (-not (Test-Path $privateDir)) { [void](New-Item -ItemType Directory -Path $privateDir -Force) }
$exists = Test-Path $KeystorePath

if ($currentPin -and -not $exists) {
    Fail "An approved certificate is already pinned in GitHub, but its local keystore is missing. Do NOT create a replacement: restore the original badminton-stable.p12 from your offline backup or use -KeystorePath."
}

Write-Host "Signing keystore location: $KeystorePath"
if ($exists) {
    Write-Host "Existing keystore found. It will NOT be overwritten." -ForegroundColor Green
} else {
    Write-Host "A NEW permanent signing key is about to be generated." -ForegroundColor Yellow
    $confirmNew = Read-Host "Type CREATE to generate it"
    if ($confirmNew -cne "CREATE") { Fail "Cancelled: no signing key created." }
}

$secure = Read-Host "Enter your permanent signing password (at least 14 characters)" -AsSecureString
$password = SecureToText $secure
if ($password.Length -lt 14) { Fail "Use a password with at least 14 characters." }
if (-not $exists) {
    $again = Read-Host "Repeat the permanent signing password" -AsSecureString
    $passwordConfirm = SecureToText $again
    if ($passwordConfirm -cne $password) { Fail "Passwords do not match. No key was created." }
    $passwordConfirm = $null
}

try {
    # -storepass:env avoids showing the password in process command-line arguments.
    $env:BADMINTON_LOCAL_PASSWORD = $password
    if (-not $exists) {
        $generateArgs = @("-genkeypair","-noprompt","-keystore",$KeystorePath,"-storetype","PKCS12","-alias",$aliasName,"-keyalg","RSA","-keysize","3072","-validity","10000","-dname","CN=Badminton Premium, OU=Vinayak, O=Vinayak, C=IN","-storepass:env","BADMINTON_LOCAL_PASSWORD","-keypass:env","BADMINTON_LOCAL_PASSWORD")
        & $keytool @generateArgs
        NativeCheck "Creating signing keystore"
    }

    $listArgs = @("-list","-v","-keystore",$KeystorePath,"-storetype","PKCS12","-alias",$aliasName,"-storepass:env","BADMINTON_LOCAL_PASSWORD")
    $certificate = & $keytool @listArgs 2>&1
    NativeCheck "Reading the signing certificate"
    $certificateText = ($certificate | Out-String)
    $match = [Regex]::Match($certificateText, 'SHA256:\s*([0-9a-fA-F:]+)')
    if (-not $match.Success) { Fail "Unable to locate a SHA-256 certificate fingerprint." }
    $fingerprint = $match.Groups[1].Value.Replace(":","").ToLowerInvariant()
    if ($fingerprint -notmatch '^[0-9a-f]{64}$') { Fail "Unexpected signing certificate fingerprint format." }

    if ($currentPin) {
        $existingValue = ([string]$currentPin.value).Trim().ToLowerInvariant()
        if ($existingValue -ne $fingerprint) {
            Fail "CRITICAL: The GitHub release certificate pin differs from this keystore. STOP. Restore the original key. No secrets have been changed."
        }
        Write-Host "Matches the already-pinned signing certificate." -ForegroundColor Green
    } else {
        Write-Host "This is the FIRST stable-signing setup." -ForegroundColor Yellow
    }
    Write-Host "Certificate SHA-256 (public identifier): $fingerprint" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Before proceeding, save an OFFLINE backup of the .p12 file and record your password in a password manager."
    Write-Host "Losing the .p12 file or password may prevent future upgrades and require another migration."
    Write-Host "Do not send either item through ChatGPT, email, or a public GitHub issue."
    $backupConfirm = Read-Host "After copying the file OFFLINE and securing the password, type BACKED UP"
    if ($backupConfirm -cne "BACKED UP") { Fail "Stopped before uploading secrets. Back up your key first." }

    # Pass values via STDIN to gh. The GitHub CLI encrypts each value client-side.
    $base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($KeystorePath))
    UploadSecret "BADMINTON_SIGNING_KEYSTORE_B64" $base64
    $base64 = $null
    UploadSecret "BADMINTON_KEYSTORE_PASSWORD" $password
    UploadSecret "BADMINTON_KEY_ALIAS" $aliasName
    UploadSecret "BADMINTON_KEY_PASSWORD" $password

    if (-not $currentPin) {
        $fingerprint | & $gh variable set $pinVariable -R $Repository
        NativeCheck "Pinning the stable signing certificate"
        Write-Host "Certificate pinned to the GitHub repository." -ForegroundColor Green
    }
    Write-Host ""
    Write-Host "SUCCESS: All signing secrets and the expected certificate pin are configured." -ForegroundColor Green
    $build = Read-Host "Type BUILD to launch your first stable v24 APK, or press Enter to exit"
    if ($build -ceq "BUILD") {
        & $gh workflow run build-native-v24-stable.yml -R $Repository --ref main
        NativeCheck "Triggering stable APK build"
        Write-Host "Build started. Visit: https://github.com/$Repository/actions/workflows/build-native-v24-stable.yml"
    }
} finally {
    Remove-Item Env:\BADMINTON_LOCAL_PASSWORD -ErrorAction SilentlyContinue
    $password = $null
    $secure = $null
}
