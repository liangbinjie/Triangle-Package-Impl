package Triangle.Tools;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;

// [RELOC] Necesitamos las constantes de la TAM (CALLop, JUMPop, JUMPIFop, CBr, etc.)
import TAM.Machine;

public class Linker {

  private static final String PROGRAM_IMPORTS = "program.imp";    // generado por Encoder del programa
  private static final String PROGRAM_RELOCS  = "program.reloc";  // sitios de parcheo: "reloc Pkg.sym instrIndex"
  private static final String PKG_MAP_EXT     = ".map";           // export lines: "export Pkg.sym addr"
  private static final String PKG_OBJ_EXT     = ".tam";           // objeto TAM binario del paquete
  private static final String LINKED_OUTPUT   = "linked.tam";     // salida final

  // Instrucción TAM serializada como 4 ints (op, n, r, d) -> 16 bytes
  private static final int INSTR_BYTES = 16;

  private static int instrCount(byte[] tamBytes) {
    if (tamBytes.length % INSTR_BYTES != 0) {
      System.out.println("Aviso: tamaño .tam no múltiplo de " + INSTR_BYTES
        + " (" + tamBytes.length + " bytes). ¿Formato distinto?");
    }
    return tamBytes.length / INSTR_BYTES;
  }

  // Escribe un int big-endian en 'arr' en la posición 'offset'
  private static void putIntBE(byte[] arr, int offset, int value) {
    arr[offset    ] = (byte)((value >>> 24) & 0xFF);
    arr[offset + 1] = (byte)((value >>> 16) & 0xFF);
    arr[offset + 2] = (byte)((value >>>  8) & 0xFF);
    arr[offset + 3] = (byte)((value       ) & 0xFF);
  }

  // [RELOC] Lee un int big-endian desde 'arr' en 'offset'
  private static int getIntBE(byte[] arr, int offset) {
    return  ((arr[offset]   & 0xFF) << 24)
          | ((arr[offset+1] & 0xFF) << 16)
          | ((arr[offset+2] & 0xFF) <<  8)
          |  (arr[offset+3] & 0xFF);
  }

  // [RELOC] Suma 'base' al campo d de todas las CALL/JUMP/JUMPIF con r == CBr
  //         dentro del código del paquete (para que apunten a la posición real).
  private static byte[] relocatePackageCode(byte[] code, int base) {
    int n = instrCount(code);
    byte[] out = code.clone();
    for (int i = 0; i < n; i++) {
      int off = i * INSTR_BYTES;
      int op = getIntBE(out, off);
      int r  = getIntBE(out, off + 8);
      int d  = getIntBE(out, off + 12);

      if ((op == Machine.CALLop || op == Machine.JUMPop || op == Machine.JUMPIFop)
           && r == Machine.CBr) {
        putIntBE(out, off + 12, d + base);
      }
    }
    return out;
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

    // 2) Leer imports (program.imp) en UTF-8 -> set de paquetes
    Path impPath = Paths.get(PROGRAM_IMPORTS);
    Set<String> pkgs = new LinkedHashSet<>();
    if (Files.exists(impPath)) {
      for (String ln : Files.readAllLines(impPath, StandardCharsets.UTF_8)) {
        ln = ln.trim();
        if (ln.startsWith("import ")) {
          String q = ln.substring("import ".length()).trim(); // "Pkg.sym"
          String pkg = q.split("\\.")[0];
          pkgs.add(pkg);
        }
      }
    } else {
      System.out.println("Aviso: no hay " + PROGRAM_IMPORTS + " (¿sin imports?).");
    }

    // 3) Cargar cada paquete: .map (UTF-8) + .tam (BINARIO).
    // Calcular direcciones reubicadas de exports: target = base + addr_local
    Map<String,Integer> relocatedExportAddr = new HashMap<>(); // qname -> addr reubicada (en # de instrucciones)
    Map<String,byte[]>  pkgCodeBytes = new LinkedHashMap<>();  // pkg -> bytes TAM
    int base = instrCount(programBytes); // offset inicial donde pegará el primer paquete (en # de instrucciones)

    for (String p : pkgs) {
      Path mapPath = Paths.get(p + PKG_MAP_EXT);
      Path tamPath = Paths.get(p + PKG_OBJ_EXT);

      if (!Files.exists(mapPath))
        throw new FileNotFoundException("Falta " + mapPath.toString());
      if (!Files.exists(tamPath))
        throw new FileNotFoundException("Falta " + tamPath.toString());

      // Reubicación de exports del paquete p
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

      // [RELOC] Reubicar el código del paquete con respecto a 'base'
      byte[] raw = Files.readAllBytes(tamPath);
      byte[] codeRelocated = relocatePackageCode(raw, base);
      pkgCodeBytes.put(p, codeRelocated);

      // Avanzar base por el tamaño del paquete (en # de instrucciones)
      base += instrCount(raw);
    }

    // 4) Parchear llamadas del programa usando program.reloc (si existe)
    Path relocPath = Paths.get(PROGRAM_RELOCS);
    byte[] programPatched = programBytes.clone();

    if (Files.exists(relocPath)) {
      List<String> relocs = Files.readAllLines(relocPath, StandardCharsets.UTF_8);
      for (String rl : relocs) {
        rl = rl.trim();
        if (!rl.startsWith("reloc ")) continue;

        // "reloc Pkg.sym instrIndex"
        String[] parts = rl.split("\\s+");
        if (parts.length < 3) continue;

        String qname = parts[1];
        int instrIndex = Integer.parseInt(parts[2]);

        Integer target = relocatedExportAddr.get(qname);
        if (target == null) {
          throw new RuntimeException("Símbolo no resuelto: " + qname
            + " (¿falta paquete o export?)");
        }

        System.out.println("patch " + qname + " at " + instrIndex + " -> " + target);

        // Campo 'd' (4º int) de la instrucción en el programa (index local del PROGRAMA)
        int bytePos = instrIndex * INSTR_BYTES + 12; // 12 = 3 * 4 bytes (op,n,r)
        if (bytePos + 4 > programPatched.length) {
          throw new RuntimeException("Índice de instrucción fuera de rango en program.reloc: " + instrIndex);
        }
        // Escribir 'd' en big-endian (TAM usa DataOutputStream.writeInt)
        putIntBE(programPatched, bytePos, target);
      }
    } else {
      System.out.println("Aviso: no hay " + PROGRAM_RELOCS + " (v1 sin parches).");
    }

    // 5) Concatenar: programa parcheado + código de paquetes reubicado(s)
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    baos.write(programPatched);
    for (var entry : pkgCodeBytes.entrySet()) {
      baos.write(entry.getValue());
    }

    Files.write(Paths.get(LINKED_OUTPUT), baos.toByteArray());
    System.out.println("Linked OK -> " + LINKED_OUTPUT);
  }
}
