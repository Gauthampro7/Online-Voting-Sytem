@echo off
echo Compiling Online Voting App...
if not exist "out" mkdir out
javac -cp "lib\mysql-connector-j.jar" -d out src\com\dsu\onlinevoting\OnlineVotingApp.java
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Compilation complete. The compiled files are in the 'out' folder.
) else (
    echo [ERROR] Compilation failed.
)
pause
