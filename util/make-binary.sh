#!/bin/bash
set -x
cmd_dir=$(cd `dirname "$0"` && pwd)
target_dir="$cmd_dir/../target"
jar_file=${target_dir}/moe-*-shaded.jar
if [ ! -f ${jar_file} ]; then
  echo "Shaded jar not found in ${target_dir}"
  exit 1
fi
cat ${cmd_dir}/java-exec-prefix ${jar_file} > ${target_dir}/moe
chmod a+x ${target_dir}/moe
