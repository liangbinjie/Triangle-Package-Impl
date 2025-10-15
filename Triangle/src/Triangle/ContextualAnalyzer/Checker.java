/*
 * @(#)Checker.java                        2.1 2003/10/07
 *
 * Copyright (C) 1999, 2003 D.A. Watt and D.F. Brown
 * Dept. of Computing Science, University of Glasgow, Glasgow G12 8QQ Scotland
 * and School of Computer and Math Sciences, The Robert Gordon University,
 * St. Andrew Street, Aberdeen AB25 1HG, Scotland.
 * All rights reserved.
 *
 * This software is provided free for educational use only. It may
 * not be used for commercial purposes without the prior written permission
 * of the authors.
 */

package Triangle.ContextualAnalyzer;

import java.util.ArrayList;
import java.util.*;

import Triangle.ErrorReporter;
import Triangle.StdEnvironment;
import Triangle.AbstractSyntaxTrees.*;
import Triangle.ContextualAnalyzer.PackageLoader;
import Triangle.ContextualAnalyzer.PackageLoader.PackageArtifact;
import Triangle.ContextualAnalyzer.PackageLoader.ExportEntry;
import Triangle.SyntacticAnalyzer.SourcePosition;

public final class Checker implements Visitor {

  // =========================
  // Helpers proyecto paquetes
  // =========================

  // Recolecta los nombres exportados (en el propio AST del paquete)
  private void collectExportNames(Declaration d, List<String> out) {
    if (d == null) return;
    if (d instanceof SequentialDeclaration) {
      SequentialDeclaration sd = (SequentialDeclaration) d;
      collectExportNames(sd.D1, out);
      collectExportNames(sd.D2, out);
      return;
    }
    if (d instanceof ExportDeclaration) {
      ExportDeclaration ed = (ExportDeclaration) d;
      if (ed.I != null) out.add(ed.I.spelling);
    }
  }

  // Indexa declaraciones top-level por nombre
  private void indexDeclarations(Declaration d, Map<String, Declaration> out) {
    if (d == null) return;
    if (d instanceof SequentialDeclaration) {
      SequentialDeclaration sd = (SequentialDeclaration) d;
      indexDeclarations(sd.D1, out);
      indexDeclarations(sd.D2, out);
      return;
    }
    if (d instanceof ConstDeclaration)   out.put(((ConstDeclaration)d).I.spelling, d);
    else if (d instanceof VarDeclaration)   out.put(((VarDeclaration)d).I.spelling, d);
    else if (d instanceof TypeDeclaration)  out.put(((TypeDeclaration)d).I.spelling, d);
    else if (d instanceof ProcDeclaration)  out.put(((ProcDeclaration)d).I.spelling, d);
    else if (d instanceof FuncDeclaration)  out.put(((FuncDeclaration)d).I.spelling, d);
  }

  // =========================
  // [PKG-TYPES] Serialización de tipos
  // =========================

  // Convierte un TypeDenoter a una cadena "parseable"
  // Soporta: Integer | Boolean | Char | Ident | array[n] of T | record(f:T, ...)
  private String typeToString(TypeDenoter t) {
    if (t == null) return "unknown";
    if (t == StdEnvironment.integerType) return "Integer";
    if (t == StdEnvironment.booleanType) return "Boolean";
    if (t == StdEnvironment.charType)    return "Char";

    if (t instanceof SimpleTypeDenoter) {
      return ((SimpleTypeDenoter)t).I.spelling;
    }
    if (t instanceof ArrayTypeDenoter) {
      ArrayTypeDenoter at = (ArrayTypeDenoter)t;
      return "array[" + at.IL.spelling + "] of " + typeToString(at.T);
    }
    if (t instanceof RecordTypeDenoter) {
      return "record(" + recordFieldsToString(((RecordTypeDenoter)t).FT) + ")";
    }
    return "unknown";
  }

  // Serializa campos de record: f1:T1,f2:T2,...
  private String recordFieldsToString(FieldTypeDenoter ft) {
    if (ft instanceof SingleFieldTypeDenoter) {
      SingleFieldTypeDenoter s = (SingleFieldTypeDenoter)ft;
      return s.I.spelling + ":" + typeToString(s.T);
    } else if (ft instanceof MultipleFieldTypeDenoter) {
      MultipleFieldTypeDenoter m = (MultipleFieldTypeDenoter)ft;
      return m.I.spelling + ":" + typeToString(m.T) + "," + recordFieldsToString(m.FT);
    }
    return "";
  }

  // Para imprimir firmas de funciones/procedimientos
  private String fpsToString(FormalParameterSequence fps) {
    if (fps instanceof EmptyFormalParameterSequence) return "";
    if (fps instanceof SingleFormalParameterSequence)
      return formalToString(((SingleFormalParameterSequence)fps).FP);
    if (fps instanceof MultipleFormalParameterSequence) {
      MultipleFormalParameterSequence m = (MultipleFormalParameterSequence)fps;
      return formalToString(m.FP) + "," + fpsToString(m.FPS);
    }
    return "";
  }

  private String formalToString(FormalParameter fp) {
    if (fp instanceof ConstFormalParameter) {
      return typeToString(((ConstFormalParameter)fp).T);
    } else if (fp instanceof VarFormalParameter) {
      return "var " + typeToString(((VarFormalParameter)fp).T);
    } else if (fp instanceof ProcFormalParameter) {
      return "proc(" + fpsToString(((ProcFormalParameter)fp).FPS) + ")";
    } else if (fp instanceof FuncFormalParameter) {
      FuncFormalParameter f = (FuncFormalParameter)fp;
      return "func(" + fpsToString(f.FPS) + "):" + typeToString(f.T);
    }
    return "?";
  }

  // =========================
  // [PKG-TYPES] Parser inverso de firmas y tipos para imports
  // =========================

