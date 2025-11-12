# Triangle Compiler - Nueva Arquitectura LLVM

## 🎉 ¡Nueva Implementación Modular!

Este proyecto contiene una **implementación completamente nueva y modular** para la generación de código LLVM IR en el compilador Triangle.

### ✨ Lo Nuevo

- ✅ **8 clases modulares** vs 1 clase monolítica
- ✅ **Sistema de tipos type-safe** con validación
- ✅ **Contexto inmutable** para mejor razonamiento
- ✅ **Builder pattern** para instrucciones fluidas
- ✅ **Separación de responsabilidades** clara
- ✅ **Fácilmente extensible y mantenible**

## 🚀 Inicio Rápido

### Compilar un programa Triangle

```bash
# Usar nueva arquitectura LLVM (recomendado)
java -jar Triangle.jar -llvm-new programa.tri

# Usar arquitectura antigua LLVM
java -jar Triangle.jar -llvm-old programa.tri

# Compilar a TAM (sin LLVM)
java -jar Triangle.jar programa.tri
```

### Ejemplo Simple

**Archivo `test.tri`:**
```triangle
let
  var x: Integer
in
  x := 42
```

**Compilar:**
```bash
java -jar Triangle.jar -llvm-new test.tri
```

**Genera `test.ll`:**
```llvm
define i32 @main() {
entry:
  %tmp0 = alloca i32, align 4
  store i32 42, ptr %tmp0, align 4
  ret i32 0
}
```

## 📚 Documentación

### Para Empezar
1. **[INDEX.md](INDEX.md)** - 📑 Índice completo de documentación
2. **[README_SUMMARY.md](README_SUMMARY.md)** - ⭐ Resumen ejecutivo (5 min)
3. **[EXAMPLES.md](EXAMPLES.md)** - 💡 Ejemplos prácticos (15 min)

### Para Desarrolladores
4. **[LLVM_ARCHITECTURE.md](LLVM_ARCHITECTURE.md)** - 📚 Arquitectura completa (30 min)
5. **[ARCHITECTURE_DIAGRAM.md](ARCHITECTURE_DIAGRAM.md)** - 🎨 Diagramas visuales (10 min)

## 🏗️ Arquitectura

### Componentes Principales

```
LLVMCodeGenerator (Visitor)
    │
    ├─► LLVMModule (Módulo completo)
    │   ├─► LLVMFunction (Funciones)
    │   │   └─► LLVMBasicBlock (Bloques básicos)
    │   └─► Variables globales
    │
    ├─► LLVMInstructionBuilder (Genera instrucciones)
    │   ├─► Aritméticas: add, sub, mul, div
    │   ├─► Lógicas: and, or, not
    │   ├─► Memoria: load, store, alloca
    │   └─► Control: br, ret, phi
    │
    ├─► LLVMContext (Estado inmutable)
    │   ├─► Tabla de símbolos
    │   ├─► Tabla de tipos
    │   └─► Gestión de scopes
    │
    ├─► LLVMType (Sistema de tipos)
    │   ├─► IntType, PointerType
    │   ├─► ArrayType, StructType
    │   └─► VoidType, FunctionType
    │
    └─► LLVMValue (Valores)
        ├─► ConstantInt, ConstantChar
        ├─► LocalVariable, GlobalVariable
        └─► TemporaryValue, Parameter
```

## 📊 Comparación

| Aspecto | Antigua | Nueva |
|---------|---------|-------|
| **Clases** | 1 monolítica | 8 modulares |
| **Líneas** | 1,242 | ~2,900 |
| **Type-safety** | ❌ Strings | ✅ Objetos |
| **Estado** | ❌ Mutable | ✅ Inmutable |
| **Testing** | ❌ Difícil | ✅ Fácil |
| **Extensible** | ❌ | ✅ |

## ✅ Estado de Implementación

