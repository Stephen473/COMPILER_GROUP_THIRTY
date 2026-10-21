# Eyra Token List

The scanner produces 32 token types plus `EOF`.

## Keywords (9)

| # | Lexeme | Token type |
|---|--------|------------|
| 1 | `int` | `KW_INT` |
| 2 | `bool` | `KW_BOOL` |
| 3 | `const` | `KW_CONST` |
| 4 | `if` | `KW_IF` |
| 5 | `else` | `KW_ELSE` |
| 6 | `while` | `KW_WHILE` |
| 7 | `break` | `KW_BREAK` |
| 8 | `continue` | `KW_CONTINUE` |
| 9 | `print` | `KW_PRINT` |

## Names and literals (3)

| # | Lexeme | Token type | Notes |
|---|--------|------------|-------|
| 10 | name such as `age` | `IDENTIFIER` | `[A-Za-z_][A-Za-z0-9_]*`, not a reserved word |
| 11 | digits such as `20` | `INT_LITERAL` | `0` or a non-zero digit followed by digits, at most 2147483647 |
| 12 | `true`, `false` | `BOOL_LITERAL` | the lexeme keeps the original word |

## Operators (15)

| # | Lexeme | Token type |
|---|--------|------------|
| 13 | `+` | `PLUS` |
| 14 | `-` | `MINUS` |
| 15 | `*` | `STAR` |
| 16 | `/` | `SLASH` |
| 17 | `%` | `PERCENT` |
| 18 | `=` | `ASSIGN` |
| 19 | `==` | `EQ` |
| 20 | `!=` | `NEQ` |
| 21 | `<` | `LT` |
| 22 | `<=` | `LE` |
| 23 | `>` | `GT` |
| 24 | `>=` | `GE` |
| 25 | `&&` | `AND` |
| 26 | `\|\|` | `OR` |
| 27 | `!` | `NOT` |

## Punctuation (5)

| # | Lexeme | Token type |
|---|--------|------------|
| 28 | `;` | `SEMICOLON` |
| 29 | `(` | `LPAREN` |
| 30 | `)` | `RPAREN` |
| 31 | `{` | `LBRACE` |
| 32 | `}` | `RBRACE` |

## End marker

| # | Lexeme | Token type |
|---|--------|------------|
| 33 | (empty) | `EOF` |

## Not tokens

Whitespace, `// line comments` and `/* block comments */` are consumed by the
scanner and never reach the parser.

## Notes

- `true` and `false` are scanned as a single token type, `BOOL_LITERAL`, so the
  parser needs one rule for boolean values instead of two.
- `-20` is two tokens, `MINUS` then `INT_LITERAL`. The parser decides unary or binary minus.
- The scanner always takes the longest match: `>=` is one `GE` token, never `GT` then `ASSIGN`.
