package Triangle.LLVM;

import Triangle.AbstractSyntaxTrees.*;

/* Generador LLVM IR paraTriangle */

public class LLVM implements Visitor {
    private StringBuilder header = new StringBuilder();
    private StringBuilder globals = new StringBuilder();
    private StringBuilder functions = new StringBuilder();
    private StringBuilder main = new StringBuilder();
    private StringBuilder codeIR = new StringBuilder();
    private StringBuilder currentFunction = null;
    
    public String sourceFileName;
    
    private java.util.Map<String, String> varMap;
    private java.util.Map<String, FuncInfo> funcMap;
    
    // Soporte records/structs
    private java.util.Map<String, java.util.Map<String, Integer>> structFieldIndices;
    private java.util.Map<String, String> varStructTypes;
    private java.util.Map<TypeDenoter, String> typeDenoterToStructName;
    private int structCounter = 0;
    
    private int tempCounter = 0;
    private int labelCounter = 0;
    
    /* Almacena información de funciones y procedimientos */
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
    
    private String getLLVMType(Triangle.AbstractSyntaxTrees.TypeDenoter type) {
        if (type instanceof IntTypeDenoter) {
            return "i32";
        } else if (type instanceof BoolTypeDenoter) {
            return "i1";
        } else if (type instanceof CharTypeDenoter) {
            return "i8";
        } else if (type instanceof RecordTypeDenoter) {
            if (typeDenoterToStructName.containsKey(type)) {
                return "%struct." + typeDenoterToStructName.get(type);
            }
            return (String) type.visit(this, null);
        } else if (type instanceof SimpleTypeDenoter) {
            String typeName = ((SimpleTypeDenoter) type).I.spelling;
            switch (typeName) {
                case "Integer": return "i32";
                case "Boolean": return "i1";
                case "Char": return "i8";
                default: return "i32";
            }
        }
        return "i32";
    }
    
    private void generarHeader() {
        String os = System.getProperty("os.name").toLowerCase();

        // Metadatos del módulo
        header.append("; ModuleID = ").append(sourceFileName).append("\n");
        header.append("source_filename = \"").append(sourceFileName).append("\"\n");
        header.append("target datalayout = \"e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128\"\n");
        
        // Configurar según el sistema operativo
        if (os.contains("win")) {
            header.append("target triple = \"x86_64-pc-windows-msvc\"\n\n");
        } else if (os.contains("mac")) {
            String arch = System.getProperty("os.arch").toLowerCase();
            if (arch.contains("aarch64") || arch.contains("arm")) {
                header.append("target triple = \"arm64-apple-macos\"\n\n");  // Mac M1/M2
            } else {
                header.append("target triple = \"x86_64-apple-macos\"\n\n");  // Mac Intel
            }
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            header.append("target triple = \"x86_64-pc-linux-gnu\"\n\n");
        }
        
        header.append("; Declaraciones de funciones externas\n");
        header.append("declare i32 @printf(ptr, ...)\n");
        header.append("declare i32 @putchar(i32)\n");
        header.append("declare i32 @scanf(ptr, ...)\n");
        header.append("declare i32 @getchar()\n\n");
        
        globals.append("@.str.putint = private unnamed_addr constant [4 x i8] c\"%d\\0A\\00\", align 1\n");
        globals.append("@.str.getint = private unnamed_addr constant [3 x i8] c\"%d\\00\", align 1\n");
    }
    
