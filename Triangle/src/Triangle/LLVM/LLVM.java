package Triangle.LLVM;

import Triangle.AbstractSyntaxTrees.*;

/**
 * LLVM IR Code Generator for Triangle Language
 * 
 */
public class LLVM implements Visitor {
    private StringBuilder header = new StringBuilder();
    private StringBuilder globals = new StringBuilder();
    private StringBuilder functions = new StringBuilder();
    private StringBuilder main = new StringBuilder();
    private StringBuilder codeIR = new StringBuilder();
    private StringBuilder currentFunction = null; // Para construir función actual
    
    public String sourceFileName;
    
    private java.util.Map<String, String> varMap; // Maps variable names to LLVM registers/pointers
    private java.util.Map<String, FuncInfo> funcMap; // Maps function names to their info
    
    // Soporte para records/structs
    private java.util.Map<String, java.util.Map<String, Integer>> structFieldIndices; // Maps struct name -> field name -> index
    private java.util.Map<String, String> varStructTypes; // Maps variable name -> struct type name
    private java.util.Map<TypeDenoter, String> typeDenoterToStructName; // Maps TypeDenoter -> struct name
    private int structCounter = 0;
    
    private int tempCounter = 0;
    private int labelCounter = 0;
    
    // Clase auxiliar para almacenar información de funciones
    private static class FuncInfo {
        String returnType;
        java.util.List<String> paramTypes;
        
        FuncInfo(String returnType) {
            this.returnType = returnType;
            this.paramTypes = new java.util.ArrayList<>();
        }
    }
    
    public LLVM(String sourceFileName) {
        this.sourceFileName = sourceFileName;
        this.varMap = new java.util.HashMap<>();
        this.funcMap = new java.util.HashMap<>();
        this.structFieldIndices = new java.util.HashMap<>();
        this.varStructTypes = new java.util.HashMap<>();
        this.typeDenoterToStructName = new java.util.HashMap<>();
    }
    
    private String newTemp() {
        return "%t" + (tempCounter++);
    }
    
    private String newLabel() {
        return "L" + (labelCounter++);
    }
    
    private String newStructName() {
        return "struct.anon." + (structCounter++);
    }
    
    // Método auxiliar para construir el mapa de índices de campos de un record
    private int buildFieldIndexMap(Object fieldDenoter, java.util.Map<String, Integer> map, int idx) {
        if (fieldDenoter instanceof MultipleFieldTypeDenoter) {
            MultipleFieldTypeDenoter mftd = (MultipleFieldTypeDenoter) fieldDenoter;
            map.put(mftd.I.spelling, idx++);
            idx = buildFieldIndexMap(mftd.FT, map, idx);
        } else if (fieldDenoter instanceof SingleFieldTypeDenoter) {
            SingleFieldTypeDenoter sftd = (SingleFieldTypeDenoter) fieldDenoter;
            map.put(sftd.I.spelling, idx++);
        }
        return idx;
    }
    
    private void emit(String instruction) {
        if (currentFunction != null) {
            currentFunction.append("  ").append(instruction).append("\n");
        } else {
            main.append("  ").append(instruction).append("\n");
        }
    }
    
    private void emitToFunction(String code) {
        if (currentFunction != null) {
            currentFunction.append(code);
        }
    }
    
