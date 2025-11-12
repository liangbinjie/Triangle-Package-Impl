package Triangle.LLVMCodeGenerator;

import java.util.*;

/**
 * Contexto inmutable para la generación de LLVM
 * Contiene el estado necesario sin efectos secundarios
 * Usa el patrón Builder para crear nuevas versiones del contexto
 */
public class LLVMContext {
    private final Map<String, LLVMValue> symbolTable;
    private final Map<String, LLVMType> typeTable;
    private final Stack<Map<String, LLVMValue>> scopeStack;
    private final String currentFunction;
    private final int scopeLevel;
    
    private LLVMContext(Builder builder) {
        this.symbolTable = Collections.unmodifiableMap(new HashMap<>(builder.symbolTable));
        this.typeTable = Collections.unmodifiableMap(new HashMap<>(builder.typeTable));
        
        // Copiar el stack de scopes
        this.scopeStack = new Stack<>();
        for (Map<String, LLVMValue> scope : builder.scopeStack) {
            this.scopeStack.push(Collections.unmodifiableMap(new HashMap<>(scope)));
        }
        
        this.currentFunction = builder.currentFunction;
        this.scopeLevel = builder.scopeLevel;
    }
    
    /**
     * Busca un símbolo en la tabla de símbolos
     * @param name Nombre del símbolo
     * @return Valor asociado o null si no existe
     */
    public LLVMValue lookup(String name) {
        // Buscar primero en los scopes (de más reciente a más antiguo)
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Map<String, LLVMValue> scope = scopeStack.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        
        // Si no está en los scopes, buscar en la tabla global
        return symbolTable.get(name);
    }
    
    /**
     * Busca un tipo en la tabla de tipos
     * @param name Nombre del tipo
     * @return Tipo asociado o null si no existe
     */
    public LLVMType getType(String name) {
        return typeTable.get(name);
    }
    
    /**
     * Verifica si un símbolo existe en el scope actual
     * @param name Nombre del símbolo
     * @return true si existe
     */
    public boolean containsSymbol(String name) {
        if (!scopeStack.isEmpty()) {
            return scopeStack.peek().containsKey(name);
        }
        return symbolTable.containsKey(name);
    }
    
    /**
     * Verifica si un tipo existe
     * @param name Nombre del tipo
     * @return true si existe
     */
    public boolean containsType(String name) {
        return typeTable.containsKey(name);
    }
    
    /**
     * Crea un nuevo contexto con un símbolo adicional
     * @param name Nombre del símbolo
     * @param value Valor del símbolo
     * @return Nuevo contexto inmutable
     */
    public LLVMContext withSymbol(String name, LLVMValue value) {
        return new Builder(this)
            .addSymbol(name, value)
            .build();
    }
    
    /**
     * Crea un nuevo contexto con un tipo adicional
     * @param name Nombre del tipo
     * @param type Tipo
     * @return Nuevo contexto inmutable
     */
    public LLVMContext withType(String name, LLVMType type) {
        return new Builder(this)
            .addType(name, type)
            .build();
    }
    
    /**
     * Entra a un nuevo scope
     * @return Nuevo contexto con scope adicional
     */
    public LLVMContext enterScope() {
        return new Builder(this)
            .pushScope()
            .build();
    }
    
    /**
     * Sale del scope actual
     * @return Nuevo contexto sin el scope más reciente
     */
    public LLVMContext exitScope() {
        return new Builder(this)
            .popScope()
            .build();
    }
    
    /**
     * Cambia la función actual
     * @param functionName Nombre de la función
     * @return Nuevo contexto con función actualizada
     */
    public LLVMContext withFunction(String functionName) {
        return new Builder(this)
            .setCurrentFunction(functionName)
            .build();
    }
    
    /**
     * Obtiene el nombre de la función actual
     * @return Nombre de la función
     */
    public String getCurrentFunction() {
        return currentFunction;
    }
    
    /**
     * Obtiene el nivel de scope actual
     * @return Nivel de scope
     */
    public int getScopeLevel() {
        return scopeLevel;
    }
    
    /**
     * Obtiene todos los símbolos del scope actual
     * @return Mapa de símbolos
     */
    public Map<String, LLVMValue> getCurrentScopeSymbols() {
        if (!scopeStack.isEmpty()) {
            return new HashMap<>(scopeStack.peek());
        }
        return new HashMap<>(symbolTable);
    }
    
