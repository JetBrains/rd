#!/bin/bash
set -e -x -v -u
base_dir=$(cd "$(dirname "$0")"; pwd)
build_dir="${base_dir}/build"
cache_dir="${base_dir}/cache"

if [[ -d ${build_dir} ]]
then
  echo "No cleanup"
#    rm -rf ${build_dir}
fi
rm -f "$cache_dir/*.tmp"

nuget_dir=${build_dir}/nuget
nuget_version="${BUILD_NUMBER:-192.0.0}"

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
if [[ ! -f "${cache_dir}/dotnet-sdk-2.2.300-linux-x64.tar.gz" ]]
then
  wget -O "${cache_dir}/dotnet-sdk-2.2.300-linux-x64.tar.gz.tmp" https://repo.labs.intellij.net/thirdparty/dotnet-sdk-2.2.300-linux-x64.tar.gz
  mv "${cache_dir}/dotnet-sdk-2.2.300-linux-x64.tar.gz.tmp" "${cache_dir}/dotnet-sdk-2.2.300-linux-x64.tar.gz"
fi
tar xf ${cache_dir}/jdk.win.tar.gz -C ${nuget_dir}/DotFiles/jdk/win
rm ${nuget_dir}/DotFiles/jdk/win/src.zip
tar xf ${cache_dir}/jdk.lin.tar.gz -C ${nuget_dir}/DotFiles/jdk/lin
rm ${nuget_dir}/DotFiles/jdk/lin/src.zip
tar xf ${cache_dir}/jdk.mac.tar.gz -C ${nuget_dir}/DotFiles/jdk/mac
rm ${nuget_dir}/DotFiles/jdk/mac/jdk/Contents/Home/src.zip
mkdir -p ${build_dir}/.dotnet
tar xf ${cache_dir}/dotnet-sdk-2.2.300-linux-x64.tar.gz -C ${build_dir}/.dotnet

export JAVA_HOME=${nuget_dir}/DotFiles/jdk/lin
chmod +x ${JAVA_HOME}/bin/*
${build_dir}/../../../gradlew fatJar

export DOTNET_SKIP_FIRST_TIME_EXPERIENCE=1

${build_dir}/.dotnet/dotnet build /p:Configuration=Release /p:PackageVersion=$nuget_version ${build_dir}/../../../rd-net/Rd.sln
${build_dir}/.dotnet/dotnet pack --include-symbols /p:Configuration=Release /p:PackageVersion=$nuget_version ${build_dir}/../../../rd-net/Lifetimes/Lifetimes.csproj
${build_dir}/.dotnet/dotnet pack --include-symbols /p:Configuration=Release /p:PackageVersion=$nuget_version ${build_dir}/../../../rd-net/RdFramework/RdFramework.csproj
mv ${build_dir}/../../../rd-net/RdFramework/bin/Release/*.nupkg $build_dir
mv ${build_dir}/../../../rd-net/Lifetimes/bin/Release/*.nupkg $build_dir
mono ${cache_dir}/nuget.exe pack -Version $nuget_version -OutputDirectory ${build_dir}