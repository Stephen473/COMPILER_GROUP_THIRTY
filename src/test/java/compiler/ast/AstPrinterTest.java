package compiler.ast;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AstPrinterTest {

    private static Expr.IntLiteral num(int v) {
        return new Expr.IntLiteral(v, 1, 1);
    }

    private static Expr.Variable var(String n) {
        return new Expr.Variable(n, 1, 1);
    }

    private static Expr.Binary bin(Expr l, String op, Expr r) {
        return new Expr.Binary(l, op, r, 1, 1);
    }

    // ---- Stage 3: the printer

    @Test
    void inlineLiteralsAndVariables() {
        assertEquals("7", AstPrinter.inline(num(7)));
        assertEquals("true", AstPrinter.inline(new Expr.BoolLiteral(true, 1, 1)));
        assertEquals("count", AstPrinter.inline(var("count")));
    }

    @Test
    void inlineUnary() {
        assertEquals("(-5)", AstPrinter.inline(new Expr.Unary("-", num(5), 1, 1)));
        assertEquals("(!done)", AstPrinter.inline(new Expr.Unary("!", var("done"), 1, 1)));
    }

    @Test
    void inlineBinaryAddsParentheses() {
        assertEquals("(1 + 2)", AstPrinter.inline(bin(num(1), "+", num(2))));
    }

    @Test
    void inlineShowsGrouping() {
        Expr e = bin(num(1), "+", bin(num(2), "*", num(3)));
        assertEquals("(1 + (2 * 3))", AstPrinter.inline(e));
        Expr f = bin(bin(num(1), "+", num(2)), "*", num(3));
        assertEquals("((1 + 2) * 3)", AstPrinter.inline(f));
    }

    @Test
    void treePrintOfLiteralsAndVariables() {
        assertEquals("Int 7\n", AstPrinter.print(num(7)));
        assertEquals("Bool false\n", AstPrinter.print(new Expr.BoolLiteral(false, 1, 1)));
        assertEquals("Var n\n", AstPrinter.print(var("n")));
    }

    @Test
    void treePrintOfExpressionIndentsChildren() {
        Expr e = bin(var("a"), "+", new Expr.Unary("-", num(1), 1, 1));
        String expected = """
                Binary +
                  Var a
                  Unary -
                    Int 1
                """;
        assertEquals(expected, AstPrinter.print(e));
    }

    @Test
    void emptyProgram() {
        assertEquals("Program\n", AstPrinter.print(new Program(List.of())));
    }

    @Test
    void declarationsShowConstAndType() {
        Program p = new Program(List.of(
                new Stmt.VarDecl(Type.INT, "LIMIT", num(10), true, 1, 1),
                new Stmt.VarDecl(Type.BOOL, "done", null, false, 2, 1)));
        String expected = """
                Program
                  VarDecl const int LIMIT
                    Int 10
                  VarDecl bool done
                """;
        assertEquals(expected, AstPrinter.print(p));
    }

    @Test
    void assignAndPrint() {
        Program p = new Program(List.of(
                new Stmt.Assign("x", bin(var("x"), "+", num(1)), 1, 1),
                new Stmt.Print(var("x"), 2, 1)));
        String expected = """
                Program
                  Assign x
                    Binary +
                      Var x
                      Int 1
                  Print
                    Var x
                """;
        assertEquals(expected, AstPrinter.print(p));
    }

    @Test
    void ifElseIfElseShowsLabelledBranches() {
        Stmt.Block empty = new Stmt.Block(List.of(), 1, 1);
        Stmt.Block body = new Stmt.Block(List.of(new Stmt.Print(num(1), 1, 1)), 1, 1);
        Stmt.If inner = new Stmt.If(var("b"), body, empty, 1, 1);
        Stmt.If outer = new Stmt.If(var("a"), empty, inner, 1, 1);
        String expected = """
                If
                  Condition
                    Var a
                  Then
                    Block
                  Else
                    If
                      Condition
                        Var b
                      Then
                        Block
                          Print
                            Int 1
                      Else
                        Block
                """;
        assertEquals(expected, AstPrinter.print((Stmt) outer));
    }

    @Test
    void whileBreakContinue() {
        Stmt.Block body = new Stmt.Block(List.of(new Stmt.Break(1, 1), new Stmt.Continue(1, 1)), 1, 1);
        Stmt w = new Stmt.While(new Expr.BoolLiteral(true, 1, 1), body, 1, 1);
        String expected = """
                While
                  Condition
                    Bool true
                  Body
                    Block
                      Break
                      Continue
                """;
        assertEquals(expected, AstPrinter.print(w));
    }
}
