package Triangle.ContextualAnalyzer;

import java.io.*;
import java.util.*;

/**
 *
 * @author Esteban
 */
public final class PackageLoader {

  public static final class ExportEntry implements Serializable {
    public String name;     // p.ej. "min"
    public String kind;     // "func" | "proc" | "const" | "type" | "var" | "unknown"
    public String typeSig;  // firma textual (por ahora puede ir vacía)
  }

  public static final class PackageArtifact implements Serializable {
    public String packageName;
    public List<ExportEntry> exports = new ArrayList<>();
  }

  private static File packagesDir() {
    File dir = new File("packages");
    if (!dir.exists()) dir.mkdirs();
    return dir;
  }

  public static void save(PackageArtifact art) throws IOException {
    File out = new File(packagesDir(), art.packageName + ".tpk");
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(out))) {
      oos.writeObject(art);
    }
  }

  public static PackageArtifact load(String packageName) throws IOException, ClassNotFoundException {
    File in = new File(packagesDir(), packageName + ".tpk");
    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(in))) {
      return (PackageArtifact) ois.readObject();
    }
  }

  // Utilidad simple para crear un export
  public static ExportEntry mk(String name, String kind, String typeSig) {
    ExportEntry e = new ExportEntry();
    e.name = name;
    e.kind = (kind == null ? "unknown" : kind);
    e.typeSig = (typeSig == null ? "" : typeSig);
    return e;
  }
}
