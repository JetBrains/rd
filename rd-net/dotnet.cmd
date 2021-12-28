:<<"::CMDLITERAL"
@ECHO OFF
GOTO :CMDSCRIPT
::CMDLITERAL

SCRIPT_VERSION=dotnet-cmd-v1
COMPANY_DIR="JetBrains"
TARGET_DIR="${TEMPDIR:-/tmp}/$COMPANY_DIR"

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

function retry_on_error() {
  local n="$1"
  shift

  for i in $(seq 2 "$n"); do
    "$@" 2>&1 && return || echo "WARNING: Command '$1' returned non-zero exit status $?, try again"
  done
  "$@"
}

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac
DOTNET_TEMP_FILE=$TARGET_DIR/dotnet-sdk-temp.tar.gz
if [ "$darwin" = "true" ]; then
    case $(uname -m) in
      x86_64)
        DOTNET_URL=https://cache-redirector.jetbrains.com/download.visualstudio.microsoft.com/download/pr/62f78047-71de-460e-85ca-254f1fa848de/ecabeefdca2902f3f06819612cd9d45c/dotnet-sdk-6.0.100-osx-x64.tar.gz
        DOTNET_TARGET_DIR=$TARGET_DIR/dotnet-sdk-6.0.100-osx-x64-$SCRIPT_VERSION
        ;;
      arm64)
        DOTNET_URL=https://cache-redirector.jetbrains.com/download.visualstudio.microsoft.com/download/pr/7f1e67c2-11a4-416b-8421-786e47b82fdf/af56581d96e15ed911cf3a172f3c8802/dotnet-sdk-6.0.100-osx-arm64.tar.gz
        DOTNET_TARGET_DIR=$TARGET_DIR/dotnet-sdk-6.0.100-osx-arm64-$SCRIPT_VERSION
        ;;
      *)
        echo "Unknown architecture $(uname -m)" >&2; exit 1
        ;;
    esac
else
    case $(uname -m) in
      x86_64)
        DOTNET_URL=https://cache-redirector.jetbrains.com/download.visualstudio.microsoft.com/download/pr/17b6759f-1af0-41bc-ab12-209ba0377779/e8d02195dbf1434b940e0f05ae086453/dotnet-sdk-6.0.100-linux-x64.tar.gz
        DOTNET_TARGET_DIR=$TARGET_DIR/dotnet-sdk-6.0.100-linux-x64-$SCRIPT_VERSION
        ;;
      aarch64)
        DOTNET_URL=https://cache-redirector.jetbrains.com/download.visualstudio.microsoft.com/download/pr/adcd9310-5072-4179-9b8b-16563b897995/15a7595966f488c74909e4a9273c0e24/dotnet-sdk-6.0.100-linux-arm64.tar.gz
        DOTNET_TARGET_DIR=$TARGET_DIR/dotnet-sdk-6.0.100-linux-arm64-$SCRIPT_VERSION
        ;;
      *)
        echo "Unknown architecture $(uname -m)" >&2; exit 1
        ;;
    esac
fi

set -eu

if grep -q -x "$DOTNET_URL" "$DOTNET_TARGET_DIR/.flag" 2>/dev/null; then
  # Everything is up-to-date in $DOTNET_TARGET_DIR, do nothing
  true
else
while true; do  # Note(k15tfu): for goto
  mkdir -p "$TARGET_DIR"

  LOCK_FILE="$TARGET_DIR/.dotnet-cmd-lock.pid"
  TMP_LOCK_FILE="$TARGET_DIR/.tmp.$$.pid"
  echo $$ >"$TMP_LOCK_FILE"

  while ! ln "$TMP_LOCK_FILE" "$LOCK_FILE" 2>/dev/null; do
    LOCK_OWNER=$(cat "$LOCK_FILE" 2>/dev/null || true)
    while [ -n "$LOCK_OWNER" ] && ps -p $LOCK_OWNER >/dev/null; do
      warn "Waiting for the process $LOCK_OWNER to finish bootstrap dotnet.cmd"
      sleep 1
      LOCK_OWNER=$(cat "$LOCK_FILE" 2>/dev/null || true)

      # Hurry up, bootstrap is ready..
      if grep -q -x "$DOTNET_URL" "$DOTNET_TARGET_DIR/.flag" 2>/dev/null; then
        break 3  # Note(k15tfu): goto out of the outer if-else block.
      fi
    done

    if [ -n "$LOCK_OWNER" ] && grep -q -x $LOCK_OWNER "$LOCK_FILE" 2>/dev/null; then
      die "ERROR: The lock file $LOCK_FILE still exists on disk after the owner process $LOCK_OWNER exited"
    fi
  done

  trap "rm -f \"$LOCK_FILE\"" EXIT
  rm "$TMP_LOCK_FILE"

  if ! grep -q -x "$DOTNET_URL" "$DOTNET_TARGET_DIR/.flag" 2>/dev/null; then
    warn "Downloading $DOTNET_URL to $DOTNET_TEMP_FILE"

    rm -f "$DOTNET_TEMP_FILE"
    if command -v curl >/dev/null 2>&1; then
      if [ -t 1 ]; then CURL_PROGRESS="--progress-bar"; else CURL_PROGRESS="--silent --show-error"; fi
      retry_on_error 5 curl -L $CURL_PROGRESS --output "${DOTNET_TEMP_FILE}" "$DOTNET_URL"
    elif command -v wget >/dev/null 2>&1; then
      if [ -t 1 ]; then WGET_PROGRESS=""; else WGET_PROGRESS="-nv"; fi
      retry_on_error 5 wget $WGET_PROGRESS -O "${DOTNET_TEMP_FILE}" "$DOTNET_URL"
    else
      die "ERROR: Please install wget or curl"
    fi

    warn "Extracting $DOTNET_TEMP_FILE to $DOTNET_TARGET_DIR"
    rm -rf "$DOTNET_TARGET_DIR"
    mkdir -p "$DOTNET_TARGET_DIR"

    tar -x -f "$DOTNET_TEMP_FILE" -C "$DOTNET_TARGET_DIR"
    rm -f "$DOTNET_TEMP_FILE"

    echo "$DOTNET_URL" >"$DOTNET_TARGET_DIR/.flag"
  fi

  rm "$LOCK_FILE"
  break
