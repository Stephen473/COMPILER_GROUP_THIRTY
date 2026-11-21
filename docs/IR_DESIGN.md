# Intermediate Representation Design (Phase 3)

The intermediate representation (IR) is a simple, flat form of the program between
the syntax tree and the final output. We use **three-address code**: every instruction
does one small thing, and control flow is made of **labels and jumps**.

Why not generate output straight from the tree? The tree is nested and rich, which
is good for checking. The IR is flat and simple, which is good for translating to a
target (Phase 4) and for optimizing (Phase 5): the optimizer only has to understand
seven kinds of instruction.

## 1. Operands

| Operand | Meaning | Printed as |
|---------|---------|------------|
| `Const` | an integer constant. `false` is 0 and `true` is 1 | `5`, `0`, `1` |
| `Name` | a variable or a temporary | `sum`, `x.2`, `%1` |

Names cannot clash with names in an Eyra program:

- A **temporary** is `%1`, `%2`, ... (the `%` cannot appear in an identifier).
- When an inner scope reuses a name, the second declaration is `x.2`, the third `x.3`,
  and so on (the `.` cannot appear in an identifier). The IR has no scopes at all.

## 2. Instructions

| Instruction | Printed as | Meaning |
|-------------|-----------|---------|
| `Copy` | `x = 5` | store a value |
| `Binary` | `%1 = a + b` | `+ - * / % < <= > >= == !=` |
| `Unary` | `%1 = -a`, `%1 = !a` | negate, or logical not |
| `Label` | `L1:` | a place to jump to |
| `Jump` | `jump L1` | go to the label |
| `JumpIfFalse` | `jumpIfFalse %1, L2` | go to the label if the value is 0 |
| `Print` | `print int x`, `print bool %1` | print; the type decides the format |

## 3. How statements become code

**Expressions:** the result of every operator goes into a new temporary.
`b = a + 2 * 3` becomes

```
    %1 = 2 * 3
    %2 = a + %1
    b = %2
```

**`if`:**

```
    <condition>
    jumpIfFalse c, Lelse
    <then>
    jump Lend          (only when there is an else)
Lelse:
    <else>
Lend:
```

**`while`** (`continue` jumps to `Lstart`, `break` jumps to `Lend`):

```
Lstart:
    <condition>
    jumpIfFalse c, Lend
    <body>
    jump Lstart
Lend:
```

**Short-circuit `&&` and `||`.** The right side must not run if the left side decides
the answer, so the generator uses jumps:

```
a && b:    r = a ; jumpIfFalse r, Lend ; r = b ; Lend:
a || b:    r = a ; n = !r ; jumpIfFalse n, Lend ; r = b ; Lend:
```

## 4. Example

`examples/sample.ey` (sum of the even numbers from 1 to 10) becomes:

```
    LIMIT = 10
    n = 1
    sum = 0
L1:
    %1 = n <= LIMIT
    jumpIfFalse %1, L2
    %2 = n % 2
    %3 = %2 == 0
    jumpIfFalse %3, L3
    %4 = sum + n
    sum = %4
L3:
    %5 = n + 1
    n = %5
    jump L1
L2:
    print int sum
    %6 = sum > 20
    big = %6
    print bool big
```

The code is not perfect (`%4 = sum + n` then `sum = %4` could be one instruction).
Cleaning that up is the job of the optimizer in Phase 5.

## 5. The interpreter

`IrInterpreter` runs the IR directly. It follows the language rules: 32-bit wrap-around,
division by zero is a runtime error, and a step limit (10 million) stops endless loops.
It lets us **run** programs now, and in Phase 5 it lets us check that an optimized program
prints exactly what the unoptimized one did.

## 6. Command line

```
java -jar target/compiler-group-30-1.0-SNAPSHOT.jar [--tokens | --ast | --run] program.ey
```

| Flag | Output |
|------|--------|
| (none) | the intermediate code |
| `--tokens` | the token list |
| `--ast` | the syntax tree |
| `--run` | what the program prints |

Exit codes: 0 success, 64 wrong usage, 65 errors in the program (lexical, syntax or
semantic), 66 file not readable, 70 runtime error.
