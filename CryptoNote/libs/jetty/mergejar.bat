@echo off
set PATH=%PATH%;C:\Program Files\Java\jdk1.8.0_45\bin
rmdir /S /Q build
mkdir build
cd build
jar -xf ..\postgresql-42.1.4.jar
jar -xf ..\jetty-util.jar
jar -xf ..\jetty-jndi.jar
jar -xf ..\jetty-plus.jar
jar -xf ..\start.jar
jar -cvfm ..\start2.jar META-INF\MANIFEST.MF .
cd ..
rmdir /S /Q build