### Completado
- ✅ Sistema de tipos completo
- ✅ Sistema de valores
- ✅ Contexto inmutable
- ✅ Instruction builder (40+ instrucciones)
- ✅ Bloques básicos y funciones
- ✅ Comandos básicos (assign, if, while, let)
- ✅ Expresiones (binarias, unarias, literales)
- ✅ Control de flujo con bloques básicos
- ✅ PHI nodes para if-expressions

### Por Implementar
- ⏳ Funciones y procedimientos
- ⏳ Arrays
- ⏳ Records/Structs
- ⏳ Optimizaciones

## 🎯 Flags del Compilador

- `-llvm-new` : Usa nueva arquitectura LLVM (modular) ⭐ **Recomendado**
- `-llvm-old` : Usa arquitectura LLVM antigua
- `-llvm` : Equivalente a `-llvm-new`
- `-ast` : Muestra el árbol sintáctico abstracto
- `-table` : Muestra la tabla de símbolos

## 💡 Ejemplos Rápidos

### Variables y Asignación
```triangle
let var x: Integer in x := 10
```

### Condicional
```triangle
let var x: Integer in
  if x > 0 then x := 1 else x := 2
```

### Bucle
```triangle
let var i: Integer in
  begin
    i := 0;
    while i < 10 do i := i + 1
  end
```

### Expresiones
```triangle
let var result: Integer in
  result := (10 + 20) * 2 - 5
```

Ver más ejemplos en **[EXAMPLES.md](EXAMPLES.md)**

## 🛠️ Desarrollo

### Estructura del Proyecto

```
Triangle-Package-Impl/
├── Triangle/
│   └── src/
│       └── Triangle/
│           ├── Compiler.java
│           └── LLVMCodeGenerator/
│               ├── LLVMType.java
│               ├── LLVMValue.java
│               ├── LLVMContext.java
│               ├── LLVMInstructionBuilder.java
│               ├── LLVMBasicBlock.java
│               ├── LLVMFunction.java
│               ├── LLVMModule.java
│               ├── LLVMCodeGenerator.java
│               └── LLVMGenerator.java (antiguo)
│
├── README.md (este archivo)
├── INDEX.md
├── README_SUMMARY.md
├── LLVM_ARCHITECTURE.md
├── ARCHITECTURE_DIAGRAM.md
└── EXAMPLES.md
```

### Compilar el Proyecto

```bash
cd Triangle
ant compile
```

## 🎓 Patrones de Diseño

- **Builder Pattern**: `LLVMContext`, `LLVMFunction`, `LLVMModule`
- **Factory Pattern**: `LLVMType` factory methods
- **Visitor Pattern**: `LLVMCodeGenerator`
- **Immutable Object**: `LLVMContext`

## 📖 Más Información

- **[INDEX.md](INDEX.md)** - Índice completo y navegación
- **[README_SUMMARY.md](README_SUMMARY.md)** - Resumen ejecutivo detallado
- **[LLVM_ARCHITECTURE.md](LLVM_ARCHITECTURE.md)** - Documentación técnica completa
- **[EXAMPLES.md](EXAMPLES.md)** - 7+ ejemplos prácticos

## 🤝 Contribuir

¿Quieres contribuir? Lee:
1. [README_SUMMARY.md](README_SUMMARY.md) - Próximos pasos
2. [LLVM_ARCHITECTURE.md](LLVM_ARCHITECTURE.md) - Roadmap
3. Código fuente en `Triangle/src/Triangle/LLVMCodeGenerator/`

## 📝 Licencia

Este software se proporciona libre para uso educativo únicamente.

## 👥 Créditos

- **Original Triangle Compiler**: D.A. Watt y D.F. Brown
- **Nueva Arquitectura LLVM**: Implementada Noviembre 2025
- **Curso**: Compiladores e Intérpretes 2025

---

**¿Primera vez aquí?** → Empieza por **[INDEX.md](INDEX.md)** o **[README_SUMMARY.md](README_SUMMARY.md)**

**¿Quieres ejemplos?** → Ve a **[EXAMPLES.md](EXAMPLES.md)**

**¿Desarrollador?** → Lee **[LLVM_ARCHITECTURE.md](LLVM_ARCHITECTURE.md)**
