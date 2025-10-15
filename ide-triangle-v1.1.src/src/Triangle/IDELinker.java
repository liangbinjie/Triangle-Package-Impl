package Triangle;

import TAM.Instruction;
import TAM.Machine;
import java.io.*;
import java.util.*;

/**
 * Linker for Triangle programs with package support
 */
public class IDELinker {
    
    private Map<String, Map<String, Integer>> packageExports;
    private List<String> packagePaths;
    private String baseDir;
    
    public IDELinker() {
        packageExports = new HashMap<>();
        packagePaths = new ArrayList<>();
        
        // Agregar directorio de paquetes por defecto
        File defaultPackageDir = new File("packages");
        if (defaultPackageDir.exists() && defaultPackageDir.isDirectory()) {
            packagePaths.add(defaultPackageDir.getAbsolutePath());
        }
    }
    
    public void addPackagePath(String path) {
        if (!packagePaths.contains(path)) {
            packagePaths.add(path);
        }
    }
    
    /**
     * Link a program with its imported packages
     * @param objectFile The compiled object file (obj.tam)
     * @param importFile The imports file (program.imp)
     * @param relocFile The relocations file (program.reloc)
     * @param outputFile The output linked file (linked.tam)
     * @return true if linking was successful
     */
    public boolean link(String objectFile, String importFile, String relocFile, String outputFile) {
        try {
            System.out.println("\n=== Linker Starting ===");
            
            // Establecer directorio base
            baseDir = new File(objectFile).getParent();
            if (baseDir == null) {
                baseDir = ".";
            }
            System.out.println("Base directory: " + baseDir);
            System.out.println("Object file: " + objectFile);
            System.out.println("Import file: " + importFile);
            System.out.println("Reloc file: " + relocFile);
            
            // 1. Cargar el archivo objeto
            System.out.println("\nReading object file...");
            Instruction[] code = loadObjectFile(objectFile);
            if (code == null) {
                System.err.println("ERROR: Failed to load object file");
                return false;
            }
            System.out.println("Object file loaded: " + code.length + " instructions");
            
            // 2. Leer archivo de imports
            System.out.println("\nReading import file...");
            List<String> imports = readImportFile(importFile);
            if (imports == null) {
                System.out.println("No imports found or file doesn't exist");
                imports = new ArrayList<>();
            } else {
                System.out.println("Loaded " + imports.size() + " imports:");
                for (String imp : imports) {
                    System.out.println("  - " + imp);
                }
            }
            
            // 3. Leer archivo de relocalizaciones
            System.out.println("\nReading relocation file...");
            Map<Integer, String> relocations = readRelocFile(relocFile);
            if (relocations == null) {
                System.out.println("No relocations found or file doesn't exist");
                relocations = new HashMap<>();
            } else {
                System.out.println("Loaded " + relocations.size() + " relocations");
            }
            
            // 4. Resolver símbolos importados
            System.out.println("\nResolving symbols...");
            Map<String, Integer> symbolTable = new HashMap<>();
            
            for (String imp : imports) {
                // Formato: "import PackageName.symbolName" o solo "PackageName.symbolName"
                String qualifiedName = imp.replace("import ", "").trim();
                
                if (!qualifiedName.contains(".")) {
                    System.err.println("WARNING: Invalid import format: " + imp);
                    continue;
                }
                
                String[] parts = qualifiedName.split("\\.", 2);
                String packageName = parts[0];
                String symbolName = parts[1];
                
                // Cargar exports del paquete si no están cargados
                if (!packageExports.containsKey(packageName)) {
                    if (!loadPackageExports(packageName)) {
                        System.err.println("ERROR: Failed to load package: " + packageName);
                        return false;
                    }
                }
                
                // Buscar el símbolo en el paquete
                Map<String, Integer> exports = packageExports.get(packageName);
                if (!exports.containsKey(symbolName)) {
                    System.err.println("ERROR: Symbol '" + symbolName + "' not found in package '" + packageName + "'");
                    System.err.println("Available symbols: " + exports.keySet());
                    return false;
                }
                
                int value = exports.get(symbolName);
                symbolTable.put(qualifiedName, value);
                System.out.println("Symbol " + qualifiedName + " -> value: " + value);
            }
            
            // 5. Aplicar relocalizaciones
            System.out.println("\nApplying relocations...");
            if (!relocations.isEmpty()) {
                for (Map.Entry<Integer, String> entry : relocations.entrySet()) {
                    int address = entry.getKey();
                    String symbol = entry.getValue();
                    
                    if (!symbolTable.containsKey(symbol)) {
                        System.err.println("ERROR: Unresolved symbol at address " + address + ": " + symbol);
                        return false;
                    }
                    
                    int value = symbolTable.get(symbol);
                    
                    // Parchear la instrucción
                    if (address >= 0 && address < code.length) {
                        // CORRECCIÓN: Para símbolos importados que son CONSTANTES,
                        // reemplazar la instrucción CALL por LOADL
                        if (code[address].op == Machine.CALLop) {
                            // Si el valor es pequeño (< 100), probablemente es una constante
                            if (value < 100) {
                                // Reemplazar CALL por LOADL (cargar literal)
                                code[address].op = Machine.LOADLop;
                                code[address].r = 0;
                                code[address].n = 0;
                                code[address].d = value;
                                System.out.println("  Patched address " + address + ": CALL -> LOADL " + value);
                            } else {
                                // Es una función/procedimiento, mantener CALL pero actualizar dirección
                                code[address].d = value;
                                System.out.println("  Patched address " + address + ": CALL to " + value);
                            }
                        } else {
                            // Para otras instrucciones, solo actualizar el desplazamiento
                            code[address].d = value;
                            System.out.println("  Patched address " + address + ": displacement = " + value);
                        }
                    } else {
                        System.err.println("WARNING: Invalid relocation address: " + address);
                    }
                }
            } else {
                System.out.println("No relocations to apply.");
            }
            
            // 6. Escribir el programa linkeado
            System.out.println("\nWriting linked program...");
            if (!writeLinkedProgram(code, outputFile)) {
                System.err.println("ERROR: Failed to write linked program");
                return false;
            }
            
            System.out.println("Linked program written: " + outputFile);
            System.out.println("Total instructions: " + code.length);
            System.out.println("\n=== Linking Complete ===");
            
            return true;
            
        } catch (Exception e) {
            System.err.println("ERROR during linking: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Load object file into instruction array
     */
    private Instruction[] loadObjectFile(String filename) {
        try {
            DataInputStream input = new DataInputStream(new FileInputStream(filename));
            
            List<Instruction> instructions = new ArrayList<>();
            
            while (input.available() > 0) {
                try {
                    Instruction instr = Instruction.read(input);
                    if (instr != null) {
                        instructions.add(instr);
                    }
                } catch (EOFException e) {
                    break;
                }
            }
            
            input.close();
            
            Instruction[] code = new Instruction[instructions.size()];
            return instructions.toArray(code);
            
        } catch (IOException e) {
            System.err.println("Error reading object file: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Read import file
     */
    private List<String> readImportFile(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            return null;
        }
        
        List<String> imports = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && line.startsWith("import ")) {
                    imports.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading import file: " + e.getMessage());
            return null;
        }
        
        return imports;
    }
    
    /**
     * Read relocation file
     */
    private Map<Integer, String> readRelocFile(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            return null;
        }
        
        Map<Integer, String> relocations = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("reloc ")) {
                    // Formato: "reloc SymbolName Address"
                    String[] parts = line.substring(6).trim().split("\\s+");
                    if (parts.length >= 2) {
                        String symbol = parts[0];
                        int address = Integer.parseInt(parts[1]);
                        relocations.put(address, symbol);
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error reading reloc file: " + e.getMessage());
            return null;
        }
        
        return relocations;
    }
    
    /**
     * Load package exports from .map file
     */
    private boolean loadPackageExports(String packageName) {
        System.out.println("Looking for package: " + packageName);
        
        // Buscar archivo .map del paquete
        File mapFile = null;
        
        // Primero buscar en el directorio base
        File localMap = new File(baseDir, packageName + ".map");
        if (localMap.exists()) {
            mapFile = localMap;
        } else {
            // Buscar en los directorios de paquetes configurados
            for (String path : packagePaths) {
                File candidate = new File(path, packageName + ".map");
                if (candidate.exists()) {
                    mapFile = candidate;
                    break;
                }
            }
        }
        
        if (mapFile == null || !mapFile.exists()) {
            System.err.println("ERROR: Package map file not found: " + packageName + ".map");
            System.err.println("Searched in:");
            System.err.println("  - " + baseDir);
            for (String path : packagePaths) {
                System.err.println("  - " + path);
            }
            return false;
        }
        
        System.out.println("  Package file: " + mapFile.getAbsolutePath());
        
        Map<String, Integer> exports = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(mapFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("export ")) {
                    // Formato: "export PackageName.symbolName value"
                    String[] parts = line.substring(7).trim().split("\\s+");
                    if (parts.length >= 2) {
                        String qualifiedName = parts[0];
                        int value = Integer.parseInt(parts[1]);
                        
                        // Extraer solo el nombre del símbolo (sin el prefijo del paquete)
                        String symbolName = qualifiedName;
                        if (qualifiedName.contains(".")) {
                            symbolName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
                        }
                        
                        exports.put(symbolName, value);
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error reading package map file: " + e.getMessage());
            return false;
        }
        
        if (exports.isEmpty()) {
            System.err.println("WARNING: No exports found in package " + packageName);
        } else {
            System.out.println("  Loaded " + exports.size() + " exports from " + packageName);
        }
        
        packageExports.put(packageName, exports);
        return true;
    }
    
    /**
     * Write linked program to file
     */
    private boolean writeLinkedProgram(Instruction[] code, String filename) {
        try {
            DataOutputStream output = new DataOutputStream(new FileOutputStream(filename));
            
            for (Instruction instr : code) {
                instr.write(output);
            }
            
            output.close();
            return true;
            
        } catch (IOException e) {
            System.err.println("Error writing linked program: " + e.getMessage());
            return false;
        }
    }
}