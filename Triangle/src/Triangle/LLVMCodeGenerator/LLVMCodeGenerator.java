package Triangle.LLVMCodeGenerator;

import Triangle.AbstractSyntaxTrees.*;
import Triangle.StdEnvironment;
import java.io.IOException;
import java.util.*;

/**
 * Generador de código LLVM usando arquitectura por capas
 * Esta implementación separa responsabilidades y usa objetos inmutables
 */
public class LLVMCodeGenerator implements Visitor {
    
    private final LLVMModule module;
    private LLVMInstructionBuilder builder;
    private LLVMContext context;
    private LLVMFunction currentFunction;
    private LLVMBasicBlock currentBlock;
    
    private int stringLiteralCounter = 0;
    private final Map<String, LLVMType.StructType> recordTypes;
    
    public LLVMCodeGenerator(String moduleName) {
        this.module = new LLVMModule.Builder(moduleName)
            .withStandardDeclarations(true)
            .build();
        this.builder = new LLVMInstructionBuilder();
        this.context = LLVMContext.createWithStandardTypes();
        this.recordTypes = new HashMap<>();
    }
    
    /**
     * Punto de entrada principal para generar código
     */
    public String generateCode(Program program) {
        program.visit(this, null);
        return module.toIR();
    }
    
    /**
     * Guarda el código generado a un archivo
     */
    public void saveToFile(String filename) throws IOException {
        module.saveToFile(filename);
    }
    
    // ==================== VISITOR METHODS - PROGRAM ====================
    
    @Override
    public Object visitProgram(Program ast, Object o) {
        // Crear función main
        currentFunction = new LLVMFunction.Builder("main")
            .returnType(LLVMType.i32())
            .build();
        
        currentBlock = new LLVMBasicBlock("entry");
        builder.reset();
        
        // Visitar el comando principal
        ast.C.visit(this, o);
        
        // Añadir return 0 si no está terminado
        if (!currentBlock.isTerminated()) {
            builder.ret(new LLVMValue.ConstantInt(0));
            currentBlock.terminate();
        }
        
        currentBlock.addInstructions(builder.getInstructions());
        currentFunction.addBasicBlock(currentBlock);
        module.addFunction(currentFunction);
        
        return null;
    }
    
    // ==================== VISITOR METHODS - COMMANDS ====================
    
    @Override
    public Object visitAssignCommand(AssignCommand ast, Object o) {
        // Evaluar el lado derecho (expresión)
        LLVMValue rhs = (LLVMValue) ast.E.visit(this, o);
        
        // Obtener el puntero del lado izquierdo (vname)
        LLVMValue lhs = (LLVMValue) ast.V.visit(this, o);
        
        // Generar store
        builder.store(rhs, lhs);
        
        return null;
    }
    
    @Override
    public Object visitCallCommand(CallCommand ast, Object o) {
        String funcName = ast.I.spelling;
        
        // Evaluar los argumentos
        List<LLVMValue> args = new ArrayList<>();
        ast.APS.visit(this, args);
        
        // Llamar a la función
        builder.callVoid(funcName, args);
        
        return null;
    }
    
    @Override
    public Object visitEmptyCommand(EmptyCommand ast, Object o) {
        // No hacer nada
        return null;
    }
    
