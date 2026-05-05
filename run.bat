@echo off
if not exist "out\com\dsu\onlinevoting\OnlineVotingApp.class" (
    echo Compiled files not found. Please run compile.bat first.
    pause
    exit /b
)
echo Starting Online Voting App...
java -cp "lib\mysql-connector-j.jar;out" com.dsu.onlinevoting.OnlineVotingApp
pause