  // Soporta: Integer | Boolean | Char | Ident | array[n] of T | record(f:T,...)
  private TypeDenoter parseTypeSig(String s, SourcePosition pos) {
    String str = (s == null) ? "" : s.trim();
    if (str.equals("Integer")) return StdEnvironment.integerType;
    if (str.equals("Boolean")) return StdEnvironment.booleanType;
    if (str.equals("Char"))    return StdEnvironment.charType;

    if (str.startsWith("array[")) {
      int rb = str.indexOf(']');
      int of = str.indexOf(" of ", rb + 1);
      int n  = Integer.parseInt(str.substring("array[".length(), rb).trim());
      TypeDenoter elem = parseTypeSig(str.substring(of + 4), pos);
      IntegerLiteral il = new IntegerLiteral(Integer.toString(n), pos);
      return new ArrayTypeDenoter(il, elem, pos);
    }

    if (str.startsWith("record(") && str.endsWith(")")) {
      String inside = str.substring("record(".length(), str.length() - 1);
      // Construimos los campos de derecha a izquierda
      String[] parts = inside.isEmpty() ? new String[0] : inside.split(",");
      FieldTypeDenoter acc = null;
      for (int i = parts.length - 1; i >= 0; --i) {
        String p = parts[i].trim();
        int colon = p.indexOf(':');
        String name = p.substring(0, colon).trim();
        String tStr = p.substring(colon + 1).trim();
        TypeDenoter t = parseTypeSig(tStr, pos);
        Identifier fid = new Identifier(name, pos);
        if (acc == null) acc = new SingleFieldTypeDenoter(fid, t, pos);
        else             acc = new MultipleFieldTypeDenoter(fid, t, acc, pos);
      }
      return new RecordTypeDenoter(acc, pos);
    }

    // Caso SimpleTypeDenoter
    return new SimpleTypeDenoter(new Identifier(str, pos), pos);
  }

  // "func(Integer,var Integer):Boolean" / "proc(Integer,var Char)"
  private String[] splitSig(String sig) {
    String s = (sig == null) ? "" : sig.trim();
    if (s.startsWith("func")) {
      int lp = s.indexOf('(');
      int rp = s.lastIndexOf(')');
      int colon = s.lastIndexOf(':');
      String params = (lp>=0 && rp>lp) ? s.substring(lp+1, rp) : "";
      String ret    = (colon>rp) ? s.substring(colon+1).trim() : "";
      return new String[]{"func", params, ret};
    }
    if (s.startsWith("proc")) {
      int lp = s.indexOf('(');
      int rp = s.lastIndexOf(')');
      String params = (lp>=0 && rp>lp) ? s.substring(lp+1, rp) : "";
      return new String[]{"proc", params, null};
    }
    return new String[]{"unknown", "", null};
  }

  // Lista de parámetros: "Integer,var Color" -> [(isVar,type), ...]
  private List<Object[]> parseParamList(String s, SourcePosition pos) {
    ArrayList<Object[]> ps = new ArrayList<>();
    if (s == null || s.trim().isEmpty()) return ps;
    for (String raw : s.split(",")) {
      String t = raw.trim();
      boolean isVar = false;
      if (t.startsWith("var ")) {
        isVar = true;
        t = t.substring(4).trim();
      }
      ps.add(new Object[]{Boolean.valueOf(isVar), parseTypeSig(t, pos)});
    }
    return ps;
  }

  private FormalParameterSequence makeFPS(List<Object[]> params, SourcePosition pos) {
    if (params.isEmpty()) return new EmptyFormalParameterSequence(pos);
    FormalParameter fp = makeFP(params.get(0), pos);
    if (params.size() == 1) return new SingleFormalParameterSequence(fp, pos);
    List<Object[]> tail = params.subList(1, params.size());
    return new MultipleFormalParameterSequence(fp, makeFPS(tail, pos), pos);
  }

  private FormalParameter makeFP(Object[] p, SourcePosition pos) {
    boolean isVar = ((Boolean)p[0]).booleanValue();
    TypeDenoter t = (TypeDenoter)p[1];
    Identifier dummy = new Identifier("_", pos);
    return isVar ? new VarFormalParameter(dummy, t, pos)
                 : new ConstFormalParameter(dummy, t, pos);
  }

  // Crea declaraciones sintéticas TIPADAS a partir del .tpk
  private Declaration makeSyntheticDeclForExport(ExportEntry e, SourcePosition pos) {
    String[] parts = splitSig(e.typeSig == null ? "" : e.typeSig);
    String kind = e.kind == null ? "unknown" : e.kind;
    Identifier name = new Identifier(e.name, pos);

    if ("func".equals(kind) && "func".equals(parts[0])) {
      List<Object[]> params = parseParamList(parts[1], pos);
      TypeDenoter ret = parseTypeSig(parts[2], pos);
      FormalParameterSequence fps = makeFPS(params, pos);
      Expression emptyBody = new EmptyExpression(pos);
      return new FuncDeclaration(name, fps, ret, emptyBody, pos);
    }

    if ("proc".equals(kind) && "proc".equals(parts[0])) {
      List<Object[]> params = parseParamList(parts[1], pos);
      FormalParameterSequence fps = makeFPS(params, pos);
      Command empty = new EmptyCommand(pos);
      return new ProcDeclaration(name, fps, empty, pos);
    }

    if ("type".equals(kind)) {
      TypeDenoter t = parseTypeSig(e.typeSig, pos); // e.g. record(...)
      return new TypeDeclaration(name, t, pos);
    }

    if ("const".equals(kind)) {
      // firma exportada con ":T"
      String ttxt = e.typeSig != null && e.typeSig.startsWith(":")
                  ? e.typeSig.substring(1).trim() : (e.typeSig == null ? "" : e.typeSig);
      TypeDenoter t = parseTypeSig(ttxt, pos);
      EmptyExpression ee = new EmptyExpression(pos);
      ee.type = t; // ¡clave! para que el checker conozca el tipo de la const importada
      return new ConstDeclaration(name, ee, pos);
    }

    if ("var".equals(kind)) {
      // firma exportada con ":T"
      String ttxt = e.typeSig != null && e.typeSig.startsWith(":")
                  ? e.typeSig.substring(1).trim() : (e.typeSig == null ? "" : e.typeSig);
      TypeDenoter t = parseTypeSig(ttxt, pos);
      return new VarDeclaration(name, t, pos);
    }

    // Desconocido: placeholder inocuo
    return new VarDeclaration(name, StdEnvironment.errorType, pos);
  }

  // =========================
  // Import/Export de paquetes
  // =========================

