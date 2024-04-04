@@echo off

java -Xms16m -Xmx2g ^
-XX:+UseG1GC -XX:MaxHeapFreeRatio=25 -XX:MinHeapFreeRatio=15 -XX:G1PeriodicGCInterval=15000 ^
-XX:+UseStringDeduplication ^
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager ^
-Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector ^
-Dlog4j2.asyncLoggerWaitStrategy=Block ^
-Dlog4j2.asyncLoggerThreadNameStrategy=CACHED ^
-p %~dp0;%~dp0lib;%~dp0schemaParts; ^
-m DigitalNetSimulator.simulator/lv.pko.DigitalNetSimulator.Simulator -m=%~dp0SymbolsDescription.xml %*
:: Check the exit code from Java application
if %ERRORLEVEL% equ 0 (
    exit
) else (
    pause
)