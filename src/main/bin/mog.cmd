@echo off
setlocal
if "%MOG_HOME%"=="" (
  echo MOG_HOME is not set. Please set MOG_HOME to your MOG installation directory.
  exit /b 1
)
java -jar "%MOG_HOME%\lib\mog.jar" %*
