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
    private String outputDirectory = null;
    
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
        outputDirectory = outputDir;
        
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
                } else {
                    System.err.println("obj.tam not found at: " + objPath);
                    return false;
                }
            } catch (Exception e) {
                System.err.println("Error renaming package file: " + e.getMessage());
                return false;
            }
        }
        
        compilingAsPackage = false;
        outputDirectory = null;
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
        
        // SourceFile constructor maneja internamente si el archivo es válido
        // Verificamos creando el scanner
        reporter = new IDEReporter();
        
        try {
            scanner = new Scanner(source);
            parser = new Parser(scanner, reporter);
        } catch (Exception e) {
            System.out.println("Can't access source file " + sourceName);
            return false;
        }
        
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
                
                // Establecer el nombre del paquete en el encoder
                encoder.setPackageName(packageName);
                
                encoder.encodeRun(theAST, showingTable);
                
                if (reporter.numErrors == 0) {
                    // Asegurar que el directorio de salida existe
                    File objFile = new File(objectName);
                    File parentDir = objFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }
                    
                    // Escribir archivo objeto
                    try {
                        FileOutputStream objectFileStream = new FileOutputStream(objectName);
                        DataOutputStream objectStream = new DataOutputStream(objectFileStream);
                        
                        // Guardar el programa objeto - el método correcto del Encoder
                        encoder.saveObjectProgram(objectName);
                        objectStream.close();
                        
                        System.out.println("Object file written: " + objectName);
                        
                        // Generar archivos adicionales
                        if (compilingAsPackage && packageName != null) {
                            // Para paquetes: generar .map en el mismo directorio que el .tam
                            generateMapFile(objectName, packageName);
                        } else {
                            // Para programas normales: generar .imp y .reloc
                            generateImportFile(objectName);
                            generateRelocFile(objectName);
                        }
                        
                        System.out.println("Compilation was successful.");
                        return true;
                    } catch (IOException e) {
                        System.out.println("Error writing object file: " + e.getMessage());
                        e.printStackTrace();
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
        
        try {
            File objFile = new File(objectName);
            String baseDir = objFile.getParent();
            if (baseDir == null) baseDir = ".";
            
            String impFileName = baseDir + File.separator + "program.imp";
            
            // Verificar si el encoder tiene el método getImports
            java.util.List<String> imports = null;
            try {
                java.lang.reflect.Method getImportsMethod = encoder.getClass().getMethod("getImports");
                imports = (java.util.List<String>) getImportsMethod.invoke(encoder);
            } catch (Exception e) {
                System.out.println("Note: Encoder does not support getImports() - skipping import file generation");
                return;
            }
            
            if (imports != null && !imports.isEmpty()) {
                FileWriter writer = new FileWriter(impFileName);
                for (String imp : imports) {
                    writer.write(imp + "\n");
                }
                writer.close();
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
            
            // Verificar si el encoder tiene el método getRelocations
            java.util.List<String> relocations = null;
            try {
                java.lang.reflect.Method getRelocationsMethod = encoder.getClass().getMethod("getRelocations");
                relocations = (java.util.List<String>) getRelocationsMethod.invoke(encoder);
            } catch (Exception e) {
                System.out.println("Note: Encoder does not support getRelocations() - skipping reloc file generation");
                return;
            }
            
            if (relocations != null && !relocations.isEmpty()) {
                FileWriter writer = new FileWriter(relocFileName);
                for (String reloc : relocations) {
                    writer.write(reloc + "\n");
                }
                writer.close();
                System.out.println("Generated relocation file: " + relocFileName);
            }
        } catch (IOException e) {
            System.err.println("Error generating relocation file: " + e.getMessage());
        }
    }
    
    /**
     * Genera archivo .map con exports del paquete
     * IMPORTANTE: Se genera en el mismo directorio que el .tam del paquete
     */
    private void generateMapFile(String objectName, String packageName) {
        if (encoder == null) return;
        
        try {
            // Usar outputDirectory si está disponible, sino usar el directorio del objectName
            String baseDir = outputDirectory;
            if (baseDir == null) {
                File objFile = new File(objectName);
                baseDir = objFile.getParent();
                if (baseDir == null) baseDir = ".";
            }
            
            String mapFileName = baseDir + File.separator + packageName + ".map";
            
            // Verificar si el encoder tiene el método getExports
            java.util.Map<String, Integer> exports = null;
            try {
                java.lang.reflect.Method getExportsMethod = encoder.getClass().getMethod("getExports");
                exports = (java.util.Map<String, Integer>) getExportsMethod.invoke(encoder);
            } catch (Exception e) {
                System.out.println("Note: Encoder does not support getExports() - skipping map file generation");
                return;
            }
            
            if (exports != null && !exports.isEmpty()) {
                FileWriter writer = new FileWriter(mapFileName);
                for (java.util.Map.Entry<String, Integer> entry : exports.entrySet()) {
                    writer.write("export " + packageName + "." + entry.getKey() + 
                                " " + entry.getValue() + "\n");
                }
                writer.close();
                System.out.println("Generated map file: " + mapFileName);
            } else {
                System.out.println("Warning: No exports found for package " + packageName);
            }
        } catch (IOException e) {
            System.err.println("Error generating map file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public ErrorReporter getErrorReporter() {
        return reporter;
    }
}