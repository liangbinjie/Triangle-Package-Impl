# Diagrama de la Nueva Arquitectura LLVM

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Triangle Compiler                            │
│                         (Compiler.java)                              │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             │ Selecciona modo
                             │
                ┌────────────┴────────────┐
                │                         │
         ┌──────▼──────┐          ┌──────▼──────────┐
         │ TAM Encoder │          │ LLVM Generator  │
         └─────────────┘          └──────┬──────────┘
                                         │
                                         │ Elige arquitectura
                                         │
                          ┌──────────────┴──────────────┐
                          │                             │
                  ┌───────▼──────┐            ┌────────▼─────────┐
                  │ LLVMGenerator│            │LLVMCodeGenerator │
                  │   (Antigua)  │            │    (Nueva)       │
                  └──────────────┘            └────────┬─────────┘
                                                       │
                                                       │ Usa componentes
                                                       │
         ┌─────────────────────────────────────────────┴───────────────────────────┐
         │                                                                           │
         │                    Nueva Arquitectura Modular                            │
         │                                                                           │
         │  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐               │
         │  │  LLVMType    │  │  LLVMValue   │  │  LLVMContext    │               │
         │  │              │  │              │  │                 │               │
         │  │ • IntType    │  │ • ConstantInt│  │ • Symbol Table  │               │
         │  │ • PointerType│  │ • LocalVar   │  │ • Type Table    │               │
         │  │ • ArrayType  │  │ • GlobalVar  │  │ • Scope Stack   │               │
         │  │ • StructType │  │ • Temporary  │  │ • Builder       │               │
         │  │ • VoidType   │  │ • Parameter  │  │ • Immutable     │               │
         │  └──────────────┘  └──────────────┘  └─────────────────┘               │
         │                                                                           │
         │  ┌───────────────────────────┐  ┌──────────────────┐                    │
         │  │ LLVMInstructionBuilder    │  │  LLVMBasicBlock  │                    │
         │  │                           │  │                  │                    │
         │  │ • Arithmetic (add, sub)   │  │ • Label          │                    │
         │  │ • Logical (and, or, not)  │  │ • Instructions   │                    │
         │  │ • Comparison (icmp)       │  │ • Termination    │                    │
         │  │ • Memory (load, store)    │  └──────────────────┘                    │
         │  │ • Control (br, ret)       │                                           │
         │  │ • Calls (call, callVoid)  │  ┌──────────────────┐                    │
         │  │ • Conversions (zext,sext) │  │  LLVMFunction    │                    │
         │  │ • PHI nodes               │  │                  │                    │
         │  └───────────────────────────┘  │ • Name           │                    │
         │                                  │ • Return Type    │                    │
         │  ┌───────────────────────────┐  │ • Parameters     │                    │
         │  │      LLVMModule           │  │ • Basic Blocks   │                    │
         │  │                           │  │ • Attributes     │                    │
         │  │ • Functions               │  └──────────────────┘                    │
         │  │ • Global Variables        │                                           │
         │  │ • Declarations            │                                           │
         │  │ • Type Definitions        │                                           │
         │  │ • Target Triple           │                                           │
         │  │ • Data Layout             │                                           │
         │  │ • Standard Lib Decls      │                                           │
         │  └───────────────────────────┘                                           │
         │                                                                           │
         └───────────────────────────────────────────────────────────────────────────┘
                                              │
                                              │ Genera
                                              ▼
                                    ┌──────────────────┐
                                    │   archivo.ll     │
                                    │   (LLVM IR)      │
                                    └──────────────────┘
```

## Flujo de Generación de Código

```
AST Node (Program)
    │
    ▼
