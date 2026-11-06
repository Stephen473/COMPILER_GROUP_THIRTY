# Parser Design (Phase 2)

The parser is the second phase of the Eyra compiler. It takes the token list from
the scanner and builds a **syntax tree** (an AST, abstract syntax tree). It also
reports syntax errors: code that is made of valid tokens but is not a valid program,
such as a missing `;` or an unmatched `}`.

```
source text  ->  Lexer  ->  tokens  ->  Parser  ->  syntax tree (Program)
                   |                       |
            lexical errors           syntax errors
```

## 1. Approach: recursive descent

There is one Java method for each rule in the grammar (`docs/LANGUAGE_SPEC.md`,
section 15). A rule that mentions another rule calls that rule's method. This
works because Eyra's grammar needs only **one token of lookahead**: by looking at
the next token the parser always knows which rule to use.

| Grammar rule | Method |
|--------------|--------|
| `statement` | `statement()` picks a rule from the first token |
| `declaration` | `declaration()` |
| `assignment` | `assignment()` |
| `printStmt` | `printStatement()` |
| `ifStmt` | `ifStatement()` |
| `whileStmt` | `whileStatement()` |
| `block` | `block()` |
| `or` ... `unary` | `or()`, `and()`, `equality()`, `comparison()`, `term()`, `factor()`, `unary()` |
| `primary` | `primary()` |

## 2. Precedence and associativity

Each precedence level is its own method, and a level calls the next tighter level.
The loosest operator is at the top, so it ends up highest in the tree.

| Level (loosest first) | Operators | Method |
|-----------------------|-----------|--------|
| or | `\|\|` | `or()` |
| and | `&&` | `and()` |
| equality | `==` `!=` | `equality()` |
| comparison | `<` `<=` `>` `>=` | `comparison()` |
| term | `+` `-` | `term()` |
| factor | `*` `/` `%` | `factor()` |
| unary | `-` `!` (prefix) | `unary()` |
| primary | literals, names, `( ... )` | `primary()` |

**Left associativity** comes from a `while` loop instead of recursion:
`10 - 4 - 3` becomes `((10 - 4) - 3)`. Each time round the loop, the tree built so
far becomes the left child of the new operator.

The `-` token is subtraction when it appears after an operand (`term()` sees it) and
negation when it appears where an operand should start (`unary()` sees it). The
scanner deliberately left this decision to the parser.

## 3. The syntax tree

Nodes are Java **records** inside two **sealed interfaces**, `Expr` and `Stmt`.
Sealed means the full list of node kinds is known, so later phases can handle every
case. Every node stores the `line` and `column` where it starts.

| Expressions (`Expr`) | Fields |
|----------------------|--------|
| `IntLiteral` | value |
| `BoolLiteral` | value |
| `Variable` | name |
| `Unary` | operator, operand |
| `Binary` | left, operator, right |

| Statements (`Stmt`) | Fields |
|---------------------|--------|
| `VarDecl` | type, name, initializer (may be null), isConst |
| `Assign` | name, value |
| `Print` | value |
| `If` | condition, thenBranch, elseBranch (null, a Block, or another If) |
| `While` | condition, body |
| `Break`, `Continue` | (position only) |
| `Block` | list of statements |

`Program` holds the list of top-level statements. Design choices:

- **No node for parentheses.** `(1 + 2) * 3` already has the right shape; a
  parenthesis node would only add clutter for later phases.
- **An `else if` is an `If` inside the `else` branch.** There is no separate
  "else if" node.
- **Binary and unary nodes are positioned at the operator**, so a later type error
  such as `1 + true` can point at the `+`.
- **Operators are stored as text** (`"+"`, `"&&"`). It is simple and easy to print.
  Later phases compare strings.

`AstPrinter` prints a tree one node per line (used by `Main` and the tests) and has
`inline()`, which shows an expression with every operator in parentheses so
precedence can be checked at a glance.

## 4. Syntax errors

`ParseError(message, line, column)` mirrors `LexicalError`. The parser **collects**
errors and keeps going, so one run reports many of them.

| Mistake | Message |
|---------|---------|
| `int x = 5` (no `;`) | `Expected ';' after variable declaration`, placed just after the `5` |
| `x = ;` | `Expected an expression, found ';'` |
| `if x {` | `Expected '(' after 'if'` |
| `if (x) print(1);` | `Expected '{' to start the 'if' body` |
| `else` alone | `'else' without a matching 'if'` |
| a stray `}` | `Unmatched '}': there is no open '{' for it` |
| a `{` never closed | `Unclosed '{': this block is never closed with '}'`, placed at the `{` |
| `const int X;` | `A const declaration must have an initializer: ...` |

### Recovery (panic mode)

When a statement is broken, the parser records the error, throws away tokens until
it reaches a safe place, and parses the next statement. A safe place is:

1. just after a `;`, or
2. just before a token that starts a statement (`int`, `bool`, `const`, `if`,
   `while`, `print`, `break`, `continue`, `{`), or
3. just before a `}`, so that an error inside a block does not swallow the end of
   the block.

Two rules prevent trouble:

- **Always make progress.** If a statement fails without consuming any token, the
  parser skips one token. Otherwise a bad token would loop forever.
- **One mistake, one error where possible.** For `const int X;` the parser records
  the error but still builds the declaration, so the `;` is consumed normally and
  does not trigger a second, confusing error.

Known limitation: an identifier is not a recovery point (it also appears inside
expressions), so after a missing `;` before an assignment such as `x = 1;` that
statement may be skipped. The error list is still correct for the first mistake.

## 5. What the parser does not check

The parser checks **shape**, not **meaning**. These are left to Phase 3:

- using an undeclared variable, or declaring a name twice
- type errors (`1 + true`, `if (5)`)
- assigning to a `const`
- `break` or `continue` outside a loop

`break;` at the top level is valid syntax. Whether it is allowed is a semantic rule.

## 6. Public API

```java
List<Token> tokens = new Lexer(source).scanTokens();
Parser parser = new Parser(tokens);
Program program = parser.parse();     // never throws on bad input
if (parser.hasErrors()) { parser.getErrors() ... }
System.out.print(AstPrinter.print(program));
```

Phase 3 will walk the `Program` tree.