    /**
     * Builder para construir contextos inmutables
     */
    public static class Builder {
        private Map<String, LLVMValue> symbolTable = new HashMap<>();
        private Map<String, LLVMType> typeTable = new HashMap<>();
        private Stack<Map<String, LLVMValue>> scopeStack = new Stack<>();
        private String currentFunction = "";
        private int scopeLevel = 0;
        
        /**
         * Constructor vacío
         */
        public Builder() {
            // Iniciar con un scope global
            scopeStack.push(new HashMap<>());
        }
        
        /**
         * Constructor a partir de un contexto existente (copia)
         * @param context Contexto a copiar
         */
        public Builder(LLVMContext context) {
            this.symbolTable = new HashMap<>(context.symbolTable);
            this.typeTable = new HashMap<>(context.typeTable);
            
            // Copiar el stack de scopes
            this.scopeStack = new Stack<>();
            for (Map<String, LLVMValue> scope : context.scopeStack) {
                this.scopeStack.push(new HashMap<>(scope));
            }
            
            this.currentFunction = context.currentFunction;
            this.scopeLevel = context.scopeLevel;
        }
        
        /**
         * Añade un símbolo al scope actual
         * @param name Nombre del símbolo
         * @param value Valor del símbolo
         * @return Este builder
         */
        public Builder addSymbol(String name, LLVMValue value) {
            if (scopeStack.isEmpty()) {
                symbolTable.put(name, value);
            } else {
                scopeStack.peek().put(name, value);
            }
            return this;
        }
        
        /**
         * Añade un símbolo a la tabla global (no al scope actual)
         * @param name Nombre del símbolo
         * @param value Valor del símbolo
         * @return Este builder
         */
        public Builder addGlobalSymbol(String name, LLVMValue value) {
            symbolTable.put(name, value);
            return this;
        }
        
        /**
         * Añade un tipo
         * @param name Nombre del tipo
         * @param type Tipo
         * @return Este builder
         */
        public Builder addType(String name, LLVMType type) {
            typeTable.put(name, type);
            return this;
        }
        
        /**
         * Entra a un nuevo scope
         * @return Este builder
         */
        public Builder pushScope() {
            scopeStack.push(new HashMap<>());
            scopeLevel++;
            return this;
        }
        
        /**
         * Sale del scope actual
         * @return Este builder
         */
        public Builder popScope() {
            if (!scopeStack.isEmpty() && scopeStack.size() > 1) {
                scopeStack.pop();
                scopeLevel--;
            }
            return this;
        }
        
        /**
         * Establece la función actual
         * @param name Nombre de la función
         * @return Este builder
         */
        public Builder setCurrentFunction(String name) {
            this.currentFunction = name;
            return this;
        }
        
        /**
         * Añade múltiples símbolos
         * @param symbols Mapa de símbolos a añadir
         * @return Este builder
         */
        public Builder addSymbols(Map<String, LLVMValue> symbols) {
            for (Map.Entry<String, LLVMValue> entry : symbols.entrySet()) {
                addSymbol(entry.getKey(), entry.getValue());
            }
            return this;
        }
        
        /**
         * Añade múltiples tipos
         * @param types Mapa de tipos a añadir
         * @return Este builder
         */
        public Builder addTypes(Map<String, LLVMType> types) {
            typeTable.putAll(types);
            return this;
        }
        
        /**
         * Construye el contexto inmutable
         * @return Contexto inmutable
         */
        public LLVMContext build() {
            return new LLVMContext(this);
        }
    }
    
    /**
     * Crea un contexto con tipos estándar precargados
     * @return Contexto con tipos básicos
     */
    public static LLVMContext createWithStandardTypes() {
        Builder builder = new Builder();
        
        // Tipos básicos
        builder.addType("Int", LLVMType.i32());
        builder.addType("Bool", LLVMType.i1());
        builder.addType("Char", LLVMType.i8());
        
        return builder.build();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LLVMContext{\n");
        sb.append("  function=").append(currentFunction).append("\n");
        sb.append("  scopeLevel=").append(scopeLevel).append("\n");
        sb.append("  symbols={\n");
        
        for (Map.Entry<String, LLVMValue> entry : symbolTable.entrySet()) {
            sb.append("    ").append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
        }
        
        for (int i = 0; i < scopeStack.size(); i++) {
            sb.append("  scope[").append(i).append("]={\n");
            for (Map.Entry<String, LLVMValue> entry : scopeStack.get(i).entrySet()) {
                sb.append("    ").append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
            }
            sb.append("  }\n");
        }
        
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }
}
