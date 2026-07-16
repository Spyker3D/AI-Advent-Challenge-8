@echo off
setlocal

rem The assistant and Gradle use UTF-8. Match the Windows terminal code page so
rem Russian questions, answers, and CLI labels are rendered correctly.
chcp 65001 >nul

rem %~dp0 always ends with a backslash. Remove it because a trailing backslash
rem immediately before Gradle's closing quote produces an unbalanced argument.
set "PROJECT_ROOT=%~dp0"
set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"

rem Build the application with Gradle, but do not run the interactive CLI through
rem Gradle: on Windows Gradle can transcode child-process output using the daemon's
rem old OEM code page and corrupt UTF-8 Russian text.
call "%~dp0gradlew.bat" --console=plain :developer-assistant:installDist
if errorlevel 1 exit /b %errorlevel%

call "%~dp0developer-assistant\build\install\developer-assistant\bin\developer-assistant.bat" "--project-root=%PROJECT_ROOT%" %*

endlocal
