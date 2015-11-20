# PPP over a 7-bit Serial console connection
 * Linux KVM virsh console
 * iLO or IPMI serial console
 * any other 7-bit ASCII terminal connection

## Dependencies:
  * Must have Perl installed on local and remote systems
  * Must have the IO::Pty Perl module installed on local and remote systems
  * Must have the Slave_Terminal Perl module installed on local and remote systems
  * Must have "screen" installed on local system

## Instructions (example usage):
  1. Start up a root Bash shell ("su -" or "sudo su -")
  2. From the root shell, initiate a "screen" session
  3. Inside the screen session, use whatever steps are necessary to connect to the serial console of the remote server
  4. Start up a root Bash shell on the remote server ("su -" or "sudo su -")
  5. Change directory (cd) into the directory containing the "run_this_remote.pl" script
  6. Detach from the screen session (CTRL-A, d)
  7. From the root shell, run the start_pppd.sh script ("./start_pppd.sh")
  8. Look for some output from pppd indicating a successful connection (you should see local and remote IP addresses)
  9. In another terminal session, ssh into the remote host via its remote PPP IP address

## Shutdown Instructions:
  1. Log out of all ssh sessions into the remote host via its PPP IP address
  2. As root, kill the local pppd process and the local Perl script (run_this_local.pl) process
  3. Use "screen -r" (or if necessary, "screen -d -r") to re-attach to the screen session
  4. Press CTRL-C three times to exit the "run_this_remote.pl" script (this should also kill the remote pppd process)

## Variations:
  * In step 9 above, Use the -w option of ssh together with "-o Tunnel=ethernet" to set up Layer-2 VPN tunneling (with "tap" devices)

Using this simple data transport mechanism, it's actually possible to put a server
completely "on the network" over its iLO, IPMI, or other "serial console" port, even if
its network interfaces (eth0, eth1, eth2, etc.) aren't even physically connected!  The
data throughput is a bit slow, but it's entirely possible to run "yum install", "apt-get install",
or run light-weight X11 GUI apps over such a connection.

NOTE:  If you have a reliable serial console connection, you can get better performance
(in terms of latency and throughput) by using the scripts for "SSH_Over_7bit_Serial".
However, the SSH protocol can be quite finicky about errors, and the connection may die
unexpectedly due to a "corrupted packet".  Using this code for runnning pppd over a 7-bit
serial connection, the performance of the connection isn't as good, but it seems to be
more robust against errors.  Thus, the connection is more likely to stay up and running in
situations where the serial console connection is a bit flaky.  Give both a try and see
which one better suits your needs.

