/*
 * IDE-Triangle v1.0
 * Compiler.java 
 *
 * Version para curso Compiladores 2025
 */

package Triangle;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import Triangle.AbstractSyntaxTrees.Program;
import Triangle.CodeGenerator.Encoder;
import Triangle.ContextualAnalyzer.Checker;
import Triangle.SyntacticAnalyzer.Parser;
import Triangle.SyntacticAnalyzer.Scanner;
import Triangle.SyntacticAnalyzer.SourceFile;

public class IDECompiler {
    private String objectName = "obj.tam";
    private boolean showingAST = false;
    private boolean showingTable = false;
    
    private ErrorReporter reporter;
    private Scanner scanner;
    private Parser parser;
    private Checker checker;
    private Encoder encoder;
    
    private boolean compilingAsPackage = false;
    private String packageName = null;
    
    public IDECompiler() {
    }
    
    /**
     * Compila un archivo fuente como paquete
     */
    public boolean compilePackage(String sourceName, String outputDir) {
        // Asegurar que el directorio de salida existe
        File outDir = new File(outputDir);
        if (!outDir.exists()) {
            if (!outDir.mkdirs()) {
                System.err.println("Failed to create output directory: " + outputDir);
                return false;
            }
            System.out.println("Created output directory: " + outputDir);
        }
        
        compilingAsPackage = true;
        // Extraer nombre del paquete del archivo fuente
        packageName = extractPackageName(sourceName);
        
        String objPath = outputDir + File.separator + "obj.tam";
        boolean success = compileProgram(sourceName, objPath, false, false);
        
        if (success && packageName != null) {
            // Renombrar obj.tam a PackageName.tam
            try {
                File objFile = new File(objPath);
                File packageFile = new File(outputDir + File.separator + packageName + ".tam");
                
                if (objFile.exists()) {
                    if (packageFile.exists()) {
                        packageFile.delete();
                    }
                    if (objFile.renameTo(packageFile)) {
                        System.out.println("Package file created: " + packageFile.getAbsolutePath());
                    } else {
                        System.err.println("Failed to rename package file");
                        return false;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error renaming package file: " + e.getMessage());
                return false;
            }
        }
        
        compilingAsPackage = false;
        packageName = null;
        return success;
    }
    
    /**
     * Extrae el nombre del paquete del archivo fuente
     */
    private String extractPackageName(String sourceName) {
        File file = new File(sourceName);
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            return name.substring(0, dotIndex);
        }
        return name;
    }
    
    public boolean compileProgram(String sourceName, String objectName,
            boolean showingAST, boolean showingTable) {
        
        this.objectName = objectName;
        this.showingAST = showingAST;
        this.showingTable = showingTable;
        
        System.out.println("********** Triangle Compiler (IDE Version) **********");
        System.out.println("Syntactic Analysis ...");
        
        SourceFile source = new SourceFile(sourceName);
        
        // Verificar si el archivo fuente se pudo abrir
        if (source == null) {
            System.out.println("Can't access source file " + sourceName);
            return false;
        }
        
        reporter = new IDEReporter();
        scanner = new Scanner(source);
        parser = new Parser(scanner, reporter);
        
        Program theAST = parser.parseProgram();
        
        if (reporter.numErrors == 0) {
            System.out.println("Contextual Analysis ...");
            checker = new Checker(reporter);
            checker.check(theAST);
            
            if (showingAST) {
                // Mostrar AST si se requiere
            }
            if (showingTable) {
                // Mostrar tabla si se requiere
            }
            
            if (reporter.numErrors == 0) {
                System.out.println("Code Generation ...");
                encoder = new Encoder(reporter);
                encoder.encodeRun(theAST, showingTable);
                
                if (reporter.numErrors == 0) {
                    // Asegurar que el directorio de salida existe
                    File objFile = new File(objectName);
                    File parentDir = objFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }
                    
                    // Escribir archivo objeto
                    try (FileOutputStream objectFile = new FileOutputStream(objectName);
                         DataOutputStream objectStream = new DataOutputStream(objectFile)) {
                        
                        encoder.saveObjectProgram(objectName);
                        
                        // Generar archivos adicionales para paquetes/imports
                        generateImportFile(objectName);
                        generateRelocFile(objectName);
                        
                        if (compilingAsPackage && packageName != null) {
                            generateMapFile(objectName, packageName);
                        }
                        
                        System.out.println("Compilation was successful.");
                        return true;
                    } catch (IOException e) {
                        System.out.println("Error writing object file: " + e.getMessage());
                        return false;
                    }
                }
            }
        }
        
        System.out.println("Compilation was unsuccessful.");
        return false;
    }
    
