@echo off
ping -n 121 127.0.0.1 >nul
echo ===== compose_up.log (tail) =====
powershell -Command "Get-Content c:\pycharmwork\EntRAG\compose_up.log -Tail 12"
echo.
echo ===== docker ps =====
"C:\Users\xiezhenyu\AppData\Local\Programs\DockerDesktop\resources\bin\docker.exe" ps -a
