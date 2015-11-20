#!/usr/bin/perl

use strict;

use Slave_Terminal;
use Fcntl;
use MIME::Base64;

system "stty -echo";
system "stty raw";

my $bashShell = Slave_Terminal->spawn("/bin/bash");
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

my @lastSix = ();

my $ch;
my $nread;

print $bashShell "screen -r\n";  # To a root shell on a serial console of remote server
select(undef, undef, undef, 0.25);
print $bashShell "./run_this_remote.pl\n";

my $encBuf = "";
while (1)
{
  my $fromBashShell = $bashShell->slave_read();

  $encBuf .= $fromBashShell;
  while ($encBuf =~ /\#\(\#([^\#]+)\#\)\#/s)
  {
    my $toDecode = $1;
    $encBuf =~ s/^.*?\#\(\#[^\#]+\#\)\#//s;
    print decode_base64($toDecode);
  }

  # Sleep 0.01 seconds
  select(undef, undef, undef, 0.01);

  $nread = sysread(STDIN, $ch, 2048);
  if (defined($nread) && ($nread > 0))
  {
    my $encodedFromStdin = encode_base64($ch);
    $encodedFromStdin =~ s/[\r\n]//g;
    print $bashShell "\r\n#(#" . $encodedFromStdin . "#)#\r\n";
  }
}

system "stty -raw";
system "stty echo";

