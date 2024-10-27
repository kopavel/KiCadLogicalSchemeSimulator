@@echo off

java -Xmx2g ^
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager ^
-XX:+UseParallelGC -XX:CompileThreshold=10 -XX:ParallelGCThreads=1 -XX:MaxInlineSize=64 -XX:MaxInlineLevel=5 ^
-p %~dp0;%~dp0lib;%~dp0schemaParts; ^
--add-opens java.base/java.lang=KiCadLogicalSchemeSimulator.simulator ^
--patch-module KiCadLogicalSchemeSimulator.simulator=%~dp0optimised ^
-m KiCadLogicalSchemeSimulator.simulator/pko.KiCadLogicalSchemeSimulator.Simulator -od=%~dp0optimised -md=%~dp0 %*
:: Check the exit code from Java application
if %ERRORLEVEL% equ 0 (
    exit
) else (
    pause
)