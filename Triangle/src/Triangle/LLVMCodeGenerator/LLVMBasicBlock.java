package Triangle.LLVMCodeGenerator;

import java.util.*;

/**
 * Representa un bloque básico de LLVM
 * Un bloque básico es una secuencia de instrucciones con una etiqueta
 */
public class LLVMBasicBlock {
    private final String name;
    private final List<String> instructions;
    private boolean isTerminated;
    
    public LLVMBasicBlock(String name) {
        this.name = name;
        this.instructions = new ArrayList<>();
        this.isTerminated = false;
    }
    
    /**
     * Obtiene el nombre del bloque
     */
    public String getName() {
        return name;
    }
    
    /**
     * Añade una instrucción al bloque
     */
    public void addInstruction(String instruction) {
        if (isTerminated) {
            throw new IllegalStateException("Cannot add instruction to terminated block");
        }
        instructions.add(instruction);
    }
    
    /**
     * Añade múltiples instrucciones
     */
    public void addInstructions(List<String> insts) {
        for (String inst : insts) {
            addInstruction(inst);
        }
    }
    
    /**
     * Marca el bloque como terminado (tiene return/br)
     */
    public void terminate() {
        isTerminated = true;
    }
    
    /**
     * Verifica si el bloque está terminado
     */
    public boolean isTerminated() {
        return isTerminated;
    }
    
    /**
     * Obtiene las instrucciones del bloque
     */
    public List<String> getInstructions() {
        return new ArrayList<>(instructions);
    }
    
    /**
     * Genera el código LLVM del bloque
     */
    public String toIR() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":\n");
        for (String inst : instructions) {
            sb.append(inst).append("\n");
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "BasicBlock{" + name + ", " + instructions.size() + " instructions}";
    }
}