  @Override
  public Object visitImportDeclaration(ImportDeclaration ast, Object o) {
    PackageArtifact art = null;
    try {
      art = PackageLoader.load(ast.packageId.spelling);
    } catch (Exception ex) {
      reporter.reportError("package \"" + ast.packageId.spelling + "\" not found", "", ast.position);
      return null;
    }

    Map<String, ExportEntry> index = new HashMap<>();
    for (ExportEntry e : art.exports) index.put(e.name, e);

    Map<String, Declaration> live = livePackages.get(ast.packageId.spelling);

    if (ast.names != null) {
      // [PKG-TYPES] PASADA 1: declarar primero los 'type'
      for (Identifier want : ast.names) {
        ExportEntry e = index.get(want.spelling);
        if (e != null && "type".equals(e.kind)) {
          Declaration d = (live != null) ? live.get(want.spelling)
                                         : makeSyntheticDeclForExport(e, want.position);
          idTable.enter(want.spelling, d);
        }
      }
      // PASADA 2: declarar el resto (const/var/func/proc)
      for (Identifier want : ast.names) {
        ExportEntry e = index.get(want.spelling);
        if (e == null) {
          reporter.reportError("\"" + want.spelling + "\" is not exported by package \"" + art.packageName + "\"", "", want.position);
          continue;
        }
        if ("type".equals(e.kind)) continue; // ya lo hicimos
        Declaration d = (live != null) ? live.get(want.spelling)
                                       : makeSyntheticDeclForExport(e, want.position);
        idTable.enter(want.spelling, d);
      }
    } else {
      // import P  (marcador de módulo)
      AnyTypeDenoter any = new AnyTypeDenoter(ast.position);
      VarDeclaration moduleMarker = new VarDeclaration(ast.packageId, any, ast.position);
      idTable.enter(ast.packageId.spelling, moduleMarker);
    }
    return null;
  }

  @Override
  public Object visitPackageCommand(PackageCommand ast, Object o) {
    // 1) Chequear declaraciones internas del paquete
    ast.D.visit(this, o);

    // 2) Construir índice nombre->Declaration
    Map<String, Declaration> index = new HashMap<>();
    indexDeclarations(ast.D, index);

    // 3) Recolectar los exportados
    List<String> names = new ArrayList<>();
    collectExportNames(ast.D, names);

    // 4) Preparar artefacto .tpk
    PackageArtifact art = new PackageArtifact();
    art.packageName = ast.I.spelling;

    // 4b) Guardar “vivo” para esta corrida (para enlazar decls reales si se importan enseguida)
    Map<String, Declaration> live = new HashMap<>();

    for (String n : names) {
      Declaration d = index.get(n);
      if (d == null) continue;

      String kind = "unknown";
      String sig  = "";

      if (d instanceof FuncDeclaration) {
        FuncDeclaration fd = (FuncDeclaration)d;
        kind = "func";
        sig  = "func(" + fpsToString(fd.FPS) + "):" + typeToString(fd.T);
      } else if (d instanceof ProcDeclaration) {
        ProcDeclaration pd = (ProcDeclaration)d;
        kind = "proc";
        sig  = "proc(" + fpsToString(pd.FPS) + ")";
      } else if (d instanceof VarDeclaration) {
        VarDeclaration vd = (VarDeclaration)d;
        kind = "var";
        sig  = ":" + typeToString(vd.T);
      } else if (d instanceof TypeDeclaration) {
        TypeDeclaration td = (TypeDeclaration)d;
        kind = "type";
        // [PKG-TYPES] exportar forma estructural del tipo
        sig  = typeToString(td.T);
      } else if (d instanceof ConstDeclaration) {
        ConstDeclaration cd = (ConstDeclaration)d;
        kind = "const";
        String t = (cd.E != null && cd.E.type != null) ? typeToString(cd.E.type) : "?";
        sig = ":" + t;
      }

      art.exports.add(PackageLoader.mk(n, kind, sig));
      live.put(n, d);
    }

    try {
      PackageLoader.save(art);
    } catch (Exception ex) {
      reporter.reportError("cannot save package \"" + ast.I.spelling + "\"", "", ast.position);
    }

    // 5) Registrar paquete “vivo”
    livePackages.put(ast.I.spelling, live);
    return null;
  }

  // =========================
  // Commands
  // =========================

  public Object visitAssignCommand(AssignCommand ast, Object o) {
    TypeDenoter vType = (TypeDenoter) ast.V.visit(this, null);
    TypeDenoter eType = (TypeDenoter) ast.E.visit(this, null);
    if (!ast.V.variable)
      reporter.reportError ("LHS of assignment is not a variable", "", ast.V.position);
    if (! eType.equals(vType))
      reporter.reportError ("assignment incompatibilty", "", ast.position);
    return null;
  }

  public Object visitCallCommand(CallCommand ast, Object o) {
    Declaration binding = (Declaration) ast.I.visit(this, null);
    if (binding == null)
      reportUndeclared(ast.I);
    else if (binding instanceof ProcDeclaration) {
      ast.APS.visit(this, ((ProcDeclaration) binding).FPS);
    } else if (binding instanceof ProcFormalParameter) {
      ast.APS.visit(this, ((ProcFormalParameter) binding).FPS);
    } else
      reporter.reportError("\"%\" is not a procedure identifier",
                           ast.I.spelling, ast.I.position);
    return null;
  }

  public Object visitEmptyCommand(EmptyCommand ast, Object o) {
    return null;
  }

  public Object visitIfCommand(IfCommand ast, Object o) {
    TypeDenoter eType = (TypeDenoter) ast.E.visit(this, null);
    if (! eType.equals(StdEnvironment.booleanType))
      reporter.reportError("Boolean expression expected here", "", ast.E.position);
    ast.C1.visit(this, null);
    ast.C2.visit(this, null);
    return null;
  }

  public Object visitLetCommand(LetCommand ast, Object o) {
    idTable.openScope();
    ast.D.visit(this, null);
    ast.C.visit(this, null);
    idTable.closeScope();
    return null;
  }

  public Object visitSequentialCommand(SequentialCommand ast, Object o) {
    ast.C1.visit(this, o);
    ast.C2.visit(this, o);
    return null;
  }

  public Object visitWhileCommand(WhileCommand ast, Object o) {
    TypeDenoter eType = (TypeDenoter) ast.E.visit(this, null);
    if (! eType.equals(StdEnvironment.booleanType))
      reporter.reportError("Boolean expression expected here", "", ast.E.position);
    ast.C.visit(this, null);
    return null;
  }

