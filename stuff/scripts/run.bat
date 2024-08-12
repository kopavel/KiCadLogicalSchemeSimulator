@@echo off

java -Xms16m -Xmx2g ^
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager ^
-XX:CompileThreshold=100 -XX:ParallelGCThreads=2 ^
-p %~dp0;%~dp0lib;%~dp0schemaParts; ^
--patch-module KiCadLogicalSchemeSimulator.simulator=optimised ^
-m KiCadLogicalSchemeSimulator.simulator/pko.KiCadLogicalSchemeSimulator.Simulator -m=%~dp0SymbolsDescription.xml %*
:: Check the exit code from Java application
if %ERRORLEVEL% equ 0 (
    exit
) else (
    pause
)