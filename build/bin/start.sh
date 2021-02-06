#!/usr/bin/env bash

# todo 指定app name
app_name=demo

# todo 指定运行脚本user
app_user=www

java="java"
# 项目根目录
base_dir=$(dirname $0)/..
# todo 项目main class
main_class=org.Main
# todo 下面指定lib和resources目录到classpath, resources目录主要用于存放一些配置文件, 主要解决spring boot项目打成jar包后, 一些在resources目录下的文件, 程序不可读, 这些文件可放在resources目录下
CLASSPATH=.:${base_dir}/lib/*:${base_dir}/resources/*:${CLASSPATH}

if [ ! -d ${base_dir}/gclogs ]; then
    mkdir ${base_dir}/gclogs -p
fi

# todo java jvm参数, 可修改
JAVA_OPT="${JAVA_OPT} -server -Xms256m -Xmx512m"
JAVA_OPT="${JAVA_OPT} -XX:MaxMetaspaceSize=256m"
JAVA_OPT="${JAVA_OPT} -verbose:gc -Xloggc:${base_dir}/gclogs/gc.log -XX:+PrintGCDetails -XX:+PrintGCTimeStamps"
JAVA_OPT="${JAVA_OPT} -XX:+UserGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=100m"
JAVA_OPT="${JAVA_OPT} -XX:-OmitStackTraceInFastThrow"
JAVA_OPT="${JAVA_OPT} -XX:MaxDirectMemorySize=512m"
JAVA_OPT="${JAVA_OPT} -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPT="${JAVA_OPT} -cp ${CLASSPATH}"

cmd=`nohup $java $JAVA_OPT $main_class $@ > /dev/null 2>&1 &`
current_user=`whoami`
if [ "${current_user}" = "root" ]; then
  su $app_user -c "$cmd"
else
  `$cmd`
fi

current_path=$(dirname $0)
echo $! >"${current_path}"/app.pid
echo "${app_name} started"