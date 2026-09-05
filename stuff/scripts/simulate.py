#  /*
#   * Copyright (C) 2024 Pavel Korzh
#   * SPDX-License-Identifier: GPL-3.0-only
#   */
#

import os
import subprocess
import sys

# Retrieve the path to the XML file from command line arguments
netXml = sys.argv[1]

# Get the directory where the XML file is located
workingFolder = os.path.dirname(netXml)

# Get the directory where the Python script is located
scriptDir = os.path.dirname(os.path.realpath(__file__))

# Detect operating system
if os.name == 'nt':  # nt means Windows
    scriptName = "start.bat"
    shellCmd = ["cmd", "/u", "/c"]
else:  # Posix (Linux, macOS, etc.)
    scriptName = "start.sh"
    shellCmd = ["/bin/sh"]

# Full path to the script file based on OS
scriptFilePath = os.path.join(scriptDir, scriptName)

# Command to run the script file with netXml as a parameter
command = shellCmd + [scriptFilePath, netXml]
print("command is:"+' '.join(command))

try:
    process = subprocess.Popen(command, cwd=workingFolder, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL , shell=False)
    print("Process started successfully with PID:", process.pid)
except Exception as e:
    print("Failed to start process:", str(e))
    sys.exit(1)
sys.exit()