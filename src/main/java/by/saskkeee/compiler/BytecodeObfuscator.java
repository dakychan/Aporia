package by.saskkeee.compiler;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BytecodeObfuscator {
    
    private static final Random random = new Random();
    private static final Map<String, String> classNameMap = new HashMap<>();
    private static final Map<String, String> methodNameMap = new HashMap<>();
    private static final Map<String, String> fieldNameMap = new HashMap<>();
    
    private static int classCounter = 0;
    private static int methodCounter = 0;
    private static int fieldCounter = 0;
    
    public static void obfuscate(File inputDir, File outputDir) throws IOException {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        System.out.println("[Obfuscator] Starting obfuscation...");
        System.out.println("[Obfuscator] Input: " + inputDir.getAbsolutePath());
        System.out.println("[Obfuscator] Output: " + outputDir.getAbsolutePath());
        
        Files.walk(inputDir.toPath())
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".class"))
            .forEach(path -> {
                try {
                    processClassFile(path.toFile(), inputDir, outputDir);
                } catch (Exception e) {
                    System.err.println("[Obfuscator] Error processing: " + path);
                    e.printStackTrace();
                }
            });
        
        System.out.println("[Obfuscator] Obfuscation complete!");
        System.out.println("[Obfuscator] Classes renamed: " + classCounter);
        System.out.println("[Obfuscator] Methods renamed: " + methodCounter);
        System.out.println("[Obfuscator] Fields renamed: " + fieldCounter);
    }
    
    private static void processClassFile(File classFile, File inputDir, File outputDir) throws IOException {
        byte[] classBytes = Files.readAllBytes(classFile.toPath());
        
        byte[] obfuscated = applyObfuscation(classBytes);
        
        Path relativePath = inputDir.toPath().relativize(classFile.toPath());
        File outputFile = new File(outputDir, relativePath.toString());
        outputFile.getParentFile().mkdirs();
        
        Files.write(outputFile.toPath(), obfuscated);
    }
    
    private static byte[] applyObfuscation(byte[] classBytes) {
        byte[] result = Arrays.copyOf(classBytes, classBytes.length);
        
        for (int i = 0; i < result.length; i++) {
            if (i % 5 == 0) {
                result[i] = (byte) (result[i] ^ 0x5A);
            }
            if (i % 7 == 0) {
                result[i] = (byte) (result[i] ^ 0x3C);
            }
        }
        
        insertJunkBytes(result);
        
        return result;
    }
    
    private static void insertJunkBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length / 100; i++) {
            int pos = random.nextInt(bytes.length);
            bytes[pos] = (byte) random.nextInt(256);
        }
    }
    
    public static String obfuscateClassName(String originalName) {
        return classNameMap.computeIfAbsent(originalName, k -> {
            classCounter++;
            return generateName("C");
        });
    }
    
    public static String obfuscateMethodName(String originalName) {
        if (originalName.equals("<init>") || originalName.equals("<clinit>")) {
            return originalName;
        }
        return methodNameMap.computeIfAbsent(originalName, k -> {
            methodCounter++;
            return generateName("m");
        });
    }
    
    public static String obfuscateFieldName(String originalName) {
        return fieldNameMap.computeIfAbsent(originalName, k -> {
            fieldCounter++;
            return generateName("f");
        });
    }
    
    private static String generateName(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        int length = 3 + random.nextInt(5);
        for (int i = 0; i < length; i++) {
            char c = (char) ('a' + random.nextInt(26));
            if (random.nextBoolean()) {
                c = Character.toUpperCase(c);
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
