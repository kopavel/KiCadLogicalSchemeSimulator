#!/bin/bash

# Store the directory where the script is located
SCRIPT_DIR="$(dirname "$0")"

# Execute Java with the specified options
java -Xms16m -Xmx2g \
-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager \
-XX:CompileThreshold=100 -XX:ParallelGCThreads=2 \
-p "${SCRIPT_DIR}":"${SCRIPT_DIR}/lib":"${SCRIPT_DIR}/schemaParts" \
--patch-module KiCadLogicalSchemeSimulator.simulator=optimised \
-m KiCadLogicalSchemeSimulator.simulator/pko.KiCadLogicalSchemeSimulator.Simulator -m="${SCRIPT_DIR}/SymbolsDescription.xml" "$@"

# Check the exit code from Java application
if [ $? -eq 0 ]; then
	exit 0
else
	read -p "Press any key to continue . . . " -n1 -s
	exit 1
fi
