package compiler.ir;

import compiler.ast.Program;
import compiler.parser.Parser;
import compiler.scanner.Lexer;
import compiler.semantic.Analyzer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IrGeneratorTest {

    /** Source in, intermediate code (as text) out. The source must be correct. */
    private static String ir(String source) {
        Parser parser = new Parser(new Lexer(source).scanTokens());
        Program program = parser.parse();
        assertFalse(parser.hasErrors(), "syntax error in test program");
        Analyzer analyzer = new Analyzer();
        analyzer.analyze(program);
        assertFalse(analyzer.hasErrors(), "semantic error in test program: " + analyzer.getErrors());
        return IrPrinter.print(new IrGenerator(program, analyzer).generate());
    }

    // ---- Stage 8: expressions, simple statements, scopes

    @Test
    void declarationWithALiteral() {
        assertEquals("    x = 5\n", ir("int x = 5;"));
    }

    @Test
    void declarationWithoutAnInitializerGetsTheDefault() {
        assertEquals("    n = 0\n    b = 0\n", ir("int n; bool b;"));
    }

    @Test
    void booleansAreZeroAndOne() {
        assertEquals("    t = 1\n    f = 0\n", ir("bool t = true; bool f = false;"));
    }

    @Test
    void assignment() {
        assertEquals("    x = 0\n    x = 7\n", ir("int x = 0; x = 7;"));
    }

    @Test
    void arithmeticUsesTemporaries() {
        String expected = """
                    a = 1
                    %1 = 2 * 3
                    %2 = a + %1
                    b = %2
                """;
        assertEquals(expected, ir("int a = 1; int b = a + 2 * 3;"));
    }

    @Test
    void operatorsOfEqualPrecedenceGoLeftToRight() {
        String expected = """
                    %1 = 1 - 2
                    %2 = %1 - 3
                    x = %2
                """;
        assertEquals(expected, ir("int x = 1 - 2 - 3;"));
    }

    @Test
    void unaryOperators() {
        String expected = """
                    a = 1
                    %1 = -a
                    b = %1
                    %2 = !1
                    t = %2
                """;
        assertEquals(expected, ir("int a = 1; int b = -a; bool t = !true;"));
    }

    @Test
    void comparisonGivesAZeroOrOne() {
        assertEquals("    %1 = 1 < 2\n    c = %1\n", ir("bool c = 1 < 2;"));
    }

    @Test
    void printRemembersTheType() {
        String expected = """
                    a = 1
                    print int a
                    %1 = a == 1
                    print bool %1
                """;
        assertEquals(expected, ir("int a = 1; print(a); print(a == 1);"));
    }

    @Test
    void anInnerVariableWithTheSameNameGetsANewName() {
        String expected = """
                    x = 1
                    x.2 = 2
                    print int x.2
                    print int x
                """;
        assertEquals(expected, ir("int x = 1; { int x = 2; print(x); } print(x);"));
    }

    @Test
    void theInitializerOfAShadowingVariableUsesTheOuterOne() {
        String expected = """
                    x = 1
                    %1 = x + 1
                    x.2 = %1
                """;
        assertEquals(expected, ir("int x = 1; { int x = x + 1; }"));
    }

    @Test
    void siblingBlocksGetDifferentNames() {
        assertEquals("    t = 1\n    t.2 = 2\n", ir("{ int t = 1; } { int t = 2; }"));
    }

    @Test
    void andSkipsTheRightSideWhenTheLeftIsFalse() {
        String expected = """
                    a = 1
                    b = 0
                    %1 = a
                    jumpIfFalse %1, L1
                    %1 = b
                L1:
                    c = %1
                """;
        assertEquals(expected, ir("bool a = true; bool b = false; bool c = a && b;"));
    }

    @Test
    void orSkipsTheRightSideWhenTheLeftIsTrue() {
        String expected = """
                    a = 1
                    b = 0
                    %1 = a
                    %2 = !%1
                    jumpIfFalse %2, L1
                    %1 = b
                L1:
                    c = %1
                """;
        assertEquals(expected, ir("bool a = true; bool b = false; bool c = a || b;"));
    }

    @Test
    void theRightSideOfAndIsOnlyEvaluatedAfterTheJump() {
        String text = ir("int x = 0; bool r = x != 0 && 10 / x > 1;");
        assertTrue(text.indexOf("jumpIfFalse") < text.indexOf("10 / x"));
    }

    @Test
    void everyTemporaryAndLabelIsDistinct() {
        String text = ir("bool a = true; bool r = (a || a) && (a || a);");
        assertTrue(text.contains("L1:"));
        assertTrue(text.contains("L2:"));
        assertTrue(text.contains("L3:"));
    }

    // ---- Stage 9: control flow

    @Test
    void ifWithoutElse() {
        String expected = """
                    x = 1
                    %1 = x > 0
                    jumpIfFalse %1, L1
                    print int x
                L1:
                """;
        assertEquals(expected, ir("int x = 1; if (x > 0) { print(x); }"));
    }

    @Test
    void ifWithElse() {
        String expected = """
                    b = 1
                    jumpIfFalse b, L1
                    print int 1
                    jump L2
                L1:
                    print int 2
                L2:
                """;
        assertEquals(expected, ir("bool b = true; if (b) { print(1); } else { print(2); }"));
    }

    @Test
    void elseIfChain() {
        String text = ir("bool a = true; bool b = false;"
                + " if (a) { print(1); } else if (b) { print(2); } else { print(3); }");
        String expected = """
                    a = 1
                    b = 0
                    jumpIfFalse a, L1
                    print int 1
                    jump L2
                L1:
                    jumpIfFalse b, L3
                    print int 2
                    jump L4
                L3:
                    print int 3
                L4:
                L2:
                """;
        assertEquals(expected, text);
    }

    @Test
    void whileLoop() {
        String expected = """
                    i = 0
                L1:
                    %1 = i < 3
                    jumpIfFalse %1, L2
                    %2 = i + 1
                    i = %2
                    jump L1
                L2:
                """;
        assertEquals(expected, ir("int i = 0; while (i < 3) { i = i + 1; }"));
    }

    @Test
    void breakJumpsToTheEndAndContinueToTheStart() {
        String expected = """
                L1:
                    jumpIfFalse 1, L2
                    jumpIfFalse 0, L3
                    jump L2
                L3:
                    jump L1
                    jump L1
                L2:
                """;
        assertEquals(expected, ir("while (true) { if (false) { break; } continue; }"));
    }

    @Test
    void breakInANestedLoopLeavesOnlyTheInnerLoop() {
        String text = ir("while (true) { while (true) { break; } }");
        String expected = """
                L1:
                    jumpIfFalse 1, L2
                L3:
                    jumpIfFalse 1, L4
                    jump L4
                    jump L3
                L4:
                    jump L1
                L2:
                """;
        assertEquals(expected, text);
    }

    @Test
    void variablesDeclaredInALoopBodyKeepOneNamePerDeclaration() {
        String text = ir("int i = 0; while (i < 2) { int t = i; i = i + 1; }");
        assertTrue(text.contains("    t = i\n"));
        assertFalse(text.contains("t.2"));
    }

    @Test
    void theSampleProgram() {
        String expected = """
                    LIMIT = 10
                    n = 1
                    sum = 0
                L1:
                    %1 = n <= LIMIT
                    jumpIfFalse %1, L2
                    %2 = n % 2
                    %3 = %2 == 0
                    jumpIfFalse %3, L3
                    %4 = sum + n
                    sum = %4
                L3:
                    %5 = n + 1
                    n = %5
                    jump L1
                L2:
                    print int sum
                    %6 = sum > 20
                    big = %6
                    print bool big
                """;
        String source = """
                const int LIMIT = 10;
                int n = 1;
                int sum = 0;
                while (n <= LIMIT) {
                    if (n % 2 == 0) {
                        sum = sum + n;
                    }
                    n = n + 1;
                }
                print(sum);
                bool big = sum > 20;
                print(big);
                """;
        assertEquals(expected, ir(source));
    }
}