    // Convert Triangle type to LLVM type
    private String getLLVMType(Triangle.AbstractSyntaxTrees.TypeDenoter type) {
        if (type instanceof IntTypeDenoter) {
            return "i32";
        } else if (type instanceof BoolTypeDenoter) {
            return "i1";
        } else if (type instanceof CharTypeDenoter) {
            return "i8";
        } else if (type instanceof RecordTypeDenoter) {
            // Para records, verificar si ya tenemos un struct type asignado
            if (typeDenoterToStructName.containsKey(type)) {
                return "%struct." + typeDenoterToStructName.get(type);
            }
            // Si no, retornar el resultado del visitor
            return (String) type.visit(this, null);
        } else if (type instanceof SimpleTypeDenoter) {
            // SimpleTypeDenoter es una referencia a otro tipo
            String typeName = ((SimpleTypeDenoter) type).I.spelling;
            
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
    
    private void generarHeader() {
        /*
        Funcion para generar el header del IR
        
        Nota: target datalayout es posible que sea diferente para algunas maquinas
              target triple es posible que sea diferente para algunas maquinas
        
        En este proyecto se utiliza Ubuntu Linux en una arquitectura x86-64
        */
        
        String os = System.getProperty("os.name").toLowerCase();

        header.append("; ModuleID = ").append(sourceFileName).append("\n");
        header.append("source_filename = \"").append(sourceFileName).append("\"\n");
        header.append("target datalayout = \"e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128\"\n");
        
        if (os.contains("win")) {
            header.append("target triple = \"x86_64-pc-windows-msvc\"\n\n");
        }
        else if (os.contains("mac")) {
            // Choose architecture — most modern Macs are ARM64
            String arch = System.getProperty("os.arch").toLowerCase();
            if (arch.contains("aarch64") || arch.contains("arm")) {
                header.append("target triple = \"arm64-apple-macos\"\n\n");
            } else {
                header.append("target triple = \"x86_64-apple-macos\"\n\n");
            }
        }
        else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            header.append("target triple = \"x86_64-pc-linux-gnu\"\n\n");
        }
        
        // Declarar printf para putint y putchar para put
        header.append("; External function declarations\n");
        header.append("declare i32 @printf(ptr, ...)\n");
        header.append("declare i32 @putchar(i32)\n");
        header.append("declare i32 @scanf(ptr, ...)\n");
        header.append("declare i32 @getchar()\n\n");
        
        // Definir el formato string para putint y getint
        globals.append("@.str.putint = private unnamed_addr constant [4 x i8] c\"%d\\0A\\00\", align 1\n");
        globals.append("@.str.getint = private unnamed_addr constant [3 x i8] c\"%d\\00\", align 1\n");
    }
    
    public void generarLLVM(Program theAST) {
        // Primero debe generar el header
        generarHeader();
        
        // luego entra al ast
        theAST.visit(this, null);
        
        // Construir el IR final en el orden correcto
        
        codeIR.append(header.toString());        // Header
        codeIR.append(globals.toString());       // Variables globales
        codeIR.append("\n");
        codeIR.append(functions.toString());     // Funciones definidas por usuario
        codeIR.append("\n");
        codeIR.append(main.toString());          // Función main
        
        System.out.println(codeIR);
    }
    
    public void guardarLLVM() {
        String outputFileName = sourceFileName.replace(".tri", ".ll");
        
        try (java.io.FileWriter writer = new java.io.FileWriter(outputFileName)) {
            writer.write(codeIR.toString());
            System.out.println("LLVM IR saved to: " + outputFileName);
        } catch (java.io.IOException e) {
            System.err.println("Error writing LLVM IR file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void ejecutarLLVM() {
        // Ejecutar el archivo LLVM optimizado usando lli (LLVM interpreter)
        String llFileName = sourceFileName.replace(".tri", ".ll");
        
        try {
            System.out.println("\n=== Ejecutando LLVM IR: " + llFileName + " ===\n");
            
            // Ejecutar: lli archivo_opt.ll
            ProcessBuilder pb = new ProcessBuilder("lli", llFileName);
            pb.inheritIO(); // Redirigir entrada/salida al terminal actual
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            System.out.println("\n=== Ejecución finalizada con código: " + exitCode + " ===");
            
        } catch (java.io.IOException e) {
            System.err.println("Error: 'lli' no encontrado. Asegúrate de tener LLVM instalado.");
        } catch (InterruptedException e) {
            System.err.println("Ejecución interrumpida: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
    
    public void optimizarLLVM() {
        String filename = sourceFileName.replace(".tri", ".ll");
        
        try {
            // Ejecutar: opt -O3 -S input.ll -o output_opt.ll
            Process process = new ProcessBuilder("opt", "-O3", "-S", filename, "-o", filename).start();
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("LLVM IR optimized successfully: " + filename);
            } else {
                System.err.println("LLVM optimization failed with exit code: " + exitCode);
            }
            
        } catch (java.io.IOException e) {
            System.err.println("Error: 'opt' not found.");
        } catch (InterruptedException e) {
            System.err.println("Optimization interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Object visitAssignCommand(AssignCommand ast, Object o) {
        // Evaluar la expresión del lado derecho
        Object rhsResult = ast.E.visit(this, null);
        
        // Obtener el puntero del lado izquierdo
        String lhs = (String) ast.V.visit(this, null);
        
        // Determinar el tipo de la variable
        String varType;
        if (ast.V.type instanceof RecordTypeDenoter) {
            String typeName = typeDenoterToStructName.get(ast.V.type);
            if (typeName != null) {
                varType = "%struct." + typeName;
            } else {
                varType = (String) ast.V.type.visit(this, o);
            }
        } else {
            varType = (String) ast.V.type.visit(this, o);
        }
        
        // Si el resultado es un ArrayList (asignación de record o array literal)
        if (rhsResult instanceof java.util.ArrayList) {
            java.util.ArrayList<String> elements = (java.util.ArrayList<String>) rhsResult;
            for (int i = 0; i < elements.size(); i++) {
                String element = elements.get(i);
                // Extraer tipo y valor del elemento "tipo valor"
                String[] parts = element.split(" ", 2);
                String elemType = parts[0];
                String elemValue = parts[1];
                
                // Generar puntero al elemento (campo para records, índice para arrays)
                String fieldPtr;
                if (i == 0) {
                    fieldPtr = lhs;
                } else {
                    fieldPtr = newTemp();
                    emit(fieldPtr + " = getelementptr inbounds " + varType + ", ptr " + lhs + ", i32 0, i32 " + i);
                }
                
                // Almacenar el valor en el elemento
                emit("store " + elemType + " " + elemValue + ", ptr " + fieldPtr + ", align 4");
            }
        } else {
            // Asignación simple
            String rhs = (String) rhsResult;
            if (varType == null) varType = "i32";
            emit("store " + varType + " " + rhs + ", ptr " + lhs + ", align 4");
        }
        
        return null;
    }
    
    // Método auxiliar para obtener el identificador de un Vname
    private String getVnameIdentifier(Vname vname) {
        if (vname instanceof SimpleVname) {
            return ((SimpleVname) vname).I.spelling;
        }
        // Para otros tipos de Vname (DotVname, SubscriptVname) se necesitaría más lógica
        throw new UnsupportedOperationException("Only SimpleVname is supported in assignments");
    }

    @Override
    public Object visitCallCommand(CallCommand ast, Object o) {
        String procName = ast.I.spelling;
        
        // Manejar procedimientos built-in
        if (procName.equals("putint")) {
            // putint(x) imprime un entero
            if (ast.APS != null) {
                // Evaluar el argumento
                StringBuilder args = new StringBuilder();
                ast.APS.visit(this, args);
                
                // Llamar a printf con el formato y el valor
                String temp = newTemp();
                emit(temp + " = call i32 (ptr, ...) @printf(ptr @.str.putint, " + args.toString() + ")");
            }
            return null;
        }
        
        if (procName.equals("put")) {
            // put(c) imprime un carácter
            if (ast.APS != null) {
                // Evaluar el argumento
                String charValue = (String) ast.APS.visit(this, null);
                
                // put usa putchar de C, que espera un i32
                // charValue ya es el valor ASCII como i32 (de visitCharacterExpression)
                String temp = newTemp();
                emit(temp + " = call i32 @putchar(i32 " + charValue + ")");
            }
            return null;
        }
        
        if (procName.equals("getint")) {
            // getint(var x) lee un entero desde stdin
            if (ast.APS != null) {
                // El parámetro debe ser una variable (var parameter)
                // Obtener el puntero de la variable del parámetro actual
                StringBuilder args = new StringBuilder();
                ast.APS.visit(this, args);
                
                // Llamar a scanf con el formato y el puntero a la variable
                String temp = newTemp();
                emit(temp + " = call i32 (ptr, ...) @scanf(ptr @.str.getint, " + args.toString() + ")");
            }
            return null;
        }
        
        if (procName.equals("get")) {
            // get(var c) lee un carácter desde stdin
            if (ast.APS != null) {
                // Llamar a getchar() que retorna el carácter como i32
                String charValue = newTemp();
                emit(charValue + " = call i32 @getchar()");
                
                // Obtener el puntero de la variable del parámetro actual
                StringBuilder args = new StringBuilder();
                ast.APS.visit(this, args);
                
                // Extraer solo el puntero (quitar "ptr " del inicio)
                String varPtr = args.toString().replace("ptr ", "");
                
                // Almacenar el carácter en la variable (como i32)
                emit("store i32 " + charValue + ", ptr " + varPtr + ", align 4");
            }
            return null;
        }
        
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
        
        throw new RuntimeException("Unknown procedure: " + procName);
    }

    @Override
    public Object visitEmptyCommand(EmptyCommand ast, Object o) {
        return null;
    }

    @Override
    public Object visitIfCommand(IfCommand ast, Object o) {
        // if E then C1 else C2
        // Generar labels para las ramas
        String thenLabel = newLabel();
        String elseLabel = newLabel();
        String endLabel = newLabel();
        
        // Evaluar la condición
        String condition = (String) ast.E.visit(this, null);
        
        // Rama condicional
        emit("br i1 " + condition + ", label %" + thenLabel + ", label %" + elseLabel);
        
        // Bloque then (label sin indentación)
        if (currentFunction != null) {
            currentFunction.append(thenLabel).append(":\n");
        } else {
            main.append(thenLabel).append(":\n");
        }
        ast.C1.visit(this, null);
        emit("br label %" + endLabel);
        
        // Bloque else (label sin indentación)
        if (currentFunction != null) {
            currentFunction.append(elseLabel).append(":\n");
        } else {
            main.append(elseLabel).append(":\n");
        }
        ast.C2.visit(this, null);
        emit("br label %" + endLabel);
        
        // Bloque de salida (label sin indentación)
        if (currentFunction != null) {
            currentFunction.append(endLabel).append(":\n");
        } else {
            main.append(endLabel).append(":\n");
        }
        
        return null;
    }

    @Override
    public Object visitLetCommand(LetCommand ast, Object o) {
        ast.D.visit(this, o); 
        ast.C.visit(this, o);
        return null;
    }

    @Override
    public Object visitSequentialCommand(SequentialCommand ast, Object o) {
        ast.C1.visit(this, o);
        ast.C2.visit(this, o);
        return null;
    }

    @Override
    public Object visitWhileCommand(WhileCommand ast, Object o) {
        // while E do C
        // Generar labels para el bucle
        String condLabel = newLabel();
        String bodyLabel = newLabel();
        String endLabel = newLabel();
        
        // Saltar a la evaluación de la condición
        emit("br label %" + condLabel);
        
        // Bloque de condición (label sin indentación)
        if (currentFunction != null) {
            currentFunction.append(condLabel).append(":\n");
        } else {
            main.append(condLabel).append(":\n");
        }
        
        // Evaluar la condición
        String condition = (String) ast.E.visit(this, null);
        
        // Rama condicional: si es true ir al body, si es false ir al end
        emit("br i1 " + condition + ", label %" + bodyLabel + ", label %" + endLabel);
        
        // Bloque del cuerpo del while (label sin indentación)
        if (currentFunction != null) {
            currentFunction.append(bodyLabel).append(":\n");
        } else {
            main.append(bodyLabel).append(":\n");
        }
        
        // Ejecutar el comando del cuerpo
        ast.C.visit(this, null);
        
        // Volver a evaluar la condición
        emit("br label %" + condLabel);
        
        // Bloque de salida (label sin indentación)
        if (currentFunction != null) {
            currentFunction.append(endLabel).append(":\n");
        } else {
            main.append(endLabel).append(":\n");
        }
        
        return null;
    }

    @Override
    public Object visitArrayExpression(ArrayExpression ast, Object o) {
        // Array literal expression: [e1, e2, e3, ...]
        // Retornar un ArrayList de elementos en formato "tipo valor"
        java.util.ArrayList<String> elements = new java.util.ArrayList<>();
        ast.AA.visit(this, elements);
        return elements;
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
            case "\\/":
                // Logical OR
                emit(result + " = or i1 " + e1 + ", " + e2);
                break;
            case "/\\":
                // Logical AND
                emit(result + " = and i1 " + e1 + ", " + e2);
                break;
            default:
                throw new UnsupportedOperationException("Operator not supported: " + operator);
        }
        
        return result;
    }

    @Override
    public Object visitCallExpression(CallExpression ast, Object o) {
        // Llamada a función en una expresión
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
        // En LLVM, los caracteres se representan como i8 con su valor ASCII
        // El spelling incluye las comillas: 'c', así que tomamos el carácter del medio
        String spelling = ast.CL.spelling;
        char c;
        
        if (spelling.length() == 3 && spelling.startsWith("'") && spelling.endsWith("'")) {
            // Carácter normal: 'c'
            c = spelling.charAt(1);
        } else if (spelling.length() > 3 && spelling.startsWith("'") && spelling.endsWith("'")) {
            // Carácter de escape: '\n', '\t', etc.
            String escaped = spelling.substring(1, spelling.length() - 1);
            if (escaped.equals("\\n")) {
                c = '\n';
            } else if (escaped.equals("\\t")) {
                c = '\t';
            } else if (escaped.equals("\\r")) {
                c = '\r';
            } else if (escaped.equals("\\'")) {
                c = '\'';
            } else if (escaped.equals("\\\\")) {
                c = '\\';
            } else {
                // Por defecto, tomar el segundo carácter
                c = spelling.charAt(1);
            }
        } else {
            // Sin comillas (caso inesperado), tomar el primer carácter
            c = spelling.charAt(0);
        }
        
        return String.valueOf((int) c);
    }

    @Override
    public Object visitEmptyExpression(EmptyExpression ast, Object o) {
        return null;
    }

    @Override
    public Object visitIfExpression(IfExpression ast, Object o) {
        // if E1 then E2 else E3
        // Generar labels para las ramas
        String thenLabel = newLabel();
        String elseLabel = newLabel();
        String endLabel = newLabel();
        
        // Variable para almacenar el resultado
        String resultVar = newTemp();
        emit(resultVar + " = alloca i32, align 4");
        
        // Evaluar la condición
        String condition = (String) ast.E1.visit(this, null);
        
        // Rama condicional
        emit("br i1 " + condition + ", label %" + thenLabel + ", label %" + elseLabel);
        
        // Bloque then (label sin indentación)
        if (currentFunction != null) {
            currentFunction.append(thenLabel).append(":\n");
        } else {
            main.append(thenLabel).append(":\n");
        }
        String thenValue = (String) ast.E2.visit(this, null);
        emit("store i32 " + thenValue + ", ptr " + resultVar + ", align 4");
        emit("br label %" + endLabel);
        
        // Bloque else (label sin indentación)
        if (currentFunction != null) {
            currentFunction.append(elseLabel).append(":\n");
        } else {
            main.append(elseLabel).append(":\n");
        }
        String elseValue = (String) ast.E3.visit(this, null);
        emit("store i32 " + elseValue + ", ptr " + resultVar + ", align 4");
        emit("br label %" + endLabel);
        
        // Bloque de salida (label sin indentación)
        if (currentFunction != null) {
            currentFunction.append(endLabel).append(":\n");
        } else {
            main.append(endLabel).append(":\n");
        }
        
        // Cargar y retornar el resultado
        String result = newTemp();
        emit(result + " = load i32, ptr " + resultVar + ", align 4");
        
        return result;
    }

    @Override
    public Object visitIntegerExpression(IntegerExpression ast, Object o) {
        // Return the literal integer value as a string
        // En LLVM, las constantes enteras se usan directamente
        return ast.IL.spelling;
    }

    @Override
    public Object visitLetExpression(LetExpression ast, Object o) {
        // let D in E
        // Crear un nuevo scope local para las declaraciones
        java.util.Map<String, String> savedVarMap = new java.util.HashMap<>(varMap);
        
        // Visitar la declaración (puede ser const, var, func, proc, etc.)
        ast.D.visit(this, null);
        
        // Visitar la expresión que usa la declaración
        String result = (String) ast.E.visit(this, null);
        
        // Restaurar el scope anterior
        varMap = savedVarMap;
        
        return result;
    }

    @Override
    public Object visitRecordExpression(RecordExpression ast, Object o) {
        // Record literal expression: {field1 is value1, field2 is value2, ...}
        // Retornar un ArrayList de elementos en formato "tipo valor"
        java.util.ArrayList<String> elements = new java.util.ArrayList<>();
        ast.RA.visit(this, elements);
        return elements;
    }

    @Override
    public Object visitUnaryExpression(UnaryExpression ast, Object o) {
        // Evaluar el operando
        String operand = (String) ast.E.visit(this, null);
        
        // Obtener el operador
        String operator = ast.O.spelling;
        String result = newTemp();
        
        // Generar la instrucción LLVM según el operador
        switch (operator) {
            case "-":
                // Negación aritmética: 0 - operand
                emit(result + " = sub i32 0, " + operand);
                break;
            case "\\":
                // Negación lógica (NOT): xor operand, true
                emit(result + " = xor i1 " + operand + ", true");
                break;
            default:
                throw new UnsupportedOperationException("Unary operator not supported: " + operator);
        }
        
        return result;
    }

    @Override
    public Object visitVnameExpression(VnameExpression ast, Object o) {
        // Para SimpleVname, verificar si es un parámetro const en funciones
        if (ast.V instanceof SimpleVname) {
            String varName = ((SimpleVname) ast.V).I.spelling;
            if (varMap.containsKey(varName)) {
                String varPtr = varMap.get(varName);
                // Si es un parámetro por valor en función (no es puntero temporal)
                if (varPtr.startsWith("%") && !varPtr.contains("result") && !varPtr.startsWith("%t") && currentFunction != null) {
                    return varPtr;
                }
            }
        }
        
        // Obtener el puntero de la variable
        String var = (String) ast.V.visit(this, o);
        
        // Determinar el tipo de la variable
        String varType;
        if (ast.V.type instanceof RecordTypeDenoter) {
            String typeName = typeDenoterToStructName.get(ast.V.type);
            if (typeName != null) {
                varType = "%struct." + typeName;
            } else {
                varType = (String) ast.V.type.visit(this, o);
            }
        } else {
            varType = (String) ast.V.type.visit(this, o);
        }
        
        // Cargar el valor
        String tmp = newTemp();
        if (varType == null) varType = "i32";
        emit(tmp + " = load " + varType + ", ptr " + var + ", align 4");
        return tmp;
    }

    @Override
    public Object visitBinaryOperatorDeclaration(BinaryOperatorDeclaration ast, Object o) {
        return null;
    }

    @Override
    public Object visitConstDeclaration(ConstDeclaration ast, Object o) {
        // const name = expression
        String constName = ast.I.spelling;
        
        // Evaluar la expresión constante
        String constValue = (String) ast.E.visit(this, null);
        
        // En LLVM, las constantes se pueden manejar con inline substitution
        // Guardamos el valor directamente en varMap sin @ o %
        varMap.put(constName, constValue);
        
        return null;
    }

    @Override
    public Object visitFuncDeclaration(FuncDeclaration ast, Object o) {
        // Declaración de función en Triangle
        String funcName = ast.I.spelling;
        String returnType = getLLVMType(ast.T);
        
        // Guardar el contexto actual
        java.util.Map<String, String> savedVarMap = new java.util.HashMap<>(varMap);
        StringBuilder savedCurrentFunction = currentFunction;
        int savedTempCounter = tempCounter;
        int savedLabelCounter = labelCounter;
        
        // Crear nuevo scope para la función
        varMap.clear();
        tempCounter = 0;
        labelCounter = 0;
        currentFunction = new StringBuilder();
        
        // Crear declaración de función
        currentFunction.append("\n; Function Attrs: noinline nounwind optnone uwtable\n");
        currentFunction.append("define dso_local ").append(returnType).append(" @").append(funcName).append("(");
        
        // Información de la función
        FuncInfo funcInfo = new FuncInfo(returnType);
        
        // Procesar parámetros formales (si los hay)
        if (ast.FPS != null) {
            StringBuilder params = new StringBuilder();
            ast.FPS.visit(this, params);
            currentFunction.append(params.toString());
        }
        
        currentFunction.append(") #0 {\n");
        currentFunction.append("entry:\n");
        
        // Variable para almacenar el resultado de retorno
        String resultVar = "%result";
        emit(resultVar + " = alloca " + returnType + ", align 4");
        varMap.put(funcName + ".result", resultVar);
        
        // Visitar el cuerpo de la función (expression que retorna el valor)
        String resultValue = (String) ast.E.visit(this, null);
        
        // Almacenar el resultado
        if (resultValue != null) {
            emit("store " + returnType + " " + resultValue + ", ptr " + resultVar + ", align 4");
        }
        
        // Cargar y retornar el resultado
        String loadResult = newTemp();
        emit(loadResult + " = load " + returnType + ", ptr " + resultVar + ", align 4");
        emit("ret " + returnType + " " + loadResult);
        
        currentFunction.append("}\n");
        
        // Agregar la función a la sección de funciones
        functions.append(currentFunction.toString());
        
        // Guardar información de la función
        funcMap.put(funcName, funcInfo);
        
        // Restaurar contexto
        currentFunction = savedCurrentFunction;
        varMap = savedVarMap;
        tempCounter = savedTempCounter;
        labelCounter = savedLabelCounter;
        
        return null;
    }

    @Override
    public Object visitProcDeclaration(ProcDeclaration ast, Object o) {
        // Declaración de procedimiento en Triangle (no retorna valor)
        String procName = ast.I.spelling;
        
        // Guardar el contexto actual
        java.util.Map<String, String> savedVarMap = new java.util.HashMap<>(varMap);
        StringBuilder savedCurrentFunction = currentFunction;
        int savedTempCounter = tempCounter;
        int savedLabelCounter = labelCounter;
        
        // Crear nuevo scope para el procedimiento
        varMap.clear();
        tempCounter = 0;
        labelCounter = 0;
        currentFunction = new StringBuilder();
        
        // Crear declaración de procedimiento (void en LLVM)
        currentFunction.append("\n; Function Attrs: noinline nounwind optnone uwtable\n");
        currentFunction.append("define dso_local void @").append(procName).append("(");
        
        // Información del procedimiento
        FuncInfo procInfo = new FuncInfo("void");
        
        // Procesar parámetros formales (si los hay)
        if (ast.FPS != null) {
            StringBuilder params = new StringBuilder();
            ast.FPS.visit(this, params);
            currentFunction.append(params.toString());
        }
        
        currentFunction.append(") #0 {\n");
        currentFunction.append("entry:\n");
        
        // Visitar el cuerpo del procedimiento (comando)
        ast.C.visit(this, null);
        
        // Retornar void
        emit("ret void");
        
        currentFunction.append("}\n");
        
        // Agregar el procedimiento a la sección de funciones
        functions.append(currentFunction.toString());
        
        // Guardar información del procedimiento
        funcMap.put(procName, procInfo);
        
        // Restaurar contexto
        currentFunction = savedCurrentFunction;
        varMap = savedVarMap;
        tempCounter = savedTempCounter;
        labelCounter = savedLabelCounter;
        
        return null;
    }

    @Override
    public Object visitSequentialDeclaration(SequentialDeclaration ast, Object o) {
        ast.D1.visit(this, o); 
        ast.D2.visit(this, o);
        return null;
    }

    @Override
    public Object visitTypeDeclaration(TypeDeclaration ast, Object o) {
        if (ast.T instanceof RecordTypeDenoter) {
            // Generar un nombre único para el struct
            String structName = ast.I.spelling;
            
            // Mapear el TypeDenoter al nombre del struct
            typeDenoterToStructName.put(ast.T, structName);
            
            // Construir el mapa de índices de campos
            java.util.Map<String, Integer> fieldMap = new java.util.HashMap<>();
            buildFieldIndexMap(((RecordTypeDenoter) ast.T).FT, fieldMap, 0);
            structFieldIndices.put(structName, fieldMap);
            
            // Generar la definición del struct en LLVM
            String fieldTypes = (String) ast.T.visit(this, o);
            globals.append("%struct.").append(structName).append(" = type { ").append(fieldTypes).append(" }\n");
        }
        return ast.T.visit(this, o);
    }

    @Override
    public Object visitUnaryOperatorDeclaration(UnaryOperatorDeclaration ast, Object o) {
        return null;
    }

    @Override
    public Object visitVarDeclaration(VarDeclaration ast, Object o) {
        String varName = ast.I.spelling;
        String llvmType = getLLVMType(ast.T);
        
        // SIEMPRE usar @ para variables globales (dentro de let en el programa principal)
        String varPtr = "@" + varName;
        
        // Determinar el valor inicial según el tipo
        String initialValue;
        if (llvmType.startsWith("%struct.") || llvmType.startsWith("[")) {
            // Para structs y arrays usar zeroinitializer
            initialValue = "zeroinitializer";
        } else {
            // Para tipos primitivos usar 0
            initialValue = "0";
        }
        
        // Declarar como variable global
        globals.append(varPtr)
              .append(" = dso_local global ")
              .append(llvmType)
              .append(" ")
              .append(initialValue)
              .append(", align 4\n");
        
        // Guardar en varMap para referencias posteriores
        varMap.put(varName, varPtr);
        
        // Si es un tipo Record, guardar el mapping de tipo
        if (ast.T instanceof RecordTypeDenoter) {
            String structType = typeDenoterToStructName.get(ast.T);
            if (structType != null) {
                varStructTypes.put(varName, "%struct." + structType);
            }
        }
        
        return null;
    }

    @Override
    public Object visitMultipleArrayAggregate(MultipleArrayAggregate ast, Object o) {
        java.util.ArrayList<String> elements = (java.util.ArrayList<String>) o;
        
        String type = (String) ast.E.type.visit(this, null);
        Object valueResult = ast.E.visit(this, null);
        
        String value;
        if (valueResult instanceof java.util.ArrayList) {
            // Si es un record u otro agregado, convertir a string
            value = valueResult.toString();
        } else {
            value = (String) valueResult;
        }
        
        elements.add(type + " " + value);
        ast.AA.visit(this, o);
        
        return null;
    }

    @Override
    public Object visitSingleArrayAggregate(SingleArrayAggregate ast, Object o) {
        java.util.ArrayList<String> elements = (java.util.ArrayList<String>) o;
        
        String type = (String) ast.E.type.visit(this, null);
        Object valueResult = ast.E.visit(this, null);
        
        String value;
        if (valueResult instanceof java.util.ArrayList) {
            // Si es un record u otro agregado, convertir a string
            value = valueResult.toString();
        } else {
            value = (String) valueResult;
        }
        
        elements.add(type + " " + value);
        return null;
    }

    @Override
    public Object visitMultipleRecordAggregate(MultipleRecordAggregate ast, Object o) {
        java.util.ArrayList<String> elements = (java.util.ArrayList<String>) o;
        String value = (String) ast.E.visit(this, null);
        String type = (String) ast.E.type.visit(this, null);
        elements.add(type + " " + value);
        ast.RA.visit(this, elements);
        return null;
    }

    @Override
    public Object visitSingleRecordAggregate(SingleRecordAggregate ast, Object o) {
        java.util.ArrayList<String> elements = (java.util.ArrayList<String>) o;
        String value = (String) ast.E.visit(this, null);
        String type = (String) ast.E.type.visit(this, null);
        elements.add(type + " " + value);
        return null;
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
        // Parámetro formal de función de orden superior: func name(params): ReturnType
        // En LLVM, las funciones se pasan como punteros
        StringBuilder params = (StringBuilder) o;
        String paramName = ast.I.spelling;
        
        // Construir el tipo de función
        String returnType = getLLVMType(ast.T);
        StringBuilder funcType = new StringBuilder();
        funcType.append(returnType).append(" (");
        
        // Procesar los parámetros de la función
        if (ast.FPS != null && !(ast.FPS instanceof EmptyFormalParameterSequence)) {
            StringBuilder innerParams = new StringBuilder();
            ast.FPS.visit(this, innerParams);
            funcType.append(innerParams.toString());
        }
        
        funcType.append(")*");
        
        // Pasar como puntero a función
        params.append("ptr %").append(paramName);
        
        // Guardar en varMap como puntero a función
        varMap.put(paramName, "%" + paramName);
        
        return null;
    }

    @Override
    public Object visitProcFormalParameter(ProcFormalParameter ast, Object o) {
        // Parámetro formal de procedimiento de orden superior: proc name(params)
        // En LLVM, los procedimientos se pasan como punteros a funciones void
        StringBuilder params = (StringBuilder) o;
        String paramName = ast.I.spelling;
        
        // Construir el tipo de procedimiento (función void)
        StringBuilder procType = new StringBuilder();
        procType.append("void (");
        
        // Procesar los parámetros del procedimiento
        if (ast.FPS != null && !(ast.FPS instanceof EmptyFormalParameterSequence)) {
            StringBuilder innerParams = new StringBuilder();
            ast.FPS.visit(this, innerParams);
            procType.append(innerParams.toString());
        }
        
        procType.append(")*");
        
        // Pasar como puntero a función
        params.append("ptr %").append(paramName);
        
        // Guardar en varMap como puntero a función
        varMap.put(paramName, "%" + paramName);
        
        return null;
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
        String value = (String) ast.E.visit(this, null);
        
        // Si hay un StringBuilder en el contexto, agregarlo (para múltiples parámetros)
        if (o instanceof StringBuilder) {
            StringBuilder args = (StringBuilder) o;
            args.append("i32 ").append(value);
        }
        
        // Retornar el valor directamente para llamadas con un solo parámetro
        return value;
    }

    @Override
    public Object visitFuncActualParameter(FuncActualParameter ast, Object o) {
        // Parámetro actual de función: pasar una función como argumento
        // Sintaxis: func functionName
        StringBuilder args = (StringBuilder) o;
        String funcName = ast.I.spelling;
        
        // Verificar que la función exista
        if (!funcMap.containsKey(funcName)) {
            throw new RuntimeException("Function not found: " + funcName);
        }
        
        // Pasar el puntero a la función usando @functionName
        args.append("ptr @").append(funcName);
        
        return null;
    }

    @Override
    public Object visitProcActualParameter(ProcActualParameter ast, Object o) {
        // Parámetro actual de procedimiento: pasar un procedimiento como argumento
        // Sintaxis: proc procedureName
        StringBuilder args = (StringBuilder) o;
        String procName = ast.I.spelling;
        
        // Verificar que el procedimiento exista
        if (!funcMap.containsKey(procName)) {
            throw new RuntimeException("Procedure not found: " + procName);
        }
        
        // Pasar el puntero al procedimiento usando @procedureName
        args.append("ptr @").append(procName);
        
        return null;
    }

    @Override
    public Object visitVarActualParameter(VarActualParameter ast, Object o) {
        // Parámetro actual variable (pasa la dirección)
        StringBuilder args = (StringBuilder) o;
        String varName = getVnameIdentifier(ast.V);
        
        if (varMap.containsKey(varName)) {
            String varPtr = varMap.get(varName);
            args.append("ptr ").append(varPtr);
        } else {
            throw new RuntimeException("Variable not found: " + varName);
        }
        return null;
    }

    @Override
    public Object visitEmptyActualParameterSequence(EmptyActualParameterSequence ast, Object o) {
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
        if (o instanceof StringBuilder) {
            StringBuilder args = (StringBuilder) o;
            ast.AP.visit(this, args);
            return null;
        } else {
            // Si no hay StringBuilder, retornar el valor directamente
            return ast.AP.visit(this, null);
        }
    }

    @Override
    public Object visitAnyTypeDenoter(AnyTypeDenoter ast, Object o) {
        return "i32";
    }

    @Override
    public Object visitArrayTypeDenoter(ArrayTypeDenoter ast, Object o) {
        int size = Integer.parseInt(ast.IL.spelling);
        String elementType = (String) ast.T.visit(this, o);
        return "[" + size + " x " + elementType + "]";
    }

    @Override
    public Object visitBoolTypeDenoter(BoolTypeDenoter ast, Object o) {
        return "i1";
    }

    @Override
    public Object visitCharTypeDenoter(CharTypeDenoter ast, Object o) {
        return "i8";
    }

    @Override
    public Object visitErrorTypeDenoter(ErrorTypeDenoter ast, Object o) {
        return "i32";
    }

    @Override
    public Object visitSimpleTypeDenoter(SimpleTypeDenoter ast, Object o) {
        if (ast.I.spelling.equals("Integer")) return "i32";
        if (ast.I.spelling.equals("Char")) return "i8";
        if (ast.I.spelling.equals("Boolean")) return "i1";
        return "i32";
    }

    @Override
    public Object visitIntTypeDenoter(IntTypeDenoter ast, Object o) {
        return "i32";
    }

    @Override
    public Object visitRecordTypeDenoter(RecordTypeDenoter ast, Object o) {
        return ast.FT.visit(this, o);
    }

    @Override
    public Object visitMultipleFieldTypeDenoter(MultipleFieldTypeDenoter ast, Object o) {
        String left = (String) ast.T.visit(this, o);
        String right = (String) ast.FT.visit(this, o);
        return left + ", " + right;
    }

    @Override
    public Object visitSingleFieldTypeDenoter(SingleFieldTypeDenoter ast, Object o) {
        String type = (String) ast.T.visit(this, o);
        return type;
    }

    @Override
    public Object visitCharacterLiteral(CharacterLiteral ast, Object o) {
        return null;
    }

    @Override
    public Object visitIdentifier(Identifier ast, Object o) {
        return null;
    }

    @Override
    public Object visitIntegerLiteral(IntegerLiteral ast, Object o) {
        return null;
    }

    @Override
    public Object visitOperator(Operator ast, Object o) {
        return null;
    }

    @Override
    public Object visitDotVname(DotVname ast, Object o) {
        // Record field access: record.field
        // Obtener el puntero base del record
        String basePtr = (String) ast.V.visit(this, o);
        
        // Obtener el nombre de la variable base (si es SimpleVname)
        String baseName = null;
        if (ast.V instanceof SimpleVname) {
            baseName = ((SimpleVname) ast.V).I.spelling;
        }
        
        // Obtener el tipo struct de la variable
        String structType = null;
        if (baseName != null && varStructTypes.containsKey(baseName)) {
            structType = varStructTypes.get(baseName);
        }
        
        if (structType == null) {
            throw new RuntimeException("Cannot determine struct type for record field access");
        }
        
        // Obtener el nombre del struct sin el prefijo "%struct."
        String structName = structType.replace("%struct.", "");
        java.util.Map<String, Integer> fieldMap = structFieldIndices.get(structName);
        int fieldIndex = 0;
        if (fieldMap != null && fieldMap.containsKey(ast.I.spelling)) {
            fieldIndex = fieldMap.get(ast.I.spelling);
        }
        
        // Generar instrucción getelementptr para obtener el puntero al campo
        String fieldPtr = newTemp();
        emit(fieldPtr + " = getelementptr inbounds " + structType + ", ptr " + basePtr + ", i32 0, i32 " + fieldIndex);
        return fieldPtr;
    }

    @Override
    public Object visitSimpleVname(SimpleVname ast, Object o) {
        String varName = ast.I.spelling;
        
        // Check if it's a variable or parameter in varMap
        if (varMap.containsKey(varName)) {
            String varPtr = varMap.get(varName);
            // Retornar el puntero directamente (@ o %)
            // El load se hace en visitVnameExpression si es necesario
            return varPtr;
        }
        
        throw new RuntimeException("Variable not found: " + varName);
    }

    @Override
    public Object visitSubscriptVname(SubscriptVname ast, Object o) {
        // Array subscript: arr[index]
        String basePtr = (String) ast.V.visit(this, o);
        String index = (String) ast.E.visit(this, null);
        String varType = (String) ast.V.type.visit(this, o);
        
        // Convertir índice a i64 para getelementptr
        String index64 = newTemp();
        emit(index64 + " = sext i32 " + index + " to i64");
        
        // Usar getelementptr para calcular la dirección del elemento
        String elementPtr = newTemp();
        emit(elementPtr + " = getelementptr inbounds " + varType + ", ptr " + basePtr + ", i64 0, i64 " + index64);
        return elementPtr;
    }

    @Override
    public Object visitProgram(Program ast, Object o) {
        // Generar la función main
        main.append("; Function Attrs: noinline nounwind optnone uwtable\n");
        main.append("define dso_local i32 @main() #0 {\n");
        main.append("entry:\n");
        main.append("  %1 = alloca i32, align 4\n");
        main.append("  store i32 0, ptr %1, align 4\n");
        
        // Visitar el comando del programa
        ast.C.visit(this, o);
        
        // Retornar 0
        emit("ret i32 0");
        main.append("}\n");
        
        // Agregar atributos de función
        main.append("\nattributes #0 = { noinline nounwind optnone uwtable \"frame-pointer\"=\"all\" ");
        main.append("\"min-legal-vector-width\"=\"0\" \"no-trapping-math\"=\"true\" ");
        main.append("\"stack-protector-buffer-size\"=\"8\" \"target-cpu\"=\"x86-64\" ");
        main.append("\"target-features\"=\"+cmov,+cx8,+fxsr,+mmx,+sse,+sse2,+x87\" \"tune-cpu\"=\"generic\" }\n");
        
        // Agregar metadata
        main.append("\n!llvm.module.flags = !{!0, !1, !2, !3, !4}\n");
        main.append("!llvm.ident = !{!5}\n\n");
        main.append("!0 = !{i32 1, !\"wchar_size\", i32 4}\n");
        main.append("!1 = !{i32 8, !\"PIC Level\", i32 2}\n");
        main.append("!2 = !{i32 7, !\"PIE Level\", i32 2}\n");
        main.append("!3 = !{i32 7, !\"uwtable\", i32 2}\n");
        main.append("!4 = !{i32 7, !\"frame-pointer\", i32 2}\n");
        main.append("!5 = !{!\"Triangle LLVM Compiler\"}\n");
        
        return null;
    }

    
}