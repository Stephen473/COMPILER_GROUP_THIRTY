# Eyra Language Specification, Version 1.0

*Eyra* is the name of our language.
Source files use the extension `.ey`.

## 1. Overview

Eyra is a small, statically typed, imperative language. It has two types
(`int` and `bool`), constants, assignment, arithmetic, comparison and logical
operators, `print`, `if / else`, `while` loops with `break` and `continue`,
and block scoping.

It is implemented in Java through five compiler phases: scanner, parser,
semantic analysis with intermediate representation, code generation, and
optimizer.

A program is a plain sequence of statements. There is no `main` function.

## 2. Types

| Type | Values | Notes |
|------|--------|-------|
| `int` | -2147483648 to 2147483647 | 32-bit signed. Arithmetic wraps on overflow (two's complement). |
| `bool` | `true`, `false` | Never converts to or from `int`. |

There are no strings and no decimal numbers in version 1.0.

Eyra has **no implicit conversion**. `if (x)` with an `int` `x` is a type
error; write `if (x != 0)`.

## 3. Declarations

```
int count = 0;
bool done = false;
const int LIMIT = 100;
int total;            // allowed: defaults to 0
bool flag;            // allowed: defaults to false
```

Rules:

1. A variable must be declared before it is used.
2. A declaration without an initializer gives the variable its default value (`0` or `false`).
3. A `const` declaration **must** have an initializer, and the constant can never be assigned again.
4. The same name cannot be declared twice in the same scope.

## 4. Assignment

```
count = count + 1;
done = count >= LIMIT;
```

The assigned value must have the variable's type. Assigning to a `const` is a
semantic error. Assignment is a statement, not an expression, so `a = b = 1;`
is invalid.

## 5. Identifiers

- Start with a letter (`A-Z`, `a-z`) or underscore.
- Continue with letters, digits or underscores.
- Case-sensitive: `total`, `Total` and `TOTAL` are three different names.
- A reserved word cannot be used as an identifier.

Valid: `age`, `_tmp`, `student_name`, `x2`. Invalid: `2x`, `my-var`, `cost$`.

## 6. Literals

**Integer literals** are decimal digits only.

- `0` is valid; leading zeros are not (`007` is an error).
- The largest literal is `2147483647`. Larger values are a lexical error.
- Negative numbers are written with the unary minus operator. The scanner
  produces a separate `MINUS` token and the parser decides whether it is unary or binary.

**Boolean literals** are `true` and `false`.

## 7. Operators

### 7.1 Arithmetic (int operands, int result)

| Operator | Meaning |
|----------|---------|
| `+` `-` `*` | addition, subtraction, multiplication |
| `/` | integer division, truncating toward zero (`-7 / 2` is `-3`) |
| `%` | remainder, sign follows the left operand (`-7 % 2` is `-1`) |
| unary `-` | negation |

### 7.2 Comparison (bool result)

| Operator | Operands |
|----------|----------|
| `<` `<=` `>` `>=` | both `int` |
| `==` `!=` | both `int`, or both `bool` |

### 7.3 Logical (bool operands, bool result)

| Operator | Meaning |
|----------|---------|
| `&&` | and, **short-circuit** |
| `\|\|` | or, **short-circuit** |
| `!` | not |

Short-circuit means the right side is not evaluated if the left side already
decides the result.

### 7.4 Precedence (lowest to highest)

| Level | Operators | Associativity |
|-------|-----------|---------------|
| 1 | `\|\|` | left |
| 2 | `&&` | left |
| 3 | `==` `!=` | left |
| 4 | `<` `<=` `>` `>=` | left |
| 5 | `+` `-` | left |
| 6 | `*` `/` `%` | left |
| 7 | unary `-` `!` | right |
| 8 | parentheses `( )` | n/a |

`2 + 3 * 4` is `14`, and `(2 + 3) * 4` is `20`.

## 8. Statements

### 8.1 print

```
print(expression);
```

Prints one value per line: an `int` in decimal, a `bool` as `true` or `false`.

### 8.2 if / else

```
if (n % 2 == 0) {
    print(n);
} else if (n > 10) {
    print(0);
} else {
    print(1);
}
```

The condition must be `bool` and in parentheses. Bodies must be in braces
(so there is no dangling-else problem). `else` is optional.

### 8.3 while, break, continue

```
while (i < 10) {
    i = i + 1;
    if (i == 3) { continue; }
    if (i == 8) { break; }
    print(i);
}
```

`break` leaves the nearest enclosing loop. `continue` jumps to the next
condition check. Using either outside a loop is a semantic error.

### 8.4 Blocks

`{ ... }` groups statements and starts a new scope. A block can also appear on
its own as a statement.

## 9. Scope

- Each block has its own scope.
- A name declared in a block is not visible after the block ends.
- An inner block may declare a name that already exists in an outer block
  (the inner one shadows the outer one inside that block).
- Declaring the same name twice in one scope is a semantic error.

## 10. Comments

```
// line comment, ends at the end of the line
/* block comment,
   may span several lines */
```

Block comments do not nest.

## 11. Lexical conventions

- Whitespace (space, tab, carriage return, newline) separates tokens and is otherwise ignored.
- Keywords are lowercase; `INT` and `Print` are ordinary identifiers.
- Source text is ASCII outside comments. Any other character is a lexical error.
- Punctuation: `;` `(` `)` `{` `}`. There are no commas in version 1.0.

## 12. Reserved words

`int` `bool` `const` `true` `false` `if` `else` `while` `break` `continue` `print`

## 13. Errors by phase

| Phase | Examples |
|-------|----------|
| Lexical | unexpected character `@`; lone `&`; `2x`; `007`; literal too large; unterminated `/*` |
| Syntax | missing `;`; unmatched `}`; `if` without parentheses; `const` without initializer |
| Semantic | undeclared name; duplicate declaration in a scope; type mismatch; non-bool condition; assignment to `const`; `break` outside a loop |
| Code generation | construct not supported by the target |
| Optimization | transformation that would change program meaning |
| Runtime | division or modulo by zero; step limit exceeded (infinite-loop guard) |

Every message names the line number, and the scanner and parser also give the column.
The scanner reports **all** lexical errors it finds in one run; later phases do
not run if the scanner reported any.

## 14. Example program

```
// Sum the even numbers from 1 to 10
const int LIMIT = 10;
int n = 1;
int sum = 0;

while (n <= LIMIT) {
    if (n % 2 == 0) {
        sum = sum + n;
    }
    n = n + 1;
}

print(sum);
bool big = sum > 20;
print(big);
```

Expected output:

```
30
true
```

## 15. Grammar (for the parser phase)

```
program     -> statement* EOF
statement   -> declaration | assignment | printStmt | ifStmt | whileStmt
             | "break" ";" | "continue" ";" | block
declaration -> ["const"] type IDENTIFIER ["=" expression] ";"
type        -> "int" | "bool"
assignment  -> IDENTIFIER "=" expression ";"
printStmt   -> "print" "(" expression ")" ";"
ifStmt      -> "if" "(" expression ")" block ["else" (ifStmt | block)]
whileStmt   -> "while" "(" expression ")" block
block       -> "{" statement* "}"

expression  -> or
or          -> and ("||" and)*
and         -> equality ("&&" equality)*
equality    -> comparison (("==" | "!=") comparison)*
comparison  -> term (("<" | "<=" | ">" | ">=") term)*
term        -> factor (("+" | "-") factor)*
factor      -> unary (("*" | "/" | "%") unary)*
unary       -> ("-" | "!") unary | primary
primary     -> INT_LITERAL | BOOL_LITERAL | IDENTIFIER | "(" expression ")"
```
The rule "`const` needs an initializer" is checked by the parser (decided in
Phase 2): `const int X;` is a syntax error.

## 16. Not in version 1.0

Strings, decimals, functions, arrays, input, `for` loops, nested block comments, commas.
