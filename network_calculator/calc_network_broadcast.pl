#!/usr/bin/perl -w

#
# Written by Bill Brassfield in 2015
#
# This program takes an IPv4 IP address and netmask as command-line
# arguments, and calculates the network address, broadcast address,
# and number of host IP addresses possible in the network.  The IP
# address should be in decimal dot-quad notation.  The netmask can
# be expressed as either decimal dot-quad notation, or as /N where
# N can range from 8 up to 31.  (Note: /31 represents a useless
# 0-host network which has only a network and broadcast address.)
#
# Example Usage (both runs give the same result):
#
#   ./calc_network_broadcast.pl 10.38.70.86 /22
#
#   ./calc_network_broadcast.pl 10.38.70.86 255.255.252.0
#

use strict;

sub getUsage();
sub bitsToDecimal($);

# Parse command line
if (scalar(@ARGV) != 2) {
  die getUsage();
}
my ($ip, $nm) = @ARGV;

# Parse IP address
my ($ip1, $ip2, $ip3, $ip4) = ($ip =~ /^(\d+)\.(\d+)\.(\d+)\.(\d+)$/);
if (!defined($ip1) || ($ip1 == 0) || ($ip1 > 255) || ($ip2 > 255) || ($ip3 > 255) || ($ip4 > 255)) {
  die "\n  Bad IP address!\n" . getUsage();
}
my $ipAddr = '' . int($ip1) . '.' . int($ip2) . '.' . int($ip3) . '.' . int($ip4);
my $ipBits = unpack("B*", chr($ip1)) . unpack("B*", chr($ip2)) . unpack("B*", chr($ip3)) . unpack("B*", chr($ip4));

# Parse netmask (/N or nm1.nm2.nm3.nm4)
my $nmBits;
if ($nm =~ /^\/(\d+)$/) {
  $nmBits = ("1" x $1) . ("0" x (32 - $1));
}
elsif ($nm =~ /^(\d+)\.(\d+)\.(\d+)\.(\d+)$/) {
  $nmBits = unpack("B*", chr($1)) . unpack("B*", chr($2)) . unpack("B*", chr($3)) . unpack("B*", chr($4));
}
else {
  die "\n  Invalid netmask!\n" . getUsage();
}
my $netmsk = $nmBits;
$netmsk =~ s/(\d{8})/ord(pack("B*", $1)) . '.'/eg;
$netmsk =~ s/\.$//;
if ((length($nmBits) != 32) || ($nmBits !~ /^1+0+$/)) {
  die "\n  Invalid netmask!\n" . getUsage();
}
my ($nmOnes)  = ($nmBits =~ /(1+)/);
my ($nmZeros) = ($nmBits =~ /(0+)/);
my $notNm = $nmBits;
$notNm =~ tr/01/10/;

# Calculate network and broadcast addresses, number of hosts in network
my $nwBits = unpack("B*", pack("B*", $ipBits) & pack("B*", $nmBits));
my $bcBits = unpack("B*", pack("B*", $ipBits) | pack("B*", $notNm));
my $nwAddr = bitsToDecimal($nwBits);
my $bcAddr = bitsToDecimal($bcBits);
my $hCount = (2 ** length($nmZeros)) - 2;

# Display results
print "IP Address:                  $ipAddr\n";
print "Net Mask:                    $netmsk   (/" . length($nmOnes) . ")\n";
print "Network Address:             $nwAddr\n";
print "Broadcast Address:           $bcAddr\n";
print "Number of hosts in network:  $hCount" . (($hCount == 0)?((" " x 17) . "(This is a useless network!)"):"") . "\n";

#########################################################################################################################

sub getUsage() {
  return "\n  $0 ip-address netmask\n\n";
}

sub bitsToDecimal($) {
  my $bits = shift;
  my ($b1, $b2, $b3, $b4) = ($bits =~ /^(\d{8})(\d{8})(\d{8})(\d{8})$/);
  my $addr = ord(pack("B*", $b1)) . '.' . ord(pack("B*", $b2)) . '.' .  ord(pack("B*", $b3)) . '.' .  ord(pack("B*", $b4));
  return $addr;
}
