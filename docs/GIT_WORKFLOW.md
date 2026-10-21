# Git workflow for COMPILER_GROUP_30

The course requires a **visible, incremental commit history**. One big commit on
the deadline day does not count. Each phase's deadline is the day of the *final*
feature commit, not the first line of code.

## Rules

1. Never commit directly to `main` once the first working version is in. Work on a branch.
2. One small feature per commit (for example "scan string literals", not "scanner done").
3. Commit at least every time a feature works, and push the same day.
4. Everyone commits under their own name and email so contributions are visible.
5. Run `mvn test` before every push. Do not push code that does not compile.

## One-time setup on each laptop

```bash
git config --global user.name  "Your Full Name"
git config --global user.email "the-email-used-on-your-github-account"
git clone https://github.com/<owner>/COMPILER_GROUP_30.git
cd COMPILER_GROUP_30
```

The repo is private, so GitHub will ask you to sign in. If a password prompt
fails, use a Personal Access Token (GitHub > Settings > Developer settings) as
the password, or sign in with `gh auth login`.

## Daily routine

```bash
git checkout main
git pull                              # get everyone's latest work
git checkout -b feature/scan-strings  # one branch per feature
# ... edit code, then:
mvn test
git add -A
git commit -m "Scanner: add string literal scanning with escapes"
git push -u origin feature/scan-strings
```

Then open a Pull Request on GitHub, ask one teammate to review, and merge it.
After merging:

```bash
git checkout main
git pull
```

## Commit message style

`Phase: what changed`, in the present tense. Examples:

- `Scanner: recognise keywords`
- `Scanner: report line and column on errors`
- `Tests: cover unterminated block comments`
- `Docs: describe token list`

## Merge conflicts

If `git pull` reports a conflict, open the marked files, keep the correct code,
delete the `<<<<<<<`, `=======` and `>>>>>>>` lines, then:

```bash
git add <file>
git commit
```

To avoid conflicts, split work by file or by feature so two people are not
editing the same lines.

## Phase 1 split: ten stages, one per member

The scanner is built in ten stages. Each stage compiles, has its own tests, and
adds one piece. Stages build on each other, so **merge them in order**: stage N
is merged before stage N+1 starts.

| Stage | Task | Branch | Main files |
|-------|------|--------|------------|
| 1 | Lexer skeleton: position tracking, errors, EOF | `feature/scanner-skeleton` | `Lexer.java`, `LexerTest.java` |
| 2 | Punctuation and single-character operators | `feature/scan-punctuation` | `Lexer.java`, `LexerTest.java` |
| 3 | Multi-character operators and longest match | `feature/scan-operators` | `Lexer.java`, `LexerTest.java` |
| 4 | Line and block comments | `feature/scan-comments` | `Lexer.java`, `LexerTest.java` |
| 5 | Identifiers, keywords, boolean literals | `feature/scan-words` | `Lexer.java`, `LexerTest.java` |
| 6 | Integer literals | `feature/scan-integers` | `Lexer.java`, `LexerTest.java` |
| 7 | Number errors (leading zeros, too large, `2age`) | `feature/scan-number-errors` | `Lexer.java`, `LexerTest.java` |
| 8 | Command-line `Main` and example programs | `feature/main-and-examples` | `Main.java`, `examples/`, `LexerTest.java` |
| 9 | Robustness tests | `feature/scanner-robustness-tests` | `LexerTest.java` |
| 10 | Documentation: README status and testing guide | `feature/docs-phase1` | `README.md`, `docs/TESTING.md` |

The group leader pushes the foundation (stage 0: Maven setup, docs, token
types) before stage 1 starts.
