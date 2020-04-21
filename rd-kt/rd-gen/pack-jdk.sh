#!/bin/bash
set -e -x -v -u

# This script create NuGet package for Jdk which required for RdGen NuGet.
# You need run it if you want to update Jdk NuGet package

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

mkdir -p "${nuget_dir}/jdk/win"
mkdir -p "${nuget_dir}/jdk/lin"
mkdir -p "${nuget_dir}/jdk/mac"


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

tar xf ${cache_dir}/jdk.win.tar.gz -C ${nuget_dir}/jdk/win
rm ${nuget_dir}/jdk/win/src.zip
tar xf ${cache_dir}/jdk.lin.tar.gz -C ${nuget_dir}/jdk/lin
rm ${nuget_dir}/jdk/lin/src.zip
tar xf ${cache_dir}/jdk.mac.tar.gz -C ${nuget_dir}/jdk/mac
rm ${nuget_dir}/jdk/mac/jdk/Contents/Home/src.zip

mkdir -p ${nuget_dir}/lib/net
touch ${nuget_dir}/lib/net/_._

mono ${cache_dir}/nuget.exe pack -Version $nuget_version -OutputDirectory ${build_dir} ${base_dir}/JetBrains.Jdk.nuspec