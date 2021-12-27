#!/usr/bin/env bash

script_dir=$(dirname "$0")
cd $script_dir
cd ..

#clojure -Sdeps "{:deps {com.bhauman/rebel-readline {:mvn/version \"0.1.4\"}}}" -m rebel-readline.main

clojure -M:repl
