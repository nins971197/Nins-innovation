@echo off
set JAVA_HOME=C:\Users\26409\AppData\Roaming\.minecraft\runtime\java-runtime-beta
set PATH=%JAVA_HOME%\bin;%PATH%
call gradlew.bat build
pause
