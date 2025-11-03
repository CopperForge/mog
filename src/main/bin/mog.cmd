@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem Resolve MOG_HOME to the parent of this script if not set
set "_BIN_DIR=%~dp0"
for %%I in ("%_BIN_DIR%..") do set "_MOG_HOME_DEFAULT=%%~fI"
if not defined MOG_HOME set "MOG_HOME=%_MOG_HOME_DEFAULT%"

set "JAR=%MOG_HOME%\lib\mog.jar"
if not exist "%JAR%" (
  echo ERROR: %JAR% not found. Set MOG_HOME to your MOG install root.
  exit /b 1
)

rem Usage text
if "%~1"=="" goto :usage
if "%~1"=="-h" goto :usage
if "%~1"=="--help" goto :usage
goto :run

:usage
echo MOG - It is what you make it
echo.
echo Usage:
echo   mog [options] ^<command^> [subcommand args]
echo.
echo Common options (forwarded to the app):
echo   --config=path        Path to site config ^(defaults to %%MOG_HOME%%\\etc\\config.mog^)
echo   --working-dir=path   Working directory for resolving relative paths
echo   --datasources=path   Datasources catalog file or directory
echo   --env=name           Environment/profile name for config resolution
echo.
echo Wrapper env vars:
echo   MOG_HOME        Install root ^(auto-detected if unset^)
echo   MOG_JAVA_OPTS   Extra JVM options ^(appended^)
echo   MOG_HEAP_MB     Set both Xms/Xmx ^(MB^). Overrides auto-sizing
echo.
rem fallthrough to run; app will also print detailed help

:run
rem Java detection
set "JAVA_EXE="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE for /f %%J in ('where java 2^>NUL') do set "JAVA_EXE=%%J"
if not defined JAVA_EXE (
  echo ERROR: java not found. Set JAVA_HOME or add java to PATH.
  exit /b 1
)

rem CPU and memory sizing
set "CPUS=%NUMBER_OF_PROCESSORS%"
for /f "tokens=2 delims==" %%M in ('wmic OS get TotalVisibleMemorySize /Value 2^>NUL') do set "MEM_KB=%%M"
if not defined MEM_KB set "MEM_KB=2097152"
set /a MEM_MB=MEM_KB/1024
if defined MOG_HEAP_MB (
  set /a HEAP_MB=%MOG_HEAP_MB%
) else (
  set /a HEAP_MB=MEM_MB/4
  if %HEAP_MB% LSS 512 set HEAP_MB=512
  if %HEAP_MB% GTR 4096 set HEAP_MB=4096
)

set "DEFAULT_JVM_OPTS=-Xms%HEAP_MB%m -Xmx%HEAP_MB%m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication -XX:ActiveProcessorCount=%CPUS% -Dfile.encoding=UTF-8 -XX:+ExitOnOutOfMemoryError"

rem Optional log level override (INFO by default). Example: set MOG_LOG_LEVEL=DEBUG
if defined MOG_LOG_LEVEL set "DEFAULT_JVM_OPTS=%DEFAULT_JVM_OPTS% -Dmog.log.level=%MOG_LOG_LEVEL%"

"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %MOG_JAVA_OPTS% -jar "%JAR%" %*
endlocal
goto :eof
