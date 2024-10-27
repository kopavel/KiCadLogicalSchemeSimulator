#!/bin/bash

# Store the directory where the script is located
SCRIPT_DIR="$(dirname "$0")"

# Execute Java with the specified options
java -Xmx2g \
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager \
-XX:+UseParallelGC -XX:CompileThreshold=10 -XX:ParallelGCThreads=1 -XX:MaxInlineSize=64 -XX:MaxInlineLevel=5 \
--add-opens java.base/java.lang=KiCadLogicalSchemeSimulator.simulator \
-p "${SCRIPT_DIR}":"${SCRIPT_DIR}/lib":"${SCRIPT_DIR}/schemaParts" \
--patch-module KiCadLogicalSchemeSimulator.simulator="${SCRIPT_DIR}/optimised" \
-m KiCadLogicalSchemeSimulator.simulator/pko.KiCadLogicalSchemeSimulator.Simulator -od="${SCRIPT_DIR}/optimised" -md="${SCRIPT_DIR}" "$@"

# Check the exit code from Java application
if [ $? -eq 0 ]; then
	exit 0
else
	read -p "Press any key to continue . . . " -n1 -s
	exit 1
fi
