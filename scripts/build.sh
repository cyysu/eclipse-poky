#!/bin/sh

help ()
{
  echo "Build the Yocto Eclipse plugins"
  echo "Usage: $0 <branch name> <release name>";
  echo ""
  echo "Options:"
  echo "<branch name> - git branch name to build upon"
  echo "<release name> - release name in the final output name"
  echo ""
  echo "Example: $0 master r0";
  exit 1;
}

fail ()
{
  local retval=$1
  shift $1
  echo "[Fail $retval]: $*"
  echo "BUILD_TOP=${BUILD_TOP}"
  cd ${TOP}
  exit ${retval}
}

find_eclipse_base ()
{
  [ -d ${ECLIPSE_HOME}/plugins ] &&  ECLIPSE_BASE=${ECLIPSE_HOME}
}

find_launcher ()
{
  local list=`ls ${ECLIPSE_BASE}/plugins/org.eclipse.equinox.launcher_*.jar`
  for launcher in $list; do
    [ -f $launcher ] && LAUNCHER=${launcher}
  done
}

find_buildfile ()
{
  local list=`ls ${ECLIPSE_BASE}/plugins/org.eclipse.pde.build_*/scripts/build.xml`
  for buildfile in $list; do
    [ -f $buildfile ] && BUILDFILE=${buildfile}
  done
}

check_env ()
{
  find_eclipse_base
  find_launcher
  find_buildfile
  
  local err=0
  [ "x${ECLIPSE_BASE}" = "x" -o "x${LAUNCHER}" = "x" -o "x${BUILDFILE}" = "x" ] && err=1
  if [ $err -eq 0 ]; then
    [ ! -d ${ECLIPSE_BASE} ] && err=1
    [ ! -f ${LAUNCHER} ] && err=1
    [ ! -f ${BUILDFILE} ] && err=1
  fi
  
  if [ $err -ne 0 ]; then
    echo "Please set env variable ECLIPSE_HOME to the eclipse installation directory!" 
    exit 1
  fi 
}

[ $# -ne 2 ] && help

#milestone
BRANCH=$1
RELEASE=$2
TOP=`pwd`

check_env

#create tmp dir for build
DATE=`date +%Y%m%d%H%M`
BUILD_TOP=`echo ${BRANCH} | sed 's%/%-%'`
BUILD_TOP=${TOP}/${BUILD_TOP}_build_${DATE}
rm -rf ${BUILD_TOP}
mkdir ${BUILD_TOP} || fail $? "Create temporary build directory ${BUILD_TOP}"
BUILD_SRC=${BUILD_TOP}/src
BUILD_DIR=${BUILD_TOP}/build
mkdir ${BUILD_DIR} || fail $? "Create temporary build directory ${BUILD_DIR}"


#git clone
GIT_URL=git://git.pokylinux.org/eclipse-poky.git
GIT_DIR=${BUILD_SRC}
#git clone ${GIT_URL} ${GIT_DIR} || fail $? "git clone ${GIT_URL}" 

mkdir ${GIT_DIR}
cp -r /home/lulianhao/working/eclipse-poky/features ${GIT_DIR}/
cp -r /home/lulianhao/working/eclipse-poky/plugins  ${GIT_DIR}/
mkdir ${GIT_DIR}/tcf
cp  /home/lulianhao/working/eclipse-poky/tcf/*.patch ${GIT_DIR}/tcf/

cd ${GIT_DIR}
#git checkout origin/${BRANCH} || fail $? "git checkout origin/${BRANCH}"
cd ${TOP}

#build 
java -jar ${LAUNCHER} -application org.eclipse.ant.core.antRunner -buildfile ${BUILDFILE} -DbaseLocation=${ECLIPSE_BASE} -Dbuilder=${GIT_DIR}/features/org.yocto.sdk.headless.build -DbuildDirectory=${BUILD_DIR} -DotherSrcDirectory=${GIT_DIR} -DbuildId=${RELEASE} -Dp2.gathering=false || fail $? "normal build"

if [ -f ${BUILD_DIR}/I.${RELEASE}/org.yocto.sdk-${RELEASE}.zip ]; then
  cp -f ${BUILD_DIR}/I.${RELEASE}/org.yocto.sdk-${RELEASE}.zip ./org.yocto.sdk-${RELEASE}-${DATE}.zip
  rm -rf ${BUILD_DIR}
else
  fail 1 "Not finding normal build output"
fi

#build archive for update site
java -jar ${LAUNCHER} -application org.eclipse.ant.core.antRunner -buildfile ${BUILDFILE} -DbaseLocation=${ECLIPSE_BASE} -Dbuilder=${GIT_DIR}/features/org.yocto.sdk.headless.build -DbuildDirectory=${BUILD_DIR} -DotherSrcDirectory=${GIT_DIR} -DbuildId=${RELEASE} || fail $? "archive build"

#clean up
if [ -f ${BUILD_DIR}/I.${RELEASE}/org.yocto.sdk-${RELEASE}-group.group.group.zip ]; then
  cp -f ${BUILD_DIR}/I.${RELEASE}/org.yocto.sdk-${RELEASE}-group.group.group.zip ./org.yocto.sdk-${RELEASE}-${DATE}-archive.zip
  rm -rf ${BUILD_TOP}
else
  fail 1 "Not finding archive build output"
fi 

exit 0