package Triangle.LLVMCodeGenerator;

/**
 * Representa valores en LLVM IR de forma type-safe
 * Los valores pueden ser constantes, variables, temporales, etc.
 */
public abstract class LLVMValue {
    protected final LLVMType type;
    
    protected LLVMValue(LLVMType type) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        this.type = type;
    }
    
    /**
     * Obtiene el tipo LLVM de este valor
     * @return Tipo del valor
     */
    public LLVMType getType() {
        return type;
    }
    
    /**
     * Genera la representación en LLVM IR de este valor
     * @return String con la sintaxis LLVM del valor
     */
    public abstract String toIR();
    
    /**
     * Verifica si este es un valor constante
     * @return true si es constante
     */
    public boolean isConstant() {
        return false;
    }
    
    /**
     * Constante entera de LLVM
     */
    public static class ConstantInt extends LLVMValue {
        private final long value;
        
        public ConstantInt(long value, LLVMType.IntType type) {
            super(type);
            this.value = value;
        }
        
        public ConstantInt(int value) {
            this((long) value, (LLVMType.IntType) LLVMType.i32());
        }
        
        public ConstantInt(boolean value) {
            this(value ? 1L : 0L, (LLVMType.IntType) LLVMType.i1());
        }
        
        public long getValue() {
            return value;
        }
        
        @Override
        public String toIR() {
            return String.valueOf(value);
        }
        
        @Override
        public boolean isConstant() {
            return true;
        }
        
        @Override
        public String toString() {
            return type.toIR() + " " + value;
        }
    }
    
    /**
     * Constante de caracteres (representada como i8)
     */
    public static class ConstantChar extends LLVMValue {
        private final char value;
        
        public ConstantChar(char value) {
            super(LLVMType.i8());
            this.value = value;
        }
        
        public char getValue() {
            return value;
        }
        
        @Override
        public String toIR() {
            return String.valueOf((int) value);
        }
        
        @Override
        public boolean isConstant() {
            return true;
        }
        
        @Override
        public String toString() {
            return type.toIR() + " " + (int) value;
        }
    }
    
    /**
     * Constante de string (global)
     */
    public static class ConstantString extends LLVMValue {
        private final String value;
        private final String globalName;
        
        public ConstantString(String value, String globalName) {
            super(LLVMType.ptr(LLVMType.i8()));
            this.value = value;
            this.globalName = globalName;
        }
        
        public String getValue() {
            return value;
        }
        
        public String getGlobalName() {
            return globalName;
        }
        
        @Override
        public String toIR() {
            return "@" + globalName;
        }
        
        @Override
        public boolean isConstant() {
            return true;
        }
        
        /**
         * Genera la definición global del string
         */
        public String toGlobalDefinition() {
            StringBuilder sb = new StringBuilder();
            sb.append("@").append(globalName).append(" = private unnamed_addr constant ");
            sb.append("[").append(value.length() + 1).append(" x i8] c\"");
            
            // Escapar caracteres especiales
            for (char c : value.toCharArray()) {
                if (c == '\n') sb.append("\\0A");
                else if (c == '\t') sb.append("\\09");
                else if (c == '\r') sb.append("\\0D");
                else if (c == '"') sb.append("\\22");
                else if (c == '\\') sb.append("\\\\");
                else if (c >= 32 && c < 127) sb.append(c);
                else sb.append(String.format("\\%02X", (int) c));
            }
            
            sb.append("\\00\", align 1");
            return sb.toString();
        }
        
        @Override
        public String toString() {
            return "ptr to \"" + value + "\"";
        }
    }
    
    /**
     * Variable local (registro SSA)
     */
    public static class LocalVariable extends LLVMValue {
        private final String name;
        
        public LocalVariable(String name, LLVMType type) {
            super(type);
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Name cannot be null or empty");
            }
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        @Override
        public String toIR() {
            return "%" + name;
        }
        
        @Override
        public String toString() {
            return "%" + name + " : " + type.toIR();
        }
    }
    
    /**
     * Variable global
     */
    public static class GlobalVariable extends LLVMValue {
        private final String name;
        private final boolean isConstant;
        
        public GlobalVariable(String name, LLVMType type) {
            this(name, type, false);
        }
        
        public GlobalVariable(String name, LLVMType type, boolean isConstant) {
            super(type);
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Name cannot be null or empty");
            }
            this.name = name;
            this.isConstant = isConstant;
        }
        
        public String getName() {
            return name;
        }
        
        @Override
        public String toIR() {
            return "@" + name;
        }
        
        @Override
        public boolean isConstant() {
            return isConstant;
        }
        
        @Override
        public String toString() {
            return "@" + name + " : " + type.toIR();
        }
    }
    
    /**
     * Valor temporal (registro SSA numerado)
     */
    public static class TemporaryValue extends LLVMValue {
        private final int id;
        
        public TemporaryValue(int id, LLVMType type) {
            super(type);
            if (id < 0) {
                throw new IllegalArgumentException("ID must be non-negative");
            }
            this.id = id;
        }
        
        public int getId() {
            return id;
        }
        
        @Override
        public String toIR() {
            return "%tmp" + id;
        }
        
        @Override
        public String toString() {
            return "%tmp" + id + " : " + type.toIR();
        }
    }
    
    /**
     * Parámetro de función
     */
    public static class Parameter extends LLVMValue {
        private final String name;
        private final int index;
        
        public Parameter(String name, int index, LLVMType type) {
            super(type);
            this.name = name;
            this.index = index;
        }
        
        public String getName() {
            return name;
        }
        
        public int getIndex() {
            return index;
        }
        
        @Override
        public String toIR() {
            return "%" + name;
        }
        
        @Override
        public String toString() {
            return "%" + name + " : " + type.toIR();
        }
    }
    
    /**
     * Valor undef (indefinido)
     */
    public static class UndefValue extends LLVMValue {
        public UndefValue(LLVMType type) {
            super(type);
        }
        
        @Override
        public String toIR() {
            return "undef";
        }
        
        @Override
        public String toString() {
            return "undef : " + type.toIR();
        }
    }
    
    /**
     * Valor null (puntero nulo)
     */
    public static class NullValue extends LLVMValue {
        public NullValue(LLVMType type) {
            super(type);
            if (!(type instanceof LLVMType.PointerType)) {
                throw new IllegalArgumentException("Null value requires pointer type");
            }
        }
        
        @Override
        public String toIR() {
            return "null";
        }
        
        @Override
        public boolean isConstant() {
            return true;
        }
        
        @Override
        public String toString() {
            return "null : " + type.toIR();
        }
    }
    
    /**
     * Valor cero (zeroinitializer)
     */
    public static class ZeroValue extends LLVMValue {
        public ZeroValue(LLVMType type) {
            super(type);
        }
        
        @Override
        public String toIR() {
            return "zeroinitializer";
        }
        
        @Override
        public boolean isConstant() {
            return true;
        }
        
        @Override
        public String toString() {
            return "zeroinitializer : " + type.toIR();
        }
    }
    
    /**
     * Etiqueta de bloque básico
     */
    public static class Label extends LLVMValue {
        private final String name;
        
        public Label(String name) {
            super(LLVMType.voidType()); // Las etiquetas no tienen tipo real
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        @Override
        public String toIR() {
            return "%" + name;
        }
        
        @Override
        public String toString() {
            return "label %" + name;
        }
    }
}
