@echo off
call polymer build --root src\main\webapp\var
echo WEB-INF ...
mkdir build\default\src\main\webapp\WEB-INF
xcopy src\main\webapp\WEB-INF build\default\src\main\webapp\WEB-INF /E /Q
echo ... done