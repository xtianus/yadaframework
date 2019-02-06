@ECHO OFF

pushd %~dp0

REM javasphinx
REM TODO javasphinx-apidoc -c ../build/javasphinx-apidoc -u -o javadoc ../../main/java

REM Command file for Sphinx documentation

if "%SPHINXBUILD%" == "" (
	set SPHINXBUILD=sphinx-build
)
set SOURCEDIR=src
set BUILDDIR=build
set SPHINXPROJ=YadaFramework

set TARGET=html
REM if "%1" == "" goto help

%SPHINXBUILD% >NUL 2>NUL
if errorlevel 9009 (
	echo.
	echo.The 'sphinx-build' command was not found. Make sure you have Sphinx
	echo.installed, then set the SPHINXBUILD environment variable to point
	echo.to the full path of the 'sphinx-build' executable. Alternatively you
	echo.may add the Sphinx directory to PATH.
	echo.
	echo.If you don't have Sphinx installed, grab it from
	echo.http://sphinx-doc.org/
	exit /b 1
)

%SPHINXBUILD% -M %TARGET% %SOURCEDIR% %BUILDDIR% %SPHINXOPTS%
goto end

:help
%SPHINXBUILD% -M help %SOURCEDIR% %BUILDDIR% %SPHINXOPTS%

:end
popd

REM Copy _static files too when changed
xcopy %SOURCEDIR%\_static %BUILDDIR%\html\_static /f/d/y/s/e


REM pause
