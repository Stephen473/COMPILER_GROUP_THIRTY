package compiler.ast;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AstTest {

    // ---- Stage 1: expression nodes

    @Test
    void integerLiteralKeepsValueAndPosition() {
        Expr.IntLiteral n = new Expr.IntLiteral(42, 3, 7);
        assertEquals(42, n.value());
        assertEquals(3, n.line());
        assertEquals(7, n.column());
    }

    @Test
    void booleanLiteralKeepsValue() {
        assertTrue(new Expr.BoolLiteral(true, 1, 1).value());
        assertFalse(new Expr.BoolLiteral(false, 1, 1).value());
    }

    @Test
    void variableKeepsName() {
        assertEquals("total", new Expr.Variable("total", 2, 5).name());
    }

    @Test
    void unaryHoldsOperatorAndOperand() {
        Expr operand = new Expr.IntLiteral(5, 1, 2);
        Expr.Unary u = new Expr.Unary("-", operand, 1, 1);
        assertEquals("-", u.operator());
        assertEquals(operand, u.operand());
    }

    @Test
    void binaryHoldsBothSidesAndOperator() {
        Expr left = new Expr.IntLiteral(1, 1, 1);
        Expr right = new Expr.Variable("x", 1, 5);
        Expr.Binary b = new Expr.Binary(left, "+", right, 1, 3);
        assertEquals(left, b.left());
        assertEquals("+", b.operator());
        assertEquals(right, b.right());
        assertEquals(3, b.column());
    }

    @Test
    void nodesWithSameContentAreEqual() {
        assertEquals(new Expr.IntLiteral(1, 1, 1), new Expr.IntLiteral(1, 1, 1));
        assertFalse(new Expr.IntLiteral(1, 1, 1).equals(new Expr.IntLiteral(2, 1, 1)));
    }

    @Test
    void anyExpressionCanBeInspectedWithInstanceof() {
        Expr e = new Expr.Binary(new Expr.IntLiteral(1, 1, 1), "*", new Expr.IntLiteral(2, 1, 5), 1, 3);
        assertTrue(e instanceof Expr.Binary);
        assertFalse(e instanceof Expr.Unary);
        assertEquals(3, e.column());
    }

    // ---- Stage 2: statement nodes and Program

    @Test
    void typeKeywords() {
        assertEquals("int", Type.INT.keyword());
        assertEquals("bool", Type.BOOL.keyword());
    }

    @Test
    void constDeclarationHasInitializer() {
        Expr init = new Expr.IntLiteral(10, 1, 17);
        Stmt.VarDecl d = new Stmt.VarDecl(Type.INT, "LIMIT", init, true, 1, 1);
        assertTrue(d.isConst());
        assertEquals(Type.INT, d.type());
        assertEquals("LIMIT", d.name());
        assertEquals(init, d.initializer());
    }

    @Test
    void declarationWithoutInitializerHoldsNull() {
        Stmt.VarDecl d = new Stmt.VarDecl(Type.BOOL, "done", null, false, 4, 1);
        assertEquals(null, d.initializer());
        assertFalse(d.isConst());
    }

    @Test
    void assignAndPrintHoldTheirExpression() {
        Expr value = new Expr.Variable("y", 2, 5);
        assertEquals(value, new Stmt.Assign("x", value, 2, 1).value());
        assertEquals(value, new Stmt.Print(value, 3, 1).value());
        assertEquals("x", new Stmt.Assign("x", value, 2, 1).name());
    }

    @Test
    void ifWithoutElseHasNullElseBranch() {
        Stmt.Block body = new Stmt.Block(List.of(), 1, 10);
        Stmt.If i = new Stmt.If(new Expr.BoolLiteral(true, 1, 5), body, null, 1, 1);
        assertEquals(body, i.thenBranch());
        assertEquals(null, i.elseBranch());
    }

    @Test
    void elseIfIsAnIfInsideTheElseBranch() {
        Stmt.Block empty = new Stmt.Block(List.of(), 1, 1);
        Expr cond = new Expr.BoolLiteral(true, 1, 1);
        Stmt.If inner = new Stmt.If(cond, empty, null, 3, 8);
        Stmt.If outer = new Stmt.If(cond, empty, inner, 1, 1);
        assertTrue(outer.elseBranch() instanceof Stmt.If);
    }

    @Test
    void whileHoldsConditionAndBody() {
        Expr cond = new Expr.Variable("go", 1, 8);
        Stmt.Block body = new Stmt.Block(List.of(new Stmt.Break(2, 5)), 1, 12);
        Stmt.While w = new Stmt.While(cond, body, 1, 1);
        assertEquals(cond, w.condition());
        assertEquals(1, w.body().statements().size());
    }

    @Test
    void breakAndContinueKeepPositions() {
        assertEquals(7, new Stmt.Break(7, 9).line());
        assertEquals(9, new Stmt.Continue(8, 9).column());
    }

    @Test
    void programHoldsStatementsInOrder() {
        Stmt first = new Stmt.Break(1, 1);
        Stmt second = new Stmt.Continue(2, 1);
        Program p = new Program(List.of(first, second));
        assertEquals(2, p.statements().size());
        assertEquals(first, p.statements().get(0));
        assertEquals(second, p.statements().get(1));
    }
}
