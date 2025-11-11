/*
 * @(#)Compiler.java                        2.1 2003/10/07
 *
 * Copyright (C) 1999, 2003 D.A. Watt and D.F. Brown
 * Dept. of Computing Science, University of Glasgow, Glasgow G12 8QQ Scotland
 * and School of Computer and Math Sciences, The Robert Gordon University,
 * St. Andrew Street, Aberdeen AB25 1HG, Scotland.
 * All rights reserved.
 *
 * This software is provided free for educational use only. It may
 * not be used for commercial purposes without the prior written permission
 * of the authors.
 *
 * Version para curso Compiladores 2025
 *
 */

package Triangle;

import Triangle.LLVMCodeGenerator.LLVMGenerator; // Cambio aquí
import Triangle.AbstractSyntaxTrees.Program;
import Triangle.CodeGenerator.Encoder;
import Triangle.ContextualAnalyzer.Checker;
import Triangle.SyntacticAnalyzer.Parser;
import Triangle.SyntacticAnalyzer.Scanner;
import Triangle.SyntacticAnalyzer.SourceFile;
import Triangle.TreeDrawer.Drawer;

/**
 * The main driver class for the Triangle compiler.
 *
 * @version		2.1 7 Oct 2003
 * @author		Deryck F. Brown
 */
public class Compiler {

    /** The filename for the object program, normally obj.tam. */
    static String objectName = "obj.tam";

    private static Scanner scanner;
    private static Parser parser;
    private static Checker checker;
    private static Encoder encoder;
    private static LLVMGenerator llvmGenerator; // Variable para LLVM
    private static ErrorReporter reporter;
    private static Drawer drawer;

    /** The AST representing the source program. */
    private static Program theAST;

    /**
     * Compile the source program to TAM machine code.
     *
     * @param	sourceName	the name of the file containing the
     *				source program.
     * @param	objectName	the name of the file containing the
     *				object program.
     * @param	showingAST	true iff the AST is to be displayed after
     *				contextual analysis (not currently implemented).
     * @param	showingTable	true iff the object description details are to
     *				be displayed during code generation (not
     *				currently implemented).
     * @return	true iff the source program is free of compile-time errors,
     *          otherwise false.
     */
    static boolean compileProgram (String sourceName, String objectName,
                                   boolean showingAST, boolean showingTable,
                                   boolean generateLLVM) {

        System.out.println("********** Triangle Compiler (LLVM Mode: " + generateLLVM + ") **********");

        SourceFile source = new SourceFile(sourceName);

        if (source == null) {
            System.out.println("Can't access source file " + sourceName);
            System.exit(1);
        }

        scanner  = new Scanner(source);
        reporter = new ErrorReporter();
        parser   = new Parser(scanner, reporter);
        checker  = new Checker(reporter);

        // Inicializar encoder (TAM) o llvmGenerator según el modo
        if (generateLLVM) {
            llvmGenerator = new LLVMGenerator();
        } else {
            encoder = new Encoder(reporter);
        }

        // Compilar
        System.out.println("Syntactic Analysis ...");
        theAST = parser.parseProgram();				// 1st pass

        if (reporter.numErrors == 0) {
            System.out.println("Contextual Analysis ...");
            checker.check(theAST);				// 2nd pass
            if (showingAST) {
                drawer.draw(theAST);
            }
            if (reporter.numErrors == 0) {
                if (generateLLVM) {
                    System.out.println("LLVM Code Generation ...");
                    llvmGenerator.generateRun(theAST, sourceName);
                } else {
                    System.out.println("TAM Code Generation ...");
                    encoder.encodeRun(theAST, showingTable);	// 3rd pass
                }
            }
        }

	boolean successful = (reporter.numErrors == 0);
        
        if (successful) {
            if (generateLLVM) {
                System.out.println("LLVM compilation was successful.");
            } else {
                try {
                    encoder.saveObjectProgram(objectName);
                    System.out.println("TAM compilation was successful.");
                } catch (Exception e) {
                    System.out.println("Error saving object file: " + e.getMessage());
                    successful = false;
                }
            }
        } else {
            System.out.println("Compilation was unsuccessful.");
        }
        return successful;
    }

    /**
     * Triangle compiler main program.
     *
     * @param	args	the only command-line argument to the program specifies
     *                  the source filename.
     */
    public static void main(String[] args) {
        boolean compilingOK;
        boolean showingAST = false;
        boolean showingTable = false;
        boolean generateLLVM = false;

        String sourceName = "test.tri";
        String objectName = "test.tam";

        // Procesar argumentos
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-llvm")) {
                generateLLVM = true;
            } else if (args[i].equals("-ast")) {
                showingAST = true;
            } else if (args[i].equals("-table")) {
                showingTable = true;
            } else if (i == args.length - 1) {
                sourceName = args[i];
                objectName = sourceName.replace(".tri", ".tam");
            }
        }

        compilingOK = compileProgram(sourceName, objectName, 
                                      showingAST, showingTable, generateLLVM);

        System.exit(compilingOK ? 0 : 1);
    }
}
