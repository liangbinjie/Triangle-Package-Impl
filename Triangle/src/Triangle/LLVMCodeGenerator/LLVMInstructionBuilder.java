package Triangle.LLVMCodeGenerator;

import java.util.*;

/**
 * Builder fluido para generar instrucciones LLVM
 * Proporciona métodos convenientes para crear instrucciones LLVM IR
 */
public class LLVMInstructionBuilder {
    private final List<String> instructions = new ArrayList<>();
    private int tempCounter = 0;
    private int labelCounter = 0;
    
    /**
     * Crea un nuevo valor temporal
     * @param type Tipo del temporal
     * @return Nuevo valor temporal
     */
    public LLVMValue createTemp(LLVMType type) {
        return new LLVMValue.TemporaryValue(tempCounter++, type);
    }
    
    /**
     * Crea una nueva etiqueta
     * @param prefix Prefijo de la etiqueta
     * @return Nueva etiqueta
     */
    public LLVMValue.Label createLabel(String prefix) {
        return new LLVMValue.Label(prefix + labelCounter++);
    }
    
    /**
     * Obtiene todas las instrucciones generadas
     * @return Lista de instrucciones
     */
    public List<String> getInstructions() {
        return new ArrayList<>(instructions);
    }
    
    /**
     * Limpia todas las instrucciones
     */
    public void clear() {
        instructions.clear();
    }
    
    /**
     * Añade una instrucción personalizada
     * @param instruction Instrucción a añadir
     */
    public void addInstruction(String instruction) {
        instructions.add(instruction);
    }
    
    // ==================== INSTRUCCIONES ARITMÉTICAS ====================
    
    /**
     * Suma de enteros (add nsw)
     */
    public LLVMValue add(LLVMValue left, LLVMValue right) {
        LLVMValue result = createTemp(left.getType());
        instructions.add(String.format("  %s = add nsw %s %s, %s", 
            result.toIR(), 
            left.getType().toIR(), 
            left.toIR(), 
            right.toIR()));
        return result;
    }
    
    /**
     * Resta de enteros (sub nsw)
     */
    public LLVMValue sub(LLVMValue left, LLVMValue right) {
        LLVMValue result = createTemp(left.getType());
        instructions.add(String.format("  %s = sub nsw %s %s, %s", 
            result.toIR(), 
            left.getType().toIR(), 
            left.toIR(), 
            right.toIR()));
        return result;
    }
    
    /**
     * Multiplicación de enteros (mul nsw)
     */
    public LLVMValue mul(LLVMValue left, LLVMValue right) {
        LLVMValue result = createTemp(left.getType());
        instructions.add(String.format("  %s = mul nsw %s %s, %s", 
            result.toIR(), 
            left.getType().toIR(), 
            left.toIR(), 
            right.toIR()));
        return result;
    }
    
    /**
     * División de enteros con signo (sdiv)
     */
    public LLVMValue sdiv(LLVMValue left, LLVMValue right) {
        LLVMValue result = createTemp(left.getType());
        instructions.add(String.format("  %s = sdiv %s %s, %s", 
            result.toIR(), 
            left.getType().toIR(), 
            left.toIR(), 
            right.toIR()));
        return result;
    }
    
    /**
     * Módulo de enteros con signo (srem)
     */
    public LLVMValue srem(LLVMValue left, LLVMValue right) {
        LLVMValue result = createTemp(left.getType());
        instructions.add(String.format("  %s = srem %s %s, %s", 
            result.toIR(), 
            left.getType().toIR(), 
            left.toIR(), 
            right.toIR()));
        return result;
    }
    
    /**
     * Negación (sub 0, value)
     */
    public LLVMValue neg(LLVMValue value) {
        LLVMValue zero = new LLVMValue.ConstantInt(0);
        return sub(zero, value);
    }
    
    // ==================== INSTRUCCIONES LÓGICAS ====================
    
    /**
     * AND lógico
     */
    public LLVMValue and(LLVMValue left, LLVMValue right) {
        LLVMValue result = createTemp(left.getType());
        instructions.add(String.format("  %s = and %s %s, %s", 
            result.toIR(), 
            left.getType().toIR(), 
            left.toIR(), 
            right.toIR()));
        return result;
    }
    
    /**
     * OR lógico
     */
    public LLVMValue or(LLVMValue left, LLVMValue right) {
        LLVMValue result = createTemp(left.getType());
        instructions.add(String.format("  %s = or %s %s, %s", 
            result.toIR(), 
            left.getType().toIR(), 
            left.toIR(), 
            right.toIR()));
        return result;
    }
    
    /**
     * XOR lógico (usado para NOT: xor x, true)
     */
    public LLVMValue xor(LLVMValue left, LLVMValue right) {
        LLVMValue result = createTemp(left.getType());
        instructions.add(String.format("  %s = xor %s %s, %s", 
            result.toIR(), 
            left.getType().toIR(), 
            left.toIR(), 
            right.toIR()));
        return result;
    }
    
