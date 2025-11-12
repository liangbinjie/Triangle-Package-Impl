package Triangle.LLVM;

import Triangle.AbstractSyntaxTrees.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * LLVM IR Code Generator for Triangle Language
 * Similar to Encoder.java but generates LLVM IR instead of TAM code
 */
public class LLVM2 implements Visitor {
    private StringBuilder irCode;
    private StringBuilder globalDeclarations; // Para declaraciones globales (funciones, procedimientos)
    private StringBuilder globalVariables; // Para variables globales
    private int tempCounter; // para generar diferentes nombres de variables cada vez que se necesite
    private int labelCounter; // para generar los diferentes nombres de los bloques de codigo cada vez que se necesite
    private Map<String, String> varMap; // Maps variable names to LLVM registers/pointers
    private Map<String, FuncInfo> funcMap; // Maps function names to their info (return type, params)
    private Map<String, String> typeMap; // Maps type names to LLVM types
    private boolean inMainScope = false; // Para saber si estamos en el scope del main
    
    // Clase auxiliar para almacenar información de funciones
    private static class FuncInfo {
        String returnType;
        java.util.List<String> paramTypes;
        
        FuncInfo(String returnType) {
            this.returnType = returnType;
            this.paramTypes = new java.util.ArrayList<>();
        }
    }
    
    public LLVM2(String sourceFileName) {
        // Por el constructor inicializamos las variables
        this.irCode = new StringBuilder();
        this.globalDeclarations = new StringBuilder();
        this.globalVariables = new StringBuilder();
        this.tempCounter = 0;
        this.labelCounter = 0;
        this.varMap = new HashMap<>();
        this.funcMap = new HashMap<>();
        this.typeMap = new HashMap<>();
        this.inMainScope = false;
        generarHeader(sourceFileName);
    }
    
