package Triangle;

import java.io.*;
import java.util.*;

/**
 * Gestiona los paquetes disponibles y sus ubicaciones
 */
public class PackageManager {
    private static PackageManager instance;
    private List<String> packagePaths;
    private Map<String, String> packageLocations; // packageName -> path to .tam file
    
    private PackageManager() {
        packagePaths = new ArrayList<>();
        packageLocations = new HashMap<>();
        
        // Crear y agregar directorio por defecto
        String defaultPath = System.getProperty("user.dir") + File.separator + "packages";
        ensureDirectoryExists(defaultPath);
        addPackagePath(defaultPath);
        
        // También agregar el directorio actual por si hay paquetes ahí
        String currentDir = System.getProperty("user.dir");
        addPackagePath(currentDir);
    }
    
    public static PackageManager getInstance() {
        if (instance == null) {
            instance = new PackageManager();
        }
        return instance;
    }
    
    /**
     * Asegura que un directorio existe, creándolo si es necesario
     */
    private boolean ensureDirectoryExists(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                System.out.println("Created package directory: " + path);
            } else {
                System.err.println("Failed to create package directory: " + path);
                return false;
            }
        }
        return true;
    }
    
    public void addPackagePath(String path) {
        // Asegurar que el directorio existe
        if (!ensureDirectoryExists(path)) {
            System.err.println("Cannot add non-existent path: " + path);
            return;
        }
        
        if (!packagePaths.contains(path)) {
            packagePaths.add(path);
            scanPackagesInPath(path);
            System.out.println("Added package path: " + path);
        }
    }
    
    public void removePackagePath(String path) {
        packagePaths.remove(path);
        rescanAllPackages();
    }
    
    public List<String> getPackagePaths() {
        return new ArrayList<>(packagePaths);
    }
    
    private void scanPackagesInPath(String path) {
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".tam"));
        if (files != null) {
            for (File file : files) {
                String packageName = file.getName().replace(".tam", "");
                packageLocations.put(packageName, file.getAbsolutePath());
            }
        }
    }
    
    public void rescanAllPackages() {
        packageLocations.clear();
        for (String path : packagePaths) {
            scanPackagesInPath(path);
        }
    }
    
    public String getPackageLocation(String packageName) {
        return packageLocations.get(packageName);
    }
    
    public List<String> getAvailablePackages() {
        return new ArrayList<>(packageLocations.keySet());
    }
    
    public boolean packageExists(String packageName) {
        return packageLocations.containsKey(packageName);
    }
    
    /**
     * Obtiene el archivo .map para un paquete
     */
    public File getMapFile(String packageName) {
        String tamPath = packageLocations.get(packageName);
        if (tamPath != null) {
            String mapPath = tamPath.replace(".tam", ".map");
            File mapFile = new File(mapPath);
            if (mapFile.exists()) {
                return mapFile;
            }
        }
        return null;
    }
    
    /**
     * Lee los exports de un paquete desde su archivo .map
     */
    public Map<String, Integer> getPackageExports(String packageName) {
        Map<String, Integer> exports = new HashMap<>();
        File mapFile = getMapFile(packageName);
        
        if (mapFile != null && mapFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(mapFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Formato: export PackageName.symbolName address
                    if (line.startsWith("export ")) {
                        String[] parts = line.substring(7).split(" ");
                        if (parts.length >= 2) {
                            String fullName = parts[0]; // PackageName.symbolName
                            int address = Integer.parseInt(parts[1]);
                            exports.put(fullName, address);
                        }
                    }
                }
            } catch (IOException | NumberFormatException e) {
                System.err.println("Error reading map file: " + e.getMessage());
            }
        }
        
        return exports;
    }
    
    /**
     * Obtiene el directorio de paquetes por defecto
     */
    public String getDefaultPackageDirectory() {
        return System.getProperty("user.dir") + File.separator + "packages";
    }
}