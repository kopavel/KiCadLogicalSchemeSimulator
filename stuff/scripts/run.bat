@@echo off

java -Xms16m -Xmx2g ^
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager ^
-XX:CompileThreshold=10 ^
-XX:MaxInlineSize=65536 ^
-Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector ^
-Dlog4j2.asyncLoggerWaitStrategy=Block ^
-Dlog4j2.asyncLoggerThreadNameStrategy=CACHED ^
-p %~dp0;%~dp0lib;%~dp0schemaParts; ^
--patch-module KiCadLogicalSchemeSimulator.simulator=optimised ^
-m KiCadLogicalSchemeSimulator.simulator/pko.KiCadLogicalSchemeSimulator.Simulator -m=%~dp0SymbolsDescription.xml %*
:: Check the exit code from Java application
if %ERRORLEVEL% equ 0 (
    exit
) else (
    pause
)