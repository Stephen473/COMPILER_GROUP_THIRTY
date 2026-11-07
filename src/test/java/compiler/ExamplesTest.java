package compiler;

import compiler.ast.Program;
import compiler.parser.Parser;
import compiler.scanner.Lexer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Runs the real example programs through the scanner, the parser and Main. */
class ExamplesTest {

    private static Parser parserForFile(String path) throws IOException {
        return new Parser(new Lexer(Files.readString(Path.of(path))).scanTokens());
    }

    /** Runs Main.run and returns {exit code, stdout, stderr}. */
    private static Object[] run(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = Main.run(args, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Object[] {code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8)};
    }

    @Test
    void sampleProgramParsesWithoutErrors() throws IOException {
        Parser p = parserForFile("examples/sample.ey");
        Program program = p.parse();
        assertFalse(p.hasErrors());
        assertEquals(7, program.statements().size());
    }

    @Test
    void featureTourParsesWithoutErrors() throws IOException {
        Parser p = parserForFile("examples/features.ey");
        Program program = p.parse();
        assertFalse(p.hasErrors(), "errors: " + p.getErrors());
        assertEquals(7, program.statements().size());
    }

    @Test
    void syntaxErrorsFileReportsEveryMistake() throws IOException {
        Parser p = parserForFile("examples/syntax_errors.ey");
        p.parse();
        assertEquals(6, p.getErrors().size());
    }

    @Test
    void mainPrintsTheTreeForAValidProgram() {
        Object[] r = run("examples/sample.ey");
        assertEquals(0, r[0]);
        String out = (String) r[1];
        assertTrue(out.startsWith("Program\n"));
        assertTrue(out.contains("VarDecl const int LIMIT"));
        assertTrue(out.contains("While"));
    }

    @Test
    void mainPrintsSyntaxErrorsAndExits65() {
        Object[] r = run("examples/syntax_errors.ey");
        assertEquals(65, r[0]);
        String err = (String) r[2];
        assertTrue(err.contains("Syntax error at line "));
        assertTrue(err.contains("6 syntax errors found. Compilation stopped."));
        assertEquals("", r[1]);
    }

    @Test
    void lexicalErrorsStopBeforeTheParser() {
        Object[] r = run("examples/errors.ey");
        assertEquals(65, r[0]);
        String err = (String) r[2];
        assertTrue(err.contains("Lexical error at line 2"));
        assertFalse(err.contains("Syntax error"));
    }

    @Test
    void tokensFlagStillPrintsTheTokenList() {
        Object[] r = run("--tokens", "examples/sample.ey");
        assertEquals(0, r[0]);
        String out = (String) r[1];
        assertTrue(out.startsWith("KW_CONST"));
        assertFalse(out.contains("Program"));
    }

    @Test
    void usageErrorExits64() {
        assertEquals(64, run()[0]);
        assertEquals(64, run("a.ey", "b.ey")[0]);
    }

    @Test
    void missingFileExits66() {
        Object[] r = run("examples/no_such_file.ey");
        assertEquals(66, r[0]);
        assertTrue(((String) r[2]).contains("Cannot read file"));
    }
}