  // =========================
  // Expressions
  // =========================

  public Object visitArrayExpression(ArrayExpression ast, Object o) {
    TypeDenoter elemType = (TypeDenoter) ast.AA.visit(this, null);
    IntegerLiteral il = new IntegerLiteral(Integer.toString(ast.AA.elemCount),
                                           ast.position);
    ast.type = new ArrayTypeDenoter(il, elemType, ast.position);
    return ast.type;
  }

  public Object visitBinaryExpression(BinaryExpression ast, Object o) {
    TypeDenoter e1Type = (TypeDenoter) ast.E1.visit(this, null);
    TypeDenoter e2Type = (TypeDenoter) ast.E2.visit(this, null);
    Declaration binding = (Declaration) ast.O.visit(this, null);

    if (binding == null)
      reportUndeclared(ast.O);
    else {
      if (! (binding instanceof BinaryOperatorDeclaration))
        reporter.reportError ("\"%\" is not a binary operator",
                              ast.O.spelling, ast.O.position);
      BinaryOperatorDeclaration bbinding = (BinaryOperatorDeclaration) binding;
      if (bbinding.ARG1 == StdEnvironment.anyType) {
        if (! e1Type.equals(e2Type))
          reporter.reportError ("incompatible argument types for \"%\"",
                                ast.O.spelling, ast.position);
      } else if (! e1Type.equals(bbinding.ARG1))
          reporter.reportError ("wrong argument type for \"%\"",
                                ast.O.spelling, ast.E1.position);
      else if (! e2Type.equals(bbinding.ARG2))
          reporter.reportError ("wrong argument type for \"%\"",
                                ast.O.spelling, ast.E2.position);
      ast.type = bbinding.RES;
    }
    return ast.type;
  }

  public Object visitCallExpression(CallExpression ast, Object o) {
    Declaration binding = (Declaration) ast.I.visit(this, null);
    if (binding == null) {
      reportUndeclared(ast.I);
      ast.type = StdEnvironment.errorType;
    } else if (binding instanceof FuncDeclaration) {
      ast.APS.visit(this, ((FuncDeclaration) binding).FPS);
      ast.type = ((FuncDeclaration) binding).T;
    } else if (binding instanceof FuncFormalParameter) {
      ast.APS.visit(this, ((FuncFormalParameter) binding).FPS);
      ast.type = ((FuncFormalParameter) binding).T;
    } else
      reporter.reportError("\"%\" is not a function identifier",
                           ast.I.spelling, ast.I.position);
    return ast.type;
  }

  public Object visitCharacterExpression(CharacterExpression ast, Object o) {
    ast.type = StdEnvironment.charType;
    return ast.type;
  }

  public Object visitEmptyExpression(EmptyExpression ast, Object o) {
    ast.type = null;
    return ast.type;
  }

  public Object visitIfExpression(IfExpression ast, Object o) {
    TypeDenoter e1Type = (TypeDenoter) ast.E1.visit(this, null);
    if (! e1Type.equals(StdEnvironment.booleanType))
      reporter.reportError ("Boolean expression expected here", "",
                            ast.E1.position);
    TypeDenoter e2Type = (TypeDenoter) ast.E2.visit(this, null);
    TypeDenoter e3Type = (TypeDenoter) ast.E3.visit(this, null);
    if (! e2Type.equals(e3Type))
      reporter.reportError ("incompatible limbs in if-expression", "", ast.position);
    ast.type = e2Type;
    return ast.type;
  }

  public Object visitIntegerExpression(IntegerExpression ast, Object o) {
    ast.type = StdEnvironment.integerType;
    return ast.type;
  }

  public Object visitLetExpression(LetExpression ast, Object o) {
    idTable.openScope();
    ast.D.visit(this, null);
    ast.type = (TypeDenoter) ast.E.visit(this, null);
    idTable.closeScope();
    return ast.type;
  }

  public Object visitRecordExpression(RecordExpression ast, Object o) {
    FieldTypeDenoter rType = (FieldTypeDenoter) ast.RA.visit(this, null);
    ast.type = new RecordTypeDenoter(rType, ast.position);
    return ast.type;
  }

  public Object visitUnaryExpression(UnaryExpression ast, Object o) {
    TypeDenoter eType = (TypeDenoter) ast.E.visit(this, null);
    Declaration binding = (Declaration) ast.O.visit(this, null);
    if (binding == null) {
      reportUndeclared(ast.O);
      ast.type = StdEnvironment.errorType;
    } else if (! (binding instanceof UnaryOperatorDeclaration))
        reporter.reportError ("\"%\" is not a unary operator",
                              ast.O.spelling, ast.O.position);
    else {
      UnaryOperatorDeclaration ubinding = (UnaryOperatorDeclaration) binding;
      if (! eType.equals(ubinding.ARG))
        reporter.reportError ("wrong argument type for \"%\"",
                              ast.O.spelling, ast.O.position);
      ast.type = ubinding.RES;
    }
    return ast.type;
  }

  public Object visitVnameExpression(VnameExpression ast, Object o) {
    ast.type = (TypeDenoter) ast.V.visit(this, null);
    return ast.type;
  }

  // =========================
  // Declarations
  // =========================

  public Object visitBinaryOperatorDeclaration(BinaryOperatorDeclaration ast, Object o) {
    return null;
  }

  public Object visitConstDeclaration(ConstDeclaration ast, Object o) {
    TypeDenoter eType = (TypeDenoter) ast.E.visit(this, null);
    idTable.enter(ast.I.spelling, ast);
    if (ast.duplicated)
      reporter.reportError ("identifier \"%\" already declared",
                            ast.I.spelling, ast.position);
    return null;
  }

  public Object visitFuncDeclaration(FuncDeclaration ast, Object o) {
    ast.T = (TypeDenoter) ast.T.visit(this, null);
    idTable.enter (ast.I.spelling, ast); // permite recursión
    if (ast.duplicated)
      reporter.reportError ("identifier \"%\" already declared",
                            ast.I.spelling, ast.position);
    idTable.openScope();
    ast.FPS.visit(this, null);
    TypeDenoter eType = (TypeDenoter) ast.E.visit(this, null);
    idTable.closeScope();
    if (! ast.T.equals(eType))
      reporter.reportError ("body of function \"%\" has wrong type",
                            ast.I.spelling, ast.E.position);
    return null;
  }

