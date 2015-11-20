#!/usr/bin/perl -w

#
# Written by Bill Brassfield in 2015
#
# This little program shows all netmasks for IPv4 networks
# from /8 (huge network) to /31 (useless 0-host network).
# It also shows how many host IP addresses are possible in
# each network.
#

for (my $i=8; $i<=31; $i++)
{
  my $hostCount = 2**(32-$i) - 2;

  my $bits    = ("1" x $i) . ("0" x (32-$i));
  my @binary  = ($bits =~ /^(\d{8})(\d{8})(\d{8})(\d{8})$/);
  my @decimal = map { ord(pack("B*", $_)) } @binary;
  my $nm      = join('.', @decimal);

  printf("%3s   %-16s %8d hosts\n", "/$i", $nm, $hostCount);
}
