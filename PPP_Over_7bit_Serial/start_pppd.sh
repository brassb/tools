#!/bin/bash

/usr/sbin/pppd nodetach 192.168.1.10:192.168.1.20 pty "(cd $PWD; ./run_this_local.pl)"

