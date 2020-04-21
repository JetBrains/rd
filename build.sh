#!/bin/bash
set -e -x -v -u

# This script produce all artifacts to deploy
# It should be running into mono:latest docker container

base_dir=$(cd "$(dirname "$0")"; pwd)
build_dir="${base_dir}/build"
cache_dir="${build_dir}/cache"
artifacts_dir="${base_dir}/build/artifacts"

#if [[ -d ${build_dir} ]]
#then
#    rm -rf ${build_dir}
#fi

nuget_dir=${build_dir}/nuget
nuget_version="${BUILD_NUMBER:-2020.2.0-preview1}"
build_configuration=${1:-Debug}

mkdir -p $cache_dir
echo $cache_dir

if [[ ! -f "${cache_dir}/dotnet-sdk-3.1.201-linux-x64.tar.gz" ]]
then
  curl -o "${cache_dir}/dotnet-sdk-3.1.201-linux-x64.tar.gz.tmp" https://download.visualstudio.microsoft.com/download/pr/f65a8eb0-4537-4e69-8ff3-1a80a80d9341/cc0ca9ff8b9634f3d9780ec5915c1c66/dotnet-sdk-3.1.201-linux-x64.tar.gz
  mv "${cache_dir}/dotnet-sdk-3.1.201-linux-x64.tar.gz.tmp" "${cache_dir}/dotnet-sdk-3.1.201-linux-x64.tar.gz"
fi

mkdir -p ${build_dir}/.dotnet
tar xf ${cache_dir}/dotnet-sdk-3.1.201-linux-x64.tar.gz -C ${build_dir}/.dotnet

export DOTNET_SKIP_FIRST_TIME_EXPERIENCE=1
export DOTNET_CLI_TELEMETRY_OPTOUT=1
export PATH=${build_dir}/.dotnet:$PATH

export BUILD_CONFIGURATION=$build_configuration

${base_dir}/gradlew -Dorg.gradle.daemon=false build fatJar --info

dotnet build /p:Configuration=$BUILD_CONFIGURATION /p:PackageVersion=$nuget_version ${base_dir}/rd-net/Rd.sln
dotnet pack --include-symbols /p:Configuration=$BUILD_CONFIGURATION /p:PackageVersion=$nuget_version ${base_dir}/rd-net/Lifetimes/Lifetimes.csproj
dotnet pack --include-symbols /p:Configuration=$BUILD_CONFIGURATION /p:PackageVersion=$nuget_version ${base_dir}/rd-net/RdFramework/RdFramework.csproj

mkdir -p $artifacts_dir
echo $artifacts_dir

mv ${base_dir}/../../../rd-net/RdFramework/bin/$BUILD_CONFIGURATION/*.nupkg $artifacts_dir
mv ${base_dir}/../../../rd-net/RdFramework/bin/$BUILD_CONFIGURATION/*.snupkg $artifacts_dir
mv ${base_dir}/../../../rd-net/Lifetimes/bin/$BUILD_CONFIGURATION/*.nupkg $artifacts_dir
mv ${base_dir}/../../../rd-net/Lifetimes/bin/$BUILD_CONFIGURATION/*.snupkg $artifacts_dir

mkdir -p ${nuget_dir}/lib/net
touch ${nuget_dir}/lib/net/_._
nuget pack -Version $nuget_version -OutputDirectory "$artifacts_dir" "${base_dir}/rd-kt/rd-gen/JetBrains.RdGen.nuspec"