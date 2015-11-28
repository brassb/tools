# Parallel (Multi-Threaded) Remote Command/Script Execution
## on tens or hundreds of remote hosts, using Java threads and SSH
 * Quick-and-dirty sysadmin power tool (separate from configuration management)
 * Good for up to roughly 250 to 500 remote hosts
 * Similar to running Ansible in ad-hoc mode
 * Very fast execution due to parallel (rather than serial) processing on remote hosts
 * Output format clearly identifies which host had which output and exit code
 * Easy to build, set up, and run -- with no weird or inconvenient dependencies
 * Can run as an ordinary non-root user on the local/client side (recommended)

## Dependencies:
 * Must have Java JDK/JRE 1.6 or later installed
 * Must have an SSH client installed
 * Must have Bash shell available (/bin/bash)
 * Must have SSH keys installed on all remote hosts for passwordless authentication
 * Terminal must support turning on/off "raw" and "echo" modes (via the "stty" command)

## Usage notes:
 * Edit paths in remote_commands.properties to fit your environment
 * Edit hostList.txt and add your hosts (by hostname or IP), one per line
 * Make sure you can ssh into each and every remote host without being prompted for anything (these utilities do NOT respond to prompts)
 * The "runScriptOnAllNodes.sh" script takes one argument:  the path to any executable script to run on each remote host
 * The "runCommandOnAllNodes.sh" script has its own interactive command-line interpreter with command history (takes no command-line args)

## TODOs:
 * Support host groups in the hostList.txt file
 * Support same-line comments in hostList.txt file
 * Suppress duplicate consecutive entries in command-history file
 * Provide an easy way to abort "stuck" or slow stragglers when waiting for output from all remote hosts
 * Relocate (and possibly rewrite) the ugly keystoke-processing loop in RunCommandOnAllNodes.java
 * Improve the remote_commands.properties file (support ENV variables and variable substitutions)
 * Create a multi-tiered version of this utility so it can fan out to tens of thousands of remote hosts
 * Implement something similar to Ansible "playbooks" (or Puppet "manifests" or Chef "recipes")
 * Provide a non-interactive "batch mode" for RunCommandOnAllNodes.java and the Bash script which calls it
