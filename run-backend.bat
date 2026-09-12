@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d c:\pycharmwork\EntRAG\backend
set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=enterprise_rag
set DB_USER=rag
set DB_PASSWORD=rag123456
set OLLAMA_BASE_URL=http://localhost:11434
set RAG_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
java -jar target/enterprise-rag.jar > c:\pycharmwork\EntRAG\backend_run.log 2>&1
