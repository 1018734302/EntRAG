@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot
set PATH=%JAVA_HOME%\bin;C:\tools\maven\apache-maven-3.9.9\bin;%PATH%
cd /d c:\pycharmwork\EntRAG\backend
mvn -s C:\tools\settings.xml -B package -DskipTests > c:\pycharmwork\EntRAG\backend_build.log 2>&1
