#!/bin/bash
args="$@"
sbt "runMain com.sri.nscore.uav2.UAV2Tree2LowCLI $args"