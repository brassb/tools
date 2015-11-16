#!/usr/bin/perl

use strict;

use Slave_Terminal;
use Fcntl;
use MIME::Base64;
use IO::Socket;
use Symbol;
use POSIX;

sub getUsage();

if (scalar(@ARGV) != 3)
{
  print getUsage();
  exit(1);
}

# Setup a TCP server socket
my $inetServer = IO::Socket::INET->new(
  LocalPort => $ARGV[0],
  Type      => SOCK_STREAM,
  Proto     => 'tcp',
  Reuse     => 1,
  Listen    => 10
)
  or die "making socket: $@\n";

my $client = $inetServer->accept();

# Unbuffer writes to the client socket
my $oldfh = select($client);
$| = 1;
select($oldfh);

my $flags;

# Make sysread on $client non-blocking
$flags = undef;
$flags = fcntl($client, F_GETFL, 0) or die "Couldn't get flags for HANDLE : $!\n";
$flags |= O_NONBLOCK;
fcntl($client, F_SETFL, $flags) or die "Couldn't set flags for HANDLE: $!\n";

my $bashShell = Slave_Terminal->spawn("/bin/bash");
my $bashPid = $bashShell->slave_pid();

select(undef, undef, undef, 0.25);
print $bashShell "screen -r\n";
select(undef, undef, undef, 0.25);
print $bashShell "./run_this_remote.pl $ARGV[1] $ARGV[2]\n";
select(undef, undef, undef, 0.25);

my $fromClient;
my $nread;

my $encBuf = "";
while (1)
{
  my $fromBashShell = $bashShell->slave_read();
  if (defined($fromBashShell) && (length($fromBashShell) > 0))
  {
    $encBuf .= $fromBashShell;
    while ($encBuf =~ /\#\(\#([^\#]+)\#\)\#/s)
    {
      my $toDecode = $1;
      $encBuf =~ s/^.*?\#\(\#[^\#]+\#\)\#//s;
      print $client decode_base64($toDecode);
    }
  }

  # Sleep 0.01 seconds
  select(undef, undef, undef, 0.01);

  $nread = sysread($client, $fromClient, 2048);
  if (defined($nread) && ($nread > 0))
  {
    my $encodedFromClient = encode_base64($fromClient);
    $encodedFromClient =~ s/[\r\n]//g;
    print $bashShell "\r\n#(#" . $encodedFromClient . "#)#\r\n";
  }
}

sub getUsage()
{
  print "\n  Usage: $0 local-tcp-listen-port-num remote-hostname-or-ip remote-tcp-port-num\n\n";
}

