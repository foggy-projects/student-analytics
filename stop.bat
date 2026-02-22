@echo off
chcp 65001 >nul 2>&1
echo ========================================
echo  Student Analytics - Stop
echo ========================================
echo.

echo [1/2] Stopping Spring Boot on port 8090...
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":8090 " ^| findstr "LISTENING"') do (
    echo        Killing PID %%a
    taskkill /PID %%a /F >nul 2>&1
)
echo        Done.
echo.

echo [2/2] Stopping MySQL Docker...
docker compose -f "%~dp0docker\docker-compose.yml" down
echo        Done.
echo.

echo ========================================
echo  All services stopped.
echo ========================================
pause
