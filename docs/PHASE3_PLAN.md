# Phase 3 Plan: Semantic Analysis and Intermediate Representation

**Deadline:** Sunday 22 November 2026

Phase 3 is built in **ten stages** plus a leader stage 0. Each stage compiles, has its own
tests, and goes in as its own pull request. Stages must be merged in order.

## Stages

| Stage | Task | Needs |
|-------|------|-------|
| 0 | Design docs (leader) | Phase 2 on `main` |
| 1 | Symbol table: `Symbol`, `SymbolTable`, `SemanticError` | 0 |
| 2 | Analyzer: declarations, scopes, undeclared names | 1 |
| 3 | Analyzer: operator types | 2 |
| 4 | Analyzer: initializer, assignment and condition types | 3 |
| 5 | Analyzer: `const`, `break` and `continue` rules | 4 |
| 6 | Analyzer: types of expressions, error order, whole-program tests | 5 |
| 7 | IR: operands, instructions and the printer | 6 |
| 8 | IR generator: expressions, statements, scopes, `&&` and `\|\|` | 7 |
| 9 | IR generator: `if`, `while`, `break`, `continue` | 8 |
| 10 | `Main`, IR interpreter, example programs | 9 |

## Schedule

| Day | Stages |
|-----|--------|
| Fri 20 Nov | 0 (leader), then 1, 2, 3 |
| Sat 21 Nov | 4, 5, 6, 7, 8 |
| Sun 22 Nov | 9, 10, report, evidence, tag `phase3-semantic`, submit |

The time is short, so stages 4 to 8 on Saturday are quick ones: each is two files.
If members are missing, one person can take several stages, but each stage stays a
separate branch and commit, in order. Record who actually did what in the report.

## Definition of done

- `mvn clean package` on `main` shows `BUILD SUCCESS` with all tests passing
- `java -jar ... examples/sample.ey` prints intermediate code, and `--run` prints `30` and `true`
- `examples/semantic_errors.ey` reports 8 semantic errors with exit code 65
- Transcript, screenshots and a short recording are in `docs/evidence/`
- `docs/PHASE3_REPORT.md` is filled in
- `main` is tagged `phase3-semantic`
