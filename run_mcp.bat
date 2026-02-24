@echo off
chcp 65001 >nul
setlocal

set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.10"

if not exist "run" mkdir "run"
cd "run"

echo Starting Aporia MCP Client...

"%JAVA_HOME%\bin\java.exe" -Xmx4G -Xms2G ^
    --enable-native-access=ALL-UNNAMED ^
    -Dfile.encoding=UTF-8 ^
    -cp "../src/classes;../src;../src/assets;../src/data;../libs/*" ^
    net.minecraft.client.main.Main ^
    --gameDir . ^
    --version 1.21.11 ^
    --assetsDir ../src/assets ^
    --assetIndex 1.21 ^
    --accessToken 0 ^
    --username DUsky2 ^
    --uuid 0 ^
    --versionType release