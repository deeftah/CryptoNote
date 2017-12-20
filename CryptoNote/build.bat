@echo off
set bp=%cd%
call polymer build --root src\main\webapp\var
echo WEB-INF ...
cd build\default\src\main\webapp
mkdir WEB-INF
xcopy "%bp%\src\main\webapp\WEB-INF" WEB-INF /E /Q
copy "%bp%\src\main\webapp\favicon.ico" .
set jar=C:\Program Files\Java\jdk1.8.0_45\bin\jar.exe
"%jar%" -cf "%bp%\build\cn.war" *
cd %bp%
echo ... done