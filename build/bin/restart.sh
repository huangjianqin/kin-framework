#!/usr/bin/env bash

current_path=$(dirname $0)
sh ${current_path}/shutdown.sh
sh ${current_path}/start.sh