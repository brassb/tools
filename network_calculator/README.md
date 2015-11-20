# IPv4 Network Calculation scripts

These can be handy tools when building out data centers and assigning
ranges of IP addresses to various host types or clusters of hosts.
Network configuration files often need values hard-coded into them
such as IP address, network address, netmask, and broadcast address.
Using these scripts can help to ensure that these config files are
populated with the correct values.  (It's so easy to get the network
and broadcast addresses wrong for networks that are not /24.)

With all of the numerous full-featured tools already out there (many
of them written in Javascript and accessible via web browser), why did
I write these tools?

  1. I was bored one day and wanted something to do that was mildly entertaining.
  2. I felt like doing some bit-twiddling in Perl.
  3. I wanted to provide some coding samples that might help new programmers.

If time permits, I might port these tools over to Ruby.  (The Ruby scripts would
have identical behavior and output.)

