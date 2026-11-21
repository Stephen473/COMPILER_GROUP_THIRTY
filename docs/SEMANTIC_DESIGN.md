# Semantic Analysis Design (Phase 3)

The scanner checks that the text is made of valid tokens. The parser checks that
the tokens form a valid program shape. The **semantic analyzer** checks that the
program **means** something: names exist, types fit, and the rules of the language
are followed.

```
tokens -> Parser -> syntax tree -> Analyzer -> checked tree -> IrGenerator -> intermediate code
                                     |
                              semantic errors
```

The analyzer runs only if the parser found no errors. The intermediate code is
generated only if the analyzer found no errors.

## 1. What is checked

| Rule (from LANGUAGE_SPEC.md) | Error message |
|------------------------------|---------------|
| A name must be declared before use | `Undeclared variable 'x'` |
| The same name cannot be declared twice in one scope | `'x' is already declared in this scope (first declared at line N)` |
| Arithmetic needs `int` operands | `Operator '+' needs int operands, but found bool and int` |
| `<` `<=` `>` `>=` need `int` operands | `Operator '<' needs int operands, but found ...` |
| `==` `!=` need operands of the same type | `Operator '==' needs two operands of the same type, but found int and bool` |
| `&&` `\|\|` need `bool` operands | `Operator '&&' needs bool operands, but found ...` |
| unary `-` needs `int`, `!` needs `bool` | `Operator '!' needs a bool operand, but found int` |
| An initializer must match the declared type | `Cannot initialize 'x' (int) with a bool value` |
| An assigned value must match the variable | `Cannot assign a bool value to 'x' (int)` |
| `if` and `while` conditions must be `bool` | `The condition of 'if' must be bool, but found int` |
| A `const` cannot be assigned | `Cannot assign to 'LIMIT': it is a const (declared at line N)` |
| `break` and `continue` only inside a loop | `'break' can only be used inside a while loop` |

Every message names the line and the column. A binary operator error points at the
operator. A type mismatch points at the value.

## 2. The symbol table

A `Symbol` is a declared name: its type, whether it is `const`, and where it was
declared. The `SymbolTable` is a **stack of scopes**. Each block pushes a scope when it
starts and pops it when it ends.

- `declare` adds a name to the innermost scope, and fails if it is already there.
- `lookup` searches from the innermost scope outwards. That is why an inner
  declaration **shadows** an outer one, and why the outer one is visible again after
  the block ends.
- `lookupInCurrentScope` looks only at the innermost scope, to find duplicates.

An initializer is checked **before** its variable is declared, so `int x = x;` is an
error (unless an outer `x` exists, in which case it refers to that one).

## 3. Types of expressions

`expr()` checks an expression and returns its type. The type of each node is stored in
a table (an `IdentityHashMap`, keyed by node identity because two nodes with the same
content are equal records). `typeOf(expr)` reads it back. The IR generator needs it to
know whether a `print` shows a number or `true`/`false`.

## 4. One mistake, one error

When something is already wrong, its type is **unknown** (`null`). An operator with an
unknown operand also gives unknown and reports nothing more. So `print(!(x + 1));` with
an undeclared `x` gives one error, not two. Errors are sorted into source order at the end.

## 5. Public API

```java
Analyzer analyzer = new Analyzer();
analyzer.analyze(program);              // never throws on a bad program
if (analyzer.hasErrors()) { analyzer.getErrors() ... }
Type t = analyzer.typeOf(expression);   // null if it had an error
```

## 6. Not checked here

- Division or remainder by zero: a runtime error (the interpreter reports it).
- Integer overflow: arithmetic wraps, by the language rules.
- Unused variables, unreachable code: not part of version 1.0.
