# Testing the Scanner

All commands run from the repository root (the folder containing `pom.xml`).

## Build everything and run all tests

```bash
mvn clean package
```

You should see `Tests run: 48, Failures: 0, Errors: 0` and `BUILD SUCCESS`.

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
```

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

## Adding a test

Add a method to `LexerTest.java`:

```java
@Test
void percentIsItsOwnToken() {
    assertEquals(List.of(PERCENT, EOF), types("%"));
}
```

Run `mvn test` and check that the test count went up by one.
