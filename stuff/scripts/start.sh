#!/bin/bash

# Change to the directory where the script is located
cd "$(dirname "$0")"

# Determine the best available terminal emulator
if [ -n "$TERMINAL" ]; then
    term_cmd="$TERMINAL"
elif command -v x-terminal-emulator >/dev/null 2>&1; then
    term_cmd="x-terminal-emulator"
elif command -v gnome-terminal >/dev/null 2>&1; then
    term_cmd="gnome-terminal"
elif command -v konsole >/dev/null 2>&1; then
    term_cmd="konsole"
elif command -v xterm >/dev/null 2>&1; then
    term_cmd="xterm"
else
    echo "No known terminal emulator found. Please install one."
    echo "Attempted to detect: TERMINAL variable, x-terminal-emulator, gnome-terminal, konsole, xterm."
    echo "Press any key to continue..."
    read -n 1 -s -r
    exit 1
fi

# Run the script 'run.sh' in the chosen terminal emulator
$term_cmd -e "bash -c 'LANG=en_US.UTF-8; ./run.sh $*; exec bash'"
