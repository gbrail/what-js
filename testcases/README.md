# Testcases

This directory contains the "WhatWG" test cases transpiled with
Babel. The Babel configuration only changes the bare minimum
while we improve Rhino. Right now it handles:

* Block-scoped const
* Lack of async / await

## To update

Ensure that you have the core tests by running:

    git submodule init
    git submodule update

Then, run the update script:

    ./update-testcases.sh
