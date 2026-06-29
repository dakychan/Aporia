@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
set "ROOT=%~dp0"
set "SBORKA=%ROOT%sborka"
set "SRC=%ROOT%src"
set "LIBS=%ROOT%libs"
set "CLASSES=%SBORKA%\classes"
set "MERGE=%SBORKA%\merge"
set "JAR_MCP=%SBORKA%\Aporia.mcp.jar"
set "JAR_OUT=%SBORKA%\Aporia.jar"
set "OBFUSCATOR=D:\moved\chaos-obfuscator\obfuscator\build\libs\ChaosObfuscator.jar"
set "TLAUNCHER_JSON=C:\Users\kotay\AppData\Roaming\.tlauncher\legacy\Minecraft\game\versions\Aporia\Aporia.json"

echo [1/10] Cleaning previous build...
if exist "%CLASSES%" rmdir /s /q "%CLASSES%"
if exist "%MERGE%" rmdir /s /q "%MERGE%"
if exist "%JAR_MCP%" del /f /q "%JAR_MCP%"
if exist "%JAR_OUT%" del /f /q "%JAR_OUT%"
mkdir "%CLASSES%" 2>nul
mkdir "%MERGE%" 2>nul

echo [2/10] Building classpath file for K2 compiler...
if exist "%SBORKA%\classpath.txt" del /f /q "%SBORKA%\classpath.txt"
echo -classpath >> "%SBORKA%\classpath.txt"
set "FULL_CP="
for %%j in ("%LIBS%\*.jar") do (
    if not defined FULL_CP (
        set "FULL_CP=%%j"
    ) else (
        set "FULL_CP=!FULL_CP!;%%j"
    )
)
echo !FULL_CP! >> "%SBORKA%\classpath.txt"

echo [3/10] Finding source files...
if exist "%SBORKA%\sources.txt" del /f /q "%SBORKA%\sources.txt"
if exist "%SBORKA%\kotlinsources.txt" del /f /q "%SBORKA%\kotlinsources.txt"

:: Ищем Котлин списком (их мало)
dir /s /b "%SRC%\*.kt" > "%SBORKA%\kotlinsources.txt" 2>nul
for /f %%a in ('type "%SBORKA%\kotlinsources.txt" 2^>nul ^| find /c /v ""') do set "KT_COUNT=%%a"
echo        Found %KT_COUNT% .kt files

echo [4/10] Compiling Kotlin via K2...
:: Сначала компилируем только Котлин, скармливая ему ПАПКУ src, чтобы он видел заглушки Java
java -Xmx4g -jar "D:\moved\kotlin-master\dist\kotlinc\lib\kotlin-compiler.jar" @"%SBORKA%\classpath.txt" "%SRC%" -d "%CLASSES%" -jvm-target 26 -java-parameters
if errorlevel 1 (echo [ERROR] Kotlin compilation failed!&exit /b 1)
echo        Kotlin compilation successful

echo [4.5/10] Compiling ALL Java files via javac...
:: Чтобы javac не упал из-за длинной строки, мы просим его найти все java-файлы рекурсивно внутри папки src.
:: Подключаем %CLASSES% в classpath, чтобы Java видела уже скомпилированный Котлин!
dir /s /b "%SRC%\*.java" > "%SBORKA%\sources.txt" 2>nul
javac -d "%CLASSES%" -sourcepath "%SRC%" --release 26 -encoding UTF-8 -cp "%FULL_CP%;%CLASSES%" -processorpath "%LIBS%\lombok.jar" -J-Xmx4g @"%SBORKA%\sources.txt"
if errorlevel 1 (echo [ERROR] Java compilation failed!&exit /b 1)
echo        Java compilation successful

echo [5/10] Extracting libs into merge directory...
set "IDX=0"
for %%j in ("%LIBS%\*.jar") do (
    set /a IDX+=1
    mkdir "%SBORKA%\_tmp_!IDX!" 2>nul
    pushd "%SBORKA%\_tmp_!IDX!"
    jar --extract --file "%%j" >nul 2>&1
    popd
    robocopy "%SBORKA%\_tmp_!IDX!" "%MERGE%" /E /R:0 /W:0 >nul
    rmdir /s /q "%SBORKA%\_tmp_!IDX!" 2>nul
)
echo        Extracted %IDX% jars

