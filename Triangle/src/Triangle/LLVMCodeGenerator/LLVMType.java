package Triangle.LLVMCodeGenerator;

import java.util.*;

/**
 * Representa tipos de LLVM de forma type-safe
 * Esta jerarquía permite validación en tiempo de compilación
 * y evita errores comunes en la generación de código LLVM
 */
public abstract class LLVMType {
    
    /**
     * Genera la representación en LLVM IR de este tipo
     * @return String con la sintaxis LLVM del tipo
     */
    public abstract String toIR();
    
    /**
     * Obtiene la alineación requerida en bytes
     * @return Alineación en bytes
     */
    public abstract int getAlignment();
    
    /**
     * Obtiene el tamaño en bytes del tipo
     * @return Tamaño en bytes
     */
    public abstract int getSize();
    
    /**
     * Tipo entero de LLVM (i1, i8, i32, i64, etc.)
     */
    public static class IntType extends LLVMType {
        private final int bits;
        
        public IntType(int bits) {
            if (bits <= 0) {
                throw new IllegalArgumentException("Bits must be positive");
            }
            this.bits = bits;
        }
        
        public int getBits() {
            return bits;
        }
        
        @Override
        public String toIR() {
            return "i" + bits;
        }
        
        @Override
        public int getAlignment() {
            // Alineación típica para enteros
            if (bits <= 8) return 1;
            if (bits <= 16) return 2;
            if (bits <= 32) return 4;
            return 8;
        }
        
