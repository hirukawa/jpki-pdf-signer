@ECHO OFF

CALL :FIND_COMMAND "MakePri.exe"  "%ProgramFiles(x86)%" "x64"
CALL :FIND_COMMAND "MakeAppx.exe" "%ProgramFiles(x86)%" "x64"
CALL :FIND_COMMAND "MakeCert.exe" "%ProgramFiles(x86)%" "x64"
CALL :FIND_COMMAND "signtool.exe" "%ProgramFiles(x86)%" "x64"
CALL :FIND_COMMAND "pvk2pfx.exe"  "%ProgramFiles(x86)%" "x64"

CALL :FIND_COMMAND "MakePri.exe"  "%ProgramFiles(x86)%" "x86"
CALL :FIND_COMMAND "MakeAppx.exe" "%ProgramFiles(x86)%" "x86"
CALL :FIND_COMMAND "MakeCert.exe" "%ProgramFiles(x86)%" "x86"
CALL :FIND_COMMAND "signtool.exe" "%ProgramFiles(x86)%" "x86"
CALL :FIND_COMMAND "pvk2pfx.exe"  "%ProgramFiles(x86)%" "x86"

CALL :SHOW_STATUS "MakePri.exe"
CALL :SHOW_STATUS "MakeAppx.exe"
CALL :SHOW_STATUS "MakeCert.exe"
CALL :SHOW_STATUS "signtool.exe"
CALL :SHOW_STATUS "pvk2pfx.exe"

GOTO START_CONSOLE

:FIND_COMMAND
SET SEARCH_CMD=%~1
SET SEARCH_DIR=%~2
SET INCLUDE_STR=%~3
WHERE "%SEARCH_CMD%" > nul 2>&1
IF ERRORLEVEL 1 (
    FOR /R "%SEARCH_DIR%" %%f IN ("*%SEARCH_CMD%") DO (
        ECHO "%%f" | FIND /I /C "%INCLUDE_STR%" > nul
        IF NOT ERRORLEVEL 1 CALL :ADD_PATH "%%f"
    )
)
EXIT /b

:ADD_PATH
SET NEWPATH=%~dp1
SET PATH=%NEWPATH:~0,-1%;%PATH%
EXIT /b

:SHOW_STATUS
SET SEARCH_CMD=%~1
WHERE "%SEARCH_CMD%" > nul 2>&1
IF ERRORLEVEL 1 (
    ECHO %SEARCH_CMD%	... not found
) ELSE (
    ECHO %SEARCH_CMD%	... found
)
EXIT /b

:START_CONSOLE
ECHO.
%COMSPEC%
