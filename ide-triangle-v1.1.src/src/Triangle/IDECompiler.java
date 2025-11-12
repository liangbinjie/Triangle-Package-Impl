/*
 * IDECompiler.java
 * 
 * Extended to support LLVM code generation
 */

package Triangle;

import Triangle.LLVMCodeGenerator.LLVMCodeGenerator;
import Triangle.AbstractSyntaxTrees.Program;
import Triangle.CodeGenerator.Encoder;
import Triangle.ContextualAnalyzer.Checker;
import Triangle.SyntacticAnalyzer.Parser;
import Triangle.SyntacticAnalyzer.Scanner;
import Triangle.SyntacticAnalyzer.SourceFile;
import Triangle.SyntacticAnalyzer.SourcePosition;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class IDECompiler {

    private Scanner scanner;
    private Parser parser;
    private Checker checker;
    private Encoder encoder;
    private LLVMCodeGenerator llvmCodeGenerator;
    
    public IDEReporter report;
    public Program rootAST;

    /**
     * Constructor
     */
    public IDECompiler() {
        this.report = new IDEReporter();
    }

    /**
     * Compile Triangle program to TAM code
     */
    public boolean compileProgram(String sourceName, String objectName, boolean showTable) {
        System.out.println("********** Triangle Compiler (TAM Mode) **********");

        SourceFile source = new SourceFile(sourceName);

        if (source == null) {
            System.out.println("Can't access source file " + sourceName);
            return false;
        }

        scanner = new Scanner(source);
        report = new IDEReporter();
        parser = new Parser(scanner, report);
        checker = new Checker(report);
        encoder = new Encoder(report);

        // Syntactic Analysis
        System.out.println("Syntactic Analysis ...");
        rootAST = parser.parseProgram();

        if (report.numErrors == 0) {
            // Contextual Analysis
            System.out.println("Contextual Analysis ...");
            checker.check(rootAST);

            if (report.numErrors == 0) {
                // Code Generation
                System.out.println("TAM Code Generation ...");
                encoder.encodeRun(rootAST, showTable);

                if (report.numErrors == 0) {
                    try {
                        encoder.saveObjectProgram(objectName);
                        System.out.println("TAM compilation was successful.");
                        return true;
                    } catch (Exception e) {
                        System.out.println("Error saving object file: " + e.getMessage());
                        // Usar reportMessage en lugar de reportError
                        System.err.println("Could not save TAM object file: " + e.getMessage());
                        return false;
                    }
                }
            }
        }

        System.out.println("TAM compilation was unsuccessful.");
        return false;
    }

    /**
     * Compile Triangle program to LLVM IR
     */
    public boolean compileProgramToLLVM(String sourceName) {
        System.out.println("********** Triangle Compiler (LLVM Mode) **********");

        SourceFile source = new SourceFile(sourceName);

        if (source == null) {
            System.out.println("Can't access source file " + sourceName);
            return false;
        }

        scanner = new Scanner(source);
        report = new IDEReporter();
        parser = new Parser(scanner, report);
        checker = new Checker(report);
        llvmCodeGenerator = new LLVMCodeGenerator();

        // Syntactic Analysis
        System.out.println("Syntactic Analysis ...");
        rootAST = parser.parseProgram();

        if (report.numErrors == 0) {
            // Contextual Analysis
            System.out.println("Contextual Analysis ...");
            checker.check(rootAST);

            if (report.numErrors == 0) {
                // LLVM Code Generation
                System.out.println("LLVM Code Generation ...");
                try {
                    llvmCodeGenerator.generateRun(rootAST, sourceName);
                    System.out.println("LLVM compilation was successful.");
                    return true;
                } catch (Exception e) {
                    System.out.println("Error during LLVM generation: " + e.getMessage());
                    e.printStackTrace();
                    // Solo imprimir el error, no usar reportError
                    System.err.println("LLVM generation failed: " + e.getMessage());
                    return false;
                }
            }
        }

        System.out.println("LLVM compilation was unsuccessful.");
        return false;
    }

    /**
     * Get the generated LLVM code from file
     */
    public String getLLVMCode(String sourceName) {
        String llvmFileName = sourceName.replace(".tri", ".ll");
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(llvmFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (IOException e) {
            System.err.println("Error reading LLVM file: " + e.getMessage());
            return "; Error: Could not read LLVM file\n; File: " + llvmFileName + "\n; " + e.getMessage();
        }
    }

    /**
     * Get the last generated LLVM file path
     */
    public String getLLVMFilePath(String sourceName) {
        return sourceName.replace(".tri", ".ll");
    }
}
