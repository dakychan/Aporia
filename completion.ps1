param(
    [string]$Mode = "menu"
)

$ROOT = Split-Path -Parent $MyInvocation.MyCommand.Path
$SBORKA = Join-Path $ROOT "sborka"
$SRC = Join-Path $ROOT "src"
$LIBS = Join-Path $ROOT "libs"
$CLASSES = Join-Path $SBORKA "classes"
$MERGE = Join-Path $SBORKA "merge"
$CACHE = Join-Path $SBORKA ".build_cache"
$HASH_FILE = Join-Path $CACHE "hashes.json"
$LIB_HASH_FILE = Join-Path $CACHE "lib_hashes.json"
$KOTLINC = "D:\moved\kotlin-master\dist\kotlinc\lib\kotlin-compiler.jar"
$TLAUNCHER_JSON = "$env:USERPROFILE\AppData\Roaming\.tlauncher\legacy\Minecraft\game\versions\Aporia\Aporia.json"

$CP = $null
function Get-CP {
    if ($CP -eq $null) {
        $CP = (Get-ChildItem -Path $LIBS -Filter "*.jar" | ForEach-Object { $_.FullName }) -join ";"
    }
    $CP
}

function Get-FileHashes($dir, $filter) {
    $h = @{}
    Get-ChildItem -Path $dir -Recurse -Include $filter -ErrorAction SilentlyContinue | ForEach-Object {
        $rel = $_.FullName.Substring($dir.Length + 1)
        $h[$rel] = (Get-FileHash $_.FullName -Algorithm MD5).Hash
    }
    $h
}

function SrcToClass($rel) {
    $noExt = [System.IO.Path]::ChangeExtension($rel, $null)
    "$CLASSES\$noExt.class"
}

function Remove-ClassFiles($rel) {
    $noExt = [System.IO.Path]::ChangeExtension($rel, $null)
    $dir = [System.IO.Path]::GetDirectoryName("$CLASSES\$noExt")
    $base = [System.IO.Path]::GetFileNameWithoutExtension($rel)
    if (Test-Path $dir) {
        Get-ChildItem -Path $dir -Filter "$base*.class" -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
    }
}

function Invoke-Compile {
    New-Item -ItemType Directory -Path $CACHE -Force | Out-Null
    New-Item -ItemType Directory -Path $CLASSES -Force | Out-Null

    $current = Get-FileHashes $SRC @("*.kt", "*.java")
    $cached = @{}
    if (Test-Path $HASH_FILE) {
        $json = Get-Content $HASH_FILE -Raw | ConvertFrom-Json
        $cached = @{}
        $json.psobject.properties | ForEach-Object { $cached[$_.Name] = $_.Value }
    }

    $changed = $false
    if ($cached.Count -ne $current.Count) { $changed = $true }
    else {
        foreach ($k in $current.Keys) {
            if (-not $cached.ContainsKey($k) -or $cached[$k] -ne $current[$k]) { $changed = $true; break }
        }
    }

    if (-not $changed) {
        Write-Host "[CACHED] 0 изменений — скип компиляции" -ForegroundColor Green
        return $false
    }

    Write-Host "=== КОМПИЛЯЦИЯ ===" -ForegroundColor Cyan

    # Clean classes but keep dir
    Remove-Item -Recurse -Force "$CLASSES\*" -ErrorAction SilentlyContinue

    $cp = Get-CP

    $ktTime = Measure-Command {
        Write-Host "[K2] Kotlin..." -NoNewline -ForegroundColor Yellow
        & java -Xmx4g -jar $KOTLINC -classpath "$cp" "$SRC" -d "$CLASSES" -jvm-target 26 -java-parameters 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            & java -Xmx4g -jar $KOTLINC -classpath "$cp" "$SRC" -d "$CLASSES" -jvm-target 26 -java-parameters
            Write-Host " FAIL" -ForegroundColor Red; exit 1
        }
        Write-Host " OK" -ForegroundColor Green
    }
    Write-Host "  K2: $($ktTime.TotalSeconds.ToString('0.0'))s"

    $javaFiles = Get-ChildItem -Path $SRC -Recurse -Filter "*.java"
    if ($javaFiles.Count -gt 0) {
        $javacTime = Measure-Command {
            Write-Host "[Javac] Java..." -NoNewline -ForegroundColor Yellow
            $srcList = Join-Path $SBORKA "sources.txt"
            $javaFiles.FullName | Set-Content $srcList -Force
            $javacArgs = @("-d", "$CLASSES", "-sourcepath", "$SRC", "--release", "26", "-encoding", "UTF-8", "-cp", "$cp;$CLASSES", "-processorpath", "$LIBS\lombok.jar", "-J-Xmx4g", "@$srcList")
            & javac @javacArgs 2>&1 | Out-Null
            if ($LASTEXITCODE -ne 0) {
                & javac @javacArgs
                Remove-Item $srcList -Force -ErrorAction SilentlyContinue
                Write-Host " FAIL" -ForegroundColor Red; exit 1
            }
            Remove-Item $srcList -Force -ErrorAction SilentlyContinue
            Write-Host " OK" -ForegroundColor Green
        }
        Write-Host "  Javac: $($javacTime.TotalSeconds.ToString('0.0'))s"
    }

    # Save updated hashes
    $current | ConvertTo-Json -Compress | Set-Content $HASH_FILE -Force
    Write-Host "=== КОМПИЛЯЦИЯ ГОТОВА ===" -ForegroundColor Cyan
    return $true
}

