#!/bin/bash
args="$@"
sbt "runMain com.sri.nscore.uav2.UAV2Seq2TreeCLI $args"