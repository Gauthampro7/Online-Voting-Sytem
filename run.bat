@echo off
SET MYSQL_JAR=C:\Users\gauth\.m2\repository\com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar
SET CLASSES=target\classes

echo ======================================
echo   Online National Polling System
echo ======================================
echo.
java -cp "%MYSQL_JAR%;%CLASSES%" com.dsu.onlinevoting.OnlineVotingApp
pause
