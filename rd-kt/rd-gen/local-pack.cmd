set BUILD_DIR=%~dp0\build
set NUGET_DIR=%BUILD_DIR%\nuget
set NUGET_VERSION="193.0.999-local"
set CONFIGURATION=Debug
set NUGET_LOCAL_SOURCE=%BUILD_DIR%\NuGetLocalSource

mkdir %BUILD_DIR%
mkdir %NUGET_LOCAL_SOURCE%

call %BUILD_DIR%\..\..\..\gradlew fatJar

mkdir %NUGET_DIR%\lib\net
echo "" > %NUGET_DIR%\lib\net\_._

call dotnet build /p:Configuration=%CONFIGURATION% /p:PackageVersion=%NUGET_VERSION% %BUILD_DIR%\..\..\..\rd-net\Rd.sln
call dotnet pack --include-symbols /p:Configuration=%CONFIGURATION% /p:PackageVersion=%NUGET_VERSION% %BUILD_DIR%\..\..\..\rd-net\Lifetimes\Lifetimes.csproj
call dotnet pack --include-symbols /p:Configuration=Release /p:PackageVersion=%NUGET_VERSION% %BUILD_DIR%\..\..\..\rd-net\RdFramework\RdFramework.csproj
call dotnet pack --include-symbols /p:Configuration=Release /p:PackageVersion=%NUGET_VERSION% %BUILD_DIR%\..\..\..\rd-net\RdFramework.Reflection\RdFramework.Reflection.csproj
move %BUILD_DIR%\..\..\..\rd-net\RdFramework.Reflection\bin\Release\*.nupkg %NUGET_LOCAL_SOURCE%
move %BUILD_DIR%\..\..\..\rd-net\RdFramework.Reflection\bin\Release\*.snupkg %NUGET_LOCAL_SOURCE%
move %BUILD_DIR%\..\..\..\rd-net\RdFramework\bin\Release\*.nupkg %NUGET_LOCAL_SOURCE%
move %BUILD_DIR%\..\..\..\rd-net\RdFramework\bin\Release\*.snupkg %NUGET_LOCAL_SOURCE%
move %BUILD_DIR%\..\..\..\rd-net\Lifetimes\bin\Release\*.nupkg %NUGET_LOCAL_SOURCE%
move %BUILD_DIR%\..\..\..\rd-net\Lifetimes\bin\Release\*.snupkg %NUGET_LOCAL_SOURCE%
call nuget pack -Version %NUGET_VERSION% -OutputDirectory %NUGET_LOCAL_SOURCE% JetBrains.RdGen.nuspec