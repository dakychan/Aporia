/*
 * Copyright (c) 2025-2026 BEVoid Project
 * Distributed under the BEVoid Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.assets;

import aporia.cc.OsManager;
import so.aporia.utils.files.FilesManager;
import so.aporia.utils.user.logger.Logger;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * AssetManager — скачивает aporia.apr архив и распаковывает в .assets
 */
public final class AssetManager {
    private static final String ASSETS_URL = "https://raw.githubusercontent.com/aporia-xyz/files/refs/heads/main/aporia.apr";
    private static final String ASSETS_DIR = ".assets";
    
    public static void downloadAssets() {
        Path assetsDir = FilesManager.ROOT.resolve(ASSETS_DIR);
        
        if (Files.exists(assetsDir) && hasValidAssets(assetsDir)) {
            return;
        }
        
        try {
            byte[] aprData = downloadFile(ASSETS_URL);
            
            if (Files.exists(assetsDir)) {
                deleteDirectory(assetsDir);
            }
            Files.createDirectories(assetsDir);
            
            extractAprArchive(aprData, assetsDir);
            
            if (OsManager.getPlatform() == OsManager.Platform.WINDOWS) {
                hideWindowsDirectory(assetsDir);
            }
        } catch (Exception e) {
            Logger.error("Failed to download assets: " + e.getMessage());
        }
    }
    
    private static boolean hasValidAssets(Path assetsDir) {
        return Files.exists(assetsDir.resolve("aporia/fonts")) && 
               Files.exists(assetsDir.resolve("aporia/shaders"));
    }
    
    private static void extractAprArchive(byte[] aprData, Path targetDir) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(aprData);
             ZipInputStream zis = new ZipInputStream(bais)) {
            
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve("aporia").resolve(entry.getName());
                
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath);
                }
            }
        }
    }
    
    private static byte[] downloadFile(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        try (InputStream in = url.openStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            
            return out.toByteArray();
        }
    }
    
    private static void hideWindowsDirectory(Path dir) {
        if (OsManager.getPlatform() != OsManager.Platform.WINDOWS) {
            return;
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder("attrib", "+s", "+h", dir.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            Logger.warn("Failed to hide directory: " + e.getMessage());
        }
    }
    
    private static void deleteDirectory(Path dir) throws IOException {
        Files.walk(dir)
            .sorted(Comparator.reverseOrder())
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    Logger.warn("Failed to delete: " + path);
                }
            });
    }
}

