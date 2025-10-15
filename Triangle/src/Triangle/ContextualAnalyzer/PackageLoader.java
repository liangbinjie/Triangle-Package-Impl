// src/Triangle/ContextualAnalyzer/PackageLoader.java
package Triangle.ContextualAnalyzer;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class PackageLoader {
  public static final class ExportEntry {
    public final String name, kind, typeSig;
    public ExportEntry(String n, String k, String s){ name=n; kind=k; typeSig=s; }
  }
  public static final class PackageArtifact {
    public String packageName;
    public List<ExportEntry> exports = new ArrayList<>();
  }

  private static final Path BASE = Paths.get("packages");

  public static Path save(PackageArtifact art) throws Exception {
    Files.createDirectories(BASE);
    Path out = BASE.resolve(art.packageName + ".tpk");
    List<String> lines = new ArrayList<>();
    lines.add("package " + art.packageName);
    for (ExportEntry e : art.exports)
      lines.add("export " + e.kind + " " + e.name + " " + (e.typeSig==null?"":e.typeSig));
    Files.write(out, lines, StandardCharsets.UTF_8);
    return out;
  }

  public static PackageArtifact load(String pkgName) throws Exception {
    Path in = BASE.resolve(pkgName + ".tpk");
    if (!Files.exists(in))
      throw new RuntimeException("tpk not found: " + in.toString());
    List<String> lines = Files.readAllLines(in, StandardCharsets.UTF_8);
    PackageArtifact art = new PackageArtifact();
    for (String ln : lines) {
      ln = ln.trim();
      if (ln.isEmpty()) continue;
      if (ln.startsWith("package ")) {
        art.packageName = ln.substring("package ".length()).trim();
      } else if (ln.startsWith("export ")) {
        String[] parts = ln.split("\\s+", 4); // export kind name sig
        String kind = parts.length>1 ? parts[1] : "unknown";
        String name = parts.length>2 ? parts[2] : "?";
        String sig  = parts.length>3 ? parts[3] : "";
        art.exports.add(new ExportEntry(name, kind, sig));
      }
    }
    if (art.packageName == null) art.packageName = pkgName;
    return art;
  }

  public static ExportEntry mk(String name, String kind, String sig) {
    return new ExportEntry(name, kind, sig);
  }
}