  public Object visitProcDeclaration(ProcDeclaration ast, Object o) {
    idTable.enter (ast.I.spelling, ast); // permite recursión
    if (ast.duplicated)
      reporter.reportError ("identifier \"%\" already declared",
                            ast.I.spelling, ast.position);
    idTable.openScope();
    ast.FPS.visit(this, null);
    ast.C.visit(this, null);
    idTable.closeScope();
    return null;
  }

  public Object visitSequentialDeclaration(SequentialDeclaration ast, Object o) {
    ast.D1.visit(this, null);
    ast.D2.visit(this, null);
    return null;
  }

  public Object visitTypeDeclaration(TypeDeclaration ast, Object o) {
    ast.T = (TypeDenoter) ast.T.visit(this, null);
    idTable.enter (ast.I.spelling, ast);
    if (ast.duplicated)
      reporter.reportError ("identifier \"%\" already declared",
                            ast.I.spelling, ast.position);
    return null;
  }

  public Object visitUnaryOperatorDeclaration(UnaryOperatorDeclaration ast, Object o) {
    return null;
  }

  public Object visitVarDeclaration(VarDeclaration ast, Object o) {
    ast.T = (TypeDenoter) ast.T.visit(this, null);
    idTable.enter (ast.I.spelling, ast);
    if (ast.duplicated)
      reporter.reportError ("identifier \"%\" already declared",
                            ast.I.spelling, ast.position);
    return null;
  }

  public Object visitExportDeclaration(ExportDeclaration ast, Object o) {
    Declaration binding = (Declaration) ast.I.visit(this, null);
    if (binding == null) {
      reportUndeclared(ast.I);
    } else {
      if (!(binding instanceof ProcDeclaration) &&
          !(binding instanceof FuncDeclaration) &&
          !(binding instanceof TypeDeclaration) &&
          !(binding instanceof ConstDeclaration) &&
          !(binding instanceof VarDeclaration)) {
        reporter.reportError("\"%\" cannot be exported", ast.I.spelling, ast.I.position);
      }
    }
    return null;
  }

  // =========================
  // Array Aggregates
  // =========================

  public Object visitMultipleArrayAggregate(MultipleArrayAggregate ast, Object o) {
    TypeDenoter eType = (TypeDenoter) ast.E.visit(this, null);
    TypeDenoter elemType = (TypeDenoter) ast.AA.visit(this, null);
    ast.elemCount = ast.AA.elemCount + 1;
    if (! eType.equals(elemType))
      reporter.reportError ("incompatible array-aggregate element", "", ast.E.position);
    return elemType;
  }

  public Object visitSingleArrayAggregate(SingleArrayAggregate ast, Object o) {
    TypeDenoter elemType = (TypeDenoter) ast.E.visit(this, null);
    ast.elemCount = 1;
    return elemType;
  }

  // =========================
  // Record Aggregates
  // =========================

  public Object visitMultipleRecordAggregate(MultipleRecordAggregate ast, Object o) {
    TypeDenoter eType = (TypeDenoter) ast.E.visit(this, null);
    FieldTypeDenoter rType = (FieldTypeDenoter) ast.RA.visit(this, null);
    TypeDenoter fType = checkFieldIdentifier(rType, ast.I);
    if (fType != StdEnvironment.errorType)
      reporter.reportError ("duplicate field \"%\" in record",
                            ast.I.spelling, ast.I.position);
    ast.type = new MultipleFieldTypeDenoter(ast.I, eType, rType, ast.position);
    return ast.type;
  }

  public Object visitSingleRecordAggregate(SingleRecordAggregate ast, Object o) {
    TypeDenoter eType = (TypeDenoter) ast.E.visit(this, null);
    ast.type = new SingleFieldTypeDenoter(ast.I, eType, ast.position);
    return ast.type;
  }

  // =========================
  // Formal Parameters
  // =========================

  public Object visitConstFormalParameter(ConstFormalParameter ast, Object o) {
    ast.T = (TypeDenoter) ast.T.visit(this, null);
    idTable.enter(ast.I.spelling, ast);
    if (ast.duplicated)
      reporter.reportError ("duplicated formal parameter \"%\"",
                            ast.I.spelling, ast.position);
    return null;
  }

  public Object visitFuncFormalParameter(FuncFormalParameter ast, Object o) {
    idTable.openScope();
    ast.FPS.visit(this, null);
    idTable.closeScope();
    ast.T = (TypeDenoter) ast.T.visit(this, null);
    idTable.enter (ast.I.spelling, ast);
    if (ast.duplicated)
      reporter.reportError ("duplicated formal parameter \"%\"",
                            ast.I.spelling, ast.position);
    return null;
  }

  public Object visitProcFormalParameter(ProcFormalParameter ast, Object o) {
    idTable.openScope();
    ast.FPS.visit(this, null);
    idTable.closeScope();
    idTable.enter (ast.I.spelling, ast);
    if (ast.duplicated)
      reporter.reportError ("duplicated formal parameter \"%\"",
                            ast.I.spelling, ast.position);
    return null;
  }

  public Object visitVarFormalParameter(VarFormalParameter ast, Object o) {
    ast.T = (TypeDenoter) ast.T.visit(this, null);
    idTable.enter (ast.I.spelling, ast);
    if (ast.duplicated)
      reporter.reportError ("duplicated formal parameter \"%\"",
                            ast.I.spelling, ast.position);
    return null;
  }

  public Object visitEmptyFormalParameterSequence(EmptyFormalParameterSequence ast, Object o) {
    return null;
  }

  public Object visitMultipleFormalParameterSequence(MultipleFormalParameterSequence ast, Object o) {
    ast.FP.visit(this, null);
    ast.FPS.visit(this, null);
    return null;
  }

  public Object visitSingleFormalParameterSequence(SingleFormalParameterSequence ast, Object o) {
    ast.FP.visit(this, null);
    return null;
  }

  // =========================
  // Actual Parameters
  // =========================