done
fi

if [ ! -x "$DOTNET_TARGET_DIR/dotnet" ]; then
  die "Unable to find dotnet under $DOTNET_TARGET_DIR"
fi

exec "$DOTNET_TARGET_DIR/dotnet" "$@"

:CMDSCRIPT

setlocal
set SCRIPT_VERSION=dotnet-cmd-v1
set COMPANY_NAME=Mikhail Filippov
set TARGET_DIR=%LOCALAPPDATA%\%COMPANY_NAME%\dotnet-cmd\
set DOTNET_TARGET_DIR=%TARGET_DIR%dotnet-sdk-6.0.100-win-x64-%SCRIPT_VERSION%\
set DOTNET_TEMP_FILE=%TARGET_DIR%dotnet-sdk-temp.zip
set DOTNET_URL=https://cache-redirector.jetbrains.com/download.visualstudio.microsoft.com/download/pr/ca65b248-9750-4c2d-89e6-ef27073d5e95/05c682ca5498bfabc95985a4c72ac635/dotnet-sdk-6.0.100-win-x64.zip

set POWERSHELL=%SystemRoot%\system32\WindowsPowerShell\v1.0\powershell.exe

if not exist "%DOTNET_TARGET_DIR%.flag" goto downloadAndExtractDotNet

set /p CURRENT_FLAG=<"%DOTNET_TARGET_DIR%.flag"
if "%CURRENT_FLAG%" == "%DOTNET_URL%" goto continueWithDotNet

:downloadAndExtractDotNet

set DOWNLOAD_AND_EXTRACT_DOTNET_PS1= ^
Set-StrictMode -Version 3.0; ^
$ErrorActionPreference = 'Stop'; ^
 ^
$createdNew = $false; ^
$lock = New-Object System.Threading.Mutex($true, 'Global\dotnet-cmd-lock', [ref]$createdNew); ^
if (-not $createdNew) { ^
    Write-Host 'Waiting for the other process to finish bootstrap dotnet.cmd'; ^
    [void]$lock.WaitOne(); ^
} ^
 ^
try { ^
    if ((Get-Content '%DOTNET_TARGET_DIR%.flag' -ErrorAction Ignore) -ne '%DOTNET_URL%') { ^
        Write-Host 'Downloading %DOTNET_URL% to %DOTNET_TEMP_FILE%'; ^
        [void](New-Item '%TARGET_DIR%' -ItemType Directory -Force); ^
        (New-Object Net.WebClient).DownloadFile('%DOTNET_URL%', '%DOTNET_TEMP_FILE%'); ^
 ^
        Write-Host 'Extracting %DOTNET_TEMP_FILE% to %DOTNET_TARGET_DIR%'; ^
        if (Test-Path '%DOTNET_TARGET_DIR%') { ^
            Remove-Item '%DOTNET_TARGET_DIR%' -Recurse; ^
        } ^
        Add-Type -A 'System.IO.Compression.FileSystem'; ^
        [IO.Compression.ZipFile]::ExtractToDirectory('%DOTNET_TEMP_FILE%', '%DOTNET_TARGET_DIR%'); ^
        Remove-Item '%DOTNET_TEMP_FILE%'; ^
 ^
        Set-Content '%DOTNET_TARGET_DIR%.flag' -Value '%DOTNET_URL%'; ^
    } ^
} ^
finally { ^
    $lock.ReleaseMutex(); ^
}

"%POWERSHELL%" -nologo -noprofile -Command %DOWNLOAD_AND_EXTRACT_DOTNET_PS1%
if errorlevel 1 goto fail

:continueWithDotNet

if not exist "%DOTNET_TARGET_DIR%\dotnet.exe" (
  echo Unable to find dotnet.exe under %DOTNET_TARGET_DIR%
  goto fail
)

call "%DOTNET_TARGET_DIR%\dotnet.exe" %*
exit /B %ERRORLEVEL%
endlocal

:fail
echo "FAIL"
exit /b 1