    public void generarLLVM(Program theAST) {
        generarHeader();
        theAST.visit(this, null);
        
        // Ensamblar todas las secciones en orden
        codeIR.append(header.toString());
        codeIR.append(globals.toString());
        codeIR.append("\n");
        codeIR.append(functions.toString());
        codeIR.append("\n");
        codeIR.append(main.toString());
        
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
        String llFileName = sourceFileName.replace(".tri", ".ll");
        
        try {
            System.out.println("\n=== Ejecutando LLVM IR: " + llFileName + " ===\n");
            
            ProcessBuilder pb = new ProcessBuilder("lli", llFileName);
            pb.inheritIO();
            
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

    /* Visit asignación (variable := expresión) */
    @Override
    public Object visitAssignCommand(AssignCommand ast, Object o) {
        Object rhsResult = ast.E.visit(this, null);
        String lhs = (String) ast.V.visit(this, null);
        
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
        
        // Asignación de record o array literal
        if (rhsResult instanceof java.util.ArrayList) {
            java.util.ArrayList<String> elements = (java.util.ArrayList<String>) rhsResult;
            for (int i = 0; i < elements.size(); i++) {
                String element = elements.get(i);
                String[] parts = element.split(" ", 2);
                String elemType = parts[0];
                String elemValue = parts[1];
                
                String fieldPtr;
                if (i == 0) {
                    fieldPtr = lhs;
                } else {
                    fieldPtr = newTemp();
                    emit(fieldPtr + " = getelementptr inbounds " + varType + ", ptr " + lhs + ", i32 0, i32 " + i);
                }
                
                emit("store " + elemType + " " + elemValue + ", ptr " + fieldPtr + ", align 4");
            }
        } else {
            String rhs = (String) rhsResult;
            if (varType == null) varType = "i32";
            emit("store " + varType + " " + rhs + ", ptr " + lhs + ", align 4");
        }
        
        return null;
    }
    
    private String getVnameIdentifier(Vname vname) {
        if (vname instanceof SimpleVname) {
            return ((SimpleVname) vname).I.spelling;
        }
        throw new UnsupportedOperationException("Only SimpleVname is supported in assignments");
    }

    @Override
    public Object visitCallCommand(CallCommand ast, Object o) {
        String procName = ast.I.spelling;
        
        // Procedimientos
        if (procName.equals("putint")) {
            if (ast.APS != null) {
                StringBuilder args = new StringBuilder();
                ast.APS.visit(this, args);
                String temp = newTemp();
                emit(temp + " = call i32 (ptr, ...) @printf(ptr @.str.putint, " + args.toString() + ")");
            }
            return null;
        }
        
        if (procName.equals("put")) {
            if (ast.APS != null) {
                String charValue = (String) ast.APS.visit(this, null);
                String temp = newTemp();
                emit(temp + " = call i32 @putchar(i32 " + charValue + ")");
            }
            return null;
        }
        
        if (procName.equals("getint")) {
            if (ast.APS != null) {
                StringBuilder args = new StringBuilder();
                ast.APS.visit(this, args);
                String temp = newTemp();
                emit(temp + " = call i32 (ptr, ...) @scanf(ptr @.str.getint, " + args.toString() + ")");
            }
            return null;
        }
        
        if (procName.equals("get")) {
            if (ast.APS != null) {
                String charValue = newTemp();
                emit(charValue + " = call i32 @getchar()");
                
                StringBuilder args = new StringBuilder();
                ast.APS.visit(this, args);
                String varPtr = args.toString().replace("ptr ", "");
                
                emit("store i32 " + charValue + ", ptr " + varPtr + ", align 4");
            }
            return null;
        }
        
        // Procedimiento del usuario
        if (funcMap.containsKey(procName)) {
            FuncInfo procInfo = funcMap.get(procName);
            
            StringBuilder callInstr = new StringBuilder();
            callInstr.append("call ").append(procInfo.returnType);
            callInstr.append(" @").append(procName).append("(");
            
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

    /* Visit comando if-then-else */
    @Override
    public Object visitIfCommand(IfCommand ast, Object o) {
        String thenLabel = newLabel();  // True
        String elseLabel = newLabel();  // False
        String endLabel = newLabel();   // Convergencia
        
        String condition = (String) ast.E.visit(this, null);
        emit("br i1 " + condition + ", label %" + thenLabel + ", label %" + elseLabel);
        
        if (currentFunction != null) {
            currentFunction.append(thenLabel).append(":\n");
        } else {
            main.append(thenLabel).append(":\n");
        }
        ast.C1.visit(this, null);
        emit("br label %" + endLabel);
        
        if (currentFunction != null) {
            currentFunction.append(elseLabel).append(":\n");
        } else {
            main.append(elseLabel).append(":\n");
        }
        ast.C2.visit(this, null);
        emit("br label %" + endLabel);
        
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

    /* Visit comando while */
    @Override
    public Object visitWhileCommand(WhileCommand ast, Object o) {
        String condLabel = newLabel();   // Evaluar condición
        String bodyLabel = newLabel();   // Cuerpo del loop
        String endLabel = newLabel();    // Salir del loop
        
        emit("br label %" + condLabel);
        
        if (currentFunction != null) {
            currentFunction.append(condLabel).append(":\n");
        } else {
            main.append(condLabel).append(":\n");
        }
        
        String condition = (String) ast.E.visit(this, null);
        emit("br i1 " + condition + ", label %" + bodyLabel + ", label %" + endLabel);
        
        if (currentFunction != null) {
            currentFunction.append(bodyLabel).append(":\n");
        } else {
            main.append(bodyLabel).append(":\n");
        }
        
        ast.C.visit(this, null);
        emit("br label %" + condLabel);
        
        if (currentFunction != null) {
            currentFunction.append(endLabel).append(":\n");
        } else {
            main.append(endLabel).append(":\n");
        }
        
        return null;
    }

    @Override
    public Object visitArrayExpression(ArrayExpression ast, Object o) {
        java.util.ArrayList<String> elements = new java.util.ArrayList<>();
        ast.AA.visit(this, elements);
        return elements;
    }

    /* Visit expresión binaria (operador con dos operandos) */

    @Override
    public Object visitBinaryExpression(BinaryExpression ast, Object o) {
        String e1 = (String) ast.E1.visit(this, null);  // Evaluar operando izquierdo
        String e2 = (String) ast.E2.visit(this, null);  // Evaluar operando derecho
        String operator = ast.O.spelling;
        String result = newTemp();
        
        switch (operator) {
            case "+": emit(result + " = add i32 " + e1 + ", " + e2); break;
            case "-": emit(result + " = sub i32 " + e1 + ", " + e2); break;
            case "*": emit(result + " = mul i32 " + e1 + ", " + e2); break;
            case "/": emit(result + " = sdiv i32 " + e1 + ", " + e2); break;
            case "<": emit(result + " = icmp slt i32 " + e1 + ", " + e2); break;
            case "<=": emit(result + " = icmp sle i32 " + e1 + ", " + e2); break;
            case ">": emit(result + " = icmp sgt i32 " + e1 + ", " + e2); break;
            case ">=": emit(result + " = icmp sge i32 " + e1 + ", " + e2); break;
            case "=": emit(result + " = icmp eq i32 " + e1 + ", " + e2); break;
            case "\\=": emit(result + " = icmp ne i32 " + e1 + ", " + e2); break;
            case "\\/": emit(result + " = or i1 " + e1 + ", " + e2); break;
            case "/\\": emit(result + " = and i1 " + e1 + ", " + e2); break;
            default: throw new UnsupportedOperationException("Operator not supported: " + operator);
        }
        
        return result;
    }

    @Override
    public Object visitCallExpression(CallExpression ast, Object o) {
        String funcName = ast.I.spelling;
        
        if (funcMap.containsKey(funcName)) {
            FuncInfo funcInfo = funcMap.get(funcName);
            String result = newTemp();
            
            StringBuilder callInstr = new StringBuilder();
            callInstr.append(result).append(" = call ").append(funcInfo.returnType);
            callInstr.append(" @").append(funcName).append("(");
            
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

    /* Visit expresión de carácter literal */
    @Override
    public Object visitCharacterExpression(CharacterExpression ast, Object o) {
        String spelling = ast.CL.spelling;
        char c;
        
        if (spelling.length() == 3 && spelling.startsWith("'") && spelling.endsWith("'")) {
            c = spelling.charAt(1);  // Carácter simple
        } else if (spelling.length() > 3 && spelling.startsWith("'") && spelling.endsWith("'")) {
            String escaped = spelling.substring(1, spelling.length() - 1);
            if (escaped.equals("\\n")) c = '\n';
            else if (escaped.equals("\\t")) c = '\t';
            else if (escaped.equals("\\r")) c = '\r';
            else if (escaped.equals("\\'")) c = '\'';
            else if (escaped.equals("\\\\")) c = '\\';
            else c = spelling.charAt(1);
        } else {
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
        String thenLabel = newLabel();
        String elseLabel = newLabel();
        String endLabel = newLabel();
        
        String resultVar = newTemp();
        emit(resultVar + " = alloca i32, align 4");
        
        String condition = (String) ast.E1.visit(this, null);
        emit("br i1 " + condition + ", label %" + thenLabel + ", label %" + elseLabel);
        
        if (currentFunction != null) {
            currentFunction.append(thenLabel).append(":\n");
        } else {
            main.append(thenLabel).append(":\n");
        }
        String thenValue = (String) ast.E2.visit(this, null);
        emit("store i32 " + thenValue + ", ptr " + resultVar + ", align 4");
        emit("br label %" + endLabel);
        
        if (currentFunction != null) {
            currentFunction.append(elseLabel).append(":\n");
        } else {
            main.append(elseLabel).append(":\n");
        }
        String elseValue = (String) ast.E3.visit(this, null);
        emit("store i32 " + elseValue + ", ptr " + resultVar + ", align 4");
        emit("br label %" + endLabel);
        
        if (currentFunction != null) {
            currentFunction.append(endLabel).append(":\n");
        } else {
            main.append(endLabel).append(":\n");
        }
        
        String result = newTemp();
        emit(result + " = load i32, ptr " + resultVar + ", align 4");
        
        return result;
    }

    @Override
    public Object visitIntegerExpression(IntegerExpression ast, Object o) {
        return ast.IL.spelling;
    }

    @Override
    public Object visitLetExpression(LetExpression ast, Object o) {
        java.util.Map<String, String> savedVarMap = new java.util.HashMap<>(varMap);
        ast.D.visit(this, null);
        String result = (String) ast.E.visit(this, null);
        varMap = savedVarMap;
        return result;
    }

    @Override
    public Object visitRecordExpression(RecordExpression ast, Object o) {
        java.util.ArrayList<String> elements = new java.util.ArrayList<>();
        ast.RA.visit(this, elements);
        return elements;
    }

    @Override
    public Object visitUnaryExpression(UnaryExpression ast, Object o) {
        String operand = (String) ast.E.visit(this, null);
        String operator = ast.O.spelling;
        String result = newTemp();
        
        switch (operator) {
            case "-":
                emit(result + " = sub i32 0, " + operand);
                break;
            case "\\":
                emit(result + " = xor i1 " + operand + ", true");
                break;
            default:
                throw new UnsupportedOperationException("Unary operator not supported: " + operator);
        }
        
        return result;
    }

    /* Visit nombre de variable */
    @Override
    public Object visitVnameExpression(VnameExpression ast, Object o) {
        // Parámetros por valor en funciones
        if (ast.V instanceof SimpleVname) {
            String varName = ((SimpleVname) ast.V).I.spelling;
            if (varMap.containsKey(varName)) {
                String varPtr = varMap.get(varName);
                if (varPtr.startsWith("%") && !varPtr.contains("result") && !varPtr.startsWith("%t") && currentFunction != null) {
                    return varPtr;
                }
            }
        }
        
        String var = (String) ast.V.visit(this, o);
        
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
        String constName = ast.I.spelling;
        String constValue = (String) ast.E.visit(this, null);
        varMap.put(constName, constValue);
        return null;
    }

    /* Visit declaración de función */
    @Override
    public Object visitFuncDeclaration(FuncDeclaration ast, Object o) {
        String funcName = ast.I.spelling;
        String returnType = getLLVMType(ast.T);
        
        java.util.Map<String, String> savedVarMap = new java.util.HashMap<>(varMap);
        StringBuilder savedCurrentFunction = currentFunction;
        int savedTempCounter = tempCounter;
        int savedLabelCounter = labelCounter;
        
        varMap.clear();
        tempCounter = 0;
        labelCounter = 0;
        currentFunction = new StringBuilder();
        
        // Generar encabezado de la función
        currentFunction.append("\n; Function Attrs: noinline nounwind optnone uwtable\n");
        currentFunction.append("define dso_local ").append(returnType).append(" @").append(funcName).append("(");
        
        FuncInfo funcInfo = new FuncInfo(returnType);
        
        if (ast.FPS != null) {
            StringBuilder params = new StringBuilder();
            ast.FPS.visit(this, params);
            currentFunction.append(params.toString());
        }
        
        currentFunction.append(") #0 {\n");
        currentFunction.append("entry:\n");
        
        String resultVar = "%result";
        emit(resultVar + " = alloca " + returnType + ", align 4");
        varMap.put(funcName + ".result", resultVar);
        
        String resultValue = (String) ast.E.visit(this, null);
        
        if (resultValue != null) {
            emit("store " + returnType + " " + resultValue + ", ptr " + resultVar + ", align 4");
        }
        
        String loadResult = newTemp();
        emit(loadResult + " = load " + returnType + ", ptr " + resultVar + ", align 4");
        emit("ret " + returnType + " " + loadResult);
        
        currentFunction.append("}\n");
        functions.append(currentFunction.toString());
        funcMap.put(funcName, funcInfo);
        
        currentFunction = savedCurrentFunction;
        varMap = savedVarMap;
        tempCounter = savedTempCounter;
        labelCounter = savedLabelCounter;
        
        return null;
    }

    @Override
    public Object visitProcDeclaration(ProcDeclaration ast, Object o) {
        String procName = ast.I.spelling;
        
        java.util.Map<String, String> savedVarMap = new java.util.HashMap<>(varMap);
        StringBuilder savedCurrentFunction = currentFunction;
        int savedTempCounter = tempCounter;
        int savedLabelCounter = labelCounter;
        
        varMap.clear();
        tempCounter = 0;
        labelCounter = 0;
        currentFunction = new StringBuilder();
        
        currentFunction.append("\n; Function Attrs: noinline nounwind optnone uwtable\n");
        currentFunction.append("define dso_local void @").append(procName).append("(");
        
        FuncInfo procInfo = new FuncInfo("void");
        
        if (ast.FPS != null) {
            StringBuilder params = new StringBuilder();
            ast.FPS.visit(this, params);
            currentFunction.append(params.toString());
        }
        
        currentFunction.append(") #0 {\n");
        currentFunction.append("entry:\n");
        
        ast.C.visit(this, null);
        emit("ret void");
        
        currentFunction.append("}\n");
        functions.append(currentFunction.toString());
        funcMap.put(procName, procInfo);
        
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
            String structName = ast.I.spelling;
            typeDenoterToStructName.put(ast.T, structName);
            
            java.util.Map<String, Integer> fieldMap = new java.util.HashMap<>();
            buildFieldIndexMap(((RecordTypeDenoter) ast.T).FT, fieldMap, 0);
            structFieldIndices.put(structName, fieldMap);
            
            String fieldTypes = (String) ast.T.visit(this, o);
            globals.append("%struct.").append(structName).append(" = type { ").append(fieldTypes).append(" }\n");
        }
        return ast.T.visit(this, o);
    }

    @Override
    public Object visitUnaryOperatorDeclaration(UnaryOperatorDeclaration ast, Object o) {
        return null;
    }

    /** Visit declaración de variable global */
    @Override
    public Object visitVarDeclaration(VarDeclaration ast, Object o) {
        String varName = ast.I.spelling;
        String llvmType = getLLVMType(ast.T);
        String varPtr = "@" + varName;  // Usan @ como prefijo
        
        String initialValue;
        if (llvmType.startsWith("%struct.") || llvmType.startsWith("[")) {
            initialValue = "zeroinitializer";  // Para structs y arrays
        } else {
            initialValue = "0";  // Para tipos primitivos
        }
        
        globals.append(varPtr)
              .append(" = dso_local global ")
              .append(llvmType)
              .append(" ")
              .append(initialValue)
              .append(", align 4\n");
        
        varMap.put(varName, varPtr);
        
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
        StringBuilder params = (StringBuilder) o;
        String paramName = ast.I.spelling;
        String paramType = getLLVMType(ast.T);
        
        params.append(paramType).append(" %").append(paramName);
        varMap.put(paramName, "%" + paramName);
        
        return null;
    }

    @Override
    public Object visitFuncFormalParameter(FuncFormalParameter ast, Object o) {
        StringBuilder params = (StringBuilder) o;
        String paramName = ast.I.spelling;
        
        String returnType = getLLVMType(ast.T);
        StringBuilder funcType = new StringBuilder();
        funcType.append(returnType).append(" (");
        
        if (ast.FPS != null && !(ast.FPS instanceof EmptyFormalParameterSequence)) {
            StringBuilder innerParams = new StringBuilder();
            ast.FPS.visit(this, innerParams);
            funcType.append(innerParams.toString());
        }
        
        funcType.append(")*");
        params.append("ptr %").append(paramName);
        varMap.put(paramName, "%" + paramName);
        
        return null;
    }

    @Override
    public Object visitProcFormalParameter(ProcFormalParameter ast, Object o) {
        StringBuilder params = (StringBuilder) o;
        String paramName = ast.I.spelling;
        
        StringBuilder procType = new StringBuilder();
        procType.append("void (");
        
        if (ast.FPS != null && !(ast.FPS instanceof EmptyFormalParameterSequence)) {
            StringBuilder innerParams = new StringBuilder();
            ast.FPS.visit(this, innerParams);
            procType.append(innerParams.toString());
        }
        
        procType.append(")*");
        params.append("ptr %").append(paramName);
        varMap.put(paramName, "%" + paramName);
        
        return null;
    }

    @Override
    public Object visitVarFormalParameter(VarFormalParameter ast, Object o) {
        StringBuilder params = (StringBuilder) o;
        String paramName = ast.I.spelling;
        String paramType = getLLVMType(ast.T);
        
        params.append("ptr %").append(paramName);
        varMap.put(paramName, "%" + paramName);
        
        return null;
    }

    @Override
    public Object visitEmptyFormalParameterSequence(EmptyFormalParameterSequence ast, Object o) {
        return null;
    }

    @Override
    public Object visitMultipleFormalParameterSequence(MultipleFormalParameterSequence ast, Object o) {
        StringBuilder params = (StringBuilder) o;
        ast.FP.visit(this, params);
        params.append(", ");
        ast.FPS.visit(this, params);
        return null;
    }

    @Override
    public Object visitSingleFormalParameterSequence(SingleFormalParameterSequence ast, Object o) {
        StringBuilder params = (StringBuilder) o;
        ast.FP.visit(this, params);
        return null;
    }

    @Override
    public Object visitConstActualParameter(ConstActualParameter ast, Object o) {
        String value = (String) ast.E.visit(this, null);
        
        if (o instanceof StringBuilder) {
            StringBuilder args = (StringBuilder) o;
            args.append("i32 ").append(value);
        }
        
        return value;
    }

    @Override
    public Object visitFuncActualParameter(FuncActualParameter ast, Object o) {
        StringBuilder args = (StringBuilder) o;
        String funcName = ast.I.spelling;
        
        if (!funcMap.containsKey(funcName)) {
            throw new RuntimeException("Function not found: " + funcName);
        }
        
        args.append("ptr @").append(funcName);
        return null;
    }

    @Override
    public Object visitProcActualParameter(ProcActualParameter ast, Object o) {
        StringBuilder args = (StringBuilder) o;
        String procName = ast.I.spelling;
        
        if (!funcMap.containsKey(procName)) {
            throw new RuntimeException("Procedure not found: " + procName);
        }
        
        args.append("ptr @").append(procName);
        return null;
    }

    @Override
    public Object visitVarActualParameter(VarActualParameter ast, Object o) {
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
        StringBuilder args = (StringBuilder) o;
        ast.AP.visit(this, args);
        args.append(", ");
        ast.APS.visit(this, args);
        return null;
    }

    @Override
    public Object visitSingleActualParameterSequence(SingleActualParameterSequence ast, Object o) {
        if (o instanceof StringBuilder) {
            StringBuilder args = (StringBuilder) o;
            ast.AP.visit(this, args);
            return null;
        } else {
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
        String basePtr = (String) ast.V.visit(this, o);
        
        // Determinar el nombre base del record
        String baseName = null;
        if (ast.V instanceof SimpleVname) {
            baseName = ((SimpleVname) ast.V).I.spelling;
        }
        
        String structType = null;
        if (baseName != null && varStructTypes.containsKey(baseName)) {
            structType = varStructTypes.get(baseName);
        }
        
        if (structType == null) {
            throw new RuntimeException("Cannot determine struct type for record field access");
        }
        
        String structName = structType.replace("%struct.", "");
        java.util.Map<String, Integer> fieldMap = structFieldIndices.get(structName);
        int fieldIndex = 0;
        if (fieldMap != null && fieldMap.containsKey(ast.I.spelling)) {
            fieldIndex = fieldMap.get(ast.I.spelling);
        }
        
        String fieldPtr = newTemp();
        emit(fieldPtr + " = getelementptr inbounds " + structType + ", ptr " + basePtr + ", i32 0, i32 " + fieldIndex);
        return fieldPtr;
    }

    @Override
    public Object visitSimpleVname(SimpleVname ast, Object o) {
        String varName = ast.I.spelling;
        
        if (varMap.containsKey(varName)) {
            String varPtr = varMap.get(varName);
            return varPtr;
        }
        
        throw new RuntimeException("Variable not found: " + varName);
    }

    /* Visit acceso a elemento de array (array[índice]) */
    @Override
    public Object visitSubscriptVname(SubscriptVname ast, Object o) {
        String basePtr = (String) ast.V.visit(this, o);  // Puntero al array
        String index = (String) ast.E.visit(this, null);  // Evaluar índice
        String varType = (String) ast.V.type.visit(this, o);
        
        // Extender índice de 32 a 64 bits
        String index64 = newTemp();
        emit(index64 + " = sext i32 " + index + " to i64");
        
        String elementPtr = newTemp();
        emit(elementPtr + " = getelementptr inbounds " + varType + ", ptr " + basePtr + ", i64 0, i64 " + index64);
        return elementPtr;
    }

    @Override
    public Object visitProgram(Program ast, Object o) {
        main.append("; Function Attrs: noinline nounwind optnone uwtable\n");
        main.append("define dso_local i32 @main() #0 {\n");
        main.append("entry:\n");
        main.append("  %1 = alloca i32, align 4\n");
        main.append("  store i32 0, ptr %1, align 4\n");
        
        ast.C.visit(this, o);
        
        emit("ret i32 0");
        main.append("}\n");
        
        main.append("\nattributes #0 = { noinline nounwind optnone uwtable \"frame-pointer\"=\"all\" ");
        main.append("\"min-legal-vector-width\"=\"0\" \"no-trapping-math\"=\"true\" ");
        main.append("\"stack-protector-buffer-size\"=\"8\" \"target-cpu\"=\"x86-64\" ");
        main.append("\"target-features\"=\"+cmov,+cx8,+fxsr,+mmx,+sse,+sse2,+x87\" \"tune-cpu\"=\"generic\" }\n");
        
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