  public Object visitConstActualParameter(ConstActualParameter ast, Object o) {
    FormalParameter fp = (FormalParameter) o;
    TypeDenoter eType = (TypeDenoter) ast.E.visit(this, null);

    if (! (fp instanceof ConstFormalParameter))
      reporter.reportError ("const actual parameter not expected here", "", ast.position);
    else if (! eType.equals(((ConstFormalParameter) fp).T))
      reporter.reportError ("wrong type for const actual parameter", "", ast.E.position);
    return null;
  }

  public Object visitFuncActualParameter(FuncActualParameter ast, Object o) {
    FormalParameter fp = (FormalParameter) o;

    Declaration binding = (Declaration) ast.I.visit(this, null);
    if (binding == null)
      reportUndeclared (ast.I);
    else if (! (binding instanceof FuncDeclaration ||
                binding instanceof FuncFormalParameter))
      reporter.reportError ("\"%\" is not a function identifier",
                            ast.I.spelling, ast.I.position);
    else if (! (fp instanceof FuncFormalParameter))
      reporter.reportError ("func actual parameter not expected here", "", ast.position);
    else {
      FormalParameterSequence FPS = null;
      TypeDenoter T = null;
      if (binding instanceof FuncDeclaration) {
        FPS = ((FuncDeclaration) binding).FPS;
        T = ((FuncDeclaration) binding).T;
      } else {
        FPS = ((FuncFormalParameter) binding).FPS;
        T = ((FuncFormalParameter) binding).T;
      }
      if (! FPS.equals(((FuncFormalParameter) fp).FPS))
        reporter.reportError ("wrong signature for function \"%\"",
                              ast.I.spelling, ast.I.position);
      else if (! T.equals(((FuncFormalParameter) fp).T))
        reporter.reportError ("wrong type for function \"%\"",
                              ast.I.spelling, ast.I.position);
    }
    return null;
  }

  public Object visitProcActualParameter(ProcActualParameter ast, Object o) {
    FormalParameter fp = (FormalParameter) o;

    Declaration binding = (Declaration) ast.I.visit(this, null);
    if (binding == null)
      reportUndeclared (ast.I);
    else if (! (binding instanceof ProcDeclaration ||
                binding instanceof ProcFormalParameter))
      reporter.reportError ("\"%\" is not a procedure identifier",
                            ast.I.spelling, ast.I.position);
    else if (! (fp instanceof ProcFormalParameter))
      reporter.reportError ("proc actual parameter not expected here", "", ast.position);
    else {
      FormalParameterSequence FPS = null;
      if (binding instanceof ProcDeclaration)
        FPS = ((ProcDeclaration) binding).FPS;
      else
        FPS = ((ProcFormalParameter) binding).FPS;
      if (! FPS.equals(((ProcFormalParameter) fp).FPS))
        reporter.reportError ("wrong signature for procedure \"%\"",
                              ast.I.spelling, ast.I.position);
    }
    return null;
  }

  public Object visitVarActualParameter(VarActualParameter ast, Object o) {
    FormalParameter fp = (FormalParameter) o;

    TypeDenoter vType = (TypeDenoter) ast.V.visit(this, null);
    if (! ast.V.variable)
      reporter.reportError ("actual parameter is not a variable", "",
                            ast.V.position);
    else if (! (fp instanceof VarFormalParameter))
      reporter.reportError ("var actual parameter not expected here", "",
                            ast.V.position);
    else if (! vType.equals(((VarFormalParameter) fp).T))
      reporter.reportError ("wrong type for var actual parameter", "",
                            ast.V.position);
    return null;
  }

  public Object visitEmptyActualParameterSequence(EmptyActualParameterSequence ast, Object o) {
    FormalParameterSequence fps = (FormalParameterSequence) o;
    if (! (fps instanceof EmptyFormalParameterSequence))
      reporter.reportError ("too few actual parameters", "", ast.position);
    return null;
  }

  public Object visitMultipleActualParameterSequence(MultipleActualParameterSequence ast, Object o) {
    FormalParameterSequence fps = (FormalParameterSequence) o;
    if (! (fps instanceof MultipleFormalParameterSequence))
      reporter.reportError ("too many actual parameters", "", ast.position);
    else {
      ast.AP.visit(this, ((MultipleFormalParameterSequence) fps).FP);
      ast.APS.visit(this, ((MultipleFormalParameterSequence) fps).FPS);
    }
    return null;
  }

  public Object visitSingleActualParameterSequence(SingleActualParameterSequence ast, Object o) {
    FormalParameterSequence fps = (FormalParameterSequence) o;
    if (! (fps instanceof SingleFormalParameterSequence))
      reporter.reportError ("incorrect number of actual parameters", "", ast.position);
    else {
      ast.AP.visit(this, ((SingleFormalParameterSequence) fps).FP);
    }
    return null;
  }

  // =========================
  // Type Denoters
  // =========================

  public Object visitAnyTypeDenoter(AnyTypeDenoter ast, Object o) {
    return StdEnvironment.anyType;
  }

  public Object visitArrayTypeDenoter(ArrayTypeDenoter ast, Object o) {
    ast.T = (TypeDenoter) ast.T.visit(this, null);
    if ((Integer.valueOf(ast.IL.spelling).intValue()) == 0)
      reporter.reportError ("arrays must not be empty", "", ast.IL.position);
    return ast;
  }

  public Object visitBoolTypeDenoter(BoolTypeDenoter ast, Object o) {
    return StdEnvironment.booleanType;
  }

  public Object visitCharTypeDenoter(CharTypeDenoter ast, Object o) {
    return StdEnvironment.charType;
  }

  public Object visitErrorTypeDenoter(ErrorTypeDenoter ast, Object o) {
    return StdEnvironment.errorType;
  }

  public Object visitSimpleTypeDenoter(SimpleTypeDenoter ast, Object o) {
    Declaration binding = (Declaration) ast.I.visit(this, null);
    if (binding == null) {
      reportUndeclared (ast.I);
      return StdEnvironment.errorType;
    } else if (! (binding instanceof TypeDeclaration)) {
      reporter.reportError ("\"%\" is not a type identifier",
                            ast.I.spelling, ast.I.position);
      return StdEnvironment.errorType;
    }
    return ((TypeDeclaration) binding).T;
  }

  public Object visitIntTypeDenoter(IntTypeDenoter ast, Object o) {
    return StdEnvironment.integerType;
  }

  public Object visitRecordTypeDenoter(RecordTypeDenoter ast, Object o) {
    ast.FT = (FieldTypeDenoter) ast.FT.visit(this, null);
    return ast;
  }

