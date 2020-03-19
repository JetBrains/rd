:<<"::CMDLITERAL"
@ECHO OFF
GOTO :CMDSCRIPT
::CMDLITERAL

set -x
git clone https://github.com/google/googletest.git -b v1.10.x
exit 0

:CMDSCRIPT
@echo on
git clone https://github.com/google/googletest.git -b v1.10.x