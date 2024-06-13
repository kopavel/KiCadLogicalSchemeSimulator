#  Copyright (c) 2024 Pavel Korzh
#  <p>
#  All rights reserved.
#  <p>
#  Redistribution and use in source and binary forms, with or without
#  modification, are permitted provided that the following conditions are met:
#  <p>
#  1. Redistributions of source code must retain the above copyright notice,
#     this list of conditions and the following disclaimer.
#  <p>
#  2. Redistributions in binary form must reproduce the above copyright notice,
#     this list of conditions and the following disclaimer in the documentation
#     and/or other materials provided with the distribution.
#  <p>
#  3. Neither the name of the copyright holder nor the names of its contributors
#     may be used to endorse or promote products derived from this software
#     without specific prior written permission.
#  <p>
#  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
#  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
#  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
#  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
#  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
#  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
#  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
#  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
#  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
#  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
#  POSSIBILITY OF SUCH DAMAGE.
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
    process = subprocess.Popen(command, cwd=workingFolder, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=False)
    print("Process started successfully with PID:", process.pid)
except Exception as e:
    print("Failed to start process:", str(e))
    sys.exit(1)

# Wait a bit to ensure process starts properly
import time
time.sleep(0.1)

# Check if the process is still running
retcode = process.poll()
if retcode is None:
    print("Process is running in the background.")
else:
    print("Process terminated with return code:", retcode)
    stdout, stderr = process.communicate()
    print("STDOUT:", stdout.decode())
    print("STDERR:", stderr.decode())

# Exit the Python script immediately
sys.exit()