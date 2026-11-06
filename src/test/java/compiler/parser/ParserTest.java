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

    // ---- Stage 6: arithmetic and unary operators

    // ---- Stage 7: comparison, equality, logic

    // ---- Stage 8: declarations, assignment, print

    // ---- Stage 9: blocks, if, while, break, continue

    // ---- Stage 10: better recovery and messages

    // ---- Stage 12: robustness
}
