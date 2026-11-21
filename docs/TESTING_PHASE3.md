# Testing Phase 3: the semantic analyzer and the intermediate code

All commands run from the repository root (the folder containing `pom.xml`).

```bash
mvn clean package
```

You should see `BUILD SUCCESS`. Run one test class with `mvn test -Dtest=SemanticTest`.

## What the tests cover

| File | What is checked |
|------|-----------------|
| `semantic/SymbolTableTest` | scopes, shadowing, duplicate detection |
| `semantic/SemanticTest` | each rule in `docs/SEMANTIC_DESIGN.md`, one mistake giving one error, error order, the example programs |
| `ir/IrTest` | every instruction and the text form of the IR |
| `ir/IrGeneratorTest` | the exact intermediate code for expressions, scopes, short-circuit operators, `if`, `while`, `break`, `continue` |
| `ir/IrInterpreterTest` | programs compiled and **run**: precedence, wrap-around, `/` and `%` rules, short-circuit really skipping, runtime errors, the step limit |
| `PipelineTest` | the whole compiler through `Main`: flags and exit codes |
| `ExamplesTest` | scanner and parser examples through `Main` (now using `--ast`) |

## Running programs

```bash
java -jar target/compiler-group-30-1.0-SNAPSHOT.jar examples/sample.ey             # intermediate code
java -jar target/compiler-group-30-1.0-SNAPSHOT.jar --run examples/sample.ey       # 30 and true
java -jar target/compiler-group-30-1.0-SNAPSHOT.jar --ast examples/features.ey     # syntax tree
java -jar target/compiler-group-30-1.0-SNAPSHOT.jar examples/semantic_errors.ey    # 8 semantic errors, exit 65
java -jar target/compiler-group-30-1.0-SNAPSHOT.jar --run examples/runtime_error.ey # prints 10, then a runtime error, exit 70
```

## Adding a test

A semantic test, in `SemanticTest.java`:

```java
@Test
void notNeedsABool() {
    assertEquals(List.of("Operator '!' needs a bool operand, but found int"), messages("print(!5);"));
}
```

An intermediate code test, in `IrGeneratorTest.java`:

```java
@Test
void assignment() {
    assertEquals("    x = 0\n    x = 7\n", ir("int x = 0; x = 7;"));
}
```
