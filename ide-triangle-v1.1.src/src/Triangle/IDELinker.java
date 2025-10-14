package Triangle;

import java.io.*;
import java.util.*;

/**
 * Linker para combinar programa con paquetes
 */
public class IDELinker {
    
    private PackageManager packageManager;
    
    public IDELinker() {
        packageManager = PackageManager.getInstance();
    }
    
    /**
     * Linkea un programa con sus dependencias
     * @param programTamPath Ruta al obj.tam del programa
     * @param outputPath Ruta donde se guardará linked.tam
     * @return true si el linkeo fue exitoso
     */
    public boolean link(String programTamPath, String outputPath) {
        try {
            // Leer archivo de imports
            String baseDir = new File(programTamPath).getParent();
            if (baseDir == null) baseDir = ".";
            
            File impFile = new File(baseDir + "/program.imp");
            File relocFile = new File(baseDir + "/program.reloc");
            
            if (!impFile.exists()) {
                System.out.println("No import file found. Linking as standalone program.");
                // Simplemente copiar obj.tam a linked.tam
                copyFile(programTamPath, outputPath);
                return true;
            }
            
            // Leer imports
            List<String> imports = readImports(impFile);
            System.out.println("Found " + imports.size() + " imports");
            
            // Leer relocations
            List<Relocation> relocations = readRelocations(relocFile);
            System.out.println("Found " + relocations.size() + " relocations");
            
            // Construir mapa de símbolos
            Map<String, Integer> symbolMap = new HashMap<>();
            int currentAddress = 0;
            
            // Leer tamaño del programa principal
            int programSize = getFileSize(programTamPath);
            currentAddress = programSize;
            
            // Procesar cada paquete importado
            List<PackageData> packages = new ArrayList<>();
            for (String imp : imports) {
                // imp formato: "import PackageName.symbol"
                String packageName = extractPackageName(imp);
                
                // Buscar paquete
                String packagePath = packageManager.getPackageLocation(packageName);
                if (packagePath == null) {
                    System.err.println("Package not found: " + packageName);
                    return false;
                }
                
                // Verificar si ya procesamos este paquete
                boolean alreadyLoaded = false;
                for (PackageData pd : packages) {
                    if (pd.name.equals(packageName)) {
                        alreadyLoaded = true;
                        break;
                    }
                }
                
                if (!alreadyLoaded) {
                    // Leer exports del paquete
                    Map<String, Integer> exports = packageManager.getPackageExports(packageName);
                    int packageSize = getFileSize(packagePath);
                    
                    PackageData pd = new PackageData();
                    pd.name = packageName;
                    pd.path = packagePath;
                    pd.baseAddress = currentAddress;
                    pd.size = packageSize;
                    
                    // Agregar símbolos al mapa global
                    for (Map.Entry<String, Integer> entry : exports.entrySet()) {
                        String fullSymbol = entry.getKey();
                        int relativeAddr = entry.getValue();
                        int absoluteAddr = currentAddress + relativeAddr;
                        symbolMap.put(fullSymbol, absoluteAddr);
                        System.out.println("Symbol: " + fullSymbol + " -> " + absoluteAddr);
                    }
                    
                    packages.add(pd);
                    currentAddress += packageSize;
                }
            }
            
            // Crear archivo linked.tam
            FileOutputStream linkedFile = new FileOutputStream(outputPath);
            
            // Escribir programa principal
            byte[] programBytes = readBinaryFile(programTamPath);
            linkedFile.write(programBytes);
            
            // Escribir cada paquete
            for (PackageData pd : packages) {
                byte[] packageBytes = readBinaryFile(pd.path);
                linkedFile.write(packageBytes);
            }
            
            linkedFile.close();
            
            // Aplicar relocations
            applyRelocations(outputPath, relocations, symbolMap);
            
            System.out.println("Linking successful -> " + outputPath);
            return true;
            
        } catch (Exception e) {
            System.err.println("Linking failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private List<String> readImports(File impFile) throws IOException {
        List<String> imports = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(impFile));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("import ")) {
                imports.add(line.substring(7));
            }
        }
        reader.close();
        return imports;
    }
    
    private List<Relocation> readRelocations(File relocFile) throws IOException {
        List<Relocation> relocs = new ArrayList<>();
        if (!relocFile.exists()) return relocs;
        
        BufferedReader reader = new BufferedReader(new FileReader(relocFile));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("reloc ")) {
                // Formato: reloc PackageName.symbol address
                String[] parts = line.substring(6).split(" ");
                if (parts.length >= 2) {
                    Relocation r = new Relocation();
                    r.symbol = parts[0];
                    r.address = Integer.parseInt(parts[1]);
                    relocs.add(r);
                }
            }
        }
        reader.close();
        return relocs;
    }
    
    private String extractPackageName(String fullSymbol) {
        int dotIndex = fullSymbol.indexOf('.');
        if (dotIndex > 0) {
            return fullSymbol.substring(0, dotIndex);
        }
        return fullSymbol;
    }
    
    private int getFileSize(String path) throws IOException {
        File f = new File(path);
        return (int) f.length();
    }
    
    private byte[] readBinaryFile(String path) throws IOException {
        File f = new File(path);
        byte[] data = new byte[(int) f.length()];
        FileInputStream fis = new FileInputStream(f);
        fis.read(data);
        fis.close();
        return data;
    }
    
    private void copyFile(String src, String dest) throws IOException {
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dest);
        byte[] buffer = new byte[4096];
        int read;
        while ((read = fis.read(buffer)) > 0) {
            fos.write(buffer, 0, read);
        }
        fis.close();
        fos.close();
    }
    
    private void applyRelocations(String linkedPath, List<Relocation> relocs,
                                  Map<String, Integer> symbolMap) throws IOException {
        
        RandomAccessFile raf = new RandomAccessFile(linkedPath, "rw");
        
        for (Relocation r : relocs) {
            Integer targetAddr = symbolMap.get(r.symbol);
            if (targetAddr != null) {
                // Leer la instrucción en r.address y patchear
                raf.seek(r.address * 4); // Cada instrucción TAM es 4 bytes
                int instruction = raf.readInt();
                
                // Modificar el operando de la instrucción con targetAddr
                // (esto depende del formato de instrucciones TAM)
                // Por simplicidad, reemplazamos el operando completo
                raf.seek(r.address * 4);
                
                // Mantener opcode y modificar operando
                int opcode = instruction & 0xFF000000;
                int newInstruction = opcode | (targetAddr & 0x00FFFFFF);
                raf.writeInt(newInstruction);
                
                System.out.println("Patched " + r.symbol + " at " + r.address + " -> " + targetAddr);
            } else {
                System.err.println("Symbol not found for relocation: " + r.symbol);
            }
        }
        
        raf.close();
    }
    
    private static class PackageData {
        String name;
        String path;
        int baseAddress;
        int size;
    }
    
    private static class Relocation {
        String symbol;
        int address;
    }
}