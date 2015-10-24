#!/usr/bin/perl -p
1 while s/\t/" " x (8 - (length($`) % 8))/e;