  public Object visitMultipleFieldTypeDenoter(MultipleFieldTypeDenoter ast, Object o) {
    ast.T = (TypeDenoter) ast.T.visit(this, null);
    ast.FT.visit(this, null);
    return ast;
  }

  public Object visitSingleFieldTypeDenoter(SingleFieldTypeDenoter ast, Object o) {
    ast.T = (TypeDenoter) ast.T.visit(this, null);
    return ast;
  }

  // =========================
  // Literals, Identifiers and Operators
  // =========================

  public Object visitCharacterLiteral(CharacterLiteral CL, Object o) {
    return StdEnvironment.charType;
  }

  public Object visitIdentifier(Identifier I, Object o) {
    Declaration binding = idTable.retrieve(I.spelling);
    if (binding != null)
      I.decl = binding;
    return binding;
  }

  public Object visitIntegerLiteral(IntegerLiteral IL, Object o) {
    return StdEnvironment.integerType;
  }

  public Object visitOperator(Operator O, Object o) {
    Declaration binding = idTable.retrieve(O.spelling);
    if (binding != null)
      O.decl = binding;
    return binding;
  }

  // =========================
  // Value-or-variable names
  // =========================

  private static TypeDenoter checkFieldIdentifier(FieldTypeDenoter ast, Identifier I) {
    if (ast instanceof MultipleFieldTypeDenoter) {
      MultipleFieldTypeDenoter ft = (MultipleFieldTypeDenoter) ast;
      if (ft.I.spelling.compareTo(I.spelling) == 0) {
        I.decl = ast;
        return ft.T;
      } else {
        return checkFieldIdentifier (ft.FT, I);
      }
    } else if (ast instanceof SingleFieldTypeDenoter) {
      SingleFieldTypeDenoter ft = (SingleFieldTypeDenoter) ast;
      if (ft.I.spelling.compareTo(I.spelling) == 0) {
        I.decl = ast;
        return ft.T;
      }
    }
    return StdEnvironment.errorType;
  }

  public Object visitDotVname(DotVname ast, Object o) {
    ast.type = null;
    TypeDenoter vType = (TypeDenoter) ast.V.visit(this, null);
    ast.variable = ast.V.variable;
    if (! (vType instanceof RecordTypeDenoter))
      reporter.reportError ("record expected here", "", ast.V.position);
    else {
      ast.type = checkFieldIdentifier(((RecordTypeDenoter) vType).FT, ast.I);
      if (ast.type == StdEnvironment.errorType)
        reporter.reportError ("no field \"%\" in this record type",
                              ast.I.spelling, ast.I.position);
    }
    return ast.type;
  }

  public Object visitSimpleVname(SimpleVname ast, Object o) {
    ast.variable = false;
    ast.type = StdEnvironment.errorType;
    Declaration binding = (Declaration) ast.I.visit(this, null);
    if (binding == null)
      reportUndeclared(ast.I);
    else if (binding instanceof ConstDeclaration) {
      ast.type = ((ConstDeclaration) binding).E.type;
      ast.variable = false;
    } else if (binding instanceof VarDeclaration) {
      ast.type = ((VarDeclaration) binding).T;
      ast.variable = true;
    } else if (binding instanceof ConstFormalParameter) {
      ast.type = ((ConstFormalParameter) binding).T;
      ast.variable = false;
    } else if (binding instanceof VarFormalParameter) {
      ast.type = ((VarFormalParameter) binding).T;
      ast.variable = true;
    } else
      reporter.reportError ("\"%\" is not a const or var identifier",
                            ast.I.spelling, ast.I.position);
    return ast.type;
  }

  public Object visitSubscriptVname(SubscriptVname ast, Object o) {
    TypeDenoter vType = (TypeDenoter) ast.V.visit(this, null);
    ast.variable = ast.V.variable;
    TypeDenoter eType = (TypeDenoter) ast.E.visit(this, null);
    if (vType != StdEnvironment.errorType) {
      if (! (vType instanceof ArrayTypeDenoter))
        reporter.reportError ("array expected here", "", ast.V.position);
      else {
        if (! eType.equals(StdEnvironment.integerType))
          reporter.reportError ("Integer expression expected here", "",
                ast.E.position);
        ast.type = ((ArrayTypeDenoter) vType).T;
      }
    }
    return ast.type;
  }

  // =========================
  // Programs
  // =========================

  public Object visitProgram(Program ast, Object o) {
    ast.C.visit(this, null);
    return null;
  }

  // =========================
  // Driver
  // =========================

  public void check(Program ast) {
    ast.visit(this, null);
  }

  public Checker (ErrorReporter reporter) {
    this.reporter = reporter;
    this.idTable = new IdentificationTable ();
    establishStdEnvironment();
  }

  private IdentificationTable idTable;
  private static SourcePosition dummyPos = new SourcePosition();
  private ErrorReporter reporter;

  // Paquetes “vivos” en esta corrida (para reutilizar decls reales si están)
  private final Map<String, Map<String, Declaration>> livePackages = new HashMap<>();

  // Reporte de no declarado
  private void reportUndeclared (Terminal leaf) {
    reporter.reportError("\"%\" is not declared", leaf.spelling, leaf.position);
  }

  // =========================
  // StdEnvironment
  // =========================

  private final static Identifier dummyI = new Identifier("", dummyPos);

  private TypeDeclaration declareStdType (String id, TypeDenoter typedenoter) {
    TypeDeclaration binding = new TypeDeclaration(new Identifier(id, dummyPos), typedenoter, dummyPos);
    idTable.enter(id, binding);
    return binding;
  }

  private ConstDeclaration declareStdConst (String id, TypeDenoter constType) {
    IntegerExpression constExpr = new IntegerExpression(null, dummyPos);
    constExpr.type = constType;
    ConstDeclaration binding = new ConstDeclaration(new Identifier(id, dummyPos), constExpr, dummyPos);
    idTable.enter(id, binding);
    return binding;
  }

  private ProcDeclaration declareStdProc (String id, FormalParameterSequence fps) {
    ProcDeclaration binding = new ProcDeclaration(new Identifier(id, dummyPos), fps,
                                  new EmptyCommand(dummyPos), dummyPos);
    idTable.enter(id, binding);
    return binding;
  }

