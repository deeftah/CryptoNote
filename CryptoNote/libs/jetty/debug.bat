@echo off
set OPT1=-agentlib:jdwp=transport=dt_socket,address=localhost:8000,server=y,suspend=y
set INST=-Dfr.cryptonote.instance=A1
java %OPT1% %INST% -jar %JETTY_HOME%\start.jar  STOP.PORT=28282 STOP.KEY=secret