    @Override
    public Object visitIfCommand(IfCommand ast, Object o) {
        // Evaluar la condición
        LLVMValue condition = (LLVMValue) ast.E.visit(this, o);
        
        // Crear etiquetas para los bloques
        LLVMValue.Label thenLabel = builder.createLabel("if.then");
        LLVMValue.Label elseLabel = builder.createLabel("if.else");
        LLVMValue.Label endLabel = builder.createLabel("if.end");
        
        // Branch condicional
        builder.brCond(condition, thenLabel, elseLabel);
        currentBlock.addInstructions(builder.getInstructions());
        currentBlock.terminate();
        currentFunction.addBasicBlock(currentBlock);
        
        // Bloque then
        builder.reset();
        currentBlock = new LLVMBasicBlock(thenLabel.getName());
        ast.C1.visit(this, o);
        if (!currentBlock.isTerminated()) {
            builder.br(endLabel);
            currentBlock.terminate();
        }
        currentBlock.addInstructions(builder.getInstructions());
        currentFunction.addBasicBlock(currentBlock);
        
        // Bloque else
        builder.reset();
        currentBlock = new LLVMBasicBlock(elseLabel.getName());
        ast.C2.visit(this, o);
        if (!currentBlock.isTerminated()) {
            builder.br(endLabel);
            currentBlock.terminate();
        }
        currentBlock.addInstructions(builder.getInstructions());
        currentFunction.addBasicBlock(currentBlock);
        
        // Bloque end (continuación)
        builder.reset();
        currentBlock = new LLVMBasicBlock(endLabel.getName());
        
        return null;
    }
    
    @Override
    public Object visitLetCommand(LetCommand ast, Object o) {
        // Entrar a un nuevo scope
        context = context.enterScope();
        
        // Visitar las declaraciones
        ast.D.visit(this, o);
        
        // Visitar el comando
        ast.C.visit(this, o);
        
        // Salir del scope
        context = context.exitScope();
        
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
        // Crear etiquetas
        LLVMValue.Label condLabel = builder.createLabel("while.cond");
        LLVMValue.Label bodyLabel = builder.createLabel("while.body");
        LLVMValue.Label endLabel = builder.createLabel("while.end");
        
        // Branch al bloque de condición
        builder.br(condLabel);
        currentBlock.addInstructions(builder.getInstructions());
        currentBlock.terminate();
        currentFunction.addBasicBlock(currentBlock);
        
        // Bloque de condición
        builder.reset();
        currentBlock = new LLVMBasicBlock(condLabel.getName());
        LLVMValue condition = (LLVMValue) ast.E.visit(this, o);
        builder.brCond(condition, bodyLabel, endLabel);
        currentBlock.addInstructions(builder.getInstructions());
        currentBlock.terminate();
        currentFunction.addBasicBlock(currentBlock);
        
        // Bloque del cuerpo
        builder.reset();
        currentBlock = new LLVMBasicBlock(bodyLabel.getName());
        ast.C.visit(this, o);
        if (!currentBlock.isTerminated()) {
            builder.br(condLabel);
            currentBlock.terminate();
        }
        currentBlock.addInstructions(builder.getInstructions());
        currentFunction.addBasicBlock(currentBlock);
        
        // Bloque end (continuación)
        builder.reset();
        currentBlock = new LLVMBasicBlock(endLabel.getName());
        
        return null;
    }
    
    // ==================== VISITOR METHODS - EXPRESSIONS ====================
    
    @Override
    public Object visitArrayExpression(ArrayExpression ast, Object o) {
        // TODO: Implementar acceso a arrays
        throw new UnsupportedOperationException("Array expressions not yet implemented");
    }
    
    @Override
    public Object visitBinaryExpression(BinaryExpression ast, Object o) {
        LLVMValue left = (LLVMValue) ast.E1.visit(this, o);
        LLVMValue right = (LLVMValue) ast.E2.visit(this, o);
        
        String op = ast.O.spelling;
        
        switch (op) {
            // Operadores aritméticos
            case "+":
                return builder.add(left, right);
            case "-":
                return builder.sub(left, right);
            case "*":
                return builder.mul(left, right);
            case "/":
                return builder.sdiv(left, right);
            case "%":
                return builder.srem(left, right);
            
            // Operadores de comparación
            case "=":
            case "==":
                return builder.icmpEq(left, right);
            case "!=":
            case "\\=":
                return builder.icmpNe(left, right);
            case "<":
                return builder.icmpSlt(left, right);
            case "<=":
                return builder.icmpSle(left, right);
            case ">":
                return builder.icmpSgt(left, right);
            case ">=":
                return builder.icmpSge(left, right);
            
            // Operadores lógicos
            case "\\/":
            case "||":
                return builder.or(left, right);
            case "/\\":
            case "&&":
                return builder.and(left, right);
            
            default:
                throw new UnsupportedOperationException("Operator not implemented: " + op);
        }
    }
    
