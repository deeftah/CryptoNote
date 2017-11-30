@echo off
set INST=-Dfr.cryptonote.instance=A1
java %INST% -jar %JETTY_HOME%\start.jar STOP.PORT=28282 STOP.KEY=secret
