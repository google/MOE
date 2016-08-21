#!/bin/bash

cmd_dir=$(cd `dirname "$0"` && pwd)
target_dir="$(dirname $cmd_dir)/client/target"
jar_file=${target_dir}/moe-*-executable.jar
if [ ! -f ${jar_file} ]; then
  echo "Monolithic executable jar not found in ${target_dir}"
  exit 1
fi
cat ${cmd_dir}/java-exec-prefix ${jar_file} > ${target_dir}/moe
chmod a+x ${target_dir}/moe
echo "MOE Mac/Unix excecutable created at ${target_dir}/moe"
