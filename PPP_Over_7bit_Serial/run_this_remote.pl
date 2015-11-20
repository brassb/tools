#!/usr/bin/perl -w

use strict;

use Slave_Terminal;
use Fcntl;
use MIME::Base64;

system "stty -echo";
system "stty raw";

my $bashShell = Slave_Terminal->spawn("/usr/sbin/pppd 192.168.1.20:192.168.1.10");
my $bashPid = $bashShell->slave_pid();

# Make sysread on STDIN non-blocking
my $flags = undef;
$flags = fcntl(STDIN, F_GETFL, 0) or die "Couldn't get flags for HANDLE : $!\n";
$flags |= O_NONBLOCK;
fcntl(STDIN, F_SETFL, $flags) or die "Couldn't set flags for HANDLE: $!\n";

binmode(STDOUT);
binmode(STDERR);

# Unbuffer STDOUT
$|++;

# Unbuffer STDERR
my $oldfh = select(STDERR);
$|++;
select($oldfh);

my $ch;
my $nread;
# User interaction loop (keyboard)
my $encBuf = "";
my $timeToDie = 0;
while (!$timeToDie)
{
  my $fromBashShell = $bashShell->slave_read();
  if (length($fromBashShell) > 0)
  {
    my $encodedFromBash = encode_base64($fromBashShell);
    $encodedFromBash =~ s/[\r\n]//g;
    print "\r\n#(#" . $encodedFromBash . "#)#\r\n";
  }

  # Sleep 0.01 seconds
  select(undef, undef, undef, 0.01);

  $nread = sysread(STDIN, $ch, 2048);
  if (defined($nread) && ($nread > 0))
  {
    $encBuf .= $ch;
    if ($encBuf =~ /\003\003\003/) { $timeToDie = 1; }
    while ($encBuf =~ /\#\(\#([^\#]+)\#\)\#/s)
    {
      my $toDecode = $1;
      $encBuf =~ s/^.*?\#\(\#[^\#]+\#\)\#//s;
      print $bashShell decode_base64($toDecode);
    }
  }
}

system "stty -raw";
system "stty echo";

