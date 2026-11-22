package compiler.ir;

import compiler.ast.Program;
import compiler.parser.Parser;
import compiler.scanner.Lexer;
import compiler.semantic.Analyzer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Compiles Eyra source to intermediate code and runs it. Checks that the code means the right thing. */
class IrInterpreterTest {

    private static IrProgram compile(String source) {
        Parser parser = new Parser(new Lexer(source).scanTokens());
        Program program = parser.parse();
        assertFalse(parser.hasErrors());
        Analyzer analyzer = new Analyzer();
        analyzer.analyze(program);
        assertFalse(analyzer.hasErrors(), "semantic errors: " + analyzer.getErrors());
        return new IrGenerator(program, analyzer).generate();
    }

    /** Runs a program and returns everything it printed, one line per print. */
    private static String output(String source) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new IrInterpreter(compile(source)).run(new PrintStream(bytes, true, StandardCharsets.UTF_8));
        return bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    @Test
    void printsIntsAndBools() {
        assertEquals("42\ntrue\nfalse\n", output("print(42); print(true); print(1 > 2);"));
    }

    @Test
    void arithmeticFollowsPrecedence() {
        assertEquals("14\n20\n", output("print(2 + 3 * 4); print((2 + 3) * 4);"));
    }

    @Test
    void divisionTruncatesTowardZeroAndRemainderFollowsTheLeftOperand() {
        assertEquals("-3\n-1\n3\n1\n", output("print(-7 / 2); print(-7 % 2); print(7 / 2); print(7 % -2);"));
    }

    @Test
    void integersWrapAround() {
        assertEquals("-2147483648\n", output("int big = 2147483647; print(big + 1);"));
    }

    @Test
    void variablesDefaultToZeroAndFalse() {
        assertEquals("0\nfalse\n", output("int n; bool b; print(n); print(b);"));
    }

    @Test
    void whileLoopCounts() {
        assertEquals("0\n1\n2\n", output("int i = 0; while (i < 3) { print(i); i = i + 1; }"));
    }

    @Test
    void breakAndContinue() {
        String source = """
                int i = 0;
                while (i < 10) {
                    i = i + 1;
                    if (i == 3) { continue; }
                    if (i == 6) { break; }
                    print(i);
                }
                """;
        assertEquals("1\n2\n4\n5\n", output(source));
    }

    @Test
    void ifElseIfElse() {
        String source = """
                int n = 7;
                if (n < 5) { print(1); } else if (n < 10) { print(2); } else { print(3); }
                """;
        assertEquals("2\n", output(source));
    }

    @Test
    void shadowingKeepsTheOuterValue() {
        assertEquals("2\n1\n", output("int x = 1; { int x = 2; print(x); } print(x);"));
    }

    @Test
    void andDoesNotEvaluateTheRightSideWhenTheLeftIsFalse() {
        // 10 / zero would be a runtime error if it were evaluated
        assertEquals("false\n", output("int zero = 0; print(zero != 0 && 10 / zero > 1);"));
    }

    @Test
    void orDoesNotEvaluateTheRightSideWhenTheLeftIsTrue() {
        assertEquals("true\n", output("int zero = 0; print(zero == 0 || 10 / zero > 1);"));
    }

    @Test
    void logicalOperatorsGiveTheRightTruthTable() {
        assertEquals("true\nfalse\nfalse\ntrue\ntrue\ntrue\nfalse\n",
                output("print(true && true); print(true && false); print(false || false);"
                        + " print(false || true); print(!false); print(!(1 > 2)); print(!true);"));
    }

    @Test
    void divisionByZeroIsARuntimeError() {
        IrInterpreter.RuntimeError e = assertThrows(IrInterpreter.RuntimeError.class,
                () -> output("int z = 0; print(1 / z);"));
        assertEquals("Division by zero", e.getMessage());
    }

    @Test
    void remainderByZeroIsARuntimeError() {
        assertThrows(IrInterpreter.RuntimeError.class, () -> output("int z = 0; print(1 % z);"));
    }

    @Test
    void anEndlessLoopHitsTheStepLimit() {
        IrInterpreter.RuntimeError e = assertThrows(IrInterpreter.RuntimeError.class,
                () -> output("while (true) { }"));
        assertTrue(e.getMessage().startsWith("Step limit exceeded"));
    }

    @Test
    void theSampleProgramPrintsThirtyAndTrue() throws IOException {
        assertEquals("30\ntrue\n", output(Files.readString(Path.of("examples/sample.ey"))));
    }

    @Test
    void theFeatureTourRuns() throws IOException {
        String text = output(Files.readString(Path.of("examples/features.ey")));
        assertEquals("-127\nfalse\ntrue\n", text);
    }
}