  private FuncDeclaration declareStdFunc (String id, FormalParameterSequence fps,
                                          TypeDenoter resultType) {
    FuncDeclaration binding = new FuncDeclaration(new Identifier(id, dummyPos), fps, resultType,
                                  new EmptyExpression(dummyPos), dummyPos);
    idTable.enter(id, binding);
    return binding;
  }

  private UnaryOperatorDeclaration declareStdUnaryOp
    (String op, TypeDenoter argType, TypeDenoter resultType) {
    UnaryOperatorDeclaration binding = new UnaryOperatorDeclaration (new Operator(op, dummyPos),
                                            argType, resultType, dummyPos);
    idTable.enter(op, binding);
    return binding;
  }

  private BinaryOperatorDeclaration declareStdBinaryOp
    (String op, TypeDenoter arg1Type, TypeDenoter arg2type, TypeDenoter resultType) {
    BinaryOperatorDeclaration binding = new BinaryOperatorDeclaration (new Operator(op, dummyPos),
                                             arg1Type, arg2type, resultType, dummyPos);
    idTable.enter(op, binding);
    return binding;
  }

  private void establishStdEnvironment () {
    StdEnvironment.booleanType = new BoolTypeDenoter(dummyPos);
    StdEnvironment.integerType = new IntTypeDenoter(dummyPos);
    StdEnvironment.charType = new CharTypeDenoter(dummyPos);
    StdEnvironment.anyType = new AnyTypeDenoter(dummyPos);
    StdEnvironment.errorType = new ErrorTypeDenoter(dummyPos);

    StdEnvironment.booleanDecl = declareStdType("Boolean", StdEnvironment.booleanType);
    StdEnvironment.falseDecl = declareStdConst("false", StdEnvironment.booleanType);
    StdEnvironment.trueDecl = declareStdConst("true", StdEnvironment.booleanType);
    StdEnvironment.notDecl = declareStdUnaryOp("\\", StdEnvironment.booleanType, StdEnvironment.booleanType);
    StdEnvironment.andDecl = declareStdBinaryOp("/\\", StdEnvironment.booleanType, StdEnvironment.booleanType, StdEnvironment.booleanType);
    StdEnvironment.orDecl = declareStdBinaryOp("\\/", StdEnvironment.booleanType, StdEnvironment.booleanType, StdEnvironment.booleanType);

    StdEnvironment.integerDecl = declareStdType("Integer", StdEnvironment.integerType);
    StdEnvironment.maxintDecl = declareStdConst("maxint", StdEnvironment.integerType);
    StdEnvironment.addDecl = declareStdBinaryOp("+", StdEnvironment.integerType, StdEnvironment.integerType, StdEnvironment.integerType);
    StdEnvironment.subtractDecl = declareStdBinaryOp("-", StdEnvironment.integerType, StdEnvironment.integerType, StdEnvironment.integerType);
    StdEnvironment.multiplyDecl = declareStdBinaryOp("*", StdEnvironment.integerType, StdEnvironment.integerType, StdEnvironment.integerType);
    StdEnvironment.divideDecl = declareStdBinaryOp("/", StdEnvironment.integerType, StdEnvironment.integerType, StdEnvironment.integerType);
    StdEnvironment.moduloDecl = declareStdBinaryOp("//", StdEnvironment.integerType, StdEnvironment.integerType, StdEnvironment.integerType);
    StdEnvironment.lessDecl = declareStdBinaryOp("<", StdEnvironment.integerType, StdEnvironment.integerType, StdEnvironment.booleanType);
    StdEnvironment.notgreaterDecl = declareStdBinaryOp("<=", StdEnvironment.integerType, StdEnvironment.integerType, StdEnvironment.booleanType);
    StdEnvironment.greaterDecl = declareStdBinaryOp(">", StdEnvironment.integerType, StdEnvironment.integerType, StdEnvironment.booleanType);
    StdEnvironment.notlessDecl = declareStdBinaryOp(">=", StdEnvironment.integerType, StdEnvironment.integerType, StdEnvironment.booleanType);

    StdEnvironment.charDecl = declareStdType("Char", StdEnvironment.charType);
    StdEnvironment.chrDecl = declareStdFunc("chr", new SingleFormalParameterSequence(
                                      new ConstFormalParameter(dummyI, StdEnvironment.integerType, dummyPos), dummyPos), StdEnvironment.charType);
    StdEnvironment.ordDecl = declareStdFunc("ord", new SingleFormalParameterSequence(
                                      new ConstFormalParameter(dummyI, StdEnvironment.charType, dummyPos), dummyPos), StdEnvironment.integerType);
    StdEnvironment.eofDecl = declareStdFunc("eof", new EmptyFormalParameterSequence(dummyPos), StdEnvironment.booleanType);
    StdEnvironment.eolDecl = declareStdFunc("eol", new EmptyFormalParameterSequence(dummyPos), StdEnvironment.booleanType);
    StdEnvironment.getDecl = declareStdProc("get", new SingleFormalParameterSequence(
                                      new VarFormalParameter(dummyI, StdEnvironment.charType, dummyPos), dummyPos));
    StdEnvironment.putDecl = declareStdProc("put", new SingleFormalParameterSequence(
                                      new ConstFormalParameter(dummyI, StdEnvironment.charType, dummyPos), dummyPos));
    StdEnvironment.getintDecl = declareStdProc("getint", new SingleFormalParameterSequence(
                                            new VarFormalParameter(dummyI, StdEnvironment.integerType, dummyPos), dummyPos));
    StdEnvironment.putintDecl = declareStdProc("putint", new SingleFormalParameterSequence(
                                            new ConstFormalParameter(dummyI, StdEnvironment.integerType, dummyPos), dummyPos));
    StdEnvironment.geteolDecl = declareStdProc("geteol", new EmptyFormalParameterSequence(dummyPos));
    StdEnvironment.puteolDecl = declareStdProc("puteol", new EmptyFormalParameterSequence(dummyPos));
    StdEnvironment.equalDecl = declareStdBinaryOp("=", StdEnvironment.anyType, StdEnvironment.anyType, StdEnvironment.booleanType);
    StdEnvironment.unequalDecl = declareStdBinaryOp("\\=", StdEnvironment.anyType, StdEnvironment.anyType, StdEnvironment.booleanType);
  }
}