function Invoke-Merge {
    $rebuildMerge = $false
    if (Test-Path $LIB_HASH_FILE) {
        $jsonLib = Get-Content $LIB_HASH_FILE -Raw | ConvertFrom-Json
    $cachedLib = @{}
    $jsonLib.psobject.properties | ForEach-Object { $cachedLib[$_.Name] = $_.Value }
        $currentLib = Get-FileHashes $LIBS "*.jar"
        if ($cachedLib.Count -ne $currentLib.Count) { $rebuildMerge = $true }
        else {
            foreach ($k in $currentLib.Keys) {
                if (-not $cachedLib.ContainsKey($k) -or $cachedLib[$k] -ne $currentLib[$k]) { $rebuildMerge = $true; break }
            }
        }
    } else { $rebuildMerge = $true }

    if ((-not $rebuildMerge) -and (Test-Path $MERGE) -and (Test-Path "$MERGE\so")) {
        Write-Host "[CACHED] Libs без изменений — мердж скип" -ForegroundColor Green
        return
    }

    Write-Host "[Merge] Распаковка libs..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $MERGE -Force | Out-Null
    Remove-Item -Recurse -Force "$MERGE\*" -ErrorAction SilentlyContinue

    $idx = 0
    Get-ChildItem -Path $LIBS -Filter "*.jar" | ForEach-Object {
        $idx++
        $tmp = Join-Path $SBORKA "_tmp_$idx"
        New-Item -ItemType Directory -Path $tmp -Force | Out-Null
        & jar --extract --file $_.FullName -C $tmp 2>&1 | Out-Null
        robocopy $tmp $MERGE /E /R:0 /W:0 | Out-Null
        Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
    }
    $currentLib | ConvertTo-Json -Compress | Set-Content $LIB_HASH_FILE -Force
}

