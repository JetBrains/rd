#!/bin/bash
set -e -x -v -u
base_dir=$(cd "$(dirname "$0")"; pwd)
build_dir="${base_dir}/build"
if [[ -d ${build_dir} ]]
then
    rm -rf ${build_dir}
fi
nuget_dir=${build_dir}/nuget
nuget_version="${BUILD_NUMBER:-192.0.0}"

mkdir -p "${nuget_dir}/DotFiles/jdk/win"
mkdir -p "${nuget_dir}/DotFiles/jdk/lin"
mkdir -p "${nuget_dir}/DotFiles/jdk/mac"

wget -O "${build_dir}/jdk.win.tar.gz" https://jetbrains.bintray.com/intellij-jbr/jbrsdk-8u202-windows-x64-b1514.23.tar.gz
wget -O "${build_dir}/jdk.lin.tar.gz" https://jetbrains.bintray.com/intellij-jbr/jbrsdk-8u202-linux-x64-b1514.23.tar.gz
wget -O "${build_dir}/jdk.mac.tar.gz" https://jetbrains.bintray.com/intellij-jbr/jbrsdk-8u202-osx-x64-b1514.23.tar.gz
wget -O "${build_dir}/nuget.exe" https://dist.nuget.org/win-x86-commandline/latest/nuget.exe
wget -O "${build_dir}/dotnet-sdk-2.2.300-linux-x64.tar.gz" https://repo.labs.intellij.net/thirdparty/dotnet-sdk-2.2.300-linux-x64.tar.gz
tar xf ${build_dir}/jdk.win.tar.gz -C ${nuget_dir}/DotFiles/jdk/win
rm ${nuget_dir}/DotFiles/jdk/win/src.zip
tar xf ${build_dir}/jdk.lin.tar.gz -C ${nuget_dir}/DotFiles/jdk/lin
rm ${nuget_dir}/DotFiles/jdk/lin/src.zip
tar xf ${build_dir}/jdk.mac.tar.gz -C ${nuget_dir}/DotFiles/jdk/mac
rm ${nuget_dir}/DotFiles/jdk/mac/jdk/Contents/Home/src.zip
mkdir -p ${build_dir}/.dotnet
tar xf ${build_dir}/dotnet-sdk-2.2.300-linux-x64.tar.gz -C ${build_dir}/.dotnet

export JAVA_HOME=${nuget_dir}/DotFiles/jdk/lin
chmod +x ${JAVA_HOME}/bin/*
${build_dir}/../../../gradlew fatJar

${build_dir}/.dotnet/dotnet build /p:Configuration=Release ${build_dir}/../../../rd-net/Rd.sln

mono ${build_dir}/nuget.exe pack -Version $nuget_version -Symbols -SymbolPackageFormat snupkg  -OutputDirectory ${build_dir}