    @Override
    public Object visitCallExpression(CallExpression ast, Object o) {
        String funcName = ast.I.spelling;
        
        // Evaluar los argumentos
        List<LLVMValue> args = new ArrayList<>();
        ast.APS.visit(this, args);
        
        // Determinar el tipo de retorno (por ahora asumimos i32)
        LLVMType returnType = LLVMType.i32();
        
        // Llamar a la función
        return builder.call(returnType, funcName, args);
    }
    
    @Override
    public Object visitCharacterExpression(CharacterExpression ast, Object o) {
        char value = ast.CL.spelling.charAt(0);
        return new LLVMValue.ConstantChar(value);
    }
    
    @Override
    public Object visitEmptyExpression(EmptyExpression ast, Object o) {
        return new LLVMValue.ConstantInt(0);
    }
    
    @Override
    public Object visitIfExpression(IfExpression ast, Object o) {
        // Evaluar la condición
        LLVMValue condition = (LLVMValue) ast.E1.visit(this, o);
        
        // Crear etiquetas
        LLVMValue.Label thenLabel = builder.createLabel("ifexpr.then");
        LLVMValue.Label elseLabel = builder.createLabel("ifexpr.else");
        LLVMValue.Label endLabel = builder.createLabel("ifexpr.end");
        
        // Branch condicional
        builder.brCond(condition, thenLabel, elseLabel);
        currentBlock.addInstructions(builder.getInstructions());
        currentBlock.terminate();
        currentFunction.addBasicBlock(currentBlock);
        
        // Bloque then
        builder.reset();
        currentBlock = new LLVMBasicBlock(thenLabel.getName());
        LLVMValue thenValue = (LLVMValue) ast.E2.visit(this, o);
        builder.br(endLabel);
        currentBlock.addInstructions(builder.getInstructions());
        currentBlock.terminate();
        LLVMBasicBlock thenBlock = currentBlock;
        currentFunction.addBasicBlock(currentBlock);
        
        // Bloque else
        builder.reset();
        currentBlock = new LLVMBasicBlock(elseLabel.getName());
        LLVMValue elseValue = (LLVMValue) ast.E3.visit(this, o);
        builder.br(endLabel);
        currentBlock.addInstructions(builder.getInstructions());
        currentBlock.terminate();
        LLVMBasicBlock elseBlock = currentBlock;
        currentFunction.addBasicBlock(currentBlock);
        
        // Bloque end con PHI node
        builder.reset();
        currentBlock = new LLVMBasicBlock(endLabel.getName());
        
        Map<LLVMValue, LLVMValue.Label> phiValues = new LinkedHashMap<>();
        phiValues.put(thenValue, new LLVMValue.Label(thenBlock.getName()));
        phiValues.put(elseValue, new LLVMValue.Label(elseBlock.getName()));
        
        LLVMValue result = builder.phi(thenValue.getType(), phiValues);
        
        return result;
    }
    
    @Override
    public Object visitIntegerExpression(IntegerExpression ast, Object o) {
        int value = Integer.parseInt(ast.IL.spelling);
        return new LLVMValue.ConstantInt(value);
    }
    
    @Override
    public Object visitLetExpression(LetExpression ast, Object o) {
        // Entrar a un nuevo scope
        context = context.enterScope();
        
        // Visitar las declaraciones
        ast.D.visit(this, o);
        
        // Visitar la expresión
        LLVMValue result = (LLVMValue) ast.E.visit(this, o);
        
        // Salir del scope
        context = context.exitScope();
        
        return result;
    }
    
    @Override
    public Object visitRecordExpression(RecordExpression ast, Object o) {
        // TODO: Implementar records
        throw new UnsupportedOperationException("Record expressions not yet implemented");
    }
    
