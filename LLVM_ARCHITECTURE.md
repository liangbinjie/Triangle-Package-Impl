# Nueva Arquitectura LLVM para Triangle Compiler

## Descripción General

Se ha implementado una arquitectura completamente nueva y modular para la generación de código LLVM IR en el compilador Triangle. Esta nueva arquitectura sigue principios de diseño orientado a objetos, separación de responsabilidades y uso de objetos inmutables.

## Estructura de la Nueva Arquitectura

### 1. **LLVMType.java** - Sistema de Tipos Type-Safe

Jerarquía de clases que representa todos los tipos LLVM de forma segura en tiempo de compilación:

- `IntType`: Tipos enteros (i1, i8, i32, i64, etc.)
- `PointerType`: Punteros opacos (ptr)
- `ArrayType`: Arrays con tamaño fijo
- `StructType`: Structs nominales o anónimos
- `VoidType`: Tipo void
- `FunctionType`: Tipos de función

**Características:**
- Validación en tiempo de compilación
- Cálculo automático de alineación y tamaño
- Factory methods para tipos comunes: `i32()`, `ptr()`, `array()`, etc.

```java
// Ejemplo de uso
LLVMType intType = LLVMType.i32();
LLVMType arrayType = LLVMType.array(10, LLVMType.i32());
LLVMType ptrType = LLVMType.ptr(intType);
```

### 2. **LLVMValue.java** - Representación de Valores

Jerarquía de clases para valores en LLVM IR:

- `ConstantInt`: Constantes enteras
- `ConstantChar`: Constantes de caracteres
- `ConstantString`: Strings constantes
- `LocalVariable`: Variables locales (registros SSA)
- `GlobalVariable`: Variables globales
- `TemporaryValue`: Valores temporales numerados
- `Parameter`: Parámetros de función
- `UndefValue`, `NullValue`, `ZeroValue`: Valores especiales
- `Label`: Etiquetas de bloques básicos

**Características:**
- Type-safe: cada valor conoce su tipo
- Generación automática de representación IR
- Distinción clara entre constantes y variables

### 3. **LLVMContext.java** - Gestión de Estado Inmutable

Contexto inmutable que mantiene el estado del generador:

- Tabla de símbolos con scopes anidados
- Tabla de tipos
- Stack de scopes para visibilidad léxica
- Función actual

**Características:**
- **Inmutabilidad**: Cada cambio crea un nuevo contexto
- **Builder Pattern**: Construcción fluida de contextos
- **Scope Management**: Entrada y salida de scopes automática

```java
// Ejemplo de uso
LLVMContext context = LLVMContext.createWithStandardTypes();
context = context.enterScope();
context = context.withSymbol("x", xValue);
context = context.exitScope();
```

### 4. **LLVMInstructionBuilder.java** - Builder Fluido de Instrucciones

Builder que genera instrucciones LLVM de forma conveniente:

**Instrucciones Aritméticas:**
- `add()`, `sub()`, `mul()`, `sdiv()`, `srem()`, `neg()`

**Instrucciones Lógicas:**
- `and()`, `or()`, `xor()`, `not()`

**Comparaciones:**
- `icmpEq()`, `icmpNe()`, `icmpSlt()`, `icmpSle()`, `icmpSgt()`, `icmpSge()`

**Memoria:**
- `alloca()`: Reserva espacio en stack
- `load()`: Carga valores desde punteros
- `store()`: Almacena valores en punteros
- `getelementptr()`: Calcula direcciones de elementos

**Control de Flujo:**
- `br()`: Branch incondicional
- `brCond()`: Branch condicional
- `ret()`, `retVoid()`: Retornos
- `switchInst()`: Switch

**Llamadas:**
- `call()`: Llamada con retorno
- `callVoid()`: Llamada sin retorno

**Conversiones:**
- `zext()`, `sext()`, `trunc()`, `bitcast()`

