@echo off
echo Starting Database...
start /b "" "mariadb\mariadb-10.6.14-winx64\bin\mysqld.exe" --defaults-file="mariadb\mariadb-10.6.14-winx64\data\my.ini"
echo Database started. Wait a few seconds for it to fully load.
pause