LLVMCodeGenerator.visitProgram()
    │
    ├─► Crea LLVMContext inicial
    │   └─► context = LLVMContext.createWithStandardTypes()
    │
    ├─► Crea LLVMModule
    │   └─► module = new LLVMModule.Builder("moduleName").build()
    │
    ├─► Crea función main
    │   └─► currentFunction = new LLVMFunction.Builder("main")
    │                           .returnType(LLVMType.i32())
    │                           .build()
    │
    ├─► Crea bloque básico entry
    │   └─► currentBlock = new LLVMBasicBlock("entry")
    │
    ├─► Crea instruction builder
    │   └─► builder = new LLVMInstructionBuilder()
    │
    ├─► Visita comandos (ast.C.visit())
    │   │
    │   ├─► VarDeclaration
    │   │   ├─► Convierte tipo: convertType(ast.T)
    │   │   ├─► Genera alloca: builder.alloca(type)
    │   │   └─► Registra: context = context.withSymbol(name, ptr)
    │   │
    │   ├─► AssignCommand
    │   │   ├─► Visita expresión: rhs = ast.E.visit()
    │   │   ├─► Visita vname: lhs = ast.V.visit()
    │   │   └─► Genera store: builder.store(rhs, lhs)
    │   │
    │   ├─► IfCommand
    │   │   ├─► Evalúa condición: cond = ast.E.visit()
    │   │   ├─► Crea etiquetas: thenLabel, elseLabel, endLabel
    │   │   ├─► Genera branch: builder.brCond(cond, then, else)
    │   │   ├─► Genera bloque then
    │   │   ├─► Genera bloque else
    │   │   └─► Genera bloque end
    │   │
    │   └─► BinaryExpression
    │       ├─► Visita left: left = ast.E1.visit()
    │       ├─► Visita right: right = ast.E2.visit()
    │       └─► Genera operación: builder.add(left, right)
    │
    ├─► Añade return si necesario
    │   └─► builder.ret(new ConstantInt(0))
    │
    ├─► Añade instrucciones al bloque
    │   └─► currentBlock.addInstructions(builder.getInstructions())
    │
    ├─► Añade bloque a función
    │   └─► currentFunction.addBasicBlock(currentBlock)
    │
    ├─► Añade función a módulo
    │   └─► module.addFunction(currentFunction)
    │
    └─► Genera código LLVM
        └─► return module.toIR()
```

## Ventajas de la Separación

### 1. Testing Independiente

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Test Types │     │ Test Values │     │Test Context │
│             │     │             │     │             │
│ LLVMType    │     │ LLVMValue   │     │LLVMContext  │
│   Tests     │     │   Tests     │     │   Tests     │
└─────────────┘     └─────────────┘     └─────────────┘

┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│Test Builder │     │Test Blocks  │     │Test Module  │
│             │     │             │     │             │
│Instruction  │     │BasicBlock   │     │LLVMModule   │
│Builder Tests│     │   Tests     │     │   Tests     │
└─────────────┘     └─────────────┘     └─────────────┘
```

### 2. Desarrollo Paralelo

```
Developer A          Developer B          Developer C
    │                    │                    │
    ▼                    ▼                    ▼
Implementa           Implementa           Implementa
Arrays               Records              Funciones
    │                    │                    │
    ├─► LLVMType        ├─► LLVMType        ├─► LLVMFunction
    │   .ArrayType      │   .StructType     │   .Builder
    │                    │                    │
    ├─► Builder         ├─► Builder         ├─► Builder
    │   .getelementptr  │   .getelementptr  │   .call
    │                    │                    │
    └─► Generator       └─► Generator       └─► Generator
        .visitArray         .visitRecord        .visitFunc
```

### 3. Mantenimiento Modular

```
Bug en generación de arrays
    │
    ▼
Identifica componente afectado
    │
    ├─► ¿Es problema de tipos?
    │   └─► Revisar LLVMType.ArrayType
    │
    ├─► ¿Es problema de instrucciones?
    │   └─► Revisar LLVMInstructionBuilder.getelementptr
    │
    └─► ¿Es problema de visitor?
        └─► Revisar LLVMCodeGenerator.visitArrayExpression
```

## Patrones de Diseño Aplicados

### Builder Pattern
```
LLVMContext context = new LLVMContext.Builder()
    .addSymbol("x", xValue)
    .addType("MyType", myType)
    .pushScope()
    .build();
```

### Factory Pattern
```
LLVMType type = LLVMType.i32();
LLVMValue value = new LLVMValue.ConstantInt(42);
```

### Visitor Pattern
```
AST Node → LLVMCodeGenerator.visit() → LLVM IR
```

### Immutable Object Pattern
```
context1 = context.withSymbol("x", val)  // Nuevo contexto
context2 = context1.enterScope()          // Otro nuevo contexto
context1 ≠ context2                       // Distintos objetos
```
