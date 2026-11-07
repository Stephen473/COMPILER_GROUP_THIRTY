# COMPILER_GROUP_30

A compiler for **Eyra**, our own small language (integers, booleans, constants,
`if/else`, `while`, `print`), written in Java 17 and built with Maven.
All five phases live in this single repository.

| Phase | Deliverable | Due (deadline day) | Status |
|-------|-------------|--------------------|--------|
| 1 | Scanner | 25 Oct 2026 | DONE |
| 2 | Parser | 8 Nov 2026 | DONE |
| 3 | Semantic analysis / IR | 22 Nov 2026 | Not started |
| 4 | Code generation | 6 Dec 2026 | Not started |
| 5 | Optimizer + final demo | 13 Dec 2026 | Not started |

## Requirements

- JDK 17 or newer (`java -version`)
- Apache Maven 3.8+ (`mvn -v`)
- Git

## Build, test, run

```bash
mvn clean package          # compile, run tests, build target/compiler-group-30-1.0-SNAPSHOT.jar
mvn test                   # tests only
java -jar target/compiler-group-30-1.0-SNAPSHOT.jar examples/sample.ey
java -jar target/compiler-group-30-1.0-SNAPSHOT.jar examples/features.ey.
```

Run `mvn` commands from the repository root (the folder containing `pom.xml`).

## Project layout

```
pom.xml
src/main/java/compiler/Main.java            entry point
src/main/java/compiler/scanner/             Phase 1: Lexer, Token, TokenType, LexicalError
src/test/java/compiler/scanner/             Phase 1 tests
  src/main/java/compiler/ast/                 Phase 2: syntax tree (Expr, Stmt, Program, AstPrinter)
  src/main/java/compiler/parser/              Phase 2: Parser, ParseError  src/main/java/compiler/ast/                 Phase 2: syntax tree (Expr, Stmt, Program, AstPrinter)
  src/main/java/compiler/parser/              Phase 2: Parser, ParseError
examples/                                    sample.ey (valid) and errors.ey (lexical errors)
docs/                                        documentation (see below)
```

Later phases get their own package: `compiler.parser`, `compiler.semantic`,
`compiler.codegen`, `compiler.optimizer`.

## Documentation

- [docs/LANGUAGE_SPEC.md](docs/LANGUAGE_SPEC.md): the Eyra language
- [docs/TOKEN_LIST.md](docs/TOKEN_LIST.md): every token the scanner produces
- [docs/SCANNER_DESIGN.md](docs/SCANNER_DESIGN.md): Phase 1 scanner design
- [docs/SETUP_GUIDE.md](docs/SETUP_GUIDE.md): install Java, Maven, Git and connect to GitHub (start here)
- [docs/GIT_WORKFLOW.md](docs/GIT_WORKFLOW.md): how the group commits and merges
- [docs/PARSER_DESIGN.md](docs/PARSER_DESIGN.md): Phase 2 parser design
- [docs/PHASE2_PLAN.md](docs/PHASE2_PLAN.md): stages and schedule

## Team

Each member adds one file, `docs/team/<first-last>.md`, with their name, index
number and Phase 1 stage (see [docs/SETUP_GUIDE.md](docs/SETUP_GUIDE.md), step 9).
