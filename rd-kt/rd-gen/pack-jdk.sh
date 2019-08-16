#!/bin/bash
set -e -x -v -u
base_dir=$(cd "$(dirname "$0")"; pwd)
build_dir="${base_dir}/build"
cache_dir="${base_dir}/cache"

if [[ -d ${build_dir} ]]
then
    rm -rf ${build_dir}
fi
rm -f "$cache_dir/*.tmp"

nuget_dir=${build_dir}/nuget
nuget_version="${BUILD_NUMBER:-193.0.0}"

mkdir -p $cache_dir
echo $cache_dir

mkdir -p "${nuget_dir}/DotFiles/jdk/win"
mkdir -p "${nuget_dir}/DotFiles/jdk/lin"
mkdir -p "${nuget_dir}/DotFiles/jdk/mac"


if [[ ! -f "${cache_dir}/jdk.win.tar.gz" ]]
then
  wget -O "${cache_dir}/jdk.win.tar.gz.tmp" https://jetbrains.bintray.com/intellij-jbr/jbrsdk-8u202-windows-x64-b1514.23.tar.gz
  mv "${cache_dir}/jdk.win.tar.gz.tmp" "${cache_dir}/jdk.win.tar.gz"
fi
if [[ ! -f "${cache_dir}/jdk.lin.tar.gz" ]]
then
  wget -O "${cache_dir}/jdk.lin.tar.gz.tmp" https://jetbrains.bintray.com/intellij-jbr/jbrsdk-8u202-linux-x64-b1514.23.tar.gz
  mv "${cache_dir}/jdk.lin.tar.gz.tmp" "${cache_dir}/jdk.lin.tar.gz"
fi
if [[ ! -f "${cache_dir}/jdk.mac.tar.gz" ]]
then
  wget -O "${cache_dir}/jdk.mac.tar.gz.tmp" https://jetbrains.bintray.com/intellij-jbr/jbrsdk-8u202-osx-x64-b1514.23.tar.gz
  mv "${cache_dir}/jdk.mac.tar.gz.tmp" "${cache_dir}/jdk.mac.tar.gz"
fi
if [[ ! -f "${cache_dir}/nuget.exe" ]]
then
  wget -O "${cache_dir}/nuget.exe.tmp" https://dist.nuget.org/win-x86-commandline/latest/nuget.exe
  mv "${cache_dir}/nuget.exe.tmp" "${cache_dir}/nuget.exe"
fi

tar xf ${cache_dir}/jdk.win.tar.gz -C ${nuget_dir}/DotFiles/jdk/win
rm ${nuget_dir}/DotFiles/jdk/win/src.zip
tar xf ${cache_dir}/jdk.lin.tar.gz -C ${nuget_dir}/DotFiles/jdk/lin
rm ${nuget_dir}/DotFiles/jdk/lin/src.zip
tar xf ${cache_dir}/jdk.mac.tar.gz -C ${nuget_dir}/DotFiles/jdk/mac
rm ${nuget_dir}/DotFiles/jdk/mac/jdk/Contents/Home/src.zip

mono ${cache_dir}/nuget.exe pack -Version $nuget_version -OutputDirectory ${build_dir} JetBrains.Jdk.nuspec