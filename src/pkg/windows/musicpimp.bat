

@REM App launcher script
@REM
@REM Environment:
@REM JAVA_HOME - location of a JDK home dir (mandatory)
@REM APP_OPTS  - JVM options (optional)

@setlocal
@echo off

set APP_HOME=%~dp0
rem APP_HOME ends with a backslash, which escapes the quote unless we add another backslash
set APP_OPTS=-Dlog.dir="%SystemDrive%\ProgramData\MusicPimp\logs" -Dmusicpimp.home="%SystemDrive%\ProgramData\MusicPimp\" -Dlogger.resource=prod-logger-win.xml
set ERROR_CODE=0
set APP_JAR=musicpimp.jar
set MAIN_CLASS=com.mle.musicpimp.Starter

rem We use the value of the JAVACMD environment variable if defined
set _JAVACMD=%JAVACMD%
if "%1" == "stop" (set ARG=stop) else (set ARG=start)
if "%1" == "silent" (set ARG=silent)
if "%ARG%" == "stop" (set JAVANAME=java) else (set JAVANAME=java)
if "%_JAVACMD%"=="" (
    if not "%JAVA_HOME%"=="" (
        if exist "%JAVA_HOME%\bin\%JAVANAME%.exe" set "_JAVACMD=%JAVA_HOME%\bin\%JAVANAME%.exe"
    )
)
if "%_JAVACMD%"=="" set _JAVACMD=%JAVANAME%

rem We use the value of the JAVA_OPTS environment variable if defined
set _JAVA_OPTS=%JAVA_OPTS%
if "%_JAVA_OPTS%"=="" set _JAVA_OPTS=-Xmx512M -XX:ReservedCodeCacheSize=128m

set RUNCMD1="%_JAVACMD%" %_JAVA_OPTS% %APP_OPTS% -cp "%APP_HOME%lib/*" %MAIN_CLASS% %ARG% %*
set RUNCMD2=%_JAVACMD% %_JAVA_OPTS% %APP_OPTS% -cp "%APP_HOME%lib/*" %MAIN_CLASS% %ARG% %*
:run

if "%ARG%" == "silent" (start /B %RUNCMD2%) else (%RUNCMD1%)

if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end

@endlocal

exit /B %ERROR_CODE%



