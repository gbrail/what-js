#!/bin/sh

testdirs='url console streams'

for d in ${testdirs}
do
  npx babel ../wpt/${d} --verbose --out-dir ${d}
  if [ -d ../wpt/${d}/resources ]
  then
    if [ ! -d ${d}/resources ]
    then
      mkdir ${d}/resources
    fi
    cp ../wpt/${d}/resources/* ${d}/resources
  fi
done
