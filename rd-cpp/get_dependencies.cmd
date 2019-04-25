:<<"::CMDLITERAL"
@ECHO OFF
GOTO :CMDSCRIPT
::CMDLITERAL

set -x
git clone https://github.com/google/googletest.git
exit 0

:CMDSCRIPT
@echo on
git clone https://github.com/google/googletest.git