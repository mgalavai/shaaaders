@echo off
set DIR=%~dp0
set WRAPPER_JAR=%DIR%gradle\wrapper\gradle-wrapper.jar
set WRAPPER_SHARED_JAR=%DIR%gradle\wrapper\gradle-wrapper-shared.jar
if not exist "%WRAPPER_JAR%" (
  echo Missing gradle-wrapper.jar at %WRAPPER_JAR%
  exit /b 1
)

set CLASSPATH=%WRAPPER_JAR%
if exist "%WRAPPER_SHARED_JAR%" (
  set CLASSPATH=%CLASSPATH%;%WRAPPER_SHARED_JAR%
)

if defined JAVA_HOME (
  "%JAVA_HOME%\bin\java" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
) else (
  java -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
)
