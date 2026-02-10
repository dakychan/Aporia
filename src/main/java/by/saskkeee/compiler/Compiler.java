package by.saskkeee.compiler;

import java.io.File;

public class Compiler {
    
    public static void main(String[] args) {
        System.out.println("=== Saskkeee Simple Obfuscator ===");
        System.out.println("Version: 1.0");
        System.out.println();
        
        if (args.length < 2) {
            printUsage();
            return;
        }
        
        String inputPath = args[0];
        String outputPath = args[1];
        
        File inputDir = new File(inputPath);
        File outputDir = new File(outputPath);
        
        if (!inputDir.exists()) {
            System.err.println("Error: Input directory does not exist: " + inputPath);
            return;
        }
        
        try {
            System.out.println("[Compiler] Starting compilation...");
            System.out.println();
            
            BytecodeObfuscator.obfuscate(inputDir, outputDir);
            
            System.out.println();
            System.out.println("[Compiler] Compilation successful!");
            System.out.println("[Compiler] Output: " + outputDir.getAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("[Compiler] Compilation failed!");
            e.printStackTrace();
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage: java -jar compiler.jar <input_dir> <output_dir>");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  input_dir  - Directory containing compiled .class files");
        System.out.println("  output_dir - Directory where obfuscated files will be saved");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  java -jar compiler.jar build/classes/java/main build/obfuscated");
    }
}
