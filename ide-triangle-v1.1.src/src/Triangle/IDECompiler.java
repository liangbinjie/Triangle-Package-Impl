package Triangle;

import Triangle.AbstractSyntaxTrees.Program;
import Triangle.CodeGenerator.Encoder;
import Triangle.ContextualAnalyzer.Checker;
import Triangle.SyntacticAnalyzer.Parser;
import Triangle.SyntacticAnalyzer.Scanner;
import Triangle.SyntacticAnalyzer.SourceFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Triangle compiler driver for IDE integration
 */
public class IDECompiler {

    private IDEReporter reporter;
    private Encoder encoder;
    private String packageName;

    public IDECompiler() {
        reporter = new IDEReporter();
        encoder = null;
        packageName = null;
    }

    /**
     * Compile a Triangle program
     * @param sourceName source file path
     * @param objectName object file path (obj.tam)
     * @param showingAST show AST
     * @param showingTable show table
     * @return true if successful
     */
    public boolean compileProgram(String sourceName, String objectName, 
                                   boolean showingAST, boolean showingTable) {
        
        System.out.println("********** Triangle Compiler (IDE Version) **********");
        
        // 1. Análisis sintáctico
        System.out.println("Syntactic Analysis ...");
        SourceFile source = new SourceFile(sourceName);
        
        // CORRECCIÓN: Verificar si el archivo existe usando File en lugar de getSource()
        File sourceFile = new File(sourceName);
        if (!sourceFile.exists()) {
            System.err.println("Can't access source file " + sourceName);
            return false;
        }
        
        Scanner scanner = new Scanner(source);
        
        // CORRECCIÓN: Crear nuevo reporter en lugar de reset()
        reporter = new IDEReporter();
        
        Parser parser = new Parser(scanner, reporter);
        Program theAST = parser.parseProgram();
        
        if (reporter.numErrors > 0) {
            System.err.println("Compilation failed with " + reporter.numErrors + " syntactic errors");
            return false;
        }
        
        // 2. Análisis contextual
        System.out.println("Contextual Analysis ...");
        Checker checker = new Checker(reporter);
        checker.check(theAST);
        
        if (reporter.numErrors > 0) {
            System.err.println("Compilation failed with " + reporter.numErrors + " contextual errors");
            return false;
        }
        
        // 3. Generación de código
        System.out.println("Code Generation ...");
        encoder = new Encoder(reporter);
        encoder.encodeRun(theAST, showingTable);
        
        if (reporter.numErrors > 0) {
            System.err.println("Compilation failed with " + reporter.numErrors + " code generation errors");
            return false;
        }
        
        // 4. Guardar código objeto
        encoder.saveObjectProgram(objectName);
        System.out.println("Object file written: " + objectName);
        
        // 5. Generar archivos de imports y relocalizaciones
        String baseDir = new File(objectName).getParent();
        if (baseDir == null) baseDir = ".";
        
        // Generar program.imp si hay imports
        if (!encoder.getImports().isEmpty()) {
            String importFile = baseDir + File.separator + "program.imp";
            if (writeLinesToFile(importFile, encoder.getImports())) {
                System.out.println("Generated import file: " + importFile);
            }
        }
        
        // Generar program.reloc si hay relocalizaciones
        if (!encoder.getRelocations().isEmpty()) {
            String relocFile = baseDir + File.separator + "program.reloc";
            if (writeLinesToFile(relocFile, encoder.getRelocations())) {
                System.out.println("Generated relocation file: " + relocFile);
            }
        }
        
        System.out.println("Compilation was successful.");
        return true;
    }

    /**
     * Compile a Triangle package
     * @param sourceName source file path
     * @param outputDir output directory for package files
     * @return true if successful
     */
    public boolean compilePackage(String sourceName, String outputDir) {
        
        System.out.println("********** Triangle Compiler (IDE Version) **********");
        
        // Crear directorio de salida si no existe
        File outDir = new File(outputDir);
        if (!outDir.exists()) {
            if (!outDir.mkdirs()) {
                System.err.println("Failed to create output directory: " + outputDir);
                return false;
            }
            System.out.println("Created package directory: " + outputDir);
        }
        
        // 1. Análisis sintáctico
        System.out.println("Syntactic Analysis ...");
        SourceFile source = new SourceFile(sourceName);
        
        // CORRECCIÓN: Verificar si el archivo existe usando File en lugar de getSource()
        File sourceFile = new File(sourceName);
        if (!sourceFile.exists()) {
            System.err.println("Can't access source file " + sourceName);
            return false;
        }
        
        Scanner scanner = new Scanner(source);
        
        // CORRECCIÓN: Crear nuevo reporter en lugar de reset()
        reporter = new IDEReporter();
        
        Parser parser = new Parser(scanner, reporter);
        Program theAST = parser.parseProgram();
        
        if (reporter.numErrors > 0) {
            System.err.println("Package compilation failed with " + reporter.numErrors + " syntactic errors");
            return false;
        }
        
        // 2. Análisis contextual
        System.out.println("Contextual Analysis ...");
        Checker checker = new Checker(reporter);
        checker.check(theAST);
        
        if (reporter.numErrors > 0) {
            System.err.println("Package compilation failed with " + reporter.numErrors + " contextual errors");
            return false;
        }
        
        // 3. Generación de código
        System.out.println("Code Generation ...");
        encoder = new Encoder(reporter);
        encoder.encodeRun(theAST, false);
        
        if (reporter.numErrors > 0) {
            System.err.println("Package compilation failed with " + reporter.numErrors + " code generation errors");
            return false;
        }
        
        // 4. Obtener nombre del paquete desde el encoder
        packageName = encoder.getCurrentPackageName();
        
        if (packageName == null) {
            System.err.println("ERROR: No package declaration found in source file");
            return false;
        }
        
        // 5. Guardar archivo TAM del paquete
        String tamFile = outputDir + File.separator + packageName + ".tam";
        encoder.saveObjectProgram(tamFile);
        System.out.println("Object file written: " + tamFile);
        
        // 6. Generar archivo .map con los exports
        if (!encoder.getExports().isEmpty()) {
            String mapFile = outputDir + File.separator + packageName + ".map";
            
            java.util.List<String> exportLines = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, Integer> entry : encoder.getExports().entrySet()) {
                String qname = packageName + "." + entry.getKey();
                exportLines.add("export " + qname + " " + entry.getValue());
            }
            
            if (writeLinesToFile(mapFile, exportLines)) {
                System.out.println("Generated map file: " + mapFile);
            }
        }
        
        System.out.println("Compilation was successful.");
        System.out.println("Package file created: " + tamFile);
        
        return true;
    }

    /**
     * Get package name after compilation
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Write lines to a file
     */
    private boolean writeLinesToFile(String filepath, java.util.List<String> lines) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filepath));
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
            writer.close();
            return true;
        } catch (IOException e) {
            System.err.println("Error writing file " + filepath + ": " + e.getMessage());
            return false;
        }
    }
}