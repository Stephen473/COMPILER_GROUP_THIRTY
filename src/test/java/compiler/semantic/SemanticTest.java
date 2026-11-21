package compiler.semantic;

import compiler.ast.Program;
import compiler.ast.Stmt;
import compiler.ast.Type;
import compiler.parser.Parser;
import compiler.scanner.Lexer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SemanticTest {

    private static Program parse(String source) {
        Parser parser = new Parser(new Lexer(source).scanTokens());
        Program program = parser.parse();
        assertFalse(parser.hasErrors(), "test program has a syntax error: " + parser.getErrors());
        return program;
    }

    private static Analyzer analyze(String source) {
        Analyzer analyzer = new Analyzer();
        analyzer.analyze(parse(source));
        return analyzer;
    }

    /** Just the error messages, in the order reported. */
    private static List<String> messages(String source) {
        List<String> result = new ArrayList<>();
        for (SemanticError e : analyze(source).getErrors()) {
            result.add(e.message());
        }
        return result;
    }

    private static void assertClean(String source) {
        assertEquals(List.of(), messages(source));
    }

    // ---- Stage 2: names and scopes

    @Test
    void aSimpleCorrectProgramHasNoErrors() {
        assertClean("int x = 1; print(x);");
    }

    @Test
    void undeclaredNameInPrint() {
        Analyzer a = analyze("print(y);");
        assertEquals(1, a.getErrors().size());
        assertEquals("Undeclared variable 'y'", a.getErrors().get(0).message());
        assertEquals(1, a.getErrors().get(0).line());
        assertEquals(7, a.getErrors().get(0).column());
    }

    @Test
    void undeclaredNameOnTheLeftOfAnAssignment() {
        Analyzer a = analyze("x = 1;");
        assertEquals(List.of("Undeclared variable 'x'"), messages("x = 1;"));
        assertEquals(1, a.getErrors().get(0).column());
    }

    @Test
    void undeclaredNameInsideAnExpression() {
        assertEquals(List.of("Undeclared variable 'b'"), messages("int a = b + 1;"));
    }

    @Test
    void usingANameBeforeItsDeclaration() {
        assertEquals(List.of("Undeclared variable 'x'"), messages("print(x); int x = 1;"));
    }

    @Test
    void aVariableCannotUseItselfInItsInitializer() {
        assertEquals(List.of("Undeclared variable 'x'"), messages("int x = x;"));
    }

    @Test
    void duplicateDeclarationInTheSameScope() {
        Analyzer a = analyze("int x = 1;\nint x = 2;");
        assertEquals(1, a.getErrors().size());
        assertEquals("'x' is already declared in this scope (first declared at line 1)",
                a.getErrors().get(0).message());
        assertEquals(2, a.getErrors().get(0).line());
    }

    @Test
    void namesAreCaseSensitive() {
        assertClean("int a; int A;");
    }

    @Test
    void innerBlockMayShadowAnOuterName() {
        assertClean("int x = 1; { int x = 2; print(x); } print(x);");
    }

    @Test
    void innerBlockCanReadOuterNames() {
        assertClean("int x = 1; { print(x); }");
    }

    @Test
    void nameIsNotVisibleAfterItsBlock() {
        assertEquals(List.of("Undeclared variable 'y'"), messages("{ int y = 1; } print(y);"));
    }

    @Test
    void duplicateInsideOneInnerBlock() {
        assertEquals(1, messages("{ int x; int x; }").size());
    }

    @Test
    void loopBodyHasItsOwnScope() {
        assertEquals(List.of("Undeclared variable 't'"),
                messages("while (true) { int t = 1; } print(t);"));
    }

    @Test
    void ifAndElseBranchesHaveSeparateScopes() {
        assertClean("if (true) { int a = 1; } else { int a = 2; }");
    }

    @Test
    void everyErrorIsCollected() {
        assertEquals(2, messages("print(a); print(b);").size());
    }

    @Test
    void aBadInitializerStillDeclaresTheName() {
        // only 'y' is wrong; using x afterwards must not add a second error
        assertEquals(List.of("Undeclared variable 'y'"), messages("int x = y; print(x);"));
    }

    // ---- Stage 3: operator types

    // ---- Stage 4: statements and their types

    // ---- Stage 5: const, break, continue

    // ---- Stage 6: types of expressions, error order, whole programs
}
