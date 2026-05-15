@echo off
echo ========================================================
echo       Blood Donor Management System - Startup Script      
echo ========================================================
echo.
echo [INFO] Checking for MySQL Database requirements...
echo Please ensure you have created a MySQL database named 'blood_db'.
echo Please ensure your username is 'root' and password is 'root@123'.
echo If your credentials differ, update src\main\resources\application.properties
echo.
echo [INFO] Starting Spring Boot application on port 8081...
echo [INFO] The application will automatically download dependencies.
echo.
call .\mvnw.cmd clean spring-boot:run
pause
