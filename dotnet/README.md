# GameLib18xx
.Net Standard 2.0 library for creating 18xx games.

This library was originally ported from the java [Rails-18xx Project](https://github.com/Rails-18xx/Rails)

## .Net Standard 2.0
If you aren't famililar with .Net Standard, it's a cross-platform standardized API specification for implementing .NET runtimes.  There are a number of implementations of .Net Standard as listed at:

https://docs.microsoft.com/en-us/dotnet/standard/net-standard

This library can be used to create games for virtually any OS in wide use today.  Development can be done in [Unity3d](https://unity3d.com), [Xamarin](https://www.xamarin.com) or using native Windows technologies like WPF and UWP.

Additionally, [.Net Core 2.x](https://github.com/dotnet/core) implements .Net Standard 2.0, so an application can be written with completely open source technologies, if desired.

## Building on Windows
The library is almost completely self-contained, and only depends on one 3rd-party nuget package, Newtonsoft's excellent Json.Net.  After restoring packages, the GameLib18xx library should just build in Visual Studio.

## Building on Linux
Since .Net Core is open source, and designed to be used on linux servers, it's fairly easy to build the library on linux also.

Follow the instructions at https://docs.microsoft.com/en-us/dotnet/core/linux-prerequisites?tabs=netcore2x to install the .Net Core SDK.

After installing the prerequisites, building the library is as simple as:
```
cd GameLib18xx
dotnet build
```

## Creating iOS, Android and UWP apps
There are 2 different technologies that are great for developing cross-platform applications using this library.

### Xamarin Forms, Xamarin.iOS, Xamarin.Android
Xamarin Forms can be used to create a shared project that can target UWP, Android and iOS.  Applications using Xamarin Forms have the UI developed using XAML and the Xamarin Forms editor.  At runtime things are translated to native controls.  This allows more code sharing, but may not be as "native" as you'd like.

Xamarin.iOS and Xamarin.Android use the native GUI builder for each platform, and therefore can use all of the platform-specific features of each OS.

### Unity3d
Unity uses mono for it's scripting engine, and therefore a .Net Standard library can be used in a Unity3d app.  

## Licensing
The source code this library was ported from is available under the terms of the GPLv2 license, and as such the code in this library is also made available under the same terms.

Additionally, code in the NonGPL directory is also available under other licenses.

When adding new code, please use the NonGPL code directory (for code not derived from the GPL code)