**Otras:**
- `phi()`: PHI nodes para SSA
- `select()`: Operador ternario

```java
// Ejemplo de uso
LLVMInstructionBuilder builder = new LLVMInstructionBuilder();
LLVMValue a = builder.alloca(LLVMType.i32());
LLVMValue val = new LLVMValue.ConstantInt(42);
builder.store(val, a);
LLVMValue loaded = builder.load(LLVMType.i32(), a);
LLVMValue result = builder.add(loaded, new LLVMValue.ConstantInt(10));
```

### 5. **LLVMBasicBlock.java** - Bloques Básicos

Representa un bloque básico de LLVM:

- Secuencia de instrucciones con una etiqueta
- Validación de terminación (un bloque solo puede terminar una vez)
- Gestión automática de instrucciones

### 6. **LLVMFunction.java** - Funciones

Representa una función completa:

- Nombre, tipo de retorno y parámetros
- Colección de bloques básicos
- Atributos de función
- Soporte para declaraciones (sin cuerpo)

**Builder Pattern:**
```java
LLVMFunction func = new LLVMFunction.Builder("myFunc")
    .returnType(LLVMType.i32())
    .addParameter("x", LLVMType.i32())
    .addParameter("y", LLVMType.i32())
    .build();
```

### 7. **LLVMModule.java** - Módulo Completo

Representa un módulo LLVM completo:

- Funciones definidas
- Variables globales
- Declaraciones externas
- Definiciones de tipos
- Metadatos
- Target triple y data layout

**Características:**
- Generación automática de declaraciones estándar (printf, putchar, etc.)
- Exportación a archivo .ll
- Builder pattern para configuración

### 8. **LLVMCodeGenerator.java** - Generador Principal

Implementa el patrón Visitor para recorrer el AST y generar código LLVM:

**Características:**
- Separación clara de responsabilidades
- Uso de los componentes modulares
- Gestión automática de scopes y contextos
- Generación de bloques básicos para control de flujo

**Soporta:**
- ✅ Comandos: assign, if, while, let, sequential
- ✅ Expresiones: binarias, unarias, enteras, caracteres, if-expression
- ✅ Declaraciones: var, const, type
- ✅ Control de flujo con bloques básicos
- ✅ PHI nodes para if-expressions
- ⏳ Funciones y procedimientos (TODO)
- ⏳ Arrays y records (TODO)

## Ventajas de la Nueva Arquitectura

### 1. **Separación de Responsabilidades**
Cada clase tiene un propósito único y bien definido:
- `LLVMType`: Maneja tipos
- `LLVMValue`: Maneja valores
- `LLVMContext`: Maneja estado
- `LLVMInstructionBuilder`: Genera instrucciones
- `LLVMModule`: Organiza el código
- `LLVMCodeGenerator`: Visita el AST

### 2. **Type-Safety**
Los tipos LLVM son objetos con validación en tiempo de compilación, reduciendo errores.

### 3. **Inmutabilidad**
`LLVMContext` es inmutable, facilitando el razonamiento sobre el estado del programa.

### 4. **Testabilidad**
Cada componente puede ser probado independientemente.

### 5. **Extensibilidad**
Fácil añadir:
- Nuevas instrucciones en `LLVMInstructionBuilder`
- Nuevos tipos en `LLVMType`
- Nuevas optimizaciones

### 6. **Mantenibilidad**
Código más limpio, organizado y fácil de entender.

### 7. **Debugging**
Estado claro y trazable en cada paso.

## Comparación con la Arquitectura Anterior

| Aspecto | Arquitectura Antigua | Nueva Arquitectura |
|---------|---------------------|-------------------|
| **Organización** | Una clase monolítica | 8 clases especializadas |
| **Estado** | Variables mutables globales | Contexto inmutable |
| **Tipos** | Strings sin validación | Jerarquía type-safe |
| **Instrucciones** | Concatenación de strings | Builder fluido |
| **Testing** | Difícil | Fácil por componentes |
| **Extensibilidad** | Modificar clase grande | Añadir a builder |
| **Scopes** | Mapas mutables | Stack inmutable |

