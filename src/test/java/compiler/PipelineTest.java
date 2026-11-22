package compiler;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** The whole compiler through Main: scanner, parser, semantic analyzer, intermediate code. */
class PipelineTest {

    private static Object[] run(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = Main.run(args, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Object[] {code, out.toString(StandardCharsets.UTF_8).replace("\r\n", "\n"),
                err.toString(StandardCharsets.UTF_8).replace("\r\n", "\n")};
    }

    @Test
    void defaultOutputIsTheIntermediateCode() {
        Object[] r = run("examples/sample.ey");
        assertEquals(0, r[0]);
        String out = (String) r[1];
        assertTrue(out.startsWith("    LIMIT = 10\n"));
        assertTrue(out.contains("jumpIfFalse"));
        assertTrue(out.contains("    print int sum\n"));
    }

    @Test
    void runFlagExecutesTheProgram() {
        Object[] r = run("--run", "examples/sample.ey");
        assertEquals(0, r[0]);
        assertEquals("30\ntrue\n", r[1]);
    }

    @Test
    void astFlagPrintsTheSyntaxTree() {
        Object[] r = run("--ast", "examples/sample.ey");
        assertEquals(0, r[0]);
        assertTrue(((String) r[1]).startsWith("Program\n"));
    }

    @Test
    void tokensFlagPrintsTokens() {
        Object[] r = run("--tokens", "examples/sample.ey");
        assertEquals(0, r[0]);
        assertTrue(((String) r[1]).startsWith("KW_CONST"));
    }

    @Test
    void semanticErrorsAreListedAndStopTheCompiler() {
        Object[] r = run("examples/semantic_errors.ey");
        assertEquals(65, r[0]);
        assertEquals("", r[1]);
        String err = (String) r[2];
        assertTrue(err.contains("Semantic error at line 3, col 1: 'count' is already declared in this scope"));
        assertTrue(err.contains("Cannot assign to 'LIMIT'"));
        assertTrue(err.contains("Undeclared variable 'missing'"));
        assertTrue(err.contains("8 semantic errors found. Compilation stopped."));
    }

    @Test
    void semanticErrorsAreNotReportedWhenThereAreSyntaxErrors() {
        Object[] r = run("examples/syntax_errors.ey");
        assertEquals(65, r[0]);
        assertFalse(((String) r[2]).contains("Semantic error"));
    }

    @Test
    void runtimeErrorsExit70AfterThePartThatRan() {
        Object[] r = run("--run", "examples/runtime_error.ey");
        assertEquals(70, r[0]);
        assertEquals("10\n", r[1]);
        assertTrue(((String) r[2]).contains("Runtime error: Division by zero"));
    }

    @Test
    void theIntermediateCodeOfARuntimeErrorProgramStillPrints() {
        Object[] r = run("examples/runtime_error.ey");
        assertEquals(0, r[0]);
    }

    @Test
    void aFlagWithoutAFileIsAUsageError() {
        assertEquals(64, run("--run")[0]);
        assertEquals(64, run("--bogus", "examples/sample.ey")[0]);
    }
}