    @Override
    public Object visitUnaryExpression(UnaryExpression ast, Object o) {
        LLVMValue operand = (LLVMValue) ast.E.visit(this, o);
        String op = ast.O.spelling;
        
        switch (op) {
            case "-":
                return builder.neg(operand);
            case "\\":
            case "!":
                return builder.not(operand);
            default:
                throw new UnsupportedOperationException("Unary operator not implemented: " + op);
        }
    }
    
    @Override
    public Object visitVnameExpression(VnameExpression ast, Object o) {
        // Obtener el puntero de la variable
        LLVMValue ptr = (LLVMValue) ast.V.visit(this, o);
        
        // Determinar el tipo (necesitamos el tipo del pointee)
        LLVMType type;
        if (ptr.getType() instanceof LLVMType.PointerType) {
            LLVMType.PointerType ptrType = (LLVMType.PointerType) ptr.getType();
            type = ptrType.getPointeeType();
        } else {
            type = LLVMType.i32(); // Default
        }
        
        // Cargar el valor
        return builder.load(type, ptr);
    }
    
    // ==================== VISITOR METHODS - DECLARATIONS ====================
    
    @Override
    public Object visitBinaryOperatorDeclaration(BinaryOperatorDeclaration ast, Object o) {
        // Los operadores binarios son parte del entorno estándar
        return null;
    }
    
    @Override
    public Object visitConstDeclaration(ConstDeclaration ast, Object o) {
        // Evaluar la expresión constante
        LLVMValue value = (LLVMValue) ast.E.visit(this, o);
        
        // Registrar en el contexto
        context = context.withSymbol(ast.I.spelling, value);
        
        return null;
    }
    
    @Override
    public Object visitFuncDeclaration(FuncDeclaration ast, Object o) {
        // TODO: Implementar declaración de funciones
        throw new UnsupportedOperationException("Function declarations not yet implemented");
    }
    
    @Override
    public Object visitProcDeclaration(ProcDeclaration ast, Object o) {
        // TODO: Implementar declaración de procedimientos
        throw new UnsupportedOperationException("Procedure declarations not yet implemented");
    }
    
    @Override
    public Object visitSequentialDeclaration(SequentialDeclaration ast, Object o) {
        ast.D1.visit(this, o);
        ast.D2.visit(this, o);
        return null;
    }
    
    @Override
    public Object visitTypeDeclaration(TypeDeclaration ast, Object o) {
        // Convertir el tipo Triangle a LLVM
        LLVMType llvmType = convertType(ast.T);
        
        // Registrar en el contexto
        context = context.withType(ast.I.spelling, llvmType);
        
        return null;
    }
    
    @Override
    public Object visitUnaryOperatorDeclaration(UnaryOperatorDeclaration ast, Object o) {
        // Los operadores unarios son parte del entorno estándar
        return null;
    }
    
    @Override
    public Object visitVarDeclaration(VarDeclaration ast, Object o) {
        // Convertir tipo Triangle a LLVM
        LLVMType llvmType = convertType(ast.T);
        
        // Allocar espacio para la variable
        LLVMValue ptr = builder.alloca(llvmType);
        
        // Registrar en el contexto
        context = context.withSymbol(ast.I.spelling, ptr);
        
        return null;
    }
    
    // ==================== VISITOR METHODS - VNAMES ====================
    
    @Override
    public Object visitDotVname(DotVname ast, Object o) {
        // TODO: Implementar acceso a campos de records
        throw new UnsupportedOperationException("Dot vnames not yet implemented");
    }
    
    @Override
    public Object visitSimpleVname(SimpleVname ast, Object o) {
        // Buscar la variable en el contexto
        LLVMValue value = context.lookup(ast.I.spelling);
        
        if (value == null) {
            throw new RuntimeException("Variable not found: " + ast.I.spelling);
        }
        
        return value;
    }
    
