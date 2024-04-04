#!/bin/bash

# Set the current directory to where the script is located
cd "$(dirname "$0")"

# Execute Java with the specified options
java -Xms16m -Xmx2g \
-XX:+UseG1GC -XX:MaxHeapFreeRatio=25 -XX:MinHeapFreeRatio=15 -XX:G1PeriodicGCInterval=15000 \
-XX:+UseStringDeduplication \
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager \
-Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector \
-Dlog4j2.asyncLoggerWaitStrategy=Block \
-Dlog4j2.asyncLoggerThreadNameStrategy=CACHED \
-p "$(pwd)":"$(pwd)"/lib:"$(pwd)"/schemaParts \
-m DigitalNetSimulator.simulator/lv.pko.DigitalNetSimulator.Simulator -m="$(pwd)"/SymbolsDescription.xml "$@"

# Check the exit code from Java application
if [ $? -eq 0 ]; then
    exit 0
else
    read -p "Press any key to continue . . . " -n1 -s
    exit 1
fi
