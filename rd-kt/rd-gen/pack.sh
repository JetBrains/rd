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
build_configuration=${1:-Debug}

mkdir -p $cache_dir
echo $cache_dir

if [[ ! -f "${cache_dir}/jdk.lin.tar.gz" ]]
then
  wget -O "${cache_dir}/jdk.lin.tar.gz.tmp" https://jetbrains.bintray.com/intellij-jbr/jbrsdk-8u202-linux-x64-b1514.23.tar.gz
  mv "${cache_dir}/jdk.lin.tar.gz.tmp" "${cache_dir}/jdk.lin.tar.gz"
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

mkdir -p ${build_dir}/jdk
tar xf ${cache_dir}/jdk.lin.tar.gz -C ${build_dir}/jdk

mkdir -p ${build_dir}/.dotnet
tar xf ${cache_dir}/dotnet-sdk-2.2.300-linux-x64.tar.gz -C ${build_dir}/.dotnet

export JAVA_HOME=${build_dir}/jdk
chmod +x ${JAVA_HOME}/bin/*
${build_dir}/../../../gradlew fatJar

export DOTNET_SKIP_FIRST_TIME_EXPERIENCE=1

mkdir -p ${nuget_dir}/lib/net
touch ${nuget_dir}/lib/net/_._

export BUILD_CONFIGURATION=$build_configuration

${build_dir}/.dotnet/dotnet build /p:Configuration=$BUILD_CONFIGURATION /p:PackageVersion=$nuget_version ${build_dir}/../../../rd-net/Rd.sln
${build_dir}/.dotnet/dotnet pack --include-symbols /p:Configuration=$BUILD_CONFIGURATION /p:PackageVersion=$nuget_version ${build_dir}/../../../rd-net/Lifetimes/Lifetimes.csproj
${build_dir}/.dotnet/dotnet pack --include-symbols /p:Configuration=$BUILD_CONFIGURATION /p:PackageVersion=$nuget_version ${build_dir}/../../../rd-net/RdFramework/RdFramework.csproj
mv ${build_dir}/../../../rd-net/RdFramework/bin/$BUILD_CONFIGURATION/*.nupkg $build_dir
mv ${build_dir}/../../../rd-net/Lifetimes/bin/$BUILD_CONFIGURATION/*.nupkg $build_dir
mono ${cache_dir}/nuget.exe pack -Version $nuget_version -OutputDirectory ${build_dir} JetBrains.RdGen.nuspec