    @Override
    public Object visitSubscriptVname(SubscriptVname ast, Object o) {
        // TODO: Implementar subscript para arrays
        throw new UnsupportedOperationException("Subscript vnames not yet implemented");
    }
    
    // ==================== VISITOR METHODS - PARAMETERS ====================
    
    @Override
    public Object visitConstActualParameter(ConstActualParameter ast, Object o) {
        List<LLVMValue> args = (List<LLVMValue>) o;
        LLVMValue value = (LLVMValue) ast.E.visit(this, null);
        args.add(value);
        return null;
    }
    
    @Override
    public Object visitConstFormalParameter(ConstFormalParameter ast, Object o) {
        // TODO: Implementar parámetros formales
        return null;
    }
    
    @Override
    public Object visitFuncActualParameter(FuncActualParameter ast, Object o) {
        // TODO: Implementar parámetros de función
        return null;
    }
    
    @Override
    public Object visitFuncFormalParameter(FuncFormalParameter ast, Object o) {
        // TODO: Implementar parámetros formales de función
        return null;
    }
    
    @Override
    public Object visitProcActualParameter(ProcActualParameter ast, Object o) {
        // TODO: Implementar parámetros de procedimiento
        return null;
    }
    
    @Override
    public Object visitProcFormalParameter(ProcFormalParameter ast, Object o) {
        // TODO: Implementar parámetros formales de procedimiento
        return null;
    }
    
    @Override
    public Object visitVarActualParameter(VarActualParameter ast, Object o) {
        List<LLVMValue> args = (List<LLVMValue>) o;
        LLVMValue value = (LLVMValue) ast.V.visit(this, null);
        args.add(value);
        return null;
    }
    
    @Override
    public Object visitVarFormalParameter(VarFormalParameter ast, Object o) {
        // TODO: Implementar parámetros formales por referencia
        return null;
    }
    
    @Override
    public Object visitEmptyActualParameterSequence(EmptyActualParameterSequence ast, Object o) {
        return null;
    }
    
    @Override
    public Object visitMultipleActualParameterSequence(MultipleActualParameterSequence ast, Object o) {
        ast.AP.visit(this, o);
        ast.APS.visit(this, o);
        return null;
    }
    
    @Override
    public Object visitSingleActualParameterSequence(SingleActualParameterSequence ast, Object o) {
        ast.AP.visit(this, o);
        return null;
    }
    
    @Override
    public Object visitEmptyFormalParameterSequence(EmptyFormalParameterSequence ast, Object o) {
        return null;
    }
    
    @Override
    public Object visitMultipleFormalParameterSequence(MultipleFormalParameterSequence ast, Object o) {
        ast.FP.visit(this, o);
        ast.FPS.visit(this, o);
        return null;
    }
    
    @Override
    public Object visitSingleFormalParameterSequence(SingleFormalParameterSequence ast, Object o) {
        ast.FP.visit(this, o);
        return null;
    }
    
    // ==================== VISITOR METHODS - TYPE DENOTERS ====================
    
    @Override
    public Object visitAnyTypeDenoter(AnyTypeDenoter ast, Object o) {
        return LLVMType.i32(); // Default type
    }
    
    @Override
    public Object visitArrayTypeDenoter(ArrayTypeDenoter ast, Object o) {
        int size = Integer.parseInt(ast.IL.spelling);
        LLVMType elemType = convertType(ast.T);
        return LLVMType.array(size, elemType);
    }
    
    @Override
    public Object visitBoolTypeDenoter(BoolTypeDenoter ast, Object o) {
        return LLVMType.i1();
    }
    
    @Override
    public Object visitCharTypeDenoter(CharTypeDenoter ast, Object o) {
        return LLVMType.i8();
    }
    
    @Override
    public Object visitErrorTypeDenoter(ErrorTypeDenoter ast, Object o) {
        return LLVMType.i32();
    }
    