        @Override
        public int getSize() {
            return (bits + 7) / 8; // Redondear hacia arriba
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof IntType)) return false;
            IntType other = (IntType) obj;
            return bits == other.bits;
        }
        
        @Override
        public int hashCode() {
            return Integer.hashCode(bits);
        }
    }
    
    /**
     * Tipo puntero de LLVM
     * En LLVM moderno (opaque pointers), todos los punteros son 'ptr'
     */
    public static class PointerType extends LLVMType {
        private final LLVMType pointeeType;
        
        public PointerType(LLVMType pointeeType) {
            this.pointeeType = pointeeType;
        }
        
        public LLVMType getPointeeType() {
            return pointeeType;
        }
        
        @Override
        public String toIR() {
            return "ptr"; // Opaque pointers (LLVM 15+)
        }
        
        @Override
        public int getAlignment() {
            return 8; // 64-bit pointer alignment
        }
        
        @Override
        public int getSize() {
            return 8; // 64-bit pointers
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof PointerType)) return false;
            PointerType other = (PointerType) obj;
            return pointeeType.equals(other.pointeeType);
        }
        
        @Override
        public int hashCode() {
            return pointeeType.hashCode();
        }
    }
    
    /**
     * Tipo array de LLVM [N x tipo]
     */
    public static class ArrayType extends LLVMType {
        private final int size;
        private final LLVMType elementType;
        
        public ArrayType(int size, LLVMType elementType) {
            if (size < 0) {
                throw new IllegalArgumentException("Array size cannot be negative");
            }
            this.size = size;
            this.elementType = elementType;
        }
        
        public int getArraySize() {
            return size;
        }
        
        public LLVMType getElementType() {
            return elementType;
        }
        
        @Override
        public String toIR() {
            return "[" + size + " x " + elementType.toIR() + "]";
        }
        
        @Override
        public int getAlignment() {
            return elementType.getAlignment();
        }
        
        @Override
        public int getSize() {
            return size * elementType.getSize();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ArrayType)) return false;
            ArrayType other = (ArrayType) obj;
            return size == other.size && elementType.equals(other.elementType);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(size, elementType);
        }
    }
    
    /**
     * Tipo struct de LLVM
     */
    public static class StructType extends LLVMType {
        private final String name;
        private final List<LLVMType> fields;
        private final boolean isPacked;
        
        public StructType(String name, List<LLVMType> fields) {
            this(name, fields, false);
        }
        
        public StructType(String name, List<LLVMType> fields, boolean isPacked) {
            this.name = name;
            this.fields = new ArrayList<>(fields);
            this.isPacked = isPacked;
        }
        
        public String getName() {
            return name;
        }
        
        public List<LLVMType> getFields() {
            return new ArrayList<>(fields);
        }
        
        @Override
        public String toIR() {
            if (name != null && !name.isEmpty()) {
                return "%struct." + name;
            }
            // Anonymous struct
            StringBuilder sb = new StringBuilder();
            sb.append(isPacked ? "<{" : "{");
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(fields.get(i).toIR());
            }
            sb.append(isPacked ? "}>" : "}");
            return sb.toString();
        }
        
        @Override
        public int getAlignment() {
            if (isPacked) return 1;
            return fields.stream()
                .mapToInt(LLVMType::getAlignment)
                .max()
                .orElse(4);
        }
        
        @Override
        public int getSize() {
            if (isPacked) {
                return fields.stream()
                    .mapToInt(LLVMType::getSize)
                    .sum();
            }
            
            // Con padding para alineación
            int size = 0;
            for (LLVMType field : fields) {
                int align = field.getAlignment();
                int padding = (align - (size % align)) % align;
                size += padding + field.getSize();
            }
            
            // Padding final para alineación del struct completo
            int structAlign = getAlignment();
            int finalPadding = (structAlign - (size % structAlign)) % structAlign;
            size += finalPadding;
            
            return size;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof StructType)) return false;
            StructType other = (StructType) obj;
            return Objects.equals(name, other.name) && 
                   fields.equals(other.fields) &&
                   isPacked == other.isPacked;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(name, fields, isPacked);
        }
    }
    
    /**
     * Tipo void de LLVM
     */
    public static class VoidType extends LLVMType {
        private static final VoidType INSTANCE = new VoidType();
        
        private VoidType() {}
        
        public static VoidType getInstance() {
            return INSTANCE;
        }
        
        @Override
        public String toIR() {
            return "void";
        }
        
        @Override
        public int getAlignment() {
            return 1;
        }
        
        @Override
        public int getSize() {
            return 0;
        }
    }
    
    /**
     * Tipo función de LLVM
     */
    public static class FunctionType extends LLVMType {
        private final LLVMType returnType;
        private final List<LLVMType> paramTypes;
        private final boolean isVarArg;
        
        public FunctionType(LLVMType returnType, List<LLVMType> paramTypes) {
            this(returnType, paramTypes, false);
        }
        
        public FunctionType(LLVMType returnType, List<LLVMType> paramTypes, boolean isVarArg) {
            this.returnType = returnType;
            this.paramTypes = new ArrayList<>(paramTypes);
            this.isVarArg = isVarArg;
        }
        
        public LLVMType getReturnType() {
            return returnType;
        }
        
        public List<LLVMType> getParamTypes() {
            return new ArrayList<>(paramTypes);
        }
        
        @Override
        public String toIR() {
            StringBuilder sb = new StringBuilder();
            sb.append(returnType.toIR()).append(" (");
            for (int i = 0; i < paramTypes.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(paramTypes.get(i).toIR());
            }
            if (isVarArg) {
                if (!paramTypes.isEmpty()) sb.append(", ");
                sb.append("...");
            }
            sb.append(")");
            return sb.toString();
        }
        
        @Override
        public int getAlignment() {
            return 8; // Function pointers
        }
        
        @Override
        public int getSize() {
            return 8; // Function pointers
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof FunctionType)) return false;
            FunctionType other = (FunctionType) obj;
            return returnType.equals(other.returnType) &&
                   paramTypes.equals(other.paramTypes) &&
                   isVarArg == other.isVarArg;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(returnType, paramTypes, isVarArg);
        }
    }
    
    // Factory methods para tipos comunes
    public static LLVMType i1() { 
        return new IntType(1); 
    }
    
    public static LLVMType i8() { 
        return new IntType(8); 
    }
    
    public static LLVMType i16() { 
        return new IntType(16); 
    }
    
    public static LLVMType i32() { 
        return new IntType(32); 
    }
    
    public static LLVMType i64() { 
        return new IntType(64); 
    }
    
    public static LLVMType ptr(LLVMType pointeeType) { 
        return new PointerType(pointeeType); 
    }
    
    public static LLVMType array(int size, LLVMType elementType) {
        return new ArrayType(size, elementType);
    }
    
    public static LLVMType voidType() {
        return VoidType.getInstance();
    }
    
    public static LLVMType functionType(LLVMType returnType, LLVMType... paramTypes) {
        return new FunctionType(returnType, Arrays.asList(paramTypes));
    }
}
