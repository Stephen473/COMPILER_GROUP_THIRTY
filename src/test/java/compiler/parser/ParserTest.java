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

    // ---- Stage 9: blocks, if, while, break, continue

    // ---- Stage 10: better recovery and messages

    // ---- Stage 12: robustness
}