    @Override
    public Object visitSimpleTypeDenoter(SimpleTypeDenoter ast, Object o) {
        String typeName = ast.I.spelling;
        
        // Buscar en el contexto
        LLVMType type = context.getType(typeName);
        if (type != null) {
            return type;
        }
        
        // Tipos por defecto
        switch (typeName) {
            case "Integer":
                return LLVMType.i32();
            case "Boolean":
                return LLVMType.i1();
            case "Char":
                return LLVMType.i8();
            default:
                return LLVMType.i32();
        }
    }
    
    @Override
    public Object visitIntTypeDenoter(IntTypeDenoter ast, Object o) {
        return LLVMType.i32();
    }
    
    @Override
    public Object visitRecordTypeDenoter(RecordTypeDenoter ast, Object o) {
        // TODO: Implementar tipos record
        throw new UnsupportedOperationException("Record types not yet implemented");
    }
    
    @Override
    public Object visitMultipleFieldTypeDenoter(MultipleFieldTypeDenoter ast, Object o) {
        // TODO: Implementar campos múltiples de records
        throw new UnsupportedOperationException("Multiple field type denoters not yet implemented");
    }
    
    @Override
    public Object visitSingleFieldTypeDenoter(SingleFieldTypeDenoter ast, Object o) {
        // TODO: Implementar campo simple de record
        throw new UnsupportedOperationException("Single field type denoter not yet implemented");
    }
    
    // ==================== VISITOR METHODS - LITERALS ====================
    
    @Override
    public Object visitCharacterLiteral(CharacterLiteral ast, Object o) {
        return null; // Handled in CharacterExpression
    }
    
    @Override
    public Object visitIdentifier(Identifier ast, Object o) {
        return null; // Handled elsewhere
    }
    
    @Override
    public Object visitIntegerLiteral(IntegerLiteral ast, Object o) {
        return null; // Handled in IntegerExpression
    }
    
    @Override
    public Object visitOperator(Operator ast, Object o) {
        return null; // Handled elsewhere
    }
    
    // ==================== VISITOR METHODS - AGGREGATES ====================
    
    @Override
    public Object visitMultipleArrayAggregate(MultipleArrayAggregate ast, Object o) {
        // TODO: Implementar agregados de arrays
        return null;
    }
    
    @Override
    public Object visitSingleArrayAggregate(SingleArrayAggregate ast, Object o) {
        // TODO: Implementar agregados de arrays
        return null;
    }
    
    @Override
    public Object visitMultipleRecordAggregate(MultipleRecordAggregate ast, Object o) {
        // TODO: Implementar agregados de records
        return null;
    }
    
    @Override
    public Object visitSingleRecordAggregate(SingleRecordAggregate ast, Object o) {
        // TODO: Implementar agregados de records
        return null;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Convierte un TypeDenoter de Triangle a un LLVMType
     */
    private LLVMType convertType(TypeDenoter triangleType) {
        if (triangleType instanceof IntTypeDenoter) {
            return LLVMType.i32();
        } else if (triangleType instanceof BoolTypeDenoter) {
            return LLVMType.i1();
        } else if (triangleType instanceof CharTypeDenoter) {
            return LLVMType.i8();
        } else if (triangleType instanceof ArrayTypeDenoter) {
            ArrayTypeDenoter arr = (ArrayTypeDenoter) triangleType;
            int size = Integer.parseInt(arr.IL.spelling);
            LLVMType elemType = convertType(arr.T);
            return LLVMType.array(size, elemType);
        } else if (triangleType instanceof SimpleTypeDenoter) {
            SimpleTypeDenoter simple = (SimpleTypeDenoter) triangleType;
            String typeName = simple.I.spelling;
            
            LLVMType type = context.getType(typeName);
            if (type != null) {
                return type;
            }
            
            // Tipos estándar
            switch (typeName) {
                case "Integer":
                    return LLVMType.i32();
                case "Boolean":
                    return LLVMType.i1();
                case "Char":
                    return LLVMType.i8();
                default:
                    return LLVMType.i32();
            }
        }
        
        // Por defecto
        return LLVMType.i32();
    }
}