## Uso

### Compilar con la Nueva Arquitectura

```bash
# Usar nueva arquitectura LLVM (por defecto con -llvm)
java -jar Triangle.jar -llvm-new archivo.tri

# Usar arquitectura antigua LLVM
java -jar Triangle.jar -llvm-old archivo.tri

# Compilar a TAM (sin LLVM)
java -jar Triangle.jar archivo.tri
```

### Flags Disponibles

- `-llvm-new`: Usa nueva arquitectura LLVM (modular)
- `-llvm-old`: Usa arquitectura LLVM antigua
- `-llvm`: Equivalente a `-llvm-new`
- `-ast`: Muestra el AST
- `-table`: Muestra tabla de símbolos

### Ejemplo de Código Generado

Para un programa simple:
```triangle
let
  var x: Integer
in
  x := 42;
  if x > 0 then
    x := x + 10
  else
    x := 0
```

Se genera:
```llvm
; Module: test
; Generated by Triangle Compiler - LLVM Code Generator

target triple = "x86_64-pc-windows-msvc"
target datalayout = "e-m:w-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"

; External Function Declarations
declare i32 @printf(ptr %format)
declare i32 @putchar(i32 %c)
declare i32 @getchar()
declare ptr @malloc(i64 %size)
declare void @free(ptr %ptr)

; Function Definitions
define i32 @main() {
entry:
  %tmp0 = alloca i32, align 4
  store i32 42, ptr %tmp0, align 4
  %tmp1 = load i32, ptr %tmp0, align 4
  %tmp2 = icmp sgt i32 %tmp1, 0
  br i1 %tmp2, label %if.then0, label %if.else1

if.then0:
  %tmp3 = load i32, ptr %tmp0, align 4
  %tmp4 = add nsw i32 %tmp3, 10
  store i32 %tmp4, ptr %tmp0, align 4
  br label %if.end2

if.else1:
  store i32 0, ptr %tmp0, align 4
  br label %if.end2

if.end2:
  ret i32 0
}
```

## Patrón de Diseño Utilizado

### Builder Pattern
Utilizado en:
- `LLVMContext.Builder`
- `LLVMFunction.Builder`
- `LLVMModule.Builder`
- `LLVMInstructionBuilder`

### Visitor Pattern
`LLVMCodeGenerator` implementa `Visitor` para recorrer el AST.

### Factory Pattern
Factory methods en `LLVMType` para crear tipos comunes.

### Immutable Object Pattern
`LLVMContext` es inmutable para facilitar el razonamiento.

## Roadmap

### Implementado ✅
- Sistema de tipos completo
- Sistema de valores
- Context management
- Instruction builder
- Bloques básicos y funciones
- Módulos
- Comandos básicos
- Expresiones básicas
- Control de flujo
- Integración con Compiler

### Por Implementar ⏳
1. **Funciones y Procedimientos**
   - Declaración de funciones
   - Llamadas con parámetros
   - Retornos de valores

2. **Arrays**
   - Acceso a elementos
   - Inicialización
   - Bounds checking

3. **Records/Structs**
   - Definición de tipos
   - Acceso a campos
   - Inicialización

4. **Optimizaciones**
   - Constant folding
   - Dead code elimination
   - Basic block merging

5. **Mejoras**
   - Mejor manejo de errores
   - Mensajes de error detallados
   - Validación de tipos más estricta

## Conclusión

La nueva arquitectura LLVM proporciona una base sólida, mantenible y extensible para la generación de código LLVM IR. Siguiendo principios de diseño modernos y patrones establecidos, el código es más fácil de entender, modificar y probar.

La separación de responsabilidades y el uso de objetos inmutables facilitan el desarrollo futuro y la adición de nuevas características sin afectar el código existente.