function Invoke-BuildZip {
    Invoke-Merge

    Write-Host "[Copy] Classes + ресурсы..." -ForegroundColor Yellow
    robocopy $CLASSES $MERGE /E /R:0 /W:0 | Out-Null

    @("data", "mediaplayerinfo") | ForEach-Object {
        $p = Join-Path $SRC $_
        if (Test-Path $p) { robocopy $p (Join-Path $MERGE $_) /E /R:0 /W:0 | Out-Null }
    }

    if (Test-Path "$SRC\assets") {
        Get-ChildItem -Path "$SRC\assets" -Directory | ForEach-Object {
            robocopy $_.FullName "$MERGE\assets\$($_.Name)" /E /R:0 /W:0 | Out-Null
        }
    }

    Write-Host "[META-INF] Чистка..." -ForegroundColor Yellow
    if (Test-Path "$SRC\META-INF\MANIFEST.MF") { Copy-Item "$SRC\META-INF\MANIFEST.MF" "$SBORKA\_manifest_backup.MF" -Force -ErrorAction SilentlyContinue }
    if (Test-Path "$MERGE\META-INF") {
        $metaServices = Join-Path $SBORKA "_meta_services"
        $metaKotlin = Join-Path $SBORKA "_meta_kotlin"
        New-Item -ItemType Directory -Path $metaServices -Force | Out-Null
        New-Item -ItemType Directory -Path $metaKotlin -Force | Out-Null
        if (Test-Path "$MERGE\META-INF\services") { robocopy "$MERGE\META-INF\services" $metaServices /E /R:0 /W:0 | Out-Null }
        if (Test-Path "$MERGE\META-INF\kotlin") { robocopy "$MERGE\META-INF\kotlin" $metaKotlin /E /R:0 /W:0 | Out-Null }
        & cmd /c "rd /s /q `"$MERGE\META-INF`" 2>nul"
        New-Item -ItemType Directory -Path "$MERGE\META-INF" -Force | Out-Null
        if (Test-Path "$metaServices") { robocopy $metaServices "$MERGE\META-INF\services" /E /R:0 /W:0 | Out-Null }
        if (Test-Path "$metaKotlin") { robocopy $metaKotlin "$MERGE\META-INF\kotlin" /E /R:0 /W:0 | Out-Null }
        Remove-Item -Recurse -Force $metaServices -ErrorAction SilentlyContinue
        Remove-Item -Recurse -Force $metaKotlin -ErrorAction SilentlyContinue
    }
    if (Test-Path "$SBORKA\_manifest_backup.MF") { Move-Item "$SBORKA\_manifest_backup.MF" "$MERGE\META-INF\MANIFEST.MF" -Force -ErrorAction SilentlyContinue }

    $JAR_MCP = Join-Path $SBORKA "Aporia.mcp.jar"
    if (Test-Path "$SRC\META-INF\MANIFEST.MF") {
        & jar --create --file $JAR_MCP --manifest "$SRC\META-INF\MANIFEST.MF" -C $MERGE . 2>&1 | Out-Null
    } else {
        & jar --create --file $JAR_MCP -C $MERGE . 2>&1 | Out-Null
    }
    if ($LASTEXITCODE -ne 0) { Write-Host "[ERROR] JAR failed!" -ForegroundColor Red; exit 1 }

    $JAR_OUT = Join-Path $SBORKA "Aporia.jar"
    Copy-Item $JAR_MCP $JAR_OUT -Force

    $zipDir = Join-Path $SBORKA "_to_zip"
    New-Item -ItemType Directory -Path $zipDir -Force | Out-Null
    Copy-Item $JAR_OUT "$zipDir\Aporia.jar" -Force
    if (Test-Path $TLAUNCHER_JSON) { Copy-Item $TLAUNCHER_JSON "$zipDir\Aporia.json" -Force }

    $zipFile = Join-Path $SBORKA "Aporia.zip"
    Remove-Item $zipFile -Force -ErrorAction SilentlyContinue
    & jar --create --file $zipFile -C $zipDir . 2>&1 | Out-Null

    $size = (Get-Item $zipFile).Length
    $sizeMB = [math]::Round($size / 1MB, 2)
    Write-Host "[OK] Aporia.zip — ${sizeMB}MB" -ForegroundColor Green

    if ($size -gt 1MB) {
        $releaseZip = Join-Path $SBORKA "Aporia_RELEASE.zip"
        Copy-Item $zipFile $releaseZip -Force
        Write-Host "[OK] Aporia_RELEASE.zip" -ForegroundColor Green
    }

    Remove-Item -Recurse -Force $zipDir -ErrorAction SilentlyContinue
}

function Invoke-Run {
    Invoke-Merge
    robocopy $CLASSES $MERGE /E /R:0 /W:0 | Out-Null

    $cp = Get-CP
    $runCp = "$MERGE;$cp;$SRC"
    Write-Host "=== ЗАПУСК ===" -ForegroundColor Cyan
    & java -Xmx4g -Xms2g -cp $runCp mcp.client.Start --username protect3ed
}

function Show-Menu {
    $choice = Read-Host "`n  [1] Build + Zip`n  [2] Run (dev)`n  [3] Build + Run`n`n  >> Choose (1/2/3)"
    switch ($choice) {
        "1" { Invoke-Compile; Invoke-BuildZip }
        "2" { Invoke-Compile; Invoke-Run }
        "3" { Invoke-Compile; Invoke-BuildZip; Invoke-Run }
        default { Write-Host "Invalid choice" -ForegroundColor Red; Show-Menu }
    }
}

switch ($Mode) {
    { $_ -in @("build", "1") } { Invoke-Compile; Invoke-BuildZip }
    { $_ -in @("run", "2") } { Invoke-Compile; Invoke-Run }
    { $_ -in @("menu", "3") } { Show-Menu }
    default { Show-Menu }
}
