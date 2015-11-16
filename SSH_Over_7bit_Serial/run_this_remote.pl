#!/usr/bin/perl

use strict;

use Fcntl;
use MIME::Base64;
use IO::Socket;
use Symbol;
use POSIX;

sub getUsage();

if (scalar(@ARGV) != 2)
{
  print getUsage();
  exit(1);
}

system "stty raw";
system "stty -echo";

# Unbuffer writes to STDOUT and STDERR
$| = 1;
my $oldfh = select(STDERR);
$| = 1;
select($oldfh);

my $flags;

# Make sysread on STDIN non-blocking
$flags = undef;
$flags = fcntl(STDIN, F_GETFL, 0) or die "Couldn't get flags for HANDLE : $!\n";
$flags |= O_NONBLOCK;
fcntl(STDIN, F_SETFL, $flags) or die "Couldn't set flags for HANDLE: $!\n";

my $server = new IO::Socket::INET (
  PeerAddr => $ARGV[0],
  PeerPort => $ARGV[1],
  Proto    => 'tcp'
);

if (!$server)
{
  print "Could not create socket: $!\n";
}
else
{
  $oldfh = select($server);
  $| = 1;
  select($oldfh);
}

# Make sysread on server socket non-blocking
$flags = undef;
$flags = fcntl($server, F_GETFL, 0) or die "Couldn't get flags for HANDLE : $!\n";
$flags |= O_NONBLOCK;
fcntl($server, F_SETFL, $flags) or die "Couldn't set flags for HANDLE: $!\n";

my $fromServer;
my $fromStdin;
my $nread;

my $encBuf = "";
my $timeToDie = 0;
while (!$timeToDie)
{
  $nread = sysread($server, $fromServer, 2048);
  if (defined($nread) && ($nread > 0))
  {
    my $encodedFromServer = encode_base64($fromServer);
    $encodedFromServer =~ s/[\r\n]//g;
    print "\r\n#(#" . $encodedFromServer . "#)#\r\n";
  }

  # Sleep 0.01 seconds
  select(undef, undef, undef, 0.01);

  $nread = sysread(STDIN, $fromStdin, 2048);
  if (defined($nread) && ($nread > 0))
  {
    $encBuf .= $fromStdin;
    if ($encBuf =~ /\003\003\003/) { $timeToDie = 1; }
    while ($encBuf =~ /\#\(\#([^\#]+)\#\)\#/s)
    {
      my $toDecode = $1;
      $encBuf =~ s/^.*?\#\(\#[^\#]+\#\)\#//s;
      print $server decode_base64($toDecode);
    }
  }
}

system "stty -raw";
system "stty echo";

sub getUsage()
{
  print "\n  Usage: $0 remote-hostname-or-ip remote-tcp-port-num\n\n";
}

