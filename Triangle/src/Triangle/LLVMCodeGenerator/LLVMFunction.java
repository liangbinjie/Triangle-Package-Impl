package Triangle.LLVMCodeGenerator;

import java.util.*;

/**
 * Representa una función de LLVM
 */
public class LLVMFunction {
    private final String name;
    private final LLVMType returnType;
    private final List<Parameter> parameters;
    private final List<LLVMBasicBlock> basicBlocks;
    private final Map<String, String> attributes;
    private final boolean isDeclaration; // true si es solo declaración (sin cuerpo)
    
    public LLVMFunction(String name, LLVMType returnType, List<Parameter> parameters) {
        this(name, returnType, parameters, false);
    }
    
    public LLVMFunction(String name, LLVMType returnType, List<Parameter> parameters, boolean isDeclaration) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = new ArrayList<>(parameters);
        this.basicBlocks = new ArrayList<>();
        this.attributes = new LinkedHashMap<>();
        this.isDeclaration = isDeclaration;
    }
    
    /**
     * Representa un parámetro de función
     */
    public static class Parameter {
        private final String name;
        private final LLVMType type;
        
        public Parameter(String name, LLVMType type) {
            this.name = name;
            this.type = type;
        }
        
        public String getName() {
            return name;
        }
        
        public LLVMType getType() {
            return type;
        }
        
        public String toIR() {
            return type.toIR() + " %" + name;
        }
    }
    
    /**
     * Obtiene el nombre de la función
     */
    public String getName() {
        return name;
    }
    
    /**
     * Obtiene el tipo de retorno
     */
    public LLVMType getReturnType() {
        return returnType;
    }
    
    /**
     * Obtiene los parámetros
     */
    public List<Parameter> getParameters() {
        return new ArrayList<>(parameters);
    }
    
    /**
     * Añade un bloque básico
     */
    public void addBasicBlock(LLVMBasicBlock block) {
        basicBlocks.add(block);
    }
    
    /**
     * Obtiene los bloques básicos
     */
    public List<LLVMBasicBlock> getBasicBlocks() {
        return new ArrayList<>(basicBlocks);
    }
    
    /**
     * Añade un atributo a la función
     */
    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }
    
    /**
     * Añade un atributo sin valor
     */
    public void addAttribute(String key) {
        attributes.put(key, "");
    }
    
    /**
     * Verifica si es solo una declaración
     */
    public boolean isDeclaration() {
        return isDeclaration;
    }
    
    /**
     * Genera el código LLVM de la función
     */
    public String toIR() {
        StringBuilder sb = new StringBuilder();
        
        // Declaración o definición
        if (isDeclaration) {
            sb.append("declare ");
        } else {
            sb.append("define ");
        }
        
        // Tipo de retorno
        sb.append(returnType.toIR()).append(" ");
        
        // Nombre
        sb.append("@").append(name).append("(");
        
        // Parámetros
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameters.get(i).toIR());
        }
        
        sb.append(")");
        
        // Atributos
        if (!attributes.isEmpty()) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                sb.append(" ").append(entry.getKey());
                if (!entry.getValue().isEmpty()) {
                    sb.append("=").append(entry.getValue());
                }
            }
        }
        
        if (isDeclaration) {
            // Solo declaración
            return sb.toString();
        }
        
        // Cuerpo de la función
        sb.append(" {\n");
        
        for (LLVMBasicBlock block : basicBlocks) {
            sb.append(block.toIR());
        }
        
        sb.append("}");
        
        return sb.toString();
    }
    
    /**
     * Builder para construir funciones fácilmente
     */
    public static class Builder {
        private String name;
        private LLVMType returnType;
        private List<Parameter> parameters = new ArrayList<>();
        private Map<String, String> attributes = new LinkedHashMap<>();
        private boolean isDeclaration = false;
        
        public Builder(String name) {
            this.name = name;
        }
        
        public Builder returnType(LLVMType type) {
            this.returnType = type;
            return this;
        }
        
        public Builder addParameter(String name, LLVMType type) {
            parameters.add(new Parameter(name, type));
            return this;
        }
        
        public Builder addParameter(Parameter param) {
            parameters.add(param);
            return this;
        }
        
        public Builder addAttribute(String key) {
            attributes.put(key, "");
            return this;
        }
        
        public Builder addAttribute(String key, String value) {
            attributes.put(key, value);
            return this;
        }
        
        public Builder declaration() {
            this.isDeclaration = true;
            return this;
        }
        
        public LLVMFunction build() {
            if (name == null || returnType == null) {
                throw new IllegalStateException("Name and return type are required");
            }
            
            LLVMFunction func = new LLVMFunction(name, returnType, parameters, isDeclaration);
            func.attributes.putAll(attributes);
            return func;
        }
    }
    
    @Override
    public String toString() {
        return "Function{" + name + ", " + returnType + ", " + parameters.size() + " params, " + 
               basicBlocks.size() + " blocks}";
    }
}
