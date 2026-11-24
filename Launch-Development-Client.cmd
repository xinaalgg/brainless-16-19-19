@echo off
setlocal
title Brainless 16/19/19 - RuneLite development client
set "CLIENT_CACHE=%USERPROFILE%\.runelite\repository2"
set "DEV_JAVA=%LOCALAPPDATA%\RuneLite\jre\bin\java.exe"
if not exist "%DEV_JAVA%" set "DEV_JAVA=java.exe"
for %%F in (client-1.13.8.jar runelite-api-1.13.8-runtime.jar injected-client-1.13.8.jar) do (
  if not exist "%CLIENT_CACHE%\%%F" (
    echo RuneLite 1.13.8 dependency %%F is missing. See the Gradle launch in README.md.
    pause
    exit /b 1
  )
)
set "DEV_CP=%~dp0build\libs\brainless-16-19-19-dev.jar;%~dp0build\libs\brainless-16-19-19-0.1.0.jar;%CLIENT_CACHE%\client-1.13.8.jar;%CLIENT_CACHE%\runelite-api-1.13.8-runtime.jar;%CLIENT_CACHE%\injected-client-1.13.8.jar;%CLIENT_CACHE%\*"
"%DEV_JAVA%" -ea -cp "%DEV_CP%" dev.tempoross.assist.DevelopmentClient --developer-mode
if errorlevel 1 pause
