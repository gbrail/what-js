#!/bin/sh

testdirs='url console streams dom/abort'

for d in ${testdirs}
do
  npx babel ../wpt/${d} \
    --verbose \
    --out-dir ${d}
  if [ -d ../wpt/${d}/resources ]
  then
    if [ ! -d ${d}/resources ]
    then
      mkdir ${d}/resources
    fi
    # cp ../wpt/${d}/resources/* ${d}/resources
    npx babel ../wpt/${d}/resources \
      --verbose \
      --out-dir ${d}/resources
  fi
done

common_files='gc.js'

for f in ${common_files}
do
  npx babel ../wpt/common/${f} \
    --verbose \
    --out-dir common
done