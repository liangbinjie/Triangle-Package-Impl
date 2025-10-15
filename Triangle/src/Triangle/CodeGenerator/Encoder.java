/*
 * @(#)Encoder.java                        2.1 2003/10/07
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

package Triangle.CodeGenerator;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import TAM.Instruction;
import TAM.Machine;
import Triangle.ErrorReporter;
import Triangle.StdEnvironment;
import Triangle.AbstractSyntaxTrees.AST;
import Triangle.AbstractSyntaxTrees.AnyTypeDenoter;
import Triangle.AbstractSyntaxTrees.ArrayExpression;
import Triangle.AbstractSyntaxTrees.ArrayTypeDenoter;
import Triangle.AbstractSyntaxTrees.AssignCommand;
import Triangle.AbstractSyntaxTrees.BinaryExpression;
import Triangle.AbstractSyntaxTrees.BinaryOperatorDeclaration;
import Triangle.AbstractSyntaxTrees.BoolTypeDenoter;
import Triangle.AbstractSyntaxTrees.CallCommand;
import Triangle.AbstractSyntaxTrees.CallExpression;
import Triangle.AbstractSyntaxTrees.CharTypeDenoter;
import Triangle.AbstractSyntaxTrees.CharacterExpression;
import Triangle.AbstractSyntaxTrees.CharacterLiteral;
import Triangle.AbstractSyntaxTrees.ConstActualParameter;
import Triangle.AbstractSyntaxTrees.ConstDeclaration;
import Triangle.AbstractSyntaxTrees.ConstFormalParameter;
import Triangle.AbstractSyntaxTrees.Declaration;
import Triangle.AbstractSyntaxTrees.DotVname;
import Triangle.AbstractSyntaxTrees.EmptyActualParameterSequence;
import Triangle.AbstractSyntaxTrees.EmptyCommand;
import Triangle.AbstractSyntaxTrees.EmptyExpression;
import Triangle.AbstractSyntaxTrees.EmptyFormalParameterSequence;
import Triangle.AbstractSyntaxTrees.ErrorTypeDenoter;
import Triangle.AbstractSyntaxTrees.ExportDeclaration;
import Triangle.AbstractSyntaxTrees.FuncActualParameter;
import Triangle.AbstractSyntaxTrees.FuncDeclaration;
import Triangle.AbstractSyntaxTrees.FuncFormalParameter;
import Triangle.AbstractSyntaxTrees.Identifier;
import Triangle.AbstractSyntaxTrees.IfCommand;
import Triangle.AbstractSyntaxTrees.IfExpression;
import Triangle.AbstractSyntaxTrees.IntTypeDenoter;
import Triangle.AbstractSyntaxTrees.IntegerExpression;
import Triangle.AbstractSyntaxTrees.IntegerLiteral;
import Triangle.AbstractSyntaxTrees.LetCommand;
import Triangle.AbstractSyntaxTrees.LetExpression;
import Triangle.AbstractSyntaxTrees.MultipleActualParameterSequence;
import Triangle.AbstractSyntaxTrees.MultipleArrayAggregate;
import Triangle.AbstractSyntaxTrees.MultipleFieldTypeDenoter;
import Triangle.AbstractSyntaxTrees.MultipleFormalParameterSequence;
import Triangle.AbstractSyntaxTrees.MultipleRecordAggregate;
import Triangle.AbstractSyntaxTrees.Operator;
import Triangle.AbstractSyntaxTrees.PackageCommand;
import Triangle.AbstractSyntaxTrees.ProcActualParameter;
import Triangle.AbstractSyntaxTrees.ProcDeclaration;
import Triangle.AbstractSyntaxTrees.ProcFormalParameter;
import Triangle.AbstractSyntaxTrees.Program;
import Triangle.AbstractSyntaxTrees.RecordExpression;
import Triangle.AbstractSyntaxTrees.RecordTypeDenoter;
import Triangle.AbstractSyntaxTrees.SequentialCommand;
import Triangle.AbstractSyntaxTrees.SequentialDeclaration;
import Triangle.AbstractSyntaxTrees.SimpleTypeDenoter;
import Triangle.AbstractSyntaxTrees.SimpleVname;
import Triangle.AbstractSyntaxTrees.SingleActualParameterSequence;
import Triangle.AbstractSyntaxTrees.SingleArrayAggregate;
import Triangle.AbstractSyntaxTrees.SingleFieldTypeDenoter;
import Triangle.AbstractSyntaxTrees.SingleFormalParameterSequence;
import Triangle.AbstractSyntaxTrees.SingleRecordAggregate;
import Triangle.AbstractSyntaxTrees.SubscriptVname;
import Triangle.AbstractSyntaxTrees.TypeDeclaration;
import Triangle.AbstractSyntaxTrees.UnaryExpression;
import Triangle.AbstractSyntaxTrees.UnaryOperatorDeclaration;
import Triangle.AbstractSyntaxTrees.VarActualParameter;
import Triangle.AbstractSyntaxTrees.VarDeclaration;
import Triangle.AbstractSyntaxTrees.VarFormalParameter;
import Triangle.AbstractSyntaxTrees.Visitor;
import Triangle.AbstractSyntaxTrees.Vname;
import Triangle.AbstractSyntaxTrees.VnameExpression;
import Triangle.AbstractSyntaxTrees.WhileCommand;
import Triangle.AbstractSyntaxTrees.ImportDeclaration;

public final class Encoder implements Visitor {

    // ===== VARIABLES PARA SOPORTE DE PAQUETES =====
    
    private String currentPackageName = null;
    private boolean compilingPackage = false;
    
    private Set<Declaration> exportedDecls = new HashSet<>();
    private List<String> exportLines = new ArrayList<>();
    private List<String> importLines = new ArrayList<>();
    private Map<Declaration, String> importedNames = new HashMap<>();
    private Map<String, String> importedByName = new HashMap<>();
    private List<String> relocLines = new ArrayList<>();
    
    /**
     * Lista de imports encontrados durante la compilación
     */
    private List<String> imports = new ArrayList<>();

    /**
     * Lista de relocalizaciones necesarias (dirección, símbolo)
     */
    private List<String> relocations = new ArrayList<>();

    /**
     * Mapa de exports del paquete actual (nombre -> dirección)
     */
    private Map<String, Integer> exports = new HashMap<>();
    
    // ===== FIN VARIABLES =====

    private boolean isCompilingProgram() {
        return currentPackageName == null;
    }

    // ===== VISITOR METHODS =====

    // Import Declaration
    public Object visitImportDeclaration(ImportDeclaration ast, Object o) {
        Frame frame = (Frame) o;
        
        // ImportDeclaration tiene:
        // - packageId: Identifier del paquete (ej: "Utils")
        // - names: array de Identifiers específicos (null si es import completo)
        
        String packageName = ast.packageId.spelling;
        
        if (ast.names == null) {
            // Import completo: "import Utils"
            imports.add("import " + packageName);
            importLines.add("import " + packageName);
            System.out.println("[Encoder] Import (complete package): " + packageName);
        } else {
            // Import específico: "from Utils import min, max"
            for (Identifier name : ast.names) {
                String fullName = packageName + "." + name.spelling;
                imports.add("import " + fullName);
                importLines.add("import " + fullName);
                
                // Guardar por nombre para lookup posterior
                importedByName.put(name.spelling, fullName);
                
                System.out.println("[Encoder] Import (specific): " + fullName);
            }
        }
        
        return new Integer(0);
    }

    // Export Declaration
    public Object visitExportDeclaration(ExportDeclaration ast, Object o) {
        // ExportDeclaration solo tiene un Identifier I (el nombre a exportar)
        // No tiene una declaración hija, es solo una marca
    
        String symbolName = ast.I.spelling;
    
        System.out.println("[Encoder] Export declaration marked: " + symbolName);
    
        // Buscar la declaración correspondiente en el entorno actual
        // y marcarla para exportación
        if (ast.I.decl != null && ast.I.decl instanceof Declaration) {  // <-- CAMBIO AQUÍ
            Declaration decl = (Declaration) ast.I.decl;  // <-- CAST EXPLÍCITO
            exportedDecls.add(decl);
            
            // Intentar registrar el export inmediatamente si es una constante
            if (decl instanceof ConstDeclaration) {
                ConstDeclaration constDecl = (ConstDeclaration) decl;
                if (constDecl.entity instanceof KnownValue) {
                    KnownValue kv = (KnownValue) constDecl.entity;
                    Integer address = kv.value;
                    exports.put(symbolName, address);
                    
                    String qname = (currentPackageName != null) 
                        ? currentPackageName + "." + symbolName 
                        : symbolName;
                    exportLines.add("export " + qname + " " + address);
                    
                    System.out.println("[Encoder] Export registered (const): " + symbolName + " = " + address);
                }
            }
            // Las funciones/procedimientos se exportarán en visitPackageCommand
        }
        
        return new Integer(0);
    }

    // Commands
    public Object visitAssignCommand(AssignCommand ast, Object o) {
        Frame frame = (Frame) o;
        Integer valSize = (Integer) ast.E.visit(this, frame);
        encodeStore(ast.V, new Frame(frame, valSize.intValue()), valSize.intValue());
        return null;
    }

    public Object visitCallCommand(CallCommand ast, Object o) {
        Frame frame = (Frame) o;
        Integer argsSize = (Integer) ast.APS.visit(this, frame);
        ast.I.visit(this, new Frame(frame.level, argsSize));
        return null;
    }

    public Object visitEmptyCommand(EmptyCommand ast, Object o) {
        return null;
    }

    public Object visitIfCommand(IfCommand ast, Object o) {
        Frame frame = (Frame) o;
        int jumpifAddr, jumpAddr;

        Integer valSize = (Integer) ast.E.visit(this, frame);
        jumpifAddr = nextInstrAddr;
        emit(Machine.JUMPIFop, Machine.falseRep, Machine.CBr, 0);
        ast.C1.visit(this, frame);
        jumpAddr = nextInstrAddr;
        emit(Machine.JUMPop, 0, Machine.CBr, 0);
        patch(jumpifAddr, nextInstrAddr);
        ast.C2.visit(this, frame);
        patch(jumpAddr, nextInstrAddr);
        return null;
    }

    public Object visitLetCommand(LetCommand ast, Object o) {
        Frame frame = (Frame) o;
        int extraSize = ((Integer) ast.D.visit(this, frame)).intValue();
        ast.C.visit(this, new Frame(frame, extraSize));
        if (extraSize > 0)
            emit(Machine.POPop, 0, 0, extraSize);
        return null;
    }

    public Object visitSequentialCommand(SequentialCommand ast, Object o) {
        ast.C1.visit(this, o);
        ast.C2.visit(this, o);
        return null;
    }

    public Object visitWhileCommand(WhileCommand ast, Object o) {
        Frame frame = (Frame) o;
        int jumpAddr, loopAddr;

        jumpAddr = nextInstrAddr;
        emit(Machine.JUMPop, 0, Machine.CBr, 0);
        loopAddr = nextInstrAddr;
        ast.C.visit(this, frame);
        patch(jumpAddr, nextInstrAddr);
        ast.E.visit(this, frame);
        emit(Machine.JUMPIFop, Machine.trueRep, Machine.CBr, loopAddr);
        return null;
    }

    public Object visitPackageCommand(PackageCommand ast, Object o) {
        this.currentPackageName = ast.I.spelling;
        compilingPackage = true;

        System.out.println("[Encoder] Compiling package (via PackageCommand): " + currentPackageName);

        // Genera el código de todas las D (funcs, procs, exports, etc.)
        Object res = ast.D.visit(this, o);

        // Al final, ya todas las entidades tienen dirección.
        // Reconstruir exportLines con las direcciones reales
        exportLines.clear();
        for (Declaration d : exportedDecls) {
            String name = null;
            ObjectAddress addr = null;

            if (d instanceof FuncDeclaration) {
                FuncDeclaration fd = (FuncDeclaration) d;
                name = fd.I.spelling;
                if (fd.entity instanceof KnownRoutine) {
                    addr = ((KnownRoutine) fd.entity).address;
                }
            } else if (d instanceof ProcDeclaration) {
                ProcDeclaration pd = (ProcDeclaration) d;
                name = pd.I.spelling;
                if (pd.entity instanceof KnownRoutine) {
                    addr = ((KnownRoutine) pd.entity).address;
                }
            } else if (d instanceof ConstDeclaration) {
                ConstDeclaration cd = (ConstDeclaration) d;
                name = cd.I.spelling;
                if (cd.entity instanceof KnownValue) {
                    int value = ((KnownValue) cd.entity).value;
                    exports.put(name, value);
                    String qname = currentPackageName + "." + name;
                    exportLines.add("export " + qname + " " + value);
                    continue; // Ya procesado
                }
            }

            if (name != null && addr != null) {
                String qname = currentPackageName + "." + name;
                exports.put(name, addr.displacement);
                exportLines.add("export " + qname + " " + addr.displacement);
            }
        }

        System.out.println("[Encoder] Package compilation complete. Exports: " + exports.size());

        return res;
    }

    // Expressions
    public Object visitArrayExpression(ArrayExpression ast, Object o) {
        ast.type.visit(this, null);
        return ast.AA.visit(this, o);
    }

    public Object visitBinaryExpression(BinaryExpression ast, Object o) {
        Frame frame = (Frame) o;
        Integer valSize = (Integer) ast.type.visit(this, null);
        int valSize1 = ((Integer) ast.E1.visit(this, frame)).intValue();
        Frame frame1 = new Frame(frame, valSize1);
        int valSize2 = ((Integer) ast.E2.visit(this, frame1)).intValue();
        Frame frame2 = new Frame(frame.level, valSize1 + valSize2);
        ast.O.visit(this, frame2);
        return valSize;
    }

    public Object visitCallExpression(CallExpression ast, Object o) {
        Frame frame = (Frame) o;
        Integer valSize = (Integer) ast.type.visit(this, null);
        Integer argsSize = (Integer) ast.APS.visit(this, frame);
        ast.I.visit(this, new Frame(frame.level, argsSize));
        return valSize;
    }

    public Object visitCharacterExpression(CharacterExpression ast, Object o) {
        Frame frame = (Frame) o;
        Integer valSize = (Integer) ast.type.visit(this, null);
        emit(Machine.LOADLop, 0, 0, ast.CL.spelling.charAt(1));
        return valSize;
    }

    public Object visitEmptyExpression(EmptyExpression ast, Object o) {
        return new Integer(0);
    }

    public Object visitIfExpression(IfExpression ast, Object o) {
        Frame frame = (Frame) o;
        Integer valSize;
        int jumpifAddr, jumpAddr;

        ast.type.visit(this, null);
        ast.E1.visit(this, frame);
        jumpifAddr = nextInstrAddr;
        emit(Machine.JUMPIFop, Machine.falseRep, Machine.CBr, 0);
        valSize = (Integer) ast.E2.visit(this, frame);
        jumpAddr = nextInstrAddr;
        emit(Machine.JUMPop, 0, Machine.CBr, 0);
        patch(jumpifAddr, nextInstrAddr);
        valSize = (Integer) ast.E3.visit(this, frame);
        patch(jumpAddr, nextInstrAddr);
        return valSize;
    }

    public Object visitIntegerExpression(IntegerExpression ast, Object o) {
        Frame frame = (Frame) o;
        Integer valSize = (Integer) ast.type.visit(this, null);
        emit(Machine.LOADLop, 0, 0, Integer.parseInt(ast.IL.spelling));
        return valSize;
    }

    public Object visitLetExpression(LetExpression ast, Object o) {
        Frame frame = (Frame) o;
        ast.type.visit(this, null);
        int extraSize = ((Integer) ast.D.visit(this, frame)).intValue();
        Frame frame1 = new Frame(frame, extraSize);
        Integer valSize = (Integer) ast.E.visit(this, frame1);
        if (extraSize > 0)
            emit(Machine.POPop, valSize.intValue(), 0, extraSize);
        return valSize;
    }

    public Object visitRecordExpression(RecordExpression ast, Object o) {
        ast.type.visit(this, null);
        return ast.RA.visit(this, o);
    }

    public Object visitUnaryExpression(UnaryExpression ast, Object o) {
        Frame frame = (Frame) o;
        Integer valSize = (Integer) ast.type.visit(this, null);
        ast.E.visit(this, frame);
        ast.O.visit(this, new Frame(frame.level, valSize.intValue()));
        return valSize;
    }

    public Object visitVnameExpression(VnameExpression ast, Object o) {
        Frame frame = (Frame) o;
        Integer valSize = (Integer) ast.type.visit(this, null);
        encodeFetch(ast.V, frame, valSize.intValue());
        return valSize;
    }

    // Declarations
    public Object visitBinaryOperatorDeclaration(BinaryOperatorDeclaration ast, Object o) {
        return new Integer(0);
    }

    public Object visitConstDeclaration(ConstDeclaration ast, Object o) {
        Frame frame = (Frame) o;
        int extraSize = 0;

        if (ast.E instanceof CharacterExpression) {
            CharacterLiteral CL = ((CharacterExpression) ast.E).CL;
            ast.entity = new KnownValue(Machine.characterSize,
                    characterValuation(CL.spelling));
        } else if (ast.E instanceof IntegerExpression) {
            IntegerLiteral IL = ((IntegerExpression) ast.E).IL;
            ast.entity = new KnownValue(Machine.integerSize,
                    Integer.parseInt(IL.spelling));
        } else {
            int valSize = ((Integer) ast.E.visit(this, frame)).intValue();
            ast.entity = new UnknownValue(valSize, frame.level, frame.size);
            extraSize = valSize;
        }
        writeTableDetails(ast);
        return new Integer(extraSize);
    }

    public Object visitFuncDeclaration(FuncDeclaration ast, Object o) {
        Frame frame = (Frame) o;
        int jumpAddr = nextInstrAddr;
        int argsSize = 0, valSize = 0;

        emit(Machine.JUMPop, 0, Machine.CBr, 0);
        ast.entity = new KnownRoutine(Machine.closureSize, frame.level, nextInstrAddr);
        writeTableDetails(ast);

        if (exportedDecls.contains(ast)) {
            if (ast.entity instanceof KnownRoutine) {
                ObjectAddress addr = ((KnownRoutine) ast.entity).address;
                String qname = (currentPackageName != null ? currentPackageName + "." + ast.I.spelling
                        : ast.I.spelling);
                exportLines.add("export " + qname + " " + addr.displacement);
            }
        }

        if (frame.level == Machine.maxRoutineLevel)
            reporter.reportRestriction("can't nest routines more than 7 deep");
        else {
            Frame frame1 = new Frame(frame.level + 1, 0);
            argsSize = ((Integer) ast.FPS.visit(this, frame1)).intValue();
            Frame frame2 = new Frame(frame.level + 1, Machine.linkDataSize);
            valSize = ((Integer) ast.E.visit(this, frame2)).intValue();
        }
        emit(Machine.RETURNop, valSize, 0, argsSize);
        patch(jumpAddr, nextInstrAddr);
        return new Integer(0);
    }

    public Object visitProcDeclaration(ProcDeclaration ast, Object o) {
        Frame frame = (Frame) o;
        int jumpAddr = nextInstrAddr;
        int argsSize = 0;

        emit(Machine.JUMPop, 0, Machine.CBr, 0);
        ast.entity = new KnownRoutine(Machine.closureSize, frame.level, nextInstrAddr);
        writeTableDetails(ast);

        if (exportedDecls.contains(ast)) {
            if (ast.entity instanceof KnownRoutine) {
                ObjectAddress addr = ((KnownRoutine) ast.entity).address;
                String qname = (currentPackageName != null ? currentPackageName + "." + ast.I.spelling
                        : ast.I.spelling);
                exportLines.add("export " + qname + " " + addr.displacement);
            }
        }

        if (frame.level == Machine.maxRoutineLevel)
            reporter.reportRestriction("can't nest routines so deeply");
        else {
            Frame frame1 = new Frame(frame.level + 1, 0);
            argsSize = ((Integer) ast.FPS.visit(this, frame1)).intValue();
            Frame frame2 = new Frame(frame.level + 1, Machine.linkDataSize);
            ast.C.visit(this, frame2);
        }
        emit(Machine.RETURNop, 0, 0, argsSize);
        patch(jumpAddr, nextInstrAddr);
        return new Integer(0);
    }

    public Object visitSequentialDeclaration(SequentialDeclaration ast, Object o) {
        Frame frame = (Frame) o;
        int extraSize1, extraSize2;

        extraSize1 = ((Integer) ast.D1.visit(this, frame)).intValue();
        Frame frame1 = new Frame(frame, extraSize1);
        extraSize2 = ((Integer) ast.D2.visit(this, frame1)).intValue();
        return new Integer(extraSize1 + extraSize2);
    }

    public Object visitTypeDeclaration(TypeDeclaration ast, Object o) {
        // just to ensure the type's representation is decided
        ast.T.visit(this, null);
        return new Integer(0);
    }

    public Object visitUnaryOperatorDeclaration(UnaryOperatorDeclaration ast, Object o) {
        return new Integer(0);
    }

    public Object visitVarDeclaration(VarDeclaration ast, Object o) {
        Frame frame = (Frame) o;
        int extraSize;

        extraSize = ((Integer) ast.T.visit(this, null)).intValue();
        emit(Machine.PUSHop, 0, 0, extraSize);
        ast.entity = new KnownAddress(Machine.addressSize, frame.level, frame.size);
        writeTableDetails(ast);
        return new Integer(extraSize);
    }

    // Array Aggregates
    public Object visitMultipleArrayAggregate(MultipleArrayAggregate ast, Object o) {
        Frame frame = (Frame) o;
        int elemSize = ((Integer) ast.E.visit(this, frame)).intValue();
        Frame frame1 = new Frame(frame, elemSize);
        int arraySize = ((Integer) ast.AA.visit(this, frame1)).intValue();
        return new Integer(elemSize + arraySize);
    }

    public Object visitSingleArrayAggregate(SingleArrayAggregate ast, Object o) {
        return ast.E.visit(this, o);
    }

    // Record Aggregates
    public Object visitMultipleRecordAggregate(MultipleRecordAggregate ast, Object o) {
        Frame frame = (Frame) o;
        int fieldSize = ((Integer) ast.E.visit(this, frame)).intValue();
        Frame frame1 = new Frame(frame, fieldSize);
        int recordSize = ((Integer) ast.RA.visit(this, frame1)).intValue();
        return new Integer(fieldSize + recordSize);
    }

    public Object visitSingleRecordAggregate(SingleRecordAggregate ast, Object o) {
        return ast.E.visit(this, o);
    }

    // Formal Parameters
    public Object visitConstFormalParameter(ConstFormalParameter ast, Object o) {
        Frame frame = (Frame) o;
        int valSize = ((Integer) ast.T.visit(this, null)).intValue();
        ast.entity = new UnknownValue(valSize, frame.level, -frame.size - valSize);
        writeTableDetails(ast);
        return new Integer(valSize);
    }

    public Object visitFuncFormalParameter(FuncFormalParameter ast, Object o) {
        Frame frame = (Frame) o;
        int argsSize = Machine.closureSize;
        ast.entity = new UnknownRoutine(Machine.closureSize, frame.level,
                -frame.size - argsSize);
        writeTableDetails(ast);
        return new Integer(argsSize);
    }

    public Object visitProcFormalParameter(ProcFormalParameter ast, Object o) {
        Frame frame = (Frame) o;
        int argsSize = Machine.closureSize;
        ast.entity = new UnknownRoutine(Machine.closureSize, frame.level,
                -frame.size - argsSize);
        writeTableDetails(ast);
        return new Integer(argsSize);
    }

    public Object visitVarFormalParameter(VarFormalParameter ast, Object o) {
        Frame frame = (Frame) o;
        ast.T.visit(this, null);
        ast.entity = new UnknownAddress(Machine.addressSize, frame.level,
                -frame.size - Machine.addressSize);
        writeTableDetails(ast);
        return new Integer(Machine.addressSize);
    }

    public Object visitEmptyFormalParameterSequence(EmptyFormalParameterSequence ast, Object o) {
        return new Integer(0);
    }

    public Object visitMultipleFormalParameterSequence(MultipleFormalParameterSequence ast, Object o) {
        Frame frame = (Frame) o;
        int argsSize1 = ((Integer) ast.FPS.visit(this, frame)).intValue();
        Frame frame1 = new Frame(frame, argsSize1);
        int argsSize2 = ((Integer) ast.FP.visit(this, frame1)).intValue();
        return new Integer(argsSize1 + argsSize2);
    }

    public Object visitSingleFormalParameterSequence(SingleFormalParameterSequence ast, Object o) {
        return ast.FP.visit(this, o);
    }

    // Actual Parameters
    public Object visitConstActualParameter(ConstActualParameter ast, Object o) {
        return ast.E.visit(this, o);
    }

    public Object visitFuncActualParameter(FuncActualParameter ast, Object o) {
        Frame frame = (Frame) o;
        if (ast.I.decl.entity instanceof KnownRoutine) {
            ObjectAddress address = ((KnownRoutine) ast.I.decl.entity).address;
            emit(Machine.LOADAop, 0, displayRegister(frame.level, address.level), 0);
            emit(Machine.LOADAop, 0, Machine.CBr, address.displacement);
        } else if (ast.I.decl.entity instanceof UnknownRoutine) {
            ObjectAddress address = ((UnknownRoutine) ast.I.decl.entity).address;
            emit(Machine.LOADop, Machine.closureSize, displayRegister(frame.level,
                    address.level), address.displacement);
        } else if (ast.I.decl.entity instanceof PrimitiveRoutine) {
            int displacement = ((PrimitiveRoutine) ast.I.decl.entity).displacement;
            emit(Machine.LOADAop, 0, Machine.SBr, 0);
            emit(Machine.LOADAop, 0, Machine.PBr, displacement);
        }
        return new Integer(Machine.closureSize);
    }

    public Object visitProcActualParameter(ProcActualParameter ast, Object o) {
        Frame frame = (Frame) o;
        if (ast.I.decl.entity instanceof KnownRoutine) {
            ObjectAddress address = ((KnownRoutine) ast.I.decl.entity).address;
            emit(Machine.LOADAop, 0, displayRegister(frame.level, address.level), 0);
            emit(Machine.LOADAop, 0, Machine.CBr, address.displacement);
        } else if (ast.I.decl.entity instanceof UnknownRoutine) {
            ObjectAddress address = ((UnknownRoutine) ast.I.decl.entity).address;
            emit(Machine.LOADop, Machine.closureSize, displayRegister(frame.level,
                    address.level), address.displacement);
        } else if (ast.I.decl.entity instanceof PrimitiveRoutine) {
            int displacement = ((PrimitiveRoutine) ast.I.decl.entity).displacement;
            emit(Machine.LOADAop, 0, Machine.SBr, 0);
            emit(Machine.LOADAop, 0, Machine.PBr, displacement);
        }
        return new Integer(Machine.closureSize);
    }

    public Object visitVarActualParameter(VarActualParameter ast, Object o) {
        encodeFetchAddress(ast.V, (Frame) o);
        return new Integer(Machine.addressSize);
    }

    public Object visitEmptyActualParameterSequence(EmptyActualParameterSequence ast, Object o) {
        return new Integer(0);
    }

    public Object visitMultipleActualParameterSequence(MultipleActualParameterSequence ast, Object o) {
        Frame frame = (Frame) o;
        int argsSize1 = ((Integer) ast.AP.visit(this, frame)).intValue();
        Frame frame1 = new Frame(frame, argsSize1);
        int argsSize2 = ((Integer) ast.APS.visit(this, frame1)).intValue();
        return new Integer(argsSize1 + argsSize2);
    }

    public Object visitSingleActualParameterSequence(SingleActualParameterSequence ast, Object o) {
        return ast.AP.visit(this, o);
    }

    // Type Denoters
    public Object visitAnyTypeDenoter(AnyTypeDenoter ast, Object o) {
        return new Integer(0);
    }

    public Object visitArrayTypeDenoter(ArrayTypeDenoter ast, Object o) {
        int typeSize;
        if (ast.entity == null) {
            int elemSize = ((Integer) ast.T.visit(this, null)).intValue();
            typeSize = Integer.parseInt(ast.IL.spelling) * elemSize;
            ast.entity = new TypeRepresentation(typeSize);
            writeTableDetails(ast);
        } else
            typeSize = ast.entity.size;
        return new Integer(typeSize);
    }

    public Object visitBoolTypeDenoter(BoolTypeDenoter ast, Object o) {
        if (ast.entity == null) {
            ast.entity = new TypeRepresentation(Machine.booleanSize);
            writeTableDetails(ast);
        }
        return new Integer(Machine.booleanSize);
    }

    public Object visitCharTypeDenoter(CharTypeDenoter ast, Object o) {
        if (ast.entity == null) {
            ast.entity = new TypeRepresentation(Machine.characterSize);
            writeTableDetails(ast);
        }
        return new Integer(Machine.characterSize);
    }

    public Object visitErrorTypeDenoter(ErrorTypeDenoter ast, Object o) {
        return new Integer(0);
    }

    public Object visitSimpleTypeDenoter(SimpleTypeDenoter ast, Object o) {
        return new Integer(0);
    }

    public Object visitIntTypeDenoter(IntTypeDenoter ast, Object o) {
        if (ast.entity == null) {
            ast.entity = new TypeRepresentation(Machine.integerSize);
            writeTableDetails(ast);
        }
        return new Integer(Machine.integerSize);
    }

    public Object visitRecordTypeDenoter(RecordTypeDenoter ast, Object o) {
        int typeSize;
        if (ast.entity == null) {
            typeSize = ((Integer) ast.FT.visit(this, new Integer(0))).intValue();
            ast.entity = new TypeRepresentation(typeSize);
            writeTableDetails(ast);
        } else
            typeSize = ast.entity.size;
        return new Integer(typeSize);
    }

    public Object visitMultipleFieldTypeDenoter(MultipleFieldTypeDenoter ast, Object o) {
        int offset = ((Integer) o).intValue();
        int fieldSize;

        if (ast.entity == null) {
            fieldSize = ((Integer) ast.T.visit(this, null)).intValue();
            ast.entity = new Field(fieldSize, offset);
            writeTableDetails(ast);
        } else
            fieldSize = ast.entity.size;

        Integer offset1 = new Integer(offset + fieldSize);
        int recSize = ((Integer) ast.FT.visit(this, offset1)).intValue();
        return new Integer(fieldSize + recSize);
    }

    public Object visitSingleFieldTypeDenoter(SingleFieldTypeDenoter ast, Object o) {
        int offset = ((Integer) o).intValue();
        int fieldSize;

        if (ast.entity == null) {
            fieldSize = ((Integer) ast.T.visit(this, null)).intValue();
            ast.entity = new Field(fieldSize, offset);
            writeTableDetails(ast);
        } else
            fieldSize = ast.entity.size;

        return new Integer(fieldSize);
    }

    // Literals, Identifiers and Operators
    public Object visitCharacterLiteral(CharacterLiteral ast, Object o) {
        return null;
    }

    public Object visitIdentifier(Identifier ast, Object o) {
        Frame frame = (Frame) o;

        // [PKG-LINK FIX] 1) Caso importado por instancia de Declaration
        if (isCompilingProgram()
                && ast.decl instanceof Declaration
                && importedNames.containsKey((Declaration) ast.decl)) {

            String qname = importedNames.get((Declaration) ast.decl);
            int callInstrIndex = nextInstrAddr;

            // Llamada a rutina top-level (nivel destino 0)
            emit(Machine.CALLop, displayRegister(frame.level, 0), Machine.CBr, 0);

            relocLines.add("reloc " + qname + " " + callInstrIndex);
            return null;
        }

        // [PKG-LINK FIX] 2) Fallback: caso importado por NOMBRE simple
        if (isCompilingProgram()) {
            String qnameByName = importedByName.get(ast.spelling);
            if (qnameByName != null) {
                int callInstrIndex = nextInstrAddr;

                emit(Machine.CALLop, displayRegister(frame.level, 0), Machine.CBr, 0);

                relocLines.add("reloc " + qnameByName + " " + callInstrIndex);
                return null;
            }
        }

        // Comportamiento original para no-importados
        if (ast.decl.entity instanceof KnownRoutine) {
            ObjectAddress address = ((KnownRoutine) ast.decl.entity).address;
            emit(Machine.CALLop, displayRegister(frame.level, address.level),
                    Machine.CBr, address.displacement);
        } else if (ast.decl.entity instanceof UnknownRoutine) {
            ObjectAddress address = ((UnknownRoutine) ast.decl.entity).address;
            emit(Machine.LOADop, Machine.closureSize, displayRegister(frame.level,
                    address.level), address.displacement);
            emit(Machine.CALLIop, 0, 0, 0);
        } else if (ast.decl.entity instanceof PrimitiveRoutine) {
            int displacement = ((PrimitiveRoutine) ast.decl.entity).displacement;
            if (displacement != Machine.idDisplacement)
                emit(Machine.CALLop, Machine.SBr, Machine.PBr, displacement);
        } else if (ast.decl.entity instanceof EqualityRoutine) {
            int displacement = ((EqualityRoutine) ast.decl.entity).displacement;
            emit(Machine.LOADLop, 0, 0, frame.size / 2);
            emit(Machine.CALLop, Machine.SBr, Machine.PBr, displacement);
        }
        return null;
    }

    public Object visitIntegerLiteral(IntegerLiteral ast, Object o) {
        return null;
    }

    public Object visitOperator(Operator ast, Object o) {
        Frame frame = (Frame) o;
        if (ast.decl.entity instanceof KnownRoutine) {
            ObjectAddress address = ((KnownRoutine) ast.decl.entity).address;
            emit(Machine.CALLop, displayRegister(frame.level, address.level),
                    Machine.CBr, address.displacement);
        } else if (ast.decl.entity instanceof UnknownRoutine) {
            ObjectAddress address = ((UnknownRoutine) ast.decl.entity).address;
            emit(Machine.LOADop, Machine.closureSize, displayRegister(frame.level,
                    address.level), address.displacement);
            emit(Machine.CALLIop, 0, 0, 0);
        } else if (ast.decl.entity instanceof PrimitiveRoutine) {
            int displacement = ((PrimitiveRoutine) ast.decl.entity).displacement;
            if (displacement != Machine.idDisplacement)
                emit(Machine.CALLop, Machine.SBr, Machine.PBr, displacement);
        } else if (ast.decl.entity instanceof EqualityRoutine) {
            int displacement = ((EqualityRoutine) ast.decl.entity).displacement;
            emit(Machine.LOADLop, 0, 0, frame.size / 2);
            emit(Machine.CALLop, Machine.SBr, Machine.PBr, displacement);
        }
        return null;
    }

    // Value-or-variable names
    public Object visitDotVname(DotVname ast, Object o) {
        Frame frame = (Frame) o;
        RuntimeEntity baseObject = (RuntimeEntity) ast.V.visit(this, frame);
        ast.offset = ast.V.offset + ((Field) ast.I.decl.entity).fieldOffset;
        ast.indexed = ast.V.indexed;
        return baseObject;
    }

    public Object visitSimpleVname(SimpleVname ast, Object o) {
        ast.offset = 0;
        ast.indexed = false;
        return ast.I.decl.entity;
    }

    public Object visitSubscriptVname(SubscriptVname ast, Object o) {
        Frame frame = (Frame) o;
        RuntimeEntity baseObject;
        int elemSize, indexSize;

        baseObject = (RuntimeEntity) ast.V.visit(this, frame);
        ast.offset = ast.V.offset;
        ast.indexed = ast.V.indexed;
        elemSize = ((Integer) ast.type.visit(this, null)).intValue();
        if (ast.E instanceof IntegerExpression) {
            IntegerLiteral IL = ((IntegerExpression) ast.E).IL;
            ast.offset = ast.offset + Integer.parseInt(IL.spelling) * elemSize;
        } else {
            if (ast.indexed)
                frame.size = frame.size + Machine.integerSize;
            indexSize = ((Integer) ast.E.visit(this, frame)).intValue();
            if (elemSize != 1) {
                emit(Machine.LOADLop, 0, 0, elemSize);
                emit(Machine.CALLop, Machine.SBr, Machine.PBr,
                        Machine.multDisplacement);
            }
            if (ast.indexed)
                emit(Machine.CALLop, Machine.SBr, Machine.PBr, Machine.addDisplacement);
            else
                ast.indexed = true;
        }
        return baseObject;
    }

    // Programs
    public Object visitProgram(Program ast, Object o) {
        return ast.C.visit(this, o);
    }

    // ===== CONSTRUCTOR =====
    
    public Encoder(ErrorReporter reporter) {
        this.reporter = reporter;
        nextInstrAddr = Machine.CB;
        elaborateStdEnvironment();
    }

    private ErrorReporter reporter;

    // ===== ENCODING METHODS =====

    private void writeLinesToFile(String path, List<String> lines) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileOutputStream(path))) {
            for (String s : lines)
                pw.println(s);
        } catch (IOException ex) {
            System.err.println("Could not write " + path + ": " + ex.getMessage());
        }
    }

    public final void encodeRun(Program theAST, boolean tableDetails) {
        // Limpiar datos previos de linkeo
        clearLinkingData();
        
        tableDetailsReqd = tableDetails;

        currentPackageName = null;
        exportedDecls.clear();
        exportLines.clear();
        importLines.clear();
        relocLines.clear();
        importedNames.clear();
        importedByName.clear();

        theAST.visit(this, new Frame(0, 0));
        emit(Machine.HALTop, 0, 0, 0);

        System.out.println("[Encoder] relocLines size = " + relocLines.size()
                + " | imports=" + importLines.size()
                + " | currentPackageName=" + currentPackageName);

        if (currentPackageName != null) {
            if (!exportLines.isEmpty())
                writeLinesToFile(currentPackageName + ".map", exportLines);
        } else {
            if (!importLines.isEmpty())
                writeLinesToFile("program.imp", importLines);
            if (!relocLines.isEmpty())
                writeLinesToFile("program.reloc", relocLines);
        }

        System.out.println("[Encoder] Compilation complete:");
        System.out.println("  Imports: " + imports.size());
        System.out.println("  Relocations: " + relocations.size());
        System.out.println("  Exports: " + exports.size());
        System.out.println("  Package name: " + currentPackageName);
    }

    private final void elaborateStdConst(Declaration constDeclaration, int value) {
        if (constDeclaration instanceof ConstDeclaration) {
            ConstDeclaration decl = (ConstDeclaration) constDeclaration;
            int typeSize = ((Integer) decl.E.type.visit(this, null)).intValue();
            decl.entity = new KnownValue(typeSize, value);
            writeTableDetails(constDeclaration);
        }
    }

    private final void elaborateStdPrimRoutine(Declaration routineDeclaration, int routineOffset) {
        routineDeclaration.entity = new PrimitiveRoutine(Machine.closureSize, routineOffset);
        writeTableDetails(routineDeclaration);
    }

    private final void elaborateStdEqRoutine(Declaration routineDeclaration, int routineOffset) {
        routineDeclaration.entity = new EqualityRoutine(Machine.closureSize, routineOffset);
        writeTableDetails(routineDeclaration);
    }

    private final void elaborateStdRoutine(Declaration routineDeclaration, int routineOffset) {
        routineDeclaration.entity = new KnownRoutine(Machine.closureSize, 0, routineOffset);
        writeTableDetails(routineDeclaration);
    }

    private final void elaborateStdEnvironment() {
        tableDetailsReqd = false;
        elaborateStdConst(StdEnvironment.falseDecl, Machine.falseRep);
        elaborateStdConst(StdEnvironment.trueDecl, Machine.trueRep);
        elaborateStdPrimRoutine(StdEnvironment.notDecl, Machine.notDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.andDecl, Machine.andDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.orDecl, Machine.orDisplacement);
        elaborateStdConst(StdEnvironment.maxintDecl, Machine.maxintRep);
        elaborateStdPrimRoutine(StdEnvironment.addDecl, Machine.addDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.subtractDecl, Machine.subDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.multiplyDecl, Machine.multDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.divideDecl, Machine.divDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.moduloDecl, Machine.modDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.lessDecl, Machine.ltDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.notgreaterDecl, Machine.leDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.greaterDecl, Machine.gtDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.notlessDecl, Machine.geDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.chrDecl, Machine.idDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.ordDecl, Machine.idDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.eolDecl, Machine.eolDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.eofDecl, Machine.eofDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.getDecl, Machine.getDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.putDecl, Machine.putDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.getintDecl, Machine.getintDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.putintDecl, Machine.putintDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.geteolDecl, Machine.geteolDisplacement);
        elaborateStdPrimRoutine(StdEnvironment.puteolDecl, Machine.puteolDisplacement);
        elaborateStdEqRoutine(StdEnvironment.equalDecl, Machine.eqDisplacement);
        elaborateStdEqRoutine(StdEnvironment.unequalDecl, Machine.neDisplacement);
    }

    public void saveObjectProgram(String objectName) {
        FileOutputStream objectFile = null;
        DataOutputStream objectStream = null;

        int addr;

        try {
            objectFile = new FileOutputStream(objectName);
            objectStream = new DataOutputStream(objectFile);

            addr = Machine.CB;
            for (addr = Machine.CB; addr < nextInstrAddr; addr++)
                Machine.code[addr].write(objectStream);
            objectFile.close();
        } catch (FileNotFoundException s) {
            System.err.println("Error opening object file: " + s);
        } catch (IOException s) {
            System.err.println("Error writing object file: " + s);
        }
    }

    boolean tableDetailsReqd;

    public static void writeTableDetails(AST ast) {
    }

    // OBJECT CODE
    private int nextInstrAddr;

    private void emit(int op, int n, int r, int d) {
        Instruction nextInstr = new Instruction();
        if (n > 255) {
            reporter.reportRestriction("length of operand can't exceed 255 words");
            n = 255;
        }
        nextInstr.op = op;
        nextInstr.n = n;
        nextInstr.r = r;
        nextInstr.d = d;
        if (nextInstrAddr == Machine.PB)
            reporter.reportRestriction("too many instructions for code segment");
        else {
            Machine.code[nextInstrAddr] = nextInstr;
            nextInstrAddr = nextInstrAddr + 1;
        }
    }

    private void patch(int addr, int d) {
        Machine.code[addr].d = d;
    }

    // DATA REPRESENTATION
    public int characterValuation(String spelling) {
        return spelling.charAt(1);
    }

    // REGISTERS
    private int displayRegister(int currentLevel, int objectLevel) {
        if (objectLevel == 0)
            return Machine.SBr;
        else if (currentLevel - objectLevel <= 6)
            return Machine.LBr + currentLevel - objectLevel;
        else {
            reporter.reportRestriction("can't access data more than 6 levels out");
            return Machine.L6r;
        }
    }

    private void encodeStore(Vname V, Frame frame, int valSize) {
        RuntimeEntity baseObject = (RuntimeEntity) V.visit(this, frame);
        if (valSize > 255) {
            reporter.reportRestriction("can't store values larger than 255 words");
            valSize = 255;
        }
        if (baseObject instanceof KnownAddress) {
            ObjectAddress address = ((KnownAddress) baseObject).address;
            if (V.indexed) {
                emit(Machine.LOADAop, 0, displayRegister(frame.level, address.level),
                        address.displacement + V.offset);
                emit(Machine.CALLop, Machine.SBr, Machine.PBr, Machine.addDisplacement);
                emit(Machine.STOREIop, valSize, 0, 0);
            } else {
                emit(Machine.STOREop, valSize, displayRegister(frame.level,
                        address.level), address.displacement + V.offset);
            }
        } else if (baseObject instanceof UnknownAddress) {
            ObjectAddress address = ((UnknownAddress) baseObject).address;
            emit(Machine.LOADop, Machine.addressSize, displayRegister(frame.level,
                    address.level), address.displacement);
            if (V.indexed)
                emit(Machine.CALLop, Machine.SBr, Machine.PBr, Machine.addDisplacement);
            if (V.offset != 0) {
                emit(Machine.LOADLop, 0, 0, V.offset);
                emit(Machine.CALLop, Machine.SBr, Machine.PBr, Machine.addDisplacement);
            }
            emit(Machine.STOREIop, valSize, 0, 0);
        }
    }

    private void encodeFetch(Vname V, Frame frame, int valSize) {
        RuntimeEntity baseObject = (RuntimeEntity) V.visit(this, frame);
        if (valSize > 255) {
            reporter.reportRestriction("can't load values larger than 255 words");
            valSize = 255;
        }
        if (baseObject instanceof KnownValue) {
            int value = ((KnownValue) baseObject).value;
            emit(Machine.LOADLop, 0, 0, value);
        } else if ((baseObject instanceof UnknownValue) ||
                (baseObject instanceof KnownAddress)) {
            ObjectAddress address = (baseObject instanceof UnknownValue) ?
                    ((UnknownValue) baseObject).address :
                    ((KnownAddress) baseObject).address;
            if (V.indexed) {
                emit(Machine.LOADAop, 0, displayRegister(frame.level, address.level),
                        address.displacement + V.offset);
                emit(Machine.CALLop, Machine.SBr, Machine.PBr, Machine.addDisplacement);
                emit(Machine.LOADIop, valSize, 0, 0);
            } else
                emit(Machine.LOADop, valSize, displayRegister(frame.level,
                        address.level), address.displacement + V.offset);
        } else if (baseObject instanceof UnknownAddress) {
            ObjectAddress address = ((UnknownAddress) baseObject).address;
            emit(Machine.LOADop, Machine.addressSize, displayRegister(frame.level,
                    address.level), address.displacement);
            if (V.indexed)
                emit(Machine.CALLop, Machine.SBr, Machine.PBr, Machine.addDisplacement);
            if (V.offset != 0) {
                emit(Machine.LOADLop, 0, 0, V.offset);
                emit(Machine.CALLop, Machine.SBr, Machine.PBr, Machine.addDisplacement);
            }
            emit(Machine.LOADIop, valSize, 0, 0);
        }
    }

    private void encodeFetchAddress(Vname V, Frame frame) {
        RuntimeEntity baseObject = (RuntimeEntity) V.visit(this, frame);
        if (baseObject instanceof KnownAddress) {
            ObjectAddress address = ((KnownAddress) baseObject).address;
            emit(Machine.LOADAop, 0, displayRegister(frame.level, address.level),
                    address.displacement + V.offset);
            if (V.indexed)
                emit(Machine.CALLop, Machine.SBr, Machine.PBr, Machine.addDisplacement);
        } else if (baseObject instanceof UnknownAddress) {
            ObjectAddress address = ((UnknownAddress) baseObject).address;
            emit(Machine.LOADop, Machine.addressSize, displayRegister(frame.level,
                    address.level), address.displacement);
            if (V.indexed)
                emit(Machine.CALLop, Machine.SBr, Machine.PBr, Machine.addDisplacement);
            if (V.offset != 0) {
                emit(Machine.LOADLop, 0, 0, V.offset);
                emit(Machine.CALLop, Machine.SBr, Machine.PBr, Machine.addDisplacement);
            }
        }
    }

    // ===== MÉTODOS PÚBLICOS PARA ACCEDER A LA INFORMACIÓN DE LINKEO =====

    /**
     * Obtiene la lista de imports del programa
     * @return Lista de strings en formato "import PackageName.symbolName"
     */
    public List<String> getImports() {
        return new ArrayList<>(imports);
    }

    /**
     * Obtiene la lista de relocalizaciones necesarias
     * @return Lista de strings en formato "reloc symbolName address"
     */
    public List<String> getRelocations() {
        return new ArrayList<>(relocations);
    }

    /**
     * Obtiene el mapa de exports del paquete
     * @return Mapa de nombre de símbolo -> dirección
     */
    public Map<String, Integer> getExports() {
        return new HashMap<>(exports);
    }

    /**
     * Establece el nombre del paquete actual
     * @param packageName Nombre del paquete
     */
    public void setPackageName(String packageName) {
        this.currentPackageName = packageName;
        this.compilingPackage = (packageName != null);
    }

    /**
     * Limpia los datos de imports, relocations y exports
     */
    public void clearLinkingData() {
        imports.clear();
        relocations.clear();
        exports.clear();
    }

    // ===== FIN MÉTODOS PÚBLICOS =====
}
