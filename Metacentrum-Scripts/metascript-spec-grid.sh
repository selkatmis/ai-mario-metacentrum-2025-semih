#!/bin/bash

# seq -> FIRST INCREMENT LAST

for ndw in 0.00 0.50 1.00 1.50 2.00 3.00
do
  for ttfw in 0.00 0.50 1.00 1.50 2.00
  do
    for dfpt in 0.00 1.00 2.00 3.00 4.00 5.00 7.00 10.00 15.00 20.00
    do
      for dfpap in 0.00 1.00 2.00 3.00 5.00 10.00 20.00 50.00
      do
          echo $ndw $ttfw $dfpt $dfpap
          qsub -v NDW=$ndw,TTFW=$ttfw,DFPT=$dfpt,DFPAP=$dfpap script-spec-grid.sh
      done
    done
  done
done
