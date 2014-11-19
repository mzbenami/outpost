#! /bin/bash
# run from src

MAP=${1:-map0}
R=${2:-10}
L=${3:-4}
W=${4:-1}
GUI=${5:-true}
GROUP0=${6:-group6}
GROUP1=${7:-dumb}
GROUP2=${8:-group4_nov19}
GROUP3=${9:-group5_nov19}
T=${10:-100}

javac outpost/sim/Outpost.java
javac outpost/$GROUP0/Player.java
javac outpost/$GROUP1/Player.java
javac outpost/$GROUP2/Player.java
javac outpost/$GROUP3/Player.java

java outpost.sim.Outpost $MAP $R $L $W $GUI $GROUP0 $GROUP1 $GROUP2 $GROUP3 $T
