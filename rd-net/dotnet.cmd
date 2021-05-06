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
        DOTNET_URL=https://cache-redirector.jetbrains.com/download.visualstudio.microsoft.com/download/pr/56da864b-1efd-46a6-b761-b285283696cb/1ad897dff863fdd3cdbaf8a447b09317/dotnet-sdk-5.0.203-osx-x64.tar.gz
        DOTNET_TARGET_DIR=$TARGET_DIR/dotnet-sdk-5.0.203-osx-x64-$SCRIPT_VERSION
        ;;
      arm64)
        DOTNET_URL=https://cache-redirector.jetbrains.com/download.visualstudio.microsoft.com/download/pr/e0a3877f-1b66-4e60-81f5-9b5d0769f60d/432bc7949c2d5b15ce218bf62e93520a/dotnet-sdk-6.0.100-preview.3.21202.5-osx-arm64.tar.gz
        DOTNET_TARGET_DIR=$TARGET_DIR/dotnet-sdk-6.0.100-preview.3.21202.5-osx-arm64-$SCRIPT_VERSION
        ;;
      *)
        echo "Unknown architecture $(uname -m)" >&2; exit 1
        ;;
    esac
else
    case $(uname -m) in
      x86_64)
        DOTNET_URL=https://cache-redirector.jetbrains.com/download.visualstudio.microsoft.com/download/pr/ef13f9da-46dc-4de9-a05e-5a4c20574189/be95913ebf1fb6c66833ca40060d3f65/dotnet-sdk-5.0.203-linux-x64.tar.gz
        DOTNET_TARGET_DIR=$TARGET_DIR/dotnet-sdk-5.0.203-linux-x64-$SCRIPT_VERSION
        ;;
      aarch64)
        DOTNET_TEMP_FILE=$TARGET_DIR/dotnet-sdk-5.0.203-linux-arm64.tar.gz
        DOTNET_URL=https://cache-redirector.jetbrains.com/download.visualstudio.microsoft.com/download/pr/af5f1e5b-d544-47af-b730-038e4258641b/bccb3982f5690134ab66748a5afc36c7/dotnet-sdk-5.0.203-linux-arm64.tar.gz
        DOTNET_TARGET_DIR=$TARGET_DIR/dotnet-sdk-5.0.203-linux-arm64-$SCRIPT_VERSION
        ;;
      *)
        echo "Unknown architecture $(uname -m)" >&2; exit 1
        ;;
    esac
fi

set -e

if [ -e "$DOTNET_TARGET_DIR/.flag" ] && [ -n "$(ls "$DOTNET_TARGET_DIR")" ] && [ "x$(cat "$DOTNET_TARGET_DIR/.flag")" = "x${DOTNET_URL}" ]; then
    # Everything is up-to-date in $DOTNET_TARGET_DIR, do nothing
    true
else
  warn "Downloading $DOTNET_URL to $DOTNET_TEMP_FILE"

  rm -f "$DOTNET_TEMP_FILE"
  mkdir -p "$TARGET_DIR"
  if command -v curl >/dev/null 2>&1; then
      if [ -t 1 ]; then CURL_PROGRESS="--progress-bar"; else CURL_PROGRESS="--silent --show-error"; fi
      curl -L $CURL_PROGRESS --output "${DOTNET_TEMP_FILE}" "$DOTNET_URL"
  elif command -v wget >/dev/null 2>&1; then
      if [ -t 1 ]; then WGET_PROGRESS=""; else WGET_PROGRESS="-nv"; fi
      wget $WGET_PROGRESS -O "${DOTNET_TEMP_FILE}" "$DOTNET_URL"
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

if [ '!' -e "$DOTNET_TARGET_DIR/dotnet" ]; then
  die "Unable to find dotnet under $DOTNET_TARGET_DIR"
fi

exec "$DOTNET_TARGET_DIR/dotnet" "$@"

:CMDSCRIPT

setlocal
set SCRIPT_VERSION=dotnet-cmd-v1
set COMPANY_NAME=JetBrains
set TARGET_DIR=%LOCALAPPDATA%\Temp\%COMPANY_NAME%\
set DOTNET_TARGET_DIR=%TARGET_DIR%dotnet-sdk-5.0.203-win-x64-%SCRIPT_VERSION%\
set DOTNET_TEMP_FILE=dotnet-sdk-temp.zip
set DOTNET_URL=https://cache-redirector.jetbrains.com/download.visualstudio.microsoft.com/download/pr/46b35cfe-4b3f-4e69-8831-0937196699b1/221f862c003a0175722c131b0941e3c4/dotnet-sdk-5.0.203-win-x64.zip


set POWERSHELL=%SystemRoot%\system32\WindowsPowerShell\v1.0\powershell.exe

if not exist "%DOTNET_TARGET_DIR%" MD "%DOTNET_TARGET_DIR%"

if not exist "%DOTNET_TARGET_DIR%.flag" goto downloadAndExtractDotNet

set /p CURRENT_FLAG=<"%DOTNET_TARGET_DIR%.flag"
if "%CURRENT_FLAG%" == "%DOTNET_URL%" goto continueWithDotNet

:downloadAndExtractDotNet

cd /d "%TARGET_DIR%"
if errorlevel 1 goto fail

echo Downloading %DOTNET_URL% to %TARGET_DIR%%DOTNET_TEMP_FILE%
if exist "%DOTNET_TEMP_FILE%" DEL /F "%DOTNET_TEMP_FILE%"
"%POWERSHELL%" -nologo -noprofile -Command "Set-StrictMode -Version 3.0; $ErrorActionPreference = \"Stop\"; (New-Object Net.WebClient).DownloadFile('%DOTNET_URL%', '%DOTNET_TEMP_FILE%')"
if errorlevel 1 goto fail

rmdir /S /Q "%DOTNET_TARGET_DIR%"
if errorlevel 1 goto fail

mkdir "%DOTNET_TARGET_DIR%"
if errorlevel 1 goto fail

cd /d %DOTNET_TARGET_DIR%"
if errorlevel 1 goto fail

echo Extracting %TARGET_DIR%%DOTNET_TEMP_FILE% to %DOTNET_TARGET_DIR%
"%POWERSHELL%" -nologo -noprofile -command "Set-StrictMode -Version 3.0; $ErrorActionPreference = \"Stop\"; Add-Type -A 'System.IO.Compression.FileSystem'; [IO.Compression.ZipFile]::ExtractToDirectory('..\\%DOTNET_TEMP_FILE%', '.');"
if errorlevel 1 goto fail

del /F "..\%DOTNET_TEMP_FILE%"
if errorlevel 1 goto fail

echo %DOTNET_URL%>"%DOTNET_TARGET_DIR%.flag"
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