    /**
     * NOT lógico (xor value, true)
     */
    public LLVMValue not(LLVMValue value) {
        LLVMValue trueVal = new LLVMValue.ConstantInt(true);
        return xor(value, trueVal);
    }
    
    // ==================== INSTRUCCIONES DE COMPARACIÓN ====================
    
    /**
     * Comparación icmp
     */
    public LLVMValue icmp(String predicate, LLVMValue left, LLVMValue right) {
        LLVMValue result = createTemp(LLVMType.i1());
        instructions.add(String.format("  %s = icmp %s %s %s, %s", 
            result.toIR(), 
            predicate,
            left.getType().toIR(), 
            left.toIR(), 
            right.toIR()));
        return result;
    }
    
    // Comparaciones específicas
    public LLVMValue icmpEq(LLVMValue left, LLVMValue right) {
        return icmp("eq", left, right);
    }
    
    public LLVMValue icmpNe(LLVMValue left, LLVMValue right) {
        return icmp("ne", left, right);
    }
    
    public LLVMValue icmpSlt(LLVMValue left, LLVMValue right) {
        return icmp("slt", left, right);
    }
    
    public LLVMValue icmpSle(LLVMValue left, LLVMValue right) {
        return icmp("sle", left, right);
    }
    
    public LLVMValue icmpSgt(LLVMValue left, LLVMValue right) {
        return icmp("sgt", left, right);
    }
    
    public LLVMValue icmpSge(LLVMValue left, LLVMValue right) {
        return icmp("sge", left, right);
    }
    
    // ==================== INSTRUCCIONES DE MEMORIA ====================
    
    /**
     * Alloca - reserva espacio en el stack
     */
    public LLVMValue alloca(LLVMType type) {
        LLVMValue result = createTemp(LLVMType.ptr(type));
        instructions.add(String.format("  %s = alloca %s, align %d", 
            result.toIR(), 
            type.toIR(), 
            type.getAlignment()));
        return result;
    }
    
    /**
     * Alloca con tamaño específico
     */
    public LLVMValue alloca(LLVMType type, LLVMValue size) {
        LLVMValue result = createTemp(LLVMType.ptr(type));
        instructions.add(String.format("  %s = alloca %s, %s %s, align %d", 
            result.toIR(), 
            type.toIR(),
            size.getType().toIR(),
            size.toIR(),
            type.getAlignment()));
        return result;
    }
    
    /**
     * Load - carga un valor desde un puntero
     */
    public LLVMValue load(LLVMType type, LLVMValue ptr) {
        LLVMValue result = createTemp(type);
        instructions.add(String.format("  %s = load %s, ptr %s, align %d", 
            result.toIR(), 
            type.toIR(), 
            ptr.toIR(), 
            type.getAlignment()));
        return result;
    }
    
    /**
     * Store - almacena un valor en un puntero
     */
    public void store(LLVMValue value, LLVMValue ptr) {
        instructions.add(String.format("  store %s %s, ptr %s, align %d", 
            value.getType().toIR(), 
            value.toIR(), 
            ptr.toIR(), 
            value.getType().getAlignment()));
    }
    