    private void generarHeader(String sourceFileName) {
        /*
        Funcion para generar el header del IR
        
        Nota: target datalayout es posible que sea diferente para algunas maquinas
              target triple es posible que sea diferente para algunas maquinas
        
        En este proyecto se utiliza Ubuntu Linux en una arquitectura x86-64
        */
        
        String os = System.getProperty("os.name").toLowerCase();

        irCode.append("; ModuleID = ").append(sourceFileName).append("\n");
        irCode.append("source_filename = \"").append(sourceFileName).append("\"\n");
        irCode.append("target datalayout = \"e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128\"\n");
        
        if (os.contains("win")) {
            irCode.append("target triple = \"x86_64-pc-windows-msvc\"\n\n");
        }
        else if (os.contains("mac")) {
            // Choose architecture — most modern Macs are ARM64
            String arch = System.getProperty("os.arch").toLowerCase();
            if (arch.contains("aarch64") || arch.contains("arm")) {
                irCode.append("target triple = \"arm64-apple-macos\"\n\n");
            } else {
                irCode.append("target triple = \"x86_64-apple-macos\"\n\n");
            }
        }
        else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            irCode.append("target triple = \"x86_64-pc-linux-gnu\"\n\n");
        }
        
    }

    public void generarLLVM(Program theAST, String sourceFileName) {
        // Visit the AST to generate LLVM IR
        theAST.visit(this, null);

        // Save the generated LLVM IR to a file
        saveLLVMIR(sourceFileName.replace(".tri", ".ll"));
    }

    private String newTemp() {
        return "%t" + (tempCounter++);
    }
    
    private String newLabel() {
        return "L" + (labelCounter++);
    }
    
    private void emit(String instruction) {
        irCode.append("  ").append(instruction).append("\n");
    }
    
    private void emitLabel(String label) {
        irCode.append(label).append(":\n");
    }
    
    // Convert Triangle type to LLVM type
    private String getLLVMType(Triangle.AbstractSyntaxTrees.TypeDenoter type) {
        if (type instanceof IntTypeDenoter) {
            return "i32";
        } else if (type instanceof BoolTypeDenoter) {
            return "i1";
        } else if (type instanceof CharTypeDenoter) {
            return "i8";
        } else if (type instanceof SimpleTypeDenoter) {
            // SimpleTypeDenoter es una referencia a otro tipo
            String typeName = ((SimpleTypeDenoter) type).I.spelling;
            
            // Buscar en el mapa de tipos definidos por el usuario
            if (typeMap.containsKey(typeName)) {
                return typeMap.get(typeName);
            }
            
            // Tipos predefinidos en Triangle
            switch (typeName) {
                case "Integer":
                    return "i32";
                case "Boolean":
                    return "i1";
                case "Char":
                    return "i8";
                default:
                    return "i32"; // default
            }
        }
        return "i32"; // default
    }
    
    public void saveLLVMIR(String outputFileName) {
        try (FileWriter writer = new FileWriter(outputFileName)) {
            // Orden de generación:
            // 1. Header (target info)
            // 2. Variables globales (@var = global ...)
            // 3. Declaraciones de funciones/procedimientos
            // 4. Función main
            // 5. Atributos y metadata
            
            String fullCode = irCode.toString();
            int defineIndex = fullCode.indexOf("; Function Attrs:");
            
            if (defineIndex > 0) {
                String header = fullCode.substring(0, defineIndex);
                String mainAndRest = fullCode.substring(defineIndex);
                
                writer.write(header);
                writer.write(globalVariables.toString());
                writer.write("\n");
                writer.write(globalDeclarations.toString());
                writer.write("\n");
                writer.write(mainAndRest);
            } else {
                // Fallback si no encuentra el marcador
                writer.write(irCode.toString());
            }
            
            System.out.println("LLVM IR generated successfully: " + outputFileName);
        } catch (IOException e) {
            System.err.println("Error writing LLVM IR file: " + e.getMessage());
        }
    }
    
    public String getLLVMIR() {
        return irCode.toString();
    }
    
    @Override
    public Object visitAssignCommand(AssignCommand ast, Object o) {
        // Get the value to assign
        String value = (String) ast.E.visit(this, null);
        
        // Get the variable name
        String varName = getVnameIdentifier(ast.V);
        
        // Store the value in the variable (puede ser @ o %)
        if (varMap.containsKey(varName)) {
            String varPtr = varMap.get(varName);
            emit("store i32 " + value + ", ptr " + varPtr + ", align 4");
        }
        
        return null;
    }
    
    private String getVnameIdentifier(Vname vname) {
        if (vname instanceof SimpleVname) {
            return ((SimpleVname) vname).I.spelling;
        }
        return null;
    }
    
    
    @Override
    public Object visitCallCommand(CallCommand ast, Object o) {
        // Llamada a procedimiento
        String procName = ast.I.spelling;
        
        // Verificar si es un procedimiento del usuario
        if (funcMap.containsKey(procName)) {
            FuncInfo procInfo = funcMap.get(procName);
            
            // Generar la llamada
            StringBuilder callInstr = new StringBuilder();
            callInstr.append("call ").append(procInfo.returnType);
            callInstr.append(" @").append(procName).append("(");
            
            // Procesar argumentos actuales
            if (ast.APS != null) {
                StringBuilder args = new StringBuilder();
                ast.APS.visit(this, args);
                callInstr.append(args.toString());
            }
            
            callInstr.append(")");
            emit(callInstr.toString());
            
            return null;
        }
        
        // Manejar procedimientos built-in
        if (procName.equals("putint") || procName.equals("put")) {
            // Estos requerirían declaración de printf o similar
            // Por ahora solo visitamos los argumentos
            if (ast.APS != null) {
                ast.APS.visit(this, null);
            }
        } else if (procName.equals("getint") || procName.equals("get")) {
            // Estos requerirían declaración de scanf o similar
            if (ast.APS != null) {
                ast.APS.visit(this, null);
            }
        }
        
        return null;
    }

    @Override
    public Object visitEmptyCommand(EmptyCommand ast, Object o) {
        // Empty command - no code generation needed
        return null;
    }

    @Override
    public Object visitIfCommand(IfCommand ast, Object o) {
        String condition = (String) ast.E.visit(this, null);
        
        String thenLabel = newLabel();
        String elseLabel = newLabel();
        String endLabel = newLabel();
        
        // Branch based on condition
        emit("br i1 " + condition + ", label %" + thenLabel + ", label %" + elseLabel);
        
        // Then branch
        emitLabel(thenLabel);
        ast.C1.visit(this, null);
        emit("br label %" + endLabel);
        
        // Else branch
        emitLabel(elseLabel);
        ast.C2.visit(this, null);
        emit("br label %" + endLabel);
        
        // End label
        emitLabel(endLabel);
        
        return null;
    }

    @Override
    public Object visitLetCommand(LetCommand ast, Object o) {
        // Visit declarations first
        ast.D.visit(this, null);
        // Then visit the command
        ast.C.visit(this, null);
        return null;
    }

    @Override
    public Object visitSequentialCommand(SequentialCommand ast, Object o) {
        ast.C1.visit(this, null);
        ast.C2.visit(this, null);
        return null;
    }

    @Override
    public Object visitWhileCommand(WhileCommand ast, Object o) {
        String loopLabel = newLabel();
        String bodyLabel = newLabel();
        String endLabel = newLabel();
        
        // Jump to loop condition
        emit("br label %" + loopLabel);
        
        // Loop condition
        emitLabel(loopLabel);
        String condition = (String) ast.E.visit(this, null);
        emit("br i1 " + condition + ", label %" + bodyLabel + ", label %" + endLabel);
        
        // Loop body
        emitLabel(bodyLabel);
        ast.C.visit(this, null);
        emit("br label %" + loopLabel);
        
        // End label
        emitLabel(endLabel);
        
        return null;
    }

    @Override
    public Object visitArrayExpression(ArrayExpression ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitBinaryExpression(BinaryExpression ast, Object o) {
        // Get operands
        String e1 = (String) ast.E1.visit(this, null);
        String e2 = (String) ast.E2.visit(this, null);
        
        // Get the operator
        String operator = ast.O.spelling;
        String result = newTemp();
        
        // Generate the LLVM instruction based on the operator
        switch (operator) {
            case "+":
                emit(result + " = add i32 " + e1 + ", " + e2);
                break;
            case "-":
                emit(result + " = sub i32 " + e1 + ", " + e2);
                break;
            case "*":
                emit(result + " = mul i32 " + e1 + ", " + e2);
                break;
            case "/":
                emit(result + " = sdiv i32 " + e1 + ", " + e2);
                break;
            case "<":
                emit(result + " = icmp slt i32 " + e1 + ", " + e2);
                break;
            case "<=":
                emit(result + " = icmp sle i32 " + e1 + ", " + e2);
                break;
            case ">":
                emit(result + " = icmp sgt i32 " + e1 + ", " + e2);
                break;
            case ">=":
                emit(result + " = icmp sge i32 " + e1 + ", " + e2);
                break;
            case "=":
                emit(result + " = icmp eq i32 " + e1 + ", " + e2);
                break;
            case "\\=":
                emit(result + " = icmp ne i32 " + e1 + ", " + e2);
                break;
            default:
                throw new UnsupportedOperationException("Operator not supported: " + operator);
        }
        
        return result;
    }

    @Override
    public Object visitCallExpression(CallExpression ast, Object o) {
        // Llamada a función
        String funcName = ast.I.spelling;
        
        // Verificar si la función está declarada
        if (funcMap.containsKey(funcName)) {
            FuncInfo funcInfo = funcMap.get(funcName);
            String result = newTemp();
            
            // Generar la llamada
            StringBuilder callInstr = new StringBuilder();
            callInstr.append(result).append(" = call ").append(funcInfo.returnType);
            callInstr.append(" @").append(funcName).append("(");
            
            // Procesar argumentos actuales
            if (ast.APS != null) {
                StringBuilder args = new StringBuilder();
                ast.APS.visit(this, args);
                callInstr.append(args.toString());
            }
            
            callInstr.append(")");
            emit(callInstr.toString());
            
            return result;
        }
        
        throw new RuntimeException("Function not found: " + funcName);
    }

    @Override
    public Object visitCharacterExpression(CharacterExpression ast, Object o) {
        // Return the ASCII value of the character
        char c = ast.CL.spelling.charAt(0);
        return String.valueOf((int) c);
    }

    @Override
    public Object visitEmptyExpression(EmptyExpression ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitIfExpression(IfExpression ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitIntegerExpression(IntegerExpression ast, Object o) {
        // Return the literal integer value as a string
        return ast.IL.spelling;
    }

    @Override
    public Object visitLetExpression(LetExpression ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitRecordExpression(RecordExpression ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitUnaryExpression(UnaryExpression ast, Object o) {
        String operand = (String) ast.E.visit(this, null);
        String operator = ast.O.spelling;
        String result = newTemp();
        
        switch (operator) {
            case "-":
                // Negate: 0 - operand
                emit(result + " = sub i32 0, " + operand);
                break;
            case "\\":
                // Logical NOT
                emit(result + " = xor i1 " + operand + ", 1");
                break;
            default:
                throw new UnsupportedOperationException("Unary operator not supported: " + operator);
        }
        
        return result;
    }

    @Override
    public Object visitVnameExpression(VnameExpression ast, Object o) {
        return ast.V.visit(this, null);
    }

    @Override
    public Object visitBinaryOperatorDeclaration(BinaryOperatorDeclaration ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitConstDeclaration(ConstDeclaration ast, Object o) {
        // Para constantes en Triangle, evaluamos la expresión
        // y guardamos el valor directamente (no necesitamos alloca)
        String value = (String) ast.E.visit(this, null);
        String constName = ast.I.spelling;
        
        // Guardamos el valor directamente - cuando se use la constante,
        // simplemente retornamos este valor
        varMap.put(constName, value);
        
        // Nota: En LLVM, las constantes se pueden manejar de varias formas:
        // 1. Inline substitution (lo que hacemos aquí - más eficiente)
        // 2. Como variables globales constantes (para valores complejos)
        // 3. Como metadatos (para información del compilador)
        
        return null;
    }

    @Override
    public Object visitFuncDeclaration(FuncDeclaration ast, Object o) {
        // Declaración de función en Triangle
        String funcName = ast.I.spelling;
        String returnType = getLLVMType(ast.T);
        
        // Guardar el contexto actual
        Map<String, String> savedVarMap = new HashMap<>(varMap);
        int savedTempCounter = tempCounter;
        int savedLabelCounter = labelCounter;
        
        // Crear nueva función con dso_local
        StringBuilder funcCode = new StringBuilder();
        funcCode.append("\n; Function Attrs: noinline nounwind optnone uwtable\n");
        funcCode.append("define dso_local ").append(returnType).append(" @").append(funcName).append("(");
        
        // Procesar parámetros formales
        FuncInfo funcInfo = new FuncInfo(returnType);
        varMap.clear(); // Nuevo scope para la función
        
        // Visitar parámetros (si los hay)
        if (ast.FPS != null) {
            // Aquí procesaríamos los parámetros formales
            ast.FPS.visit(this, funcCode);
        }
        
        funcCode.append(") {\n");
        funcCode.append("entry:\n");
        
        // Guardar irCode actual y crear uno temporal para la función
        StringBuilder savedIrCode = irCode;
        irCode = new StringBuilder();
        
        // Variable para almacenar el resultado de retorno
        String resultVar = "%result";
        emit(resultVar + " = alloca " + returnType + ", align 4");
        varMap.put(funcName + ".result", resultVar);
        
        // Visitar el cuerpo de la función (expression)
        String resultValue = (String) ast.E.visit(this, null);
        
        // Almacenar el resultado
        if (resultValue != null) {
            emit("store " + returnType + " " + resultValue + ", ptr " + resultVar + ", align 4");
        }
        
        // Cargar y retornar el resultado
        String loadResult = newTemp();
        emit(loadResult + " = load " + returnType + ", ptr " + resultVar + ", align 4");
        emit("ret " + returnType + " " + loadResult);
        
        funcCode.append(irCode.toString());
        funcCode.append("} #0\n");  // Agregar atributo #0
        
        // Agregar la función a las declaraciones globales
        globalDeclarations.append(funcCode.toString());
        
        // Guardar información de la función
        funcMap.put(funcName, funcInfo);
        
        // Restaurar contexto
        irCode = savedIrCode;
        varMap = savedVarMap;
        tempCounter = savedTempCounter;
        labelCounter = savedLabelCounter;
        
        return null;
    }

    @Override
    public Object visitProcDeclaration(ProcDeclaration ast, Object o) {
        // Declaración de procedimiento en Triangle (como función void)
        String procName = ast.I.spelling;
        
        // Guardar el contexto actual
        Map<String, String> savedVarMap = new HashMap<>(varMap);
        int savedTempCounter = tempCounter;
        int savedLabelCounter = labelCounter;
        
        // Crear nuevo procedimiento con dso_local
        StringBuilder procCode = new StringBuilder();
        procCode.append("\n; Function Attrs: noinline nounwind optnone uwtable\n");
        procCode.append("define dso_local void @").append(procName).append("(");
        
        // Procesar parámetros formales
        FuncInfo procInfo = new FuncInfo("void");
        varMap.clear(); // Nuevo scope para el procedimiento
        
        // Visitar parámetros (si los hay)
        if (ast.FPS != null) {
            ast.FPS.visit(this, procCode);
        }
        
        procCode.append(") {\n");
        procCode.append("entry:\n");
        
        // Guardar irCode actual y crear uno temporal para el procedimiento
        StringBuilder savedIrCode = irCode;
        irCode = new StringBuilder();
        
        // Visitar el cuerpo del procedimiento (command)
        ast.C.visit(this, null);
        
        // Retornar void
        emit("ret void");
        
        procCode.append(irCode.toString());
        procCode.append("} #0\n");  // Agregar atributo #0
        
        // Agregar el procedimiento a las declaraciones globales
        globalDeclarations.append(procCode.toString());
        
        // Guardar información del procedimiento
        funcMap.put(procName, procInfo);
        
        // Restaurar contexto
        irCode = savedIrCode;
        varMap = savedVarMap;
        tempCounter = savedTempCounter;
        labelCounter = savedLabelCounter;
        
        return null;
    }

    @Override
    public Object visitSequentialDeclaration(SequentialDeclaration ast, Object o) {
        ast.D1.visit(this, null);
        ast.D2.visit(this, null);
        return null;
    }

    @Override
    public Object visitTypeDeclaration(TypeDeclaration ast, Object o) {
        // Declaración de tipo en Triangle (type alias)
        String typeName = ast.I.spelling;
        String llvmType = getLLVMType(ast.T);
        
        // Guardar el alias de tipo
        typeMap.put(typeName, llvmType);
        
        // En LLVM, los tipos primitivos no requieren declaración explícita
        // Para tipos estructurados (records, arrays) se haría diferente
        
        // Ejemplo de lo que se generaría para un record:
        // %TypeName = type { i32, i8, i32 }
        
        return null;
    }

    @Override
    public Object visitUnaryOperatorDeclaration(UnaryOperatorDeclaration ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitVarDeclaration(VarDeclaration ast, Object o) {
        String varName = ast.I.spelling;
        String llvmType = getLLVMType(ast.T);
        
        // En Triangle, las variables declaradas en let son GLOBALES
        // Usar @ para variables globales
        String varPtr = "@" + varName;
        
        // Declarar como variable global
        globalVariables.append(varPtr)
                      .append(" = dso_local global ")
                      .append(llvmType)
                      .append(" 0, align 4\n");
        
        // Guardar en varMap
        varMap.put(varName, varPtr);
        
        return null;
    }

    @Override
    public Object visitMultipleArrayAggregate(MultipleArrayAggregate ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitSingleArrayAggregate(SingleArrayAggregate ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitMultipleRecordAggregate(MultipleRecordAggregate ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitSingleRecordAggregate(SingleRecordAggregate ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitConstFormalParameter(ConstFormalParameter ast, Object o) {
        // Parámetro formal constante: const name: Type
        StringBuilder params = (StringBuilder) o;
        String paramName = ast.I.spelling;
        String paramType = getLLVMType(ast.T);
        
        // En LLVM, los parámetros se pasan por valor
        params.append(paramType).append(" %").append(paramName);
        
        // Guardar en varMap como referencia al parámetro
        varMap.put(paramName, "%" + paramName);
        
        return null;
    }

    @Override
    public Object visitFuncFormalParameter(FuncFormalParameter ast, Object o) {
        // Parámetro formal función: func name(params): Type
        // Por ahora no soportado completamente
        throw new UnsupportedOperationException("Function parameters not yet supported");
    }

    @Override
    public Object visitProcFormalParameter(ProcFormalParameter ast, Object o) {
        // Parámetro formal procedimiento: proc name(params)
        // Por ahora no soportado completamente
        throw new UnsupportedOperationException("Procedure parameters not yet supported");
    }

    @Override
    public Object visitVarFormalParameter(VarFormalParameter ast, Object o) {
        // Parámetro formal variable: var name: Type
        // En LLVM se pasa como puntero
        StringBuilder params = (StringBuilder) o;
        String paramName = ast.I.spelling;
        String paramType = getLLVMType(ast.T);
        
        // Pasar como puntero (ptr)
        params.append("ptr %").append(paramName);
        
        // Guardar en varMap como puntero
        varMap.put(paramName, "%" + paramName);
        
        return null;
    }

    @Override
    public Object visitEmptyFormalParameterSequence(EmptyFormalParameterSequence ast, Object o) {
        // Secuencia vacía de parámetros - no hacer nada
        return null;
    }

    @Override
    public Object visitMultipleFormalParameterSequence(MultipleFormalParameterSequence ast, Object o) {
        // Múltiples parámetros: param1, param2, ...
        StringBuilder params = (StringBuilder) o;
        
        // Visitar el primer parámetro
        ast.FP.visit(this, params);
        
        // Agregar coma y visitar el resto
        params.append(", ");
        ast.FPS.visit(this, params);
        
        return null;
    }

    @Override
    public Object visitSingleFormalParameterSequence(SingleFormalParameterSequence ast, Object o) {
        // Un solo parámetro
        StringBuilder params = (StringBuilder) o;
        ast.FP.visit(this, params);
        return null;
    }

    @Override
    public Object visitConstActualParameter(ConstActualParameter ast, Object o) {
        // Parámetro actual (expresión)
        StringBuilder args = (StringBuilder) o;
        String value = (String) ast.E.visit(this, null);
        args.append("i32 ").append(value);
        return null;
    }

    @Override
    public Object visitFuncActualParameter(FuncActualParameter ast, Object o) {
        // Parámetro actual función
        throw new UnsupportedOperationException("Function parameters not yet supported");
    }

    @Override
    public Object visitProcActualParameter(ProcActualParameter ast, Object o) {
        // Parámetro actual procedimiento
        throw new UnsupportedOperationException("Procedure parameters not yet supported");
    }

    @Override
    public Object visitVarActualParameter(VarActualParameter ast, Object o) {
        // Parámetro actual variable (pasa la dirección)
        StringBuilder args = (StringBuilder) o;
        String varName = getVnameIdentifier(ast.V);
        
        if (varMap.containsKey(varName)) {
            String varPtr = varMap.get(varName);
            args.append("ptr ").append(varPtr);
        }
        return null;
    }

    @Override
    public Object visitEmptyActualParameterSequence(EmptyActualParameterSequence ast, Object o) {
        // Secuencia vacía de argumentos - no hacer nada
        return null;
    }

    @Override
    public Object visitMultipleActualParameterSequence(MultipleActualParameterSequence ast, Object o) {
        // Múltiples argumentos: arg1, arg2, ...
        StringBuilder args = (StringBuilder) o;
        
        // Visitar el primer argumento
        ast.AP.visit(this, args);
        
        // Agregar coma y visitar el resto
        args.append(", ");
        ast.APS.visit(this, args);
        
        return null;
    }

    @Override
    public Object visitSingleActualParameterSequence(SingleActualParameterSequence ast, Object o) {
        // Un solo argumento
        StringBuilder args = (StringBuilder) o;
        ast.AP.visit(this, args);
        return null;
    }

    @Override
    public Object visitAnyTypeDenoter(AnyTypeDenoter ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitArrayTypeDenoter(ArrayTypeDenoter ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitBoolTypeDenoter(BoolTypeDenoter ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitCharTypeDenoter(CharTypeDenoter ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitErrorTypeDenoter(ErrorTypeDenoter ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitSimpleTypeDenoter(SimpleTypeDenoter ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitIntTypeDenoter(IntTypeDenoter ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitRecordTypeDenoter(RecordTypeDenoter ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitMultipleFieldTypeDenoter(MultipleFieldTypeDenoter ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitSingleFieldTypeDenoter(SingleFieldTypeDenoter ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitCharacterLiteral(CharacterLiteral ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitIdentifier(Identifier ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitIntegerLiteral(IntegerLiteral ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitOperator(Operator ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitDotVname(DotVname ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitSimpleVname(SimpleVname ast, Object o) {
        String varName = ast.I.spelling;
        
        // Check if it's a constant (stored directly as value)
        if (varMap.containsKey(varName)) {
            String varPtr = varMap.get(varName);
            
            // Si empieza con % o @, es un puntero y necesitamos hacer load
            if (varPtr.startsWith("%") || varPtr.startsWith("@")) {
                String result = newTemp();
                emit(result + " = load i32, ptr " + varPtr + ", align 4");
                return result;
            } else {
                // Es un valor constante (inline substitution)
                return varPtr;
            }
        }
        
        throw new RuntimeException("Variable not found: " + varName);
    }

    @Override
    public Object visitSubscriptVname(SubscriptVname ast, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Object visitProgram(Program ast, Object o) {
        inMainScope = true;
        
        // Funcion Main
        irCode.append("; Function Attrs: noinline nounwind optnone uwtable\n");
        irCode.append("define dso_local i32 @main() #0 {\n");
        
        // Variable temporal para el return value
        irCode.append("  %1 = alloca i32, align 4\n");
        irCode.append("  store i32 0, ptr %1, align 4\n");
        
        // Visit the program command
        ast.C.visit(this, null);
        
        // Return 0 from main
        emit("ret i32 0");
        irCode.append("}\n");
        
        // Agregar atributos de función al final
        irCode.append("\nattributes #0 = { noinline nounwind optnone uwtable \"frame-pointer\"=\"all\" \"min-legal-vector-width\"=\"0\" \"no-trapping-math\"=\"true\" \"stack-protector-buffer-size\"=\"8\" \"target-cpu\"=\"x86-64\" \"target-features\"=\"+cmov,+cx8,+fxsr,+mmx,+sse,+sse2,+x87\" \"tune-cpu\"=\"generic\" }\n");
        
        // Agregar metadata
        irCode.append("\n!llvm.module.flags = !{!0, !1, !2, !3, !4}\n");
        irCode.append("!llvm.ident = !{!5}\n\n");
        irCode.append("!0 = !{i32 1, !\"wchar_size\", i32 4}\n");
        irCode.append("!1 = !{i32 8, !\"PIC Level\", i32 2}\n");
        irCode.append("!2 = !{i32 7, !\"PIE Level\", i32 2}\n");
        irCode.append("!3 = !{i32 7, !\"uwtable\", i32 2}\n");
        irCode.append("!4 = !{i32 7, !\"frame-pointer\", i32 2}\n");
        irCode.append("!5 = !{!\"Triangle LLVM Compiler\"}\n");
        
        inMainScope = false;
        return null;
    }
    
}
