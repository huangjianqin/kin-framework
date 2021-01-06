#!/usr/bin/env bash

# todo 自定义app name
app_name=Main
# todo 填写具体的java app main class
main_class=org.Main

pids=`ps -ef | grep java | grep "${main_class}" | awk '{print $2}'`
if [ -z $pids ]; then
  echo "no need stop: the ${app_name} does not started!"
fi

echo -e "stoppin the ${app_name} ...\c"
for pid in $pids ; do
  kill $pid > /dev/null 2>&1
done

count=0
while [ $count -lt 1 ]; do
  echo -e ".\c"
  sleep 1
  count=1
  for pid in $pids ; do
    pid_exit=`ps -f -p $pid | grep java`
    if [ -n "${pid_exit}" ]; then
      count=0
      break
    fi
  done
done

echo "ok"
