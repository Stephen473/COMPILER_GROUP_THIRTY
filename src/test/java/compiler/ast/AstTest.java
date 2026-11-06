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
}
