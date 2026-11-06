# Phase 2 Plan: Parser

**Deadline:** Sunday 8 November 2026

The parser is built in **twelve stages** plus a leader stage 0. The scripts and code for each
stage are in the `EYRA_PARSER_STAGES` folder the leader shares. There are ten
members and twelve stages, so two members take two stages each.

## Stages

| Stage | Task | Needs |
|-------|------|-------|
| 0 | Design docs (leader) | Phase 1 on `main` |
| 1 | AST: expression nodes | 0 |
| 2 | AST: statement nodes and `Program` | 1 |
| 3 | `AstPrinter` | 2 |
| 4 | Parser skeleton: cursor, errors, recovery loop | 3 |
| 5 | Primary expressions | 4 |
| 6 | Arithmetic and unary operators | 5 |
| 7 | Comparison, equality and logic | 6 |
| 8 | Declarations, assignment, `print` | 7 |
| 9 | Blocks, `if`, `while`, `break`, `continue` | 8 |
| 10 | Better error recovery and messages | 9 |
| 11 | `Main` runs the parser; example programs | 10 |
| 12 | Robustness tests and documentation | 11 |

Merge in order, one pull request per stage, each reviewed by a teammate.

## Schedule

| Day | Stages |
|-----|--------|
| Wed 4 Nov | 0 (leader), then 1, 2, 3 |
| Thu 5 Nov | 4, 5, 6 |
| Fri 6 Nov | 7, 8, 9 |
| Sat 7 Nov | 10, 11, 12, then a full check on `main` |
| Sun 8 Nov | Phase 2 report, evidence, tag `phase2-parser`, submit |

## Who does what

Record the names in the table in `EYRA_PARSER_STAGES/README.md` and in the
Phase 2 report. Suggested pairs for the two extra stages: stages 1 and 2 (both small)
go to one member; stage 3 and stage 11 go to one member.

## Definition of done

- `mvn clean package` on `main` shows `BUILD SUCCESS` and `Tests run: 175`
- `examples/features.ey` prints a syntax tree; `examples/syntax_errors.ey` reports 6 errors
- Transcript, screenshots and a short recording are in `docs/evidence/`
- `docs/PHASE2_REPORT.md` is filled in
- `main` is tagged `phase2-parser`
