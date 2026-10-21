# Phase 1: Scanner Design

The scanner (`compiler.scanner.Lexer`) is the first phase of the compiler. It
reads Eyra source text and produces a list of tokens for the parser. The
token types are listed in [TOKEN_LIST.md](TOKEN_LIST.md) and the language rules
are in [LANGUAGE_SPEC.md](LANGUAGE_SPEC.md).

## 1. What a token carries

| Field | Meaning | Example |
|-------|---------|---------|
| `type` | token category (`TokenType`) | `KW_INT` |
| `lexeme` | exact text from the source | `int` |
| `line` | line where the token starts (from 1) | `1` |
| `column` | column where the token starts (from 1) | `1` |

Columns are recorded as well as lines so later phases can point at the exact
spot in an error message.

## 2. How the scanner works

1. Keep a position in the source plus the current line and column.
2. Look at the next character and decide what kind of token starts there.
3. Skip whitespace and comments without producing tokens (line numbers still advance).
4. For words and numbers, read the **whole** run of characters first, then classify it.
5. For operators, prefer the **longest match** (`==` before `=`, `<=` before `<`).
6. When all input is consumed, add one `EOF` token.

### Words

Read `[A-Za-z0-9_]*` after the first letter or underscore, then look the text up
in the keyword table. `int` is `KW_INT`, `integer` is `IDENTIFIER`, and
`true`/`false` become `BOOL_LITERAL`. Lookup is case-sensitive.

### Numbers

Read all digits, then check:

1. If a letter or underscore follows (`2age`), report an invalid identifier and consume the whole word.
2. If the text has leading zeros (`007`), report an error.
3. If the value is above `2147483647`, report an error.
4. Otherwise emit `INT_LITERAL`.

A minus sign is never part of a number.

### Comments

- `//` skips to the end of the line.
- `/*` skips to the matching `*/`, across lines. No nesting.
- If the file ends before `*/`, report the error at the position where the comment **started**.

## 3. Error handling

The scanner does **not** stop at the first problem. It records a
`LexicalError` (message, line, column), skips the offending text, and keeps
scanning, so one run shows every lexical mistake.

| Situation | Example | Message |
|-----------|---------|---------|
| Unknown character | `@` | `Unexpected character '@'` |
| Lone ampersand | `a & b` | `Unexpected '&' (did you mean '&&'?)` |
| Lone bar | `a \| b` | `Unexpected '\|' (did you mean '\|\|'?)` |
| Digit-first name | `2age` | `Invalid token '2age': identifiers cannot start with a digit` |
| Leading zeros | `007` | `Leading zeros are not allowed in '007'` |
| Number too big | `99999999999` | `Integer literal '99999999999' is too large (maximum 2147483647)` |
| Open comment | `/* ...` | `Unterminated block comment` |

Non-ASCII characters outside comments are reported as `Unexpected character U+XXXX`.

If the scanner reports any error, `Main` prints them all, prints a count, and
exits with code 65 without running the later phases.

## 4. Worked example

Source:

```
int count = 3;
while (count > 0) {
```

Tokens (then `EOF`):

| Token type | Lexeme | Line | Col |
|------------|--------|------|-----|
| `KW_INT` | `int` | 1 | 1 |
| `IDENTIFIER` | `count` | 1 | 5 |
| `ASSIGN` | `=` | 1 | 11 |
| `INT_LITERAL` | `3` | 1 | 13 |
| `SEMICOLON` | `;` | 1 | 14 |
| `KW_WHILE` | `while` | 2 | 1 |
| `LPAREN` | `(` | 2 | 7 |
| `IDENTIFIER` | `count` | 2 | 8 |
| `GT` | `>` | 2 | 14 |
| `INT_LITERAL` | `0` | 2 | 16 |
| `RPAREN` | `)` | 2 | 17 |
| `LBRACE` | `{` | 2 | 19 |

## 5. Class overview

| Class | Role |
|-------|------|
| `TokenType` | enum of all token kinds |
| `Token` | immutable record: type, lexeme, line, column |
| `LexicalError` | immutable record: message, line, column |
| `Lexer` | the scanner; `scanTokens()`, `getErrors()`, `hasErrors()` |
| `Main` | reads a file, runs the scanner, prints tokens or errors |

## 6. Testing plan

- Every keyword, operator and punctuation symbol on its own.
- Longest match: `== != <= >= && ||`.
- Keywords vs identifiers (`int` vs `integer`, `INT`).
- Number rules: `0`, `2147483647`, `2147483648`, `007`, `2age`, `-20`.
- Both comment styles, including multi-line block comments and line counting after them.
- Line and column positions.
- Several errors in one file are all reported and scanning continues.
- The sample programs in `examples/`.
