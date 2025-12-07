param(
    [Parameter(ValueFromRemainingArguments=$true)]
    [String[]]$Args
)

# Resolve java.exe from PATH
try {
    $javaCmd = (Get-Command java -ErrorAction Stop).Source
} catch {
    Write-Error "java not found in PATH. Please install JDK or add java to PATH."
    exit 1
}

# Determine JAVA_HOME by taking parent of 'bin' directory
$javaBin = Split-Path -Parent $javaCmd
$javaHome = Split-Path -Parent $javaBin

if (-not (Test-Path $javaHome)) {
    Write-Error "Resolved JAVA_HOME does not exist: $javaHome"
    exit 1
}

Write-Host "Using JAVA_HOME: $javaHome"
$env:JAVA_HOME = $javaHome

# Forward args to gradlew.bat in repo root
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$gradleWrapper = Join-Path $repoRoot '..\gradlew.bat'
$gradleWrapper = [System.IO.Path]::GetFullPath($gradleWrapper)

if (-not (Test-Path $gradleWrapper)) {
    Write-Error "gradlew.bat not found at $gradleWrapper"
    exit 1
}

$argString = $Args -join ' '
& $gradleWrapper $Args

exit $LASTEXITCODE
