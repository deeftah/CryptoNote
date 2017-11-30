@echo off
java -jar %JETTY_HOME%\start.jar STOP.PORT=28282 STOP.KEY=secret --stop
