package by.saskkeee.compiler;

import by.saskkeee.annotations.*;
import by.saskkeee.annotations.vmprotect.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class SimpleObfuscator {
    
    private static final Map<String, String> nameMap = new HashMap<>();
    private static int counter = 0;
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java SimpleObfuscator <input_dir> <output_dir>");
            return;
        }
        
        String inputDir = args[0];
        String outputDir = args[1];
        
        try {
            obfuscateDirectory(new File(inputDir), new File(outputDir));
            System.out.println("Obfuscation complete!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void obfuscateDirectory(File input, File output) throws IOException {
        if (!output.exists()) {
            output.mkdirs();
        }
        
        Files.walk(input.toPath())
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".class"))
            .forEach(path -> {
                try {
                    obfuscateClass(path.toFile(), output);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
    }
    
    private static void obfuscateClass(File classFile, File outputDir) throws IOException {
        byte[] classBytes = Files.readAllBytes(classFile.toPath());
        byte[] obfuscated = applySimpleObfuscation(classBytes);
        
        File outputFile = new File(outputDir, classFile.getName());
        Files.write(outputFile.toPath(), obfuscated);
    }
    
    private static byte[] applySimpleObfuscation(byte[] classBytes) {
        for (int i = 0; i < classBytes.length; i++) {
            if (i % 3 == 0) {
                classBytes[i] = (byte) (classBytes[i] ^ 0x42);
            }
        }
        return classBytes;
    }
    
    private static String generateObfuscatedName() {
        return "a" + (counter++);
    }
}
