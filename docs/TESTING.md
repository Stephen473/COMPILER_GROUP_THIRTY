# Testing the Scanner and the Parser

All commands run from the repository root (the folder containing `pom.xml`).

## Build everything and run all tests

```bash
mvn clean package
```

You should see `Tests run: 175, Failures: 0, Errors: 0` and `BUILD SUCCESS`.

## Run only the tests

```bash
mvn test
```

## Run a single test

```bash
mvn test -Dtest=LexerTest#longestMatchOperators
```

## Run the scanner on a program

```bash
java -jar target/compiler-group-30-1.0-SNAPSHOT.jar examples/sample.ey
java -jar target/compiler-group-30-1.0-SNAPSHOT.jar examples/errors.ey
java -jar target/compiler-group-30-1.0-SNAPSHOT.jar examples/features.ey
java -jar target/compiler-group-30-1.0-SNAPSHOT.jar examples/syntax_errors.ey
java -jar target/compiler-group-30-1.0-SNAPSHOT.jar --tokens examples/sample.ey
```

| File | What happens | Exit code |
|------|--------------|-----------|
| `sample.ey`, `features.ey` | the scanner and parser succeed; the **syntax tree** is printed | 0 |
| `errors.ey` | five lexical errors; the parser does not run | 65 |
| `syntax_errors.ey` | six syntax errors with line and column | 65 |
| any file with `--tokens` | only the scanner runs; the token list is printed | 0 |

- `sample.ey` is a valid program. The scanner prints one line per token and ends with `EOF`.
- `errors.ey` has five deliberate mistakes. The scanner prints each error with its
  line and column, then `5 lexical errors found. Compilation stopped.`, and exits with code 65.

## Without Maven (run only, no tests)

```powershell
mkdir out
javac -d out (Get-ChildItem -Recurse src\main -Filter *.java).FullName
java -cp out compiler.Main examples\sample.ey
```

## What the tests cover

`src/test/java/compiler/scanner/LexerTest.java` is organised by the stage that
introduced each group of tests:

| Group | What is checked |
|-------|-----------------|
| Skeleton | empty input, whitespace, positions, error collection |
| Punctuation | `; ( ) { }` and `+ - * %` |
| Operators | `/`, `== != <= >= < > =`, `&&`, `||`, `!`, longest match |
| Comments | `//` and `/* */`, line counting, unterminated comments |
| Words | keywords, identifiers, `true`/`false`, case sensitivity |
| Integers | literals, `-` as a separate token |
| Number errors | leading zeros, too-large values, `2age` |
| Example programs | `examples/sample.ey` and `examples/errors.ey` |
| Robustness | errors inside real code, Windows line endings, non-ASCII input |

### Parser tests (Phase 2)

| File | Group | What is checked |
|------|-------|-----------------|
| `ast/AstTest.java` | Expression and statement nodes | fields, positions, equality |
| `ast/AstPrinterTest.java` | Printer | tree text and one-line form |
| `parser/ParserTest.java` | Skeleton | empty program, errors collected, positions |
| | Expressions | primary, precedence, associativity, unary |
| | Statements | declarations, assignment, print, blocks, if, while, break, continue |
| | Recovery | missing `;`, unmatched `}`, unclosed `{`, many errors, no endless loops |
| | Robustness | deep nesting, long input, Windows line endings, a golden tree |
| `ExamplesTest.java` | Example programs and `Main` | the four examples, exit codes 0, 64, 65, 66 |

Run one parser test class: `mvn test -Dtest=ParserTest`.

## Adding a test

For a parser test, add a method to `ParserTest.java`:

```java
@Test
void negativeOfAGroup() {
    assertEquals("(-(1 + 2))", expr("-(1 + 2)"));
}
```

For a scanner test, add a method to `LexerTest.java`:

Add a method to `LexerTest.java`:

```java
@Test
void percentIsItsOwnToken() {
    assertEquals(List.of(PERCENT, EOF), types("%"));
}
```

Run `mvn test` and check that the test count went up by one.
