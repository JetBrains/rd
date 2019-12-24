Auto update RdFramework and Lifetimes
=====

This target intended to use with dotnet-products repository to help with
auto-update dll in `Packages` in `Bin.<hive>` folder.

To use this target:
1. Copy file Directory.Build.targets to rd-net\RdFramework\
2. Update `<DotnetProducts>` and `<DotnetHive>` properties.

On every build dll's will be replaced.

You can invoke `Clean` target to restore original version from NuGet package