package compiler.ast;

/**
 * An expression node. Every expression remembers where it starts in the source,
 * so later phases can report errors with a line and column.
 *
 * Operators are stored as their source text ("+", "&&", "-", "!").
 * Parentheses leave no node behind: the shape of the tree already shows the grouping.
 */
public sealed interface Expr {

    int line();

    int column();

    record IntLiteral(int value, int line, int column) implements Expr {}

    record BoolLiteral(boolean value, int line, int column) implements Expr {}

    record Variable(String name, int line, int column) implements Expr {}

    /** A prefix operator: {@code -x} or {@code !x}. Position is the operator's. */
    record Unary(String operator, Expr operand, int line, int column) implements Expr {}

    /** A binary operator such as {@code a + b}. Position is the operator's. */
    record Binary(Expr left, String operator, Expr right, int line, int column) implements Expr {}
}
