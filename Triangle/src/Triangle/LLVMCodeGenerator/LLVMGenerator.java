package Triangle.LLVMCodeGenerator;

import Triangle.AbstractSyntaxTrees.*;
import Triangle.StdEnvironment;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LLVMGenerator implements Visitor {
    
    private StringBuilder output = new StringBuilder();
    private StringBuilder globals = new StringBuilder();
    private StringBuilder procedures = new StringBuilder();
    private StringBuilder procBody = new StringBuilder();
    private StringBuilder mainBody = new StringBuilder();
    
    private int tempCounter = 0;
    private int labelCounter = 0;
    private boolean usesPrintf = false;
    private boolean usesScanf = false;
    private boolean usesPutchar = false;
    private boolean inProcedure = false;
    
    private Map<String, String> localVars = new HashMap<>();
    private ArrayList<String> paramList = new ArrayList<>();
    private ArrayList<String> paramNames = new ArrayList<>();
    private Map<String, String> paramTypes = new HashMap<>();
    private Map<String, Map<String, Integer>> structFieldIndices = new HashMap<>();
    private Map<String, String> varStructTypes = new HashMap<>();
    private Map<TypeDenoter, String> typeDenoterToStructName = new HashMap<>();
    private String currentBlockLabel = "entry";
    
    // ==================== UTILITY METHODS ====================
    
    private String generateTemp() {
        return "%tmp" + (tempCounter++);
    }
    
    private String generateLabel(String base) {
        return base + (labelCounter++);
    }
    
    private String getCurrentBlockLabel() {
        return currentBlockLabel;
    }
    
    private void setCurrentBlockLabel(String label) {
        currentBlockLabel = label;
    }
    
    private String getAlign(String llvmType) {
        if (llvmType.startsWith("%struct.")) {
            return "8";
        }
        
        if (!llvmType.startsWith("[")) {
            return getScalarAlign(llvmType);
        }

        Pattern arrayPattern = Pattern.compile("\\[(\\d+) x (i\\d+)\\]");
        Matcher matcher = arrayPattern.matcher(llvmType);
        if (matcher.matches()) {
            int count = Integer.parseInt(matcher.group(1));
            String baseType = matcher.group(2);
            int baseAlign = Integer.parseInt(getScalarAlign(baseType));
            int totalAlign = baseAlign * count;
            int finalAlign = nextPowerOfTwo(Math.max(baseAlign, totalAlign));
            return Integer.toString(finalAlign);
        }

        return "4";
    }
    
    private String getScalarAlign(String type) {
        switch (type) {
            case "i1":
            case "i8": return "1";
            case "i16": return "2";
            case "i32": return "4";
            case "i64": return "8";
            default: return "4";
        }
    }
    
    private int nextPowerOfTwo(int n) {
        if (n <= 1) return 1;
        int power = 1;
        while (power < n) power *= 2;
        return power;
    }
    
    private int buildFieldIndexMap(Object fieldDenoter, Map<String, Integer> map, int idx) {
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
    
    // ==================== EMISSION METHODS ====================
    
    private void emitIODeclarations() {
        emitGlobal("@.str_int = private unnamed_addr constant [3 x i8] c\"%d\\00\", align 1");
        emitGlobal("@.str_int_space = private unnamed_addr constant [4 x i8] c\"%d \\00\", align 1");
        emitGlobal("@.str_char = private unnamed_addr constant [3 x i8] c\"%c\\00\", align 1");
        emitGlobal("@.str_scan_int = private unnamed_addr constant [3 x i8] c\"%d\\00\", align 1");
        emitGlobal("@.str_scan_char = private unnamed_addr constant [4 x i8] c\" %c\\00\", align 1");
        
        if (usesPrintf) {
            emitGlobal("declare i32 @printf(ptr, ...) #0");
        }
        if (usesScanf) {
            emitGlobal("declare i32 @scanf(ptr, ...) #0");
        }
        if (usesPutchar) {
            emitGlobal("declare i32 @putchar(i32) #0");
        }
        
        emitGlobal("");
        emitGlobal("attributes #0 = { nounwind }");
        emitGlobal("");
    }

    private void emitGlobal(String line) {
        globals.append(line).append("\n");
    }
    
    private void emitProcedure(String line) {
        procedures.append(line).append("\n");
    }
    
    public void emit(String line) {
        if (inProcedure) {
            emitProc(line);
        } else {
            if (line.endsWith(":")) {
                mainBody.append(line).append("\n");
            } else {
                mainBody.append("  ").append(line).append("\n");
            }
        }
    }
    
    public void emitRaw(String line) {
        if (inProcedure) {
            procBody.append(line).append("\n");
        } else {
            mainBody.append(line).append("\n");
        }
        
        if (line.endsWith(":")) {
            setCurrentBlockLabel(line.substring(0, line.length() - 1));
        }
    }
    
    public void emitComment(String comment) {
        if (inProcedure) {
            procBody.append("  ; ").append(comment).append("\n");
        } else {
            mainBody.append("  ; ").append(comment).append("\n");
        }
    }
    
    public void emitProc(String line) {
        if (line.endsWith(":")) {
            procBody.append(line).append("\n");
        } else {
            procBody.append("  ").append(line).append("\n");
        }
    }

    private void writeToFile(String filename) {
        try (PrintWriter writer = new PrintWriter(filename)) {
            writer.print(output.toString());
        } catch (IOException e) {
            System.err.println("Error writing .ll file: " + e);
        }
    }

    // ==================== MAIN GENERATION ====================
    
    public void generateRun(Program theAST, String triangleSourceFileName) {
        // Reset state
        output.setLength(0);
        globals.setLength(0);
        procedures.setLength(0);
        procBody.setLength(0);
        mainBody.setLength(0);
        tempCounter = 0;
        labelCounter = 0;
        usesPrintf = false;
        usesScanf = false;
        usesPutchar = false;
        inProcedure = false;
        localVars.clear();
        paramList.clear();
        paramNames.clear();
        paramTypes.clear();
        structFieldIndices.clear();
        varStructTypes.clear();
        typeDenoterToStructName.clear();
        currentBlockLabel = "entry";
        
        // Visit the AST
        theAST.visit(this, null);

        // Build final output
        output.append("; LLVM IR generated by Triangle compiler\n");
        output.append("; Source: ").append(triangleSourceFileName).append("\n\n");
        
        // Target information
        output.append("target triple = \"x86_64-pc-windows-msvc\"\n");
        output.append("target datalayout = \"e-m:w-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128\"\n\n");
        
        if (usesPrintf || usesScanf || usesPutchar) {
            emitIODeclarations();
        }
        
        output.append(globals.toString());
        
        if (procedures.length() > 0) {
            output.append("\n; ==================== PROCEDURES ====================\n\n");
            output.append(procedures.toString());
        }
        
        output.append("\n; ==================== MAIN ====================\n\n");
        output.append("define dso_local i32 @main() {\n");
        output.append("entry:\n");
        output.append(mainBody.toString());
        output.append("  ret i32 0\n");
        output.append("}\n");

        // Convert input name to .ll output
        String llvmFileName = triangleSourceFileName.replaceAll("\\.tri$", ".ll");
        writeToFile(llvmFileName);
        System.out.println("LLVM IR generated: " + llvmFileName);
    }
    
    // ==================== PROGRAM ====================
    
    @Override
    public Object visitProgram(Program ast, Object o) {
        ast.C.visit(this, o);
        return null;
    }
    
    // ==================== COMMANDS ====================
    
    public Object visitAssignCommand(AssignCommand ast, Object o) {
        Object rhsResult = ast.E.visit(this, o);
        String lhs = (String) ast.V.visit(this, o);
        
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

        if (rhsResult instanceof ArrayList) {
            ArrayList<String> elements = (ArrayList<String>) rhsResult;
            for (int i = 0; i < elements.size(); i++) {
                String element = elements.get(i);
                String[] parts = element.split(" ", 2);
                String elementType = parts[0];
                String elementValue = parts[1];

                String fieldPtr;
                if (i == 0) {
                    fieldPtr = lhs;
                } else {
                    fieldPtr = generateTemp();
                    emit(fieldPtr + " = getelementptr inbounds " + varType + ", ptr " + lhs + ", i32 0, i32 " + i);
                }
                emit("store " + elementType + " " + elementValue + ", ptr " + fieldPtr + ", align " + getAlign(elementType));
            }
        } else {
            String rhs = (String) rhsResult;
            if (varType == null) varType = "i32";
            emit("store " + varType + " " + rhs + ", ptr " + lhs + ", align " + getAlign(varType));
        }
        return null;
    }
    
    public Object visitCallCommand(CallCommand ast, Object o) {
        String procName = ast.I.spelling;
        
        switch (procName) {
            case "putint": {
                usesPrintf = true;
                String value = (String) ast.APS.visit(this, o);
                emit("call i32 (ptr, ...) @printf(ptr @.str_int, i32 " + value + ")");
                break;
            }
            case "put": {
                usesPrintf = true;
                String value = (String) ast.APS.visit(this, o);
                
                // Si es un registro temporal (i8), extender a i32
                if (value.startsWith("%")) {
                    String extended = generateTemp();
                    emit(extended + " = zext i8 " + value + " to i32");
                    value = extended;
                }
                
                emit("call i32 (ptr, ...) @printf(ptr @.str_char, i32 " + value + ")");
                break;
            }
            case "getint": {
                usesScanf = true;
                String address = (String) ast.APS.visit(this, o);
                emit("call i32 (ptr, ...) @scanf(ptr @.str_scan_int, ptr " + address + ")");
                break;
            }
            case "get": {
                usesScanf = true;
                String address = (String) ast.APS.visit(this, o);
                emit("call i32 (ptr, ...) @scanf(ptr @.str_scan_char, ptr " + address + ")");
                break;
            }
            case "puteol": {
                usesPutchar = true;
                emit("call i32 @putchar(i32 10)");
                break;
            }
            case "geteol": {
                // No hace nada en nuestra implementación
                break;
            }
            default: {
                ArrayList<String> args = new ArrayList<>();
                ast.APS.visit(this, args);
                
                String procReference;
                if (paramTypes.containsKey(procName)) {
                    procReference = "%" + procName;
                } else {
                    procReference = "@" + procName;
                }
                
                emit("call void " + procReference + "(" + String.join(", ", args) + ")");
                break;
            }
        }
        
        return null;
    }
    
    public Object visitEmptyCommand(EmptyCommand ast, Object o) {
        return null;
    }
    
    public Object visitIfCommand(IfCommand ast, Object o) {
        String thenLabel = generateLabel("then");
        String elseLabel = generateLabel("else");
        String endLabel = generateLabel("endif");
        
        String conditionResult = (String) ast.E.visit(this, o);
        
        emit("br i1 " + conditionResult + ", label %" + thenLabel + ", label %" + elseLabel);
        
        emitRaw("");
        emitRaw(thenLabel + ":");
        ast.C1.visit(this, o);
        emit("br label %" + endLabel);
        
        emitRaw("");
        emitRaw(elseLabel + ":");
        ast.C2.visit(this, o);
        emit("br label %" + endLabel);
        
        emitRaw("");
        emitRaw(endLabel + ":");
        
        return null;
    }
    
    public Object visitLetCommand(LetCommand ast, Object o) {
        ast.D.visit(this, o);
        ast.C.visit(this, o);
        return null;
    }
    
    public Object visitSequentialCommand(SequentialCommand ast, Object o) {
        ast.C1.visit(this, o);
        ast.C2.visit(this, o);
        return null;
    }
    
    public Object visitWhileCommand(WhileCommand ast, Object o) {
        String condLabel = generateLabel("loop_cond");
        String bodyLabel = generateLabel("loop_body");
        String endLabel = generateLabel("loop_end");
        
        emit("br label %" + condLabel);
        
        emitRaw("");
        emitRaw(condLabel + ":");
        String condResult = (String) ast.E.visit(this, o);
        emit("br i1 " + condResult + ", label %" + bodyLabel + ", label %" + endLabel);
        
        emitRaw("");
        emitRaw(bodyLabel + ":");
        ast.C.visit(this, o);
        emit("br label %" + condLabel);
        
        emitRaw("");
        emitRaw(endLabel + ":");
        
        return null;
    }
    
    // ==================== EXPRESSIONS ====================
    
    public Object visitArrayExpression(ArrayExpression ast, Object o) {
        boolean isConst = o instanceof Boolean && (Boolean) o;
        
        ArrayList<String> elems = new ArrayList<>();
        ast.AA.visit(this, elems);
        
        if (isConst) {
            return "[" + String.join(", ", elems) + "]";
        } else {
            return elems;
        }
    }
    
    public Object visitBinaryExpression(BinaryExpression ast, Object o) {
        boolean isConst = o instanceof Boolean && (Boolean) o;

        String left = (String) ast.E1.visit(this, o);
        String right = (String) ast.E2.visit(this, o);

        if (isConst) {
            try {
                int leftVal = Integer.parseInt(left);
                int rightVal = Integer.parseInt(right);
                int result;

                switch (ast.O.spelling) {
                    case "+": result = leftVal + rightVal; break;
                    case "-": result = leftVal - rightVal; break;
                    case "*": result = leftVal * rightVal; break;
                    case "/": 
                        if (rightVal == 0) throw new RuntimeException("Division by zero");
                        result = leftVal / rightVal; 
                        break;
                    case "//": 
                        if (rightVal <= 0) throw new RuntimeException("Modulo by non-positive");
                        result = leftVal % rightVal; 
                        break;
                    case ">": result = leftVal > rightVal ? 1 : 0; break;
                    case "<": result = leftVal < rightVal ? 1 : 0; break;
                    case ">=": result = leftVal >= rightVal ? 1 : 0; break;
                    case "<=": result = leftVal <= rightVal ? 1 : 0; break;
                    case "=": result = leftVal == rightVal ? 1 : 0; break;
                    case "\\=": result = leftVal != rightVal ? 1 : 0; break;
                    case "/\\": result = (leftVal != 0 && rightVal != 0) ? 1 : 0; break;
                    case "\\/": result = (leftVal != 0 || rightVal != 0) ? 1 : 0; break;
                    default:
                        throw new RuntimeException("Unsupported operator: " + ast.O.spelling);
                }

                return String.valueOf(result);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Non-constant operand in constant expression");
            }
        }

        String tmp = generateTemp();
        String op = ast.O.spelling;

        switch(op) {
            case "+": emit(tmp + " = add nsw i32 " + left + ", " + right); break;
            case "-": emit(tmp + " = sub nsw i32 " + left + ", " + right); break;
            case "*": emit(tmp + " = mul nsw i32 " + left + ", " + right); break;
            case "/": emit(tmp + " = sdiv i32 " + left + ", " + right); break;
            case "//": emit(tmp + " = srem i32 " + left + ", " + right); break;
            case ">": emit(tmp + " = icmp sgt i32 " + left + ", " + right); break;
            case "<": emit(tmp + " = icmp slt i32 " + left + ", " + right); break;
            case ">=": emit(tmp + " = icmp sge i32 " + left + ", " + right); break;
            case "<=": emit(tmp + " = icmp sle i32 " + left + ", " + right); break;
            case "=": emit(tmp + " = icmp eq i32 " + left + ", " + right); break;
            case "\\=": emit(tmp + " = icmp ne i32 " + left + ", " + right); break;
            
            case "/\\": { // AND lógico
                String leftBool = left;
                String rightBool = right;
                
                // Convertir a booleano si es necesario
                if (ast.E1.type instanceof IntTypeDenoter) {
                    String tmpLeft = generateTemp();
                    emit(tmpLeft + " = icmp ne i32 " + left + ", 0");
                    leftBool = tmpLeft;
                }
                
                if (ast.E2.type instanceof IntTypeDenoter) {
                    String tmpRight = generateTemp();
                    emit(tmpRight + " = icmp ne i32 " + right + ", 0");
                    rightBool = tmpRight;
                }
                
                emit(tmp + " = and i1 " + leftBool + ", " + rightBool);
                break;
            }
            
            case "\\/": { // OR lógico
                String leftBool = left;
                String rightBool = right;
                
                if (ast.E1.type instanceof IntTypeDenoter) {
                    String tmpLeft = generateTemp();
                    emit(tmpLeft + " = icmp ne i32 " + left + ", 0");
                    leftBool = tmpLeft;
                }
                
                if (ast.E2.type instanceof IntTypeDenoter) {
                    String tmpRight = generateTemp();
                    emit(tmpRight + " = icmp ne i32 " + right + ", 0");
                    rightBool = tmpRight;
                }
                
                emit(tmp + " = or i1 " + leftBool + ", " + rightBool);
                break;
            }
            
            default:
                throw new RuntimeException("Unsupported operator: " + op);
        }

        return tmp;
    }
    
    public Object visitCallExpression(CallExpression ast, Object o) {
        String funcName = ast.I.spelling;
        String tmp = generateTemp();
        
        ArrayList<String> args = new ArrayList<>();
        
        if (ast.APS != null) {
            ast.APS.visit(this, args);
        }
        
        String returnType = (String) ast.type.visit(this, null);
        if (returnType == null) returnType = "i32";
        
        String funcReference;
        if (paramTypes.containsKey(funcName)) {
            funcReference = "%" + funcName;
        } else {
            funcReference = "@" + funcName;
        }
        
        emit(tmp + " = call " + returnType + " " + funcReference + "(" + String.join(", ", args) + ")");
        
        return tmp;
    }
    
    public Object visitCharacterExpression(CharacterExpression ast, Object o) {
        String literal = ast.CL.spelling;
        char c;
        
        if (literal.length() == 3 && literal.startsWith("'") && literal.endsWith("'")) {
            c = literal.charAt(1);
        } else {
            c = literal.charAt(0);
        }
        
        int ascii = (int) c;
        String value = String.valueOf(ascii);
        
        if (o instanceof ArrayList) {
            // Caracteres son i8 en LLVM
            ((ArrayList<String>) o).add("i8 " + value);
        }
        
        return value;
    }
    
    public Object visitEmptyExpression(EmptyExpression ast, Object o) {
        return null;
    }
    
    public Object visitIfExpression(IfExpression ast, Object o) {
        String cond = (String) ast.E1.visit(this, o);
        
        String thenLabel = generateLabel("then");
        String elseLabel = generateLabel("else");
        String endLabel  = generateLabel("ifend");
        
        emit("br i1 " + cond + ", label %" + thenLabel + ", label %" + elseLabel);
        
        emitRaw(thenLabel + ":");
        String thenVal = (String) ast.E2.visit(this, o);
        String actualThenBlock = getCurrentBlockLabel();
        emit("br label %" + endLabel);
        
        emitRaw(elseLabel + ":");
        String elseVal = (String) ast.E3.visit(this, o);
        String actualElseBlock = getCurrentBlockLabel();
        emit("br label %" + endLabel);
        
        emitRaw(endLabel + ":");
        String result = generateTemp();
        emit(result + " = phi i32 [" + thenVal + ", %" + actualThenBlock + "], [" + elseVal + ", %" + actualElseBlock + "]");

        return result;
    }
    
    public Object visitIntegerExpression(IntegerExpression ast, Object o) {
        String value = ast.IL.spelling;
        
        if (o instanceof ArrayList) {
            ((ArrayList<String>) o).add("i32 " + value);
        }
        
        return value;
    }
    
    public Object visitLetExpression(LetExpression ast, Object o) {
        ast.D.visit(this, o);
        return ast.E.visit(this, o);
    }
    
    public Object visitRecordExpression(RecordExpression ast, Object o) {
        ArrayList<String> elements = new ArrayList<>();
        ast.RA.visit(this, elements);
        return elements;
    }
    
    public Object visitUnaryExpression(UnaryExpression ast, Object o) {
        boolean isConst = o instanceof Boolean && (Boolean) o;
        String operand = (String) ast.E.visit(this, o);
        String op = ast.O.spelling;

        if (isConst) {
            try {
                int operandVal = Integer.parseInt(operand);
                int result;

                switch (op) {
                    case "\\":
                        result = operandVal == 0 ? 1 : 0;
                        break;
                    case "-":
                        result = -operandVal;
                        break;
                    default:
                        throw new RuntimeException("Unsupported unary operator: " + op);
                }

                return String.valueOf(result);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Non-constant operand");
            }
        }

        String tmp = generateTemp();
        switch (op) {
            case "\\":
                emit(tmp + " = xor i1 " + operand + ", true");
                break;
            case "-":
                emit(tmp + " = sub nsw i32 0, " + operand);
                break;
            default:
                throw new RuntimeException("Unsupported unary operator: " + op);
        }
        return tmp;
    }
    
    public Object visitVnameExpression(VnameExpression ast, Object o) {
        if (ast.V instanceof SimpleVname) {
            String name = ((SimpleVname) ast.V).I.spelling;
            if (name.equals("true")) {
                return "1";
            } else if (name.equals("false")) {
                return "0";
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
        
        String tmp = generateTemp();
        if (varType == null) varType = "i32";
        emit(tmp + " = load " + varType + ", ptr " + var + ", align " + getAlign(varType));
        return tmp;
    }
    
    // ==================== DECLARATIONS ====================
    
    public Object visitBinaryOperatorDeclaration(BinaryOperatorDeclaration ast, Object o) {
        return null;
    }
    
    public Object visitConstDeclaration(ConstDeclaration ast, Object o) {
        String varName = ast.I.spelling;
        String varPtr = "%" + varName;
        
        // Obtener tipo de la expresión
        String exprType = (String) ast.E.type.visit(this, o);
        if (exprType == null) exprType = "i32";
        
        // Allocate space on stack
        emit(varPtr + " = alloca " + exprType + ", align " + getAlign(exprType));
        
        // Evaluate and store initial value
        String value = (String) ast.E.visit(this, null);
        emit("store " + exprType + " " + value + ", ptr " + varPtr + ", align " + getAlign(exprType));
        
        localVars.put(varName, varPtr);
        return null;
    }
    
    public Object visitFuncDeclaration(FuncDeclaration ast, Object o) {
        String funcName = ast.I.spelling;
        
        procBody.setLength(0);
        localVars.clear();
        paramTypes.clear();
        paramList.clear();
        paramNames.clear();
        inProcedure = true;
        
        if (ast.FPS != null) {
            ast.FPS.visit(this, null);
        }
        
        String funcType = (String) ast.T.visit(this, o);
        if (funcType == null) funcType = "i32";
        
        StringBuilder funcDecl = new StringBuilder();
        funcDecl.append("define dso_local ").append(funcType).append(" @").append(funcName);
        funcDecl.append("(").append(String.join(", ", paramList)).append(") {\n");
        funcDecl.append("entry:\n");
        
        for (String name : paramNames) {
            String reg = "%" + name;
            String addr = localVars.get(name);
            String type = paramTypes.get(name);
            if (!addr.equals(reg)) {
                funcDecl.append("  ").append(addr).append(" = alloca ").append(type).append(", align ").append(getAlign(type)).append("\n");
                funcDecl.append("  store ").append(type).append(" ").append(reg).append(", ptr ").append(addr).append(", align ").append(getAlign(type)).append("\n");
            }
        }
        
        String resultReg = (String) ast.E.visit(this, o);
        
        procBody.append("  ret ").append(funcType).append(" ").append(resultReg).append("\n");
        
        funcDecl.append(procBody.toString());
        funcDecl.append("}\n");
    
        emitProcedure(funcDecl.toString());

        inProcedure = false;
        
        return null;
    }
    
    public Object visitProcDeclaration(ProcDeclaration ast, Object o) {
        String procName = ast.I.spelling;
        
        procBody.setLength(0);
        localVars.clear();
        paramTypes.clear();
        paramList.clear();
        paramNames.clear();
        inProcedure = true;
        
        if (ast.FPS != null) {
            ast.FPS.visit(this, null);
        }
        
        StringBuilder procDecl = new StringBuilder();
        procDecl.append("define dso_local void @").append(procName);
        procDecl.append("(").append(String.join(", ", paramList)).append(") {\n");
        procDecl.append("entry:\n");
        
        for (String name : paramNames) {
            String reg = "%" + name;
            String addr = localVars.get(name);
            String type = paramTypes.get(name);
            if (!addr.equals(reg)) {
                procDecl.append("  ").append(addr).append(" = alloca ").append(type).append(", align ").append(getAlign(type)).append("\n");
                procDecl.append("  store ").append(type).append(" ").append(reg).append(", ptr ").append(addr).append(", align ").append(getAlign(type)).append("\n");
            }
        }
        
        ast.C.visit(this, o);
        
        procBody.append("  ret void\n");
        
        procDecl.append(procBody.toString());
        procDecl.append("}\n");
    
        emitProcedure(procDecl.toString());

        inProcedure = false;
        return null;
    }
    
    public Object visitSequentialDeclaration(SequentialDeclaration ast, Object o) {
        ast.D1.visit(this, o);
        ast.D2.visit(this, o);
        return null;
    }
    
    public Object visitTypeDeclaration(TypeDeclaration ast, Object o) {
        if (ast.T instanceof RecordTypeDenoter) {
            String typeName = ast.I.spelling;
            Map<String, Integer> fieldMap = new LinkedHashMap<>();

            RecordTypeDenoter recordType = (RecordTypeDenoter) ast.T;
            buildFieldIndexMap(recordType.FT, fieldMap, 0);

            structFieldIndices.put(typeName, fieldMap);
            typeDenoterToStructName.put(ast.T, typeName);

            String structDef = (String) ast.T.visit(this, o);
            emitGlobal("%struct." + typeName + " = type { " + structDef + " }");
            return "%struct." + typeName;
        }
        return ast.T.visit(this, o);
    }
    
    public Object visitUnaryOperatorDeclaration(UnaryOperatorDeclaration ast, Object o) {
        return null;
    }
    
    public Object visitVarDeclaration(VarDeclaration ast, Object o) {
        String varName = ast.I.spelling;
        String varPtr = "%" + varName;
        
        // Obtener el tipo correcto del AST
        String varType = (String) ast.T.visit(this, o);
        if (varType == null) varType = "i32";
        
        // Allocate local variable
        emit(varPtr + " = alloca " + varType + ", align " + getAlign(varType));
        
        // Inicializar según el tipo
        if (varType.equals("i1")) {
            emit("store i1 false, ptr " + varPtr + ", align 1");
        } else if (varType.equals("i8")) {
            emit("store i8 0, ptr " + varPtr + ", align 1");
        } else if (varType.startsWith("[")) {
            // Arrays no necesitan inicialización explícita
        } else if (varType.startsWith("%struct.")) {
            // Structs no necesitan inicialización explícita
        } else {
            // i32 y otros tipos numéricos
            emit("store " + varType + " 0, ptr " + varPtr + ", align " + getAlign(varType));
        }
        
        localVars.put(varName, varPtr);
        return null;
    }
    
    // ==================== PARAMETERS ====================
    
    public Object visitConstActualParameter(ConstActualParameter ast, Object o) {
        String value = (String) ast.E.visit(this, null);
        
        if (o instanceof ArrayList) {
            ArrayList<String> argList = (ArrayList<String>) o;
            
            String argType;
            if (ast.E.type instanceof RecordTypeDenoter) {
                String typeName = typeDenoterToStructName.get(ast.E.type);
                if (typeName != null) {
                    argType = "%struct." + typeName;
                } else {
                    argType = (String) ast.E.type.visit(this, null);
                }
            } else {
                argType = (String) ast.E.type.visit(this, null);
            }
            
            argList.add(argType + " " + value);
            return null;
        } else {
            return value;
        }
    }
    
    public Object visitConstFormalParameter(ConstFormalParameter ast, Object o) {
        String name = ast.I.spelling;
        
        String llvmType;
        if (ast.T instanceof RecordTypeDenoter) {
            String typeName = typeDenoterToStructName.get(ast.T);
            if (typeName != null) {
                String structType = "%struct." + typeName;
                llvmType = structType;
                varStructTypes.put(name, structType);
            } else {
                llvmType = (String) ast.T.visit(this, o);
            }
        } else {
            llvmType = (String) ast.T.visit(this, o);
        }
        
        String alloca = "%" + name + ".addr";

        paramList.add(llvmType + " %" + name);
        paramNames.add(name);
        paramTypes.put(name, llvmType);
        localVars.put(name, alloca);
        
        return null;
    }
    
    public Object visitEmptyActualParameterSequence(EmptyActualParameterSequence ast, Object o) {
        return null;
    }
    
    public Object visitEmptyFormalParameterSequence(EmptyFormalParameterSequence ast, Object arg) {
        return null;
    }
    
    public Object visitFuncActualParameter(FuncActualParameter ast, Object o) {
        String funcName = ast.I.spelling;
        
        String funcType = "i32 (i32)*";
        String funcArg = funcType + " @" + funcName;
        
        if (o instanceof ArrayList) {
            ((ArrayList<String>) o).add(funcArg);
        }
        
        return funcArg;
    }
    
    public Object visitFuncFormalParameter(FuncFormalParameter ast, Object o) {
        String funcType = "i32 (i32)*";
        String name = ast.I.spelling;

        paramTypes.put(name, funcType);
        paramList.add(funcType + " %" + name);
        paramNames.add(name);
        localVars.put(name, "%" + name);

        return null;
    }
    
    public Object visitMultipleActualParameterSequence(MultipleActualParameterSequence ast, Object o) {
        ast.AP.visit(this, o);
        ast.APS.visit(this, o);
        return null;
    }
    
    public Object visitMultipleFormalParameterSequence(MultipleFormalParameterSequence ast, Object arg) {
        ast.FP.visit(this, null);
        ast.FPS.visit(this, null);
        return null;
    }
    
    public Object visitProcActualParameter(ProcActualParameter ast, Object o) {
        String procName = ast.I.spelling;
        String procType = "void ()*";
        
        if (o instanceof ArrayList) {
            ((ArrayList<String>) o).add(procType + " @" + procName);
        }
        
        return "@" + procName;
    }
    
    public Object visitProcFormalParameter(ProcFormalParameter ast, Object o) {
        String procType = "void ()*";
        String name = ast.I.spelling;
        
        paramTypes.put(name, procType);
        paramList.add(procType + " %" + name);
        paramNames.add(name);
        localVars.put(name, "%" + name);
        
        return null;
    }
    
    public Object visitSingleActualParameterSequence(SingleActualParameterSequence ast, Object o) {
        return ast.AP.visit(this, o);
    }
    
    public Object visitSingleFormalParameterSequence(SingleFormalParameterSequence ast, Object arg) {
        return ast.FP.visit(this, null);
    }
    
    public Object visitVarActualParameter(VarActualParameter ast, Object o) {
        String addr = (String) ast.V.visit(this, null);

        if (o instanceof ArrayList) {
            ArrayList<String> argList = (ArrayList<String>) o;
            argList.add("ptr " + addr);
            return null;
        } else {
            return addr;
        }
    }
    
    public Object visitVarFormalParameter(VarFormalParameter ast, Object o) {
        String name = ast.I.spelling;
        paramList.add("ptr %" + name);
        paramNames.add(name);
        localVars.put(name, "%" + name);
        
        if (ast.T instanceof RecordTypeDenoter) {
            String typeName = typeDenoterToStructName.get(ast.T);
            if (typeName != null) {
                String llvmType = "%struct." + typeName;
                varStructTypes.put(name, llvmType);
            }
        }
        
        return null;
    }
    
    // ==================== TYPE DENOTERS ====================
    
    public Object visitAnyTypeDenoter(AnyTypeDenoter ast, Object o) {
        return "i32";
    }
    
    public Object visitArrayTypeDenoter(ArrayTypeDenoter ast, Object o) {
        int size = Integer.parseInt(ast.IL.spelling);
        String elementType = (String) ast.T.visit(this, o);
        return "[" + size + " x " + elementType + "]";
    }
    
    public Object visitBoolTypeDenoter(BoolTypeDenoter ast, Object o) {
        return "i1";
    }
    
    public Object visitCharTypeDenoter(CharTypeDenoter ast, Object o) {
        return "i8";
    }
    
    public Object visitErrorTypeDenoter(ErrorTypeDenoter ast, Object o) {
        return "i32";
    }
    
    public Object visitIntTypeDenoter(IntTypeDenoter ast, Object o) {
        return "i32";
    }
    
    public Object visitMultipleFieldTypeDenoter(MultipleFieldTypeDenoter ast, Object o) {
        String left = (String) ast.T.visit(this, o);
        String right = (String) ast.FT.visit(this, o);
        return left + ", " + right;
    }
    
    public Object visitRecordTypeDenoter(RecordTypeDenoter ast, Object o) {
        return ast.FT.visit(this, o);
    }
    
    public Object visitSimpleTypeDenoter(SimpleTypeDenoter ast, Object o) {
        if (ast.I.spelling.equals("Integer")) return "i32";
        if (ast.I.spelling.equals("Char")) return "i8";
        if (ast.I.spelling.equals("Boolean")) return "i1";
        return "i32";
    }
    
    public Object visitSingleFieldTypeDenoter(SingleFieldTypeDenoter ast, Object o) {
        String type = (String) ast.T.visit(this, o);
        return type;
    }
    
    // ==================== AGGREGATES ====================
    
    public Object visitMultipleArrayAggregate(MultipleArrayAggregate ast, Object o) {
        ArrayList<String> elements = (ArrayList<String>) o;

        boolean isConst = (o instanceof Boolean && (Boolean) o);
        String type = (String) ast.E.type.visit(this, isConst);
        Object valueResult = ast.E.visit(this, isConst);
        
        String value;
        if (valueResult instanceof ArrayList) {
            ArrayList<String> nestedElements = (ArrayList<String>) valueResult;
            value = "[" + String.join(", ", nestedElements) + "]";
        } else {
            value = (String) valueResult;
        }

        elements.add(type + " " + value);
        ast.AA.visit(this, o);
        
        return null;
    }
    
    public Object visitMultipleRecordAggregate(MultipleRecordAggregate ast, Object o) {
        ArrayList<String> elements = (ArrayList<String>) o;
        String value = (String) ast.E.visit(this, null);
        String type = (String) ast.E.type.visit(this, null);
        elements.add(type + " " + value);
        ast.RA.visit(this, elements);
        return null;
    }
    
    public Object visitSingleArrayAggregate(SingleArrayAggregate ast, Object o) {
        ArrayList<String> elements = (ArrayList<String>) o;

        boolean isConst = (o instanceof Boolean && (Boolean) o);
        String type = (String) ast.E.type.visit(this, isConst);
        Object valueResult = ast.E.visit(this, isConst);
        
        String value;
        if (valueResult instanceof ArrayList) {
            ArrayList<String> nestedElements = (ArrayList<String>) valueResult;
            value = "[" + String.join(", ", nestedElements) + "]";
        } else {
            value = (String) valueResult;
        }

        elements.add(type + " " + value);
        return null;
    }
    
    public Object visitSingleRecordAggregate(SingleRecordAggregate ast, Object o) {
        ArrayList<String> elements = (ArrayList<String>) o;
        String value = (String) ast.E.visit(this, null);
        String type = (String) ast.E.type.visit(this, null);
        elements.add(type + " " + value);
        return null;
    }
    
    // ==================== VNAMES ====================
    
    public Object visitDotVname(DotVname ast, Object o) {
        String basePtr = (String) ast.V.visit(this, o);

        String baseName = null;
        if (ast.V instanceof SimpleVname) {
            baseName = ((SimpleVname) ast.V).I.spelling;
        }

        String structType = null;
        if (baseName != null && varStructTypes.containsKey(baseName)) {
            structType = varStructTypes.get(baseName);
        }

        if (structType == null) {
            throw new RuntimeException("Cannot determine struct type for: " + baseName);
        }

        String structName = structType.replace("%struct.", "");
        Map<String, Integer> fieldMap = structFieldIndices.get(structName);
        int fieldIndex = 0;
        if (fieldMap != null && fieldMap.containsKey(ast.I.spelling)) {
            fieldIndex = fieldMap.get(ast.I.spelling);
        }

        String fieldPtr = generateTemp();
        emit(fieldPtr + " = getelementptr inbounds " + structType + ", ptr " + basePtr + ", i32 0, i32 " + fieldIndex);
        return fieldPtr;
    }
    
    public Object visitSimpleVname(SimpleVname ast, Object o) {
        String name = ast.I.spelling;

        if (localVars.containsKey(name)) {
            return localVars.get(name);
        } else {
            return "@" + name;
        }
    }
    
    public Object visitSubscriptVname(SubscriptVname ast, Object o) {
        String basePtr = (String) ast.V.visit(this, o);
        String index = (String) ast.E.visit(this, o);
        String varType = (String) ast.V.type.visit(this, o);
        
        String index64 = generateTemp();
        emit(index64 + " = sext i32 " + index + " to i64");
        
        String elementPtr = generateTemp();
        emit(elementPtr + " = getelementptr inbounds " + varType + ", ptr " + basePtr + ", i64 0, i64 " + index64);
        return elementPtr;
    }
    
    // ==================== LITERALS, IDENTIFIERS, OPERATORS ====================
    
    public Object visitCharacterLiteral(CharacterLiteral ast, Object o) {
        return ast.spelling;
    }
    
    public Object visitIdentifier(Identifier ast, Object o) {
        return ast.spelling;
    }
    
    public Object visitIntegerLiteral(IntegerLiteral ast, Object o) {
        return ast.spelling;
    }
    
    public Object visitOperator(Operator ast, Object o) {
        return null;
    }
}