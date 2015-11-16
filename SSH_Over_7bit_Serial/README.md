# SSH over a 7-bit Serial console connection
 * Linux KVM virsh console
 * iLO or IPMI serial console
 * any other 7-bit ASCII terminal connection

## Dependencies:
  * Must have the IO::Pty Perl module installed on local system
  * Must have "screen" installed on local system
  * Perl must be installed on the remote system (just basic core Perl)

## Instructions (example usage):
  1. Initiate a "screen" session
  2. Inside the screen session, use whatever steps are necessary to connect to the serial console of the remote server
  3. Change directory (cd) into the directory containing the "run_this_remote.pl" script.
  4. Detach from the screen session (CTRL-A, d)
  5. Run the "run_this_local.pl" script:  ./run_this_local.pl 2022 localhost 22
  6. In a different terminal session, run:  ssh -p 2022 username@localhost
  7. You should now be connected to the remote server via a "direct" SSH connection!

## NOTE: This transport mechanism can be used only once -- shutdown instructions:
  1. After logging out of the SSH session, press CTRL-C to exit the "run_this_local.pl" script.
  2. Use "screen -r" (or if necessary, "screen -d -r") to re-attach to the screen session
  3. Press CTRL-C three times to exit the "run_this_remote.pl" script.

## Variations:
  * Use the -L or -R options of ssh in Step 6 to set up TCP port-forwarding
  * Use the -w option of ssh in Step 6 to set up Layer-3 VPN tunneling (with "tun" devices)
  * Use the -w option of ssh together with "-o Tunnel=ethernet" to set up Layer-2 VPN tunneling (with "tap" devices)

### Using this simple data transport mechanism, it's actually possible to put a server \
completely "on the network" over its iLO, IPMI, or other "serial console" port, even if \
its network interfaces (eth0, eth1, eth2, etc.) aren't even physically connected!  The \
data throughput is a bit slow, but it's entirely possible to run "yum install", "apt-get install", \
or run light-weight X11 GUI apps over such a connection.
