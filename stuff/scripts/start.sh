#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

run_cmd="bash -c 'export LANG=en_US.UTF-8; cd \"$SCRIPT_DIR\"; pwd; \"$SCRIPT_DIR/run.sh\" $*'"

# Detect terminal
if [ -n "$TERMINAL" ]; then
    term_cmd="$TERMINAL"
elif command -v gnome-terminal >/dev/null 2>&1; then
    term_cmd="gnome-terminal"
elif command -v konsole >/dev/null 2>&1; then
    term_cmd="konsole"
elif command -v foot >/dev/null 2>&1; then
    term_cmd="foot"
elif command -v xterm >/dev/null 2>&1; then
    term_cmd="xterm"
elif command -v lxterminal >/dev/null 2>&1; then
    term_cmd="lxterminal"
elif command -v x-terminal-emulator >/dev/null 2>&1; then
    term_cmd="x-terminal-emulator"
else
    echo "No known terminal emulator found."
    exit 1
fi

echo "Using terminal: $term_cmd"

case "$(basename "$term_cmd")" in
    gnome-terminal)
        gnome-terminal -- bash -c "cd \"$SCRIPT_DIR\"; LANG=en_US.UTF-8 \"$SCRIPT_DIR/run.sh\" $*"
        ;;

    konsole)
        konsole -e bash -c "cd \"$SCRIPT_DIR\"; LANG=en_US.UTF-8 \"$SCRIPT_DIR/run.sh\" $*"
        ;;

    foot)
        foot bash -c "cd \"$SCRIPT_DIR\"; LANG=en_US.UTF-8 \"$SCRIPT_DIR/run.sh\" $*"
        ;;

    xterm|lxterminal|x-terminal-emulator)
        "$term_cmd" -e bash -c "cd \"$SCRIPT_DIR\"; LANG=en_US.UTF-8 \"$SCRIPT_DIR/run.sh\" $*"
        ;;

    *)
        # generic fallback
        "$term_cmd" bash -c "cd \"$SCRIPT_DIR\"; LANG=en_US.UTF-8 \"$SCRIPT_DIR/run.sh\" $*"
        ;;
esac