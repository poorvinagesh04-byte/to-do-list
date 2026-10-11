@echo off
set "MAVEN_HOME=C:\Users\User\apache-maven-3.9.9"
if exist "%MAVEN_HOME%\bin\mvn.cmd" (
    "%MAVEN_HOME%\bin\mvn.cmd" %*
) else (
    mvn %*
)

