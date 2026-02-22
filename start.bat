@echo off
chcp 65001 >nul 2>&1
echo ========================================
echo  Student Analytics - Start
echo ========================================
echo.

echo [1/2] Starting MySQL Docker...
docker compose -f "%~dp0docker\docker-compose.yml" up -d
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Docker failed. Is Docker Desktop running?
    pause
    exit /b 1
)
echo.

echo [2/2] Waiting for MySQL...
:wait_mysql
docker exec student-analytics-mysql mysqladmin ping -uroot -proot123 --silent >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo        Waiting...
    timeout /t 3 /nobreak >nul
    goto wait_mysql
)
echo        MySQL ready!
echo.

echo ========================================
echo  MySQL:  localhost:3307 (root/root123)
echo  App:    http://localhost:8090
echo ========================================
echo.

cd /d "%~dp0"
call mvn spring-boot:run -DskipTests
