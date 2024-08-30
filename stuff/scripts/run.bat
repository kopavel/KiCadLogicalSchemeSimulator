@@echo off

java -Xmx2g ^
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager ^
-XX:+UseParallelGC -XX:CompileThreshold=10 -XX:ParallelGCThreads=1 -XX:MaxInlineSize=64 -XX:MaxInlineLevel=14 ^
-p %~dp0;%~dp0lib;%~dp0schemaParts; ^
--patch-module KiCadLogicalSchemeSimulator.simulator=%~dp0optimised ^
-m KiCadLogicalSchemeSimulator.simulator/pko.KiCadLogicalSchemeSimulator.Simulator -od=%~dp0optimised -m=%~dp0kicad.sym_map -m=%~dp0chip.sym_map %*
:: Check the exit code from Java application
if %ERRORLEVEL% equ 0 (
    exit
) else (
    pause
)