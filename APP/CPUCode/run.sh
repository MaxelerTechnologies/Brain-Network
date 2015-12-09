#!/bin/bash

bindir=.
if [[ $# -gt 0 ]] ; then
	bindir=$1
fi
topdir=../..

export LAUNCH_FROM_SCRIPT=1
java -jar ./gui/brain_network.jar $topdir/data/brain_images.zip $bindir/compute_correlation $bindir/libdfe.so

