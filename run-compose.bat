@echo off
set PATH=C:\Users\xiezhenyu\AppData\Local\Programs\DockerDesktop\resources\bin;%PATH%
cd /d c:\pycharmwork\EntRAG
docker.exe compose up -d --build postgres backend > compose_up.log 2>&1
