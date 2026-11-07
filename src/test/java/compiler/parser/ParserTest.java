package compiler.parser;

import compiler.ast.AstPrinter;
import compiler.ast.Expr;
import compiler.ast.Program;
import compiler.ast.Stmt;
import compiler.scanner.Lexer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    private static Parser parserFor(String source) {
        return new Parser(new Lexer(source).scanTokens());
    }

    private static Program parse(String source) {
        return parserFor(source).parse();
    }

    /** Parses just one expression and shows it fully parenthesised. */
    private static String expr(String source) {
        return AstPrinter.inline(parserFor(source).expression());
    }

    private static ParseError exprError(String source) {
        Parser.ParseException e = assertThrows(Parser.ParseException.class,
                () -> parserFor(source).expression());
        return e.error();
    }

    private static String tree(String source) {
        Parser p = parserFor(source);
        Program program = p.parse();
        assertFalse(p.hasErrors(), "unexpected errors: " + p.getErrors());
        return AstPrinter.print(program);
    }

    private static ParseError firstError(String source) {
        Parser p = parserFor(source);
        p.parse();
        assertTrue(p.hasErrors(), "expected a syntax error");
        return p.getErrors().get(0);
    }

    // ---- Stage 4: skeleton, errors, recovery loop

    @Test
    void emptyInputIsAnEmptyProgram() {
        Parser p = parserFor("");
        Program program = p.parse();
        assertEquals(0, program.statements().size());
        assertFalse(p.hasErrors());
    }

    @Test
    void commentsAndWhitespaceOnlyIsAnEmptyProgram() {
        Parser p = parserFor("// nothing here\n   /* or here */\n");
        assertEquals(0, p.parse().statements().size());
        assertFalse(p.hasErrors());
    }

    @Test
    void strayTokenIsReportedNotThrown() {
        Parser p = parserFor(";");
        p.parse();
        assertTrue(p.hasErrors());
        assertEquals(1, p.getErrors().size());
        assertEquals("Expected a statement, found ';'", p.getErrors().get(0).message());
    }

    @Test
    void errorHasLineAndColumn() {
        Parser p = parserFor("\n  )");
        p.parse();
        ParseError e = p.getErrors().get(0);
        assertEquals(2, e.line());
        assertEquals(3, e.column());
        assertEquals("Expected a statement, found ')'", e.message());
    }

    @Test
    void everyBadStatementIsReported() {
        Parser p = parserFor(";;;");
        p.parse();
        assertEquals(3, p.getErrors().size());
    }

    @Test
    void errorToStringIsReadable() {
        Parser p = parserFor(";");
        p.parse();
        assertEquals("Syntax error at line 1, col 1: Expected a statement, found ';'",
                p.getErrors().get(0).toString());
    }

    // ---- Stage 5: primary expressions

    @Test
    void integerLiteral() {
        Expr e = parserFor("42").expression();
        assertTrue(e instanceof Expr.IntLiteral);
        assertEquals(42, ((Expr.IntLiteral) e).value());
    }

    @Test
    void maximumInteger() {
        assertEquals("2147483647", expr("2147483647"));
    }

    @Test
    void booleanLiterals() {
        assertEquals("true", expr("true"));
        assertEquals("false", expr("false"));
    }

    @Test
    void variable() {
        assertEquals("total", expr("total"));
    }

    @Test
    void literalRemembersItsPosition() {
        Expr e = parserFor("\n   abc").expression();
        assertEquals(2, e.line());
        assertEquals(4, e.column());
    }

    @Test
    void parenthesesLeaveNoNode() {
        assertEquals("7", expr("(7)"));
        assertEquals("x", expr("((x))"));
    }

    @Test
    void missingClosingParenthesis() {
        ParseError e = exprError("(1");
        assertEquals("Expected ')' after the expression", e.message());
        assertEquals(1, e.line());
        assertEquals(3, e.column());
    }

    @Test
    void notAnExpression() {
        assertEquals("Expected an expression, found ')'", exprError(")").message());
        assertEquals("Expected an expression, found 'int'", exprError("int").message());
    }

    @Test
    void emptyInputIsNotAnExpression() {
        assertEquals("Expected an expression, found end of input", exprError("").message());
    }

    // ---- Stage 6: arithmetic and unary operators

    @Test
    void multiplicationBindsTighterThanAddition() {
        assertEquals("(1 + (2 * 3))", expr("1 + 2 * 3"));
        assertEquals("((1 * 2) + 3)", expr("1 * 2 + 3"));
    }

    @Test
    void parenthesesOverridePrecedence() {
        assertEquals("((1 + 2) * 3)", expr("(1 + 2) * 3"));
    }

    @Test
    void subtractionIsLeftAssociative() {
        assertEquals("((10 - 4) - 3)", expr("10 - 4 - 3"));
    }

    @Test
    void divisionAndModuloAreLeftAssociative() {
        assertEquals("((8 / 4) / 2)", expr("8 / 4 / 2"));
        assertEquals("((7 % 3) * 2)", expr("7 % 3 * 2"));
    }

    @Test
    void unaryMinusAndNot() {
        assertEquals("(-5)", expr("-5"));
        assertEquals("(!done)", expr("!done"));
    }

    @Test
    void unaryOperatorsNest() {
        assertEquals("(-(-5))", expr("--5"));
        assertEquals("(!(!x))", expr("!!x"));
    }

    @Test
    void unaryBindsTighterThanMultiplication() {
        assertEquals("((-a) * b)", expr("-a * b"));
    }

    @Test
    void minusAfterOperatorIsNegation() {
        assertEquals("(1 - (-2))", expr("1 - -2"));
    }

    @Test
    void negatedParenthesesGroup() {
        assertEquals("(-(1 + 2))", expr("-(1 + 2)"));
    }

    @Test
    void binaryNodeIsPositionedAtItsOperator() {
        Expr e = parserFor("x  + y").expression();
        assertEquals(1, e.line());
        assertEquals(4, e.column());
    }

    @Test
    void missingRightOperand() {
        assertEquals("Expected an expression, found end of input", exprError("1 +").message());
        assertEquals("Expected an expression, found ')'", exprError("2 * )").message());
    }

    // ---- Stage 7: comparison, equality, logic

    @Test
    void comparisonOperators() {
        assertEquals("(a < b)", expr("a < b"));
        assertEquals("(a <= b)", expr("a <= b"));
        assertEquals("(a > b)", expr("a > b"));
        assertEquals("(a >= b)", expr("a >= b"));
    }

    @Test
    void equalityOperators() {
        assertEquals("(a == b)", expr("a == b"));
        assertEquals("(a != b)", expr("a != b"));
    }

    @Test
    void arithmeticBindsTighterThanComparison() {
        assertEquals("((a + 1) <= (b * 2))", expr("a + 1 <= b * 2"));
    }

    @Test
    void comparisonBindsTighterThanEquality() {
        assertEquals("((a < b) == (c > d))", expr("a < b == c > d"));
    }

    @Test
    void equalityIsLeftAssociative() {
        assertEquals("((a == b) != c)", expr("a == b != c"));
    }

    @Test
    void andBindsTighterThanOr() {
        assertEquals("(a || (b && c))", expr("a || b && c"));
        assertEquals("((a && b) || c)", expr("a && b || c"));
    }

    @Test
    void logicalOperatorsAreLeftAssociative() {
        assertEquals("((a || b) || c)", expr("a || b || c"));
        assertEquals("((a && b) && c)", expr("a && b && c"));
    }

    @Test
    void notBindsTighterThanAnd() {
        assertEquals("((!a) && b)", expr("!a && b"));
    }

    @Test
    void remainderTestFromTheSampleProgram() {
        assertEquals("((n % 2) == 0)", expr("n % 2 == 0"));
    }

    @Test
    void everyPrecedenceLevelTogether() {
        assertEquals("((((1 + 2) == 3) && (x > 0)) || done)", expr("1 + 2 == 3 && x > 0 || done"));
    }

    @Test
    void binaryOperatorIsStoredAsSourceText() {
        Expr e = parserFor("a && b").expression();
        assertEquals("&&", ((Expr.Binary) e).operator());
    }

    @Test
    void parenthesesStillWorkAtTheTopLevel() {
        assertEquals("(a && (b || c))", expr("a && (b || c)"));
    }

    // ---- Stage 8: declarations, assignment, print

    @Test
    void declarationWithInitializer() {
        String expected = """
                Program
                  VarDecl int x
                    Int 5
                """;
        assertEquals(expected, tree("int x = 5;"));
    }

    @Test
    void declarationWithoutInitializer() {
        String expected = """
                Program
                  VarDecl bool done
                """;
        assertEquals(expected, tree("bool done;"));
    }

    @Test
    void constDeclaration() {
        String expected = """
                Program
                  VarDecl const int LIMIT
                    Int 10
                """;
        assertEquals(expected, tree("const int LIMIT = 10;"));
    }

    @Test
    void assignment() {
        String expected = """
                Program
                  Assign x
                    Binary +
                      Var x
                      Int 1
                """;
        assertEquals(expected, tree("x = x + 1;"));
    }

    @Test
    void printStatement() {
        String expected = """
                Program
                  Print
                    Var x
                """;
        assertEquals(expected, tree("print(x);"));
    }

    @Test
    void statementsKeepTheirOrder() {
        Program p = parse("int a = 1; int b = 2; print(a);");
        assertEquals(3, p.statements().size());
        assertTrue(p.statements().get(0) instanceof Stmt.VarDecl);
        assertTrue(p.statements().get(2) instanceof Stmt.Print);
    }

    @Test
    void statementsRememberTheirPosition() {
        Program p = parse("int a = 1;\n  const bool b = true;\n  a = 2;");
        Stmt second = p.statements().get(1);
        assertEquals(2, second.line());
        assertEquals(3, second.column());
        Stmt third = p.statements().get(2);
        assertEquals(3, third.line());
        assertEquals(3, third.column());
    }

    @Test
    void missingSemicolonAfterDeclaration() {
        assertTrue(firstError("int x = 5").message().startsWith("Expected ';' after variable declaration"));
    }

    @Test
    void missingSemicolonAfterAssignmentAndPrint() {
        assertTrue(firstError("x = 1").message().startsWith("Expected ';' after assignment"));
        assertTrue(firstError("print(1)").message().startsWith("Expected ';' after print statement"));
    }

    @Test
    void assignmentNeedsEqualsSign() {
        assertEquals("Expected '=' after 'x'", firstError("x 5;").message());
    }

    @Test
    void printNeedsParentheses() {
        assertEquals("Expected '(' after 'print'", firstError("print x;").message());
        assertEquals("Expected ')' after the expression", firstError("print(x;").message());
    }

    @Test
    void declarationNeedsAName() {
        assertEquals("Expected a variable name after 'int'", firstError("int = 5;").message());
    }

    @Test
    void constNeedsATypeAndAnInitializer() {
        assertEquals("Expected 'int' or 'bool' after 'const'", firstError("const x = 1;").message());

        Parser p = parserFor("const int LIMIT;");
        Program program = p.parse();
        assertEquals(1, p.getErrors().size());
        assertTrue(p.getErrors().get(0).message().startsWith("A const declaration must have an initializer"));
        assertEquals(11, p.getErrors().get(0).column());
        assertEquals(1, program.statements().size()); // still parsed, so one mistake is one error
    }

    @Test
    void errorsInDifferentStatementsAreAllReported() {
        Parser p = parserFor("int = 5;\nprint(;\nint ok = 1;");
        Program program = p.parse();
        assertEquals(2, p.getErrors().size());
        assertEquals(1, program.statements().size()); // only 'int ok = 1;' survived
    }

    // ---- Stage 9: blocks, if, while, break, continue

    @Test
    void emptyBlock() {
        String expected = """
                Program
                  Block
                """;
        assertEquals(expected, tree("{}"));
    }

    @Test
    void blockHoldsStatements() {
        String expected = """
                Program
                  Block
                    VarDecl int x
                      Int 1
                    Print
                      Var x
                """;
        assertEquals(expected, tree("{ int x = 1; print(x); }"));
    }

    @Test
    void nestedBlocks() {
        String expected = """
                Program
                  Block
                    Block
                      Break
                """;
        assertEquals(expected, tree("{ { break; } }"));
    }

    @Test
    void ifWithoutElse() {
        String expected = """
                Program
                  If
                    Condition
                      Var b
                    Then
                      Block
                        Print
                          Int 1
                """;
        assertEquals(expected, tree("if (b) { print(1); }"));
    }

    @Test
    void ifWithElse() {
        String expected = """
                Program
                  If
                    Condition
                      Var b
                    Then
                      Block
                    Else
                      Block
                        Print
                          Int 2
                """;
        assertEquals(expected, tree("if (b) { } else { print(2); }"));
    }

    @Test
    void elseIfChainNestsInTheElseBranch() {
        Program p = parse("if (a) { } else if (b) { } else if (c) { } else { }");
        Stmt.If first = (Stmt.If) p.statements().get(0);
        Stmt.If second = (Stmt.If) first.elseBranch();
        Stmt.If third = (Stmt.If) second.elseBranch();
        assertTrue(third.elseBranch() instanceof Stmt.Block);
    }

    @Test
    void whileLoop() {
        String expected = """
                Program
                  While
                    Condition
                      Binary <
                        Var i
                        Int 10
                    Body
                      Block
                        Assign i
                          Binary +
                            Var i
                            Int 1
                """;
        assertEquals(expected, tree("while (i < 10) { i = i + 1; }"));
    }

    @Test
    void breakAndContinue() {
        String expected = """
                Program
                  Break
                  Continue
                """;
        assertEquals(expected, tree("break; continue;"));
    }

    @Test
    void breakOutsideALoopIsNotASyntaxError() {
        // Whether break is allowed here is a meaning rule, checked in the semantic phase.
        Parser p = parserFor("break;");
        p.parse();
        assertFalse(p.hasErrors());
    }

    @Test
    void ifAndWhileRememberTheirPosition() {
        Program p = parse("int a;\nwhile (true) {\n  if (a == 1) { }\n}");
        Stmt.While w = (Stmt.While) p.statements().get(1);
        assertEquals(2, w.line());
        assertEquals(1, w.column());
        Stmt.If i = (Stmt.If) w.body().statements().get(0);
        assertEquals(3, i.line());
        assertEquals(3, i.column());
        assertEquals(2, w.body().line());
        assertEquals(14, w.body().column());
    }

    @Test
    void ifNeedsParenthesesAroundTheCondition() {
        assertEquals("Expected '(' after 'if'", firstError("if x { }").message());
        assertEquals("Expected ')' after the condition", firstError("if (x { }").message());
    }

    @Test
    void ifAndWhileBodiesMustBeBlocks() {
        assertEquals("Expected '{' to start the 'if' body", firstError("if (x) print(1);").message());
        assertEquals("Expected '{' to start the 'while' body", firstError("while (x) y = 1;").message());
    }

    @Test
    void elseNeedsABlockOrIf() {
        assertEquals("Expected '{' or 'if' after 'else'", firstError("if (a) { } else print(1);").message());
    }

    @Test
    void jumpsNeedASemicolon() {
        assertEquals("Expected ';' after 'break'", firstError("break").message());
        assertEquals("Expected ';' after 'continue'", firstError("continue").message());
    }

    @Test
    void unclosedBlockIsAnError() {
        Parser p = parserFor("{ int x = 1;");
        p.parse();
        assertTrue(p.hasErrors());
    }

    // ---- Stage 10: better recovery and messages

    @Test
    void missingSemicolonIsReportedJustAfterThePreviousToken() {
        ParseError e = firstError("int x = 5\nprint(x);");
        assertEquals("Expected ';' after variable declaration", e.message());
        assertEquals(1, e.line());
        assertEquals(10, e.column());
    }

    @Test
    void missingSemicolonAtTheEndOfInput() {
        ParseError e = firstError("x = 1");
        assertEquals("Expected ';' after assignment", e.message());
        assertEquals(1, e.line());
        assertEquals(6, e.column());
    }

    @Test
    void parsingContinuesAtTheNextStatement() {
        Parser p = parserFor("int x = 5\nprint(x);");
        Program program = p.parse();
        assertEquals(1, p.getErrors().size());
        assertEquals(1, program.statements().size());
        assertTrue(program.statements().get(0) instanceof Stmt.Print);
    }

    @Test
    void recoveryStopsAtAStatementKeyword() {
        Parser p = parserFor("x =\nint y = 1;");
        Program program = p.parse();
        assertEquals(1, p.getErrors().size());
        assertEquals("Expected an expression, found 'int'", p.getErrors().get(0).message());
        assertEquals(1, program.statements().size());
    }

    @Test
    void recoveryInsideABlockKeepsTheBlockClosed() {
        Parser p = parserFor("{ int x = 1 print(x); }\nint y = 2;");
        Program program = p.parse();
        assertEquals(1, p.getErrors().size());
        assertEquals(2, program.statements().size());
        Stmt.Block block = (Stmt.Block) program.statements().get(0);
        assertEquals(1, block.statements().size());
    }

    @Test
    void errorRightBeforeClosingBrace() {
        Parser p = parserFor("{ int x = 1 }\nint y;");
        Program program = p.parse();
        assertEquals(1, p.getErrors().size());
        assertEquals(2, program.statements().size());
    }

    @Test
    void unmatchedClosingBrace() {
        Parser p = parserFor("}\nint x;");
        Program program = p.parse();
        assertEquals(1, p.getErrors().size());
        ParseError e = p.getErrors().get(0);
        assertTrue(e.message().startsWith("Unmatched '}'"));
        assertEquals(1, e.line());
        assertEquals(1, e.column());
        assertEquals(1, program.statements().size());
    }

    @Test
    void elseWithoutIf() {
        Parser p = parserFor("else { }");
        p.parse();
        assertEquals(1, p.getErrors().size());
        assertEquals("'else' without a matching 'if'", p.getErrors().get(0).message());
    }

    @Test
    void unclosedBraceIsReportedAtTheOpeningBrace() {
        Parser p = parserFor("int a;\n{ int x = 1;");
        p.parse();
        assertEquals(1, p.getErrors().size());
        ParseError e = p.getErrors().get(0);
        assertTrue(e.message().startsWith("Unclosed '{'"));
        assertEquals(2, e.line());
        assertEquals(1, e.column());
    }

    @Test
    void everyUnclosedBraceIsReportedInnerFirst() {
        Parser p = parserFor("while (true) {\n  if (x) {\n");
        p.parse();
        assertEquals(2, p.getErrors().size());
        assertEquals(2, p.getErrors().get(0).line());
        assertEquals(1, p.getErrors().get(1).line());
    }

    @Test
    void oneMistakeInAConditionIsOneError() {
        Parser p = parserFor("if (1 + ) { }");
        p.parse();
        assertEquals(1, p.getErrors().size());
        assertEquals("Expected an expression, found ')'", p.getErrors().get(0).message());
    }

    @Test
    void doubledEqualsInAnAssignmentIsOneError() {
        Parser p = parserFor("x = = 2;");
        p.parse();
        assertEquals(1, p.getErrors().size());
        assertEquals("Expected an expression, found '='", p.getErrors().get(0).message());
    }

    @Test
    void garbageNeverLoopsForever() {
        Parser p = parserFor(") ) ) ; } } else ; ( ( ;");
        p.parse();
        assertTrue(p.getErrors().size() >= 3);
    }

    @Test
    void manyErrorsInOneProgram() {
        Parser p = parserFor("int a = ;\nprint(a + );\nint b = 3\nx = 1;\n}");
        p.parse();
        assertEquals(4, p.getErrors().size());
    }

    @Test
    void missingClosingParenthesisAtEndOfInput() {
        ParseError e = firstError("print(1");
        assertEquals("Expected ')' after the expression", e.message());
        assertEquals(1, e.line());
        assertEquals(8, e.column());
    }

    // ---- Stage 12: robustness

    @Test
    void windowsLineEndings() {
        Parser p = parserFor("int a = 1;\r\nint b = 2\r\nprint(b);\r\n");
        p.parse();
        assertEquals(1, p.getErrors().size());
        assertEquals(2, p.getErrors().get(0).line());
        assertEquals(10, p.getErrors().get(0).column());
    }

    @Test
    void commentsBetweenAnyTokens() {
        String expected = """
                Program
                  VarDecl int x
                    Binary +
                      Int 1
                      Int 2
                """;
        assertEquals(expected, tree("int /* type */ x // name\n = 1 /* one */ + 2; // done"));
    }

    @Test
    void parenthesesNestedTwoHundredDeep() {
        String source = "(".repeat(200) + "1" + ")".repeat(200);
        assertEquals("1", expr(source));
    }

    @Test
    void longChainOfAdditions() {
        StringBuilder sb = new StringBuilder("1");
        for (int i = 0; i < 2000; i++) {
            sb.append(" + 1");
        }
        Expr e = parserFor(sb.toString()).expression();
        assertTrue(e instanceof Expr.Binary);
        assertEquals("+", ((Expr.Binary) e).operator());
    }

    @Test
    void thousandStatements() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("x = ").append(i).append(";\n");
        }
        Parser p = parserFor(sb.toString());
        assertEquals(1000, p.parse().statements().size());
        assertFalse(p.hasErrors());
    }

    @Test
    void blocksNestedOneHundredDeep() {
        String source = "{".repeat(100) + "}".repeat(100);
        Parser p = parserFor(source);
        assertEquals(1, p.parse().statements().size());
        assertFalse(p.hasErrors());
    }

    @Test
    void longElseIfChain() {
        StringBuilder sb = new StringBuilder("if (a) { }");
        for (int i = 0; i < 50; i++) {
            sb.append(" else if (a) { }");
        }
        Parser p = parserFor(sb.toString());
        assertEquals(1, p.parse().statements().size());
        assertFalse(p.hasErrors());
    }

    @Test
    void errorPositionsInALongerProgram() {
        String source = """
                const int LIMIT = 10;
                int n = 1;
                while (n <= LIMIT) {
                    n = n + ;
                }
                """;
        ParseError e = firstError(source);
        assertEquals(4, e.line());
        assertEquals(13, e.column());
    }

    @Test
    void goldenTreeForASmallProgram() {
        String source = """
                const int LIMIT = 3;
                int n = 0;
                while (n < LIMIT) {
                    if (n % 2 == 0) {
                        print(n);
                    } else {
                        continue;
                    }
                    n = n + 1;
                }
                """;
        String expected = """
                Program
                  VarDecl const int LIMIT
                    Int 3
                  VarDecl int n
                    Int 0
                  While
                    Condition
                      Binary <
                        Var n
                        Var LIMIT
                    Body
                      Block
                        If
                          Condition
                            Binary ==
                              Binary %
                                Var n
                                Int 2
                              Int 0
                          Then
                            Block
                              Print
                                Var n
                          Else
                            Block
                              Continue
                        Assign n
                          Binary +
                            Var n
                            Int 1
                """;
        assertEquals(expected, tree(source));
    }
}
