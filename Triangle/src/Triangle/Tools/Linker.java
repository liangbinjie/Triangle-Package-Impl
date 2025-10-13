package Triangle.Tools;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;

public class Linker {

  private static final String PROGRAM_IMPORTS = "program.imp";
  private static final String PKG_MAP_EXT     = ".map";
  private static final String PKG_OBJ_EXT     = ".tam";
  private static final String LINKED_OUTPUT   = "linked.tam";

  // TAM Instruction serialized as 4 ints (op, n, r, d) -> 16 bytes total
  private static final int INSTR_BYTES = 16;

  private static int instrCount(byte[] tamBytes) {
    if (tamBytes.length % INSTR_BYTES != 0) {
      System.out.println("Aviso: tamaño .tam no múltiplo de " + INSTR_BYTES
        + " (" + tamBytes.length + " bytes). ¿Formato distinto?");
    }
    return tamBytes.length / INSTR_BYTES;
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("Uso: java Triangle.Tools.Linker <programa_obj.tam>");
      System.exit(2);
    }
    Path progObj = Paths.get(args[0]);
    if (!Files.exists(progObj)) {
      System.err.println("No existe objeto de programa: " + progObj);
      System.exit(3);
    }

    // 1) Leer objeto del programa (BINARIO)
    byte[] programBytes = Files.readAllBytes(progObj);

    // 2) Leer imports (program.imp) en UTF-8
    Path impPath = Paths.get(PROGRAM_IMPORTS);
    Set<String> pkgs = new LinkedHashSet<>();
    List<String> qnames = new ArrayList<>();
    if (Files.exists(impPath)) {
      for (String ln : Files.readAllLines(impPath, StandardCharsets.UTF_8)) {
        ln = ln.trim();
        if (ln.startsWith("import ")) {
          String q = ln.substring("import ".length()).trim(); // "Pkg.sym"
          qnames.add(q);
          String pkg = q.split("\\.")[0];
          pkgs.add(pkg);
        }
      }
    } else {
      System.out.println("Aviso: no hay " + PROGRAM_IMPORTS + " (¿sin imports?).");
    }

    // 3) Cargar cada paquete: .map (UTF-8) + .tam (BINARIO)
    Map<String,Integer> relocatedExportAddr = new HashMap<>(); // qname -> addr reubicado
    Map<String,byte[]>  pkgCodeBytes = new LinkedHashMap<>();  // pkg -> bytes TAM

    int base = instrCount(programBytes); // offset inicial en unidades de instrucción
    for (String p : pkgs) {
      Path mapPath = Paths.get(p + PKG_MAP_EXT);
      Path tamPath = Paths.get(p + PKG_OBJ_EXT);

      if (!Files.exists(mapPath))
        throw new FileNotFoundException("Falta " + mapPath.toString());
      if (!Files.exists(tamPath))
        throw new FileNotFoundException("Falta " + tamPath.toString());

      // Reubicación: addr final = base + addr
      List<String> mapLines = Files.readAllLines(mapPath, StandardCharsets.UTF_8);
      for (String ml : mapLines) {
        ml = ml.trim();
        if (ml.startsWith("export ")) {
          // formato: "export Pkg.sym <addr>"
          String[] parts = ml.split("\\s+");
          if (parts.length >= 3) {
            String q  = parts[1];
            int addr  = Integer.parseInt(parts[2]);
            relocatedExportAddr.put(q, base + addr);
          }
        }
      }

      byte[] code = Files.readAllBytes(tamPath);
      pkgCodeBytes.put(p, code);
      base += instrCount(code);
    }

    // 4) (v1) Sin parches de llamadas: solo concatenar binario
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    baos.write(programBytes);
    for (var entry : pkgCodeBytes.entrySet()) {
      baos.write(entry.getValue());
    }
    Files.write(Paths.get(LINKED_OUTPUT), baos.toByteArray());
    System.out.println("Linked OK -> " + LINKED_OUTPUT);

    // NOTA: cuando agregues "program.reloc" con sitios de llamada, usa 'relocatedExportAddr'
    // para reescribir los 'd' (displacement) de CALLs en el binario.
  }
}