    /**
     * GetElementPtr (GEP) - calcula dirección de un elemento
     */
    public LLVMValue getelementptr(LLVMType type, LLVMValue ptr, LLVMValue... indices) {
        LLVMValue result = createTemp(LLVMType.ptr(type));
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  %s = getelementptr inbounds %s, ptr %s", 
            result.toIR(), 
            type.toIR(), 
            ptr.toIR()));
        
        for (LLVMValue index : indices) {
            sb.append(String.format(", %s %s", 
                index.getType().toIR(), 
                index.toIR()));
        }
        
        instructions.add(sb.toString());
        return result;
    }
    
    // ==================== INSTRUCCIONES DE CONTROL DE FLUJO ====================
    
    /**
     * Branch incondicional
     */
    public void br(LLVMValue.Label label) {
        instructions.add(String.format("  br label %s", label.toIR()));
    }
    
    /**
     * Branch condicional
     */
    public void brCond(LLVMValue condition, LLVMValue.Label trueLabel, LLVMValue.Label falseLabel) {
        instructions.add(String.format("  br i1 %s, label %s, label %s", 
            condition.toIR(), 
            trueLabel.toIR(), 
            falseLabel.toIR()));
    }
    
    /**
     * Return void
     */
    public void retVoid() {
        instructions.add("  ret void");
    }
    
    /**
     * Return con valor
     */
    public void ret(LLVMValue value) {
        instructions.add(String.format("  ret %s %s", 
            value.getType().toIR(), 
            value.toIR()));
    }
    
    /**
     * Switch
     */
    public void switchInst(LLVMValue value, LLVMValue.Label defaultLabel, 
                          Map<LLVMValue, LLVMValue.Label> cases) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  switch %s %s, label %s [", 
            value.getType().toIR(), 
            value.toIR(), 
            defaultLabel.toIR()));
        
        for (Map.Entry<LLVMValue, LLVMValue.Label> entry : cases.entrySet()) {
            sb.append(String.format("\n    %s %s, label %s", 
                entry.getKey().getType().toIR(), 
                entry.getKey().toIR(), 
                entry.getValue().toIR()));
        }
        
        sb.append("\n  ]");
        instructions.add(sb.toString());
    }
    
    // ==================== INSTRUCCIONES DE LLAMADA ====================
    
    /**
     * Call void - llama a una función sin retorno
     */
    public void callVoid(String functionName, List<LLVMValue> args) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  call void @%s(", functionName));
        
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(", ");
            LLVMValue arg = args.get(i);
            sb.append(arg.getType().toIR()).append(" ").append(arg.toIR());
        }
        
        sb.append(")");
        instructions.add(sb.toString());
    }
    
    /**
     * Call - llama a una función con retorno
     */
    public LLVMValue call(LLVMType returnType, String functionName, List<LLVMValue> args) {
        LLVMValue result = createTemp(returnType);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  %s = call %s @%s(", 
            result.toIR(), 
            returnType.toIR(), 
            functionName));
        
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(", ");
            LLVMValue arg = args.get(i);
            sb.append(arg.getType().toIR()).append(" ").append(arg.toIR());
        }
        
        sb.append(")");
        instructions.add(sb.toString());
        return result;
    }
    
    // ==================== INSTRUCCIONES DE CONVERSIÓN ====================
    
    /**
     * Zero extend - extiende con ceros
     */
    public LLVMValue zext(LLVMValue value, LLVMType targetType) {
        LLVMValue result = createTemp(targetType);
        instructions.add(String.format("  %s = zext %s %s to %s", 
            result.toIR(), 
            value.getType().toIR(), 
            value.toIR(), 
            targetType.toIR()));
        return result;
    }
    
    /**
     * Sign extend - extiende con signo
     */
    public LLVMValue sext(LLVMValue value, LLVMType targetType) {
        LLVMValue result = createTemp(targetType);
        instructions.add(String.format("  %s = sext %s %s to %s", 
            result.toIR(), 
            value.getType().toIR(), 
            value.toIR(), 
            targetType.toIR()));
        return result;
    }
    
    /**
     * Truncate - reduce el tamaño
     */
    public LLVMValue trunc(LLVMValue value, LLVMType targetType) {
        LLVMValue result = createTemp(targetType);
        instructions.add(String.format("  %s = trunc %s %s to %s", 
            result.toIR(), 
            value.getType().toIR(), 
            value.toIR(), 
            targetType.toIR()));
        return result;
    }
    
    /**
     * Bitcast - reinterpreta el tipo
     */
    public LLVMValue bitcast(LLVMValue value, LLVMType targetType) {
        LLVMValue result = createTemp(targetType);
        instructions.add(String.format("  %s = bitcast %s %s to %s", 
            result.toIR(), 
            value.getType().toIR(), 
            value.toIR(), 
            targetType.toIR()));
        return result;
    }
    
    // ==================== OTRAS INSTRUCCIONES ====================
    
    /**
     * PHI node - para SSA
     */
    public LLVMValue phi(LLVMType type, Map<LLVMValue, LLVMValue.Label> incomingValues) {
        LLVMValue result = createTemp(type);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  %s = phi %s ", result.toIR(), type.toIR()));
        
        boolean first = true;
        for (Map.Entry<LLVMValue, LLVMValue.Label> entry : incomingValues.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(String.format("[ %s, %s ]", 
                entry.getKey().toIR(), 
                entry.getValue().toIR()));
            first = false;
        }
        
        instructions.add(sb.toString());
        return result;
    }
    
    /**
     * Select - operador ternario
     */
    public LLVMValue select(LLVMValue condition, LLVMValue trueValue, LLVMValue falseValue) {
        LLVMValue result = createTemp(trueValue.getType());
        instructions.add(String.format("  %s = select i1 %s, %s %s, %s %s", 
            result.toIR(), 
            condition.toIR(), 
            trueValue.getType().toIR(), 
            trueValue.toIR(), 
            falseValue.getType().toIR(), 
            falseValue.toIR()));
        return result;
    }
    
    /**
     * Añade una etiqueta al flujo de instrucciones
     */
    public void label(LLVMValue.Label label) {
        instructions.add(label.getName() + ":");
    }
    
    /**
     * Añade un comentario
     */
    public void comment(String text) {
        instructions.add("  ; " + text);
    }
    
    /**
     * Resetea los contadores (útil para nueva función)
     */
    public void reset() {
        tempCounter = 0;
        labelCounter = 0;
        instructions.clear();
    }
}
