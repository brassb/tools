#!/usr/bin/perl

# Written by Bill Brassfield in 2014

#
# Sends "virtual keystrokes" into a pseudo-terminal device.  Any
# shell or app running inside the target device will behave as if
# the keystrokes had been entered via a physical keyboard.  This
# can come in handy in some situations.  Most likely, this script
# will need to be run as root or with sudo.  Otherwise, the virtual
# keystrokes will fail to have any effect upon the target device.
#

require "sys/ioctl.ph";

if (scalar(@ARGV) != 2)
{
  die "\n  Usage: $0 pseudo-terminal-device \"command to execute\"\n\n" .
      "  Example: $0 /dev/pts/7 \"service network restart\"\n\n" .
      "  (NOTE:  Most likely, you'll need to be root to run this script.)\n\n";
}

my $ttyTarget = $ARGV[0];
my $cmdToSend = $ARGV[1];
$cmdToSend .= "\r";

my @charsToSend = split('', $cmdToSend);

open(TTY, "> $ttyTarget");
foreach my $chr (@charsToSend)
{
  ioctl(TTY, &TIOCSTI, $chr);
}
close(TTY);