echo [6/10] Copying compiled classes...
:: Копируем ВСЕ классы (и Котлин, и Майнкрафт из папок net и com)
robocopy "%CLASSES%" "%MERGE%" /E /R:0 /W:0 >nul

echo [7/10] Copying target resources from src...
if exist "%SRC%\data" robocopy "%SRC%\data" "%MERGE%\data" /E /R:0 /W:0 >nul
if exist "%SRC%\mediaplayerinfo" robocopy "%SRC%\mediaplayerinfo" "%MERGE%\mediaplayerinfo" /E /R:0 /W:0 >nul
if exist "%SRC%\assets\minecraft" robocopy "%SRC%\assets\minecraft" "%MERGE%\assets\minecraft" /E /R:0 /W:0 >nul
if exist "%SRC%\assets\aporia" robocopy "%SRC%\assets\aporia" "%MERGE%\assets\aporia" /E /R:0 /W:0 >nul
if exist "%SRC%\assets\indexes" robocopy "%SRC%\assets\indexes" "%MERGE%\assets\indexes" /E /R:0 /W:0 >nul
if exist "%SRC%\assets\natives" robocopy "%SRC%\assets\natives" "%MERGE%\assets\natives" /E /R:0 /W:0 >nul

echo        Cleaning META-INF...
if exist "%MERGE%\META-INF" (
    if exist "%SRC%\META-INF\MANIFEST.MF" copy /y "%SRC%\META-INF\MANIFEST.MF" "%SBORKA%\manifest_backup.MF" >nul
    rmdir /s /q "%MERGE%\META-INF"
    mkdir "%MERGE%\META-INF" 2>nul
    if exist "%SBORKA%\manifest_backup.MF" (
        copy /y "%SBORKA%\manifest_backup.MF" "%MERGE%\META-INF\MANIFEST.MF" >nul
        del /f /q "%SBORKA%\manifest_backup.MF" 2>nul
    )
)

echo [8/10] Creating Aporia.mcp.jar...
if exist "%SRC%\META-INF\MANIFEST.MF" (
    jar --create --file "%JAR_MCP%" --manifest "%SRC%\META-INF\MANIFEST.MF" -C "%MERGE%" .
) else (
    jar --create --file "%JAR_MCP%" -C "%MERGE%" .
)
if errorlevel 1 (echo [ERROR] JAR creation failed!&exit /b 1)
for %%f in ("%JAR_MCP%") do echo        Aporia.mcp.jar — %%~zf bytes

:: ========== [9/10] OBFUSCATOR DISABLED ==========
:: if exist "%OBFUSCATOR%" (
::     echo [9/10] Running ChaosObfuscator...
::     java -Xmx4g -jar "%OBFUSCATOR%" --jar "%JAR_MCP%" "%JAR_OUT%"
::     if errorlevel 1 (echo [ERROR] Obfuscation failed!&exit /b 1)
:: ) else (
::     copy /y "%JAR_MCP%" "%JAR_OUT%" >nul
:: )
copy /y "%JAR_MCP%" "%JAR_OUT%" >nul

echo [10/10] Packaging Aporia.jar + Aporia.json ^> Aporia.zip...
if exist "%SBORKA%\_to_zip" rmdir /s /q "%SBORKA%\_to_zip"
mkdir "%SBORKA%\_to_zip" 2>nul

:: Копируем файлы во временную папку
if exist "%JAR_OUT%" copy /y "%JAR_OUT%" "%SBORKA%\_to_zip\Aporia.jar" >nul
if exist "%TLAUNCHER_JSON%" copy /y "%TLAUNCHER_JSON%" "%SBORKA%\_to_zip\Aporia.json" >nul

:: Проверяем и пакуем по абсолютному пути
if exist "%SBORKA%\Aporia.zip" del /f /q "%SBORKA%\Aporia.zip"
echo        Packing files via Java JAR tool...
jar --create --file "%SBORKA%\Aporia.zip" -C "%SBORKA%\_to_zip" .
for %%f in ("%SBORKA%\Aporia.zip") do echo        Aporia.zip — %%~zf bytes


:: Выдаем ахуенное уведомление на рабочий стол
powershell -NoProfile -Command "$w=(New-Object -ComObject WScript.Shell); $w.Popup('Ночной билд Апории успешно собран!',4,'Aporia Client',64)" >nul

echo ============================================
echo   BUILD COMPLETE -> sborka\Aporia.zip
echo ============================================

