#!/usr/bin/env bash

current_path=$(dirname $0)
sh ${current_path}/stop.sh
sh ${current_path}/start.sh