    /**
     * Genera archivo .imp con imports del programa
     */
    private void generateImportFile(String objectName) {
        if (encoder == null) return;
        
        // Verificar si el encoder tiene el método getImports
        // Si no existe, esta funcionalidad debe implementarse en Encoder
        try {
            File objFile = new File(objectName);
            String baseDir = objFile.getParent();
            if (baseDir == null) baseDir = ".";
            
            String impFileName = baseDir + File.separator + "program.imp";
            
            // Placeholder: Esta funcionalidad requiere modificar Encoder
            // para rastrear los imports durante la compilación
            java.util.List<String> imports = new java.util.ArrayList<>();
            // imports = encoder.getImports(); // Este método debe implementarse en Encoder
            
            if (!imports.isEmpty()) {
                try (FileWriter writer = new FileWriter(impFileName)) {
                    for (String imp : imports) {
                        writer.write(imp + "\n");
                    }
                }
                System.out.println("Generated import file: " + impFileName);
            }
        } catch (IOException e) {
            System.err.println("Error generating import file: " + e.getMessage());
        }
    }
    
    /**
     * Genera archivo .reloc con información de reubicación
     */
    private void generateRelocFile(String objectName) {
        if (encoder == null) return;
        
        try {
            File objFile = new File(objectName);
            String baseDir = objFile.getParent();
            if (baseDir == null) baseDir = ".";
            
            String relocFileName = baseDir + File.separator + "program.reloc";
            
            // Placeholder: Esta funcionalidad requiere modificar Encoder
            // para rastrear las relocalizaciones durante la compilación
            java.util.List<String> relocations = new java.util.ArrayList<>();
            // relocations = encoder.getRelocations(); // Este método debe implementarse en Encoder
            
            if (!relocations.isEmpty()) {
                try (FileWriter writer = new FileWriter(relocFileName)) {
                    for (String reloc : relocations) {
                        writer.write(reloc + "\n");
                    }
                }
                System.out.println("Generated relocation file: " + relocFileName);
            }
        } catch (IOException e) {
            System.err.println("Error generating relocation file: " + e.getMessage());
        }
    }
    
    /**
     * Genera archivo .map con exports del paquete
     */
    private void generateMapFile(String objectName, String packageName) {
        if (encoder == null) return;
        
        try {
            File objFile = new File(objectName);
            String baseDir = objFile.getParent();
            if (baseDir == null) baseDir = ".";
            
            String mapFileName = baseDir + File.separator + packageName + ".map";
            
            // Placeholder: Esta funcionalidad requiere modificar Encoder
            // para rastrear los exports durante la compilación
            java.util.Map<String, Integer> exports = new java.util.HashMap<>();
            // exports = encoder.getExports(); // Este método debe implementarse en Encoder
            
            if (!exports.isEmpty()) {
                try (FileWriter writer = new FileWriter(mapFileName)) {
                    for (java.util.Map.Entry<String, Integer> entry : exports.entrySet()) {
                        writer.write("export " + packageName + "." + entry.getKey() + 
                                    " " + entry.getValue() + "\n");
                    }
                }
                System.out.println("Generated map file: " + mapFileName);
            }
        } catch (IOException e) {
            System.err.println("Error generating map file: " + e.getMessage());
        }
    }
    
    public ErrorReporter getErrorReporter() {
        return reporter;
    }
}
