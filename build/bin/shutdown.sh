#!/usr/bin/env bash

# todo 自定义app name
app_name=Main
# todo 填写具体的java app main class
#main_class=org.Main

#grep_pid=`ps -ef | grep java | grep "${main_class}" | awk '{print $2}'`
#目标pid
pid='cat "${current_path}"/app.pid'
#获取运行中的目标pid
grep_pid=`ps -ef | awk '{print $2}' | grep "${pid}"`
if [ -z $grep_pid ]; then
  echo "no need stop: the ${app_name} does not started!"
fi

echo -e "stopping the ${app_name} ...\c"
#for pid in $grep_pid ; do
#  kill $pid > /dev/null 2>&1
#done

#当前目录
current_path=$(dirname "$0")
# 杀进程
kill "${pid}"
# 移除pid目录
rm -rf "${current_path}"/app.pid

count=0
while [ $count -lt 1 ]; do
  echo -e ".\c"
  sleep 1
  count=1
  for pid in $grep_pid ; do
#    pid_exit=`ps -f -p $pid | grep java`
    pid_exit=`ps -f -p $pid`
    if [ -n "${pid_exit}" ]; then
      count=0
      break
    fi
  done
done

echo "ok"



