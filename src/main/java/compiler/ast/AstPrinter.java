package compiler.ast;

/**
 * Turns a syntax tree into text, for tests and for the command line.
 *
 * {@link #print} draws the tree with one node per line, indented two spaces per level.
 * {@link #inline} writes an expression on one line with every operator wrapped in
 * parentheses, which makes precedence easy to see: {@code 1 + 2 * 3} becomes
 * {@code (1 + (2 * 3))}.
 */
public final class AstPrinter {

    private AstPrinter() {}

    public static String print(Program program) {
        StringBuilder out = new StringBuilder();
        line(out, 0, "Program");
        for (Stmt statement : program.statements()) {
            stmt(out, statement, 1);
        }
        return out.toString();
    }

    public static String print(Stmt statement) {
        StringBuilder out = new StringBuilder();
        stmt(out, statement, 0);
        return out.toString();
    }

    public static String print(Expr expression) {
        StringBuilder out = new StringBuilder();
        expr(out, expression, 0);
        return out.toString();
    }

    /** One-line form with full parentheses. */
    public static String inline(Expr e) {
        if (e instanceof Expr.IntLiteral i) {
            return String.valueOf(i.value());
        } else if (e instanceof Expr.BoolLiteral b) {
            return String.valueOf(b.value());
        } else if (e instanceof Expr.Variable v) {
            return v.name();
        } else if (e instanceof Expr.Unary u) {
            return "(" + u.operator() + inline(u.operand()) + ")";
        } else {
            Expr.Binary b = (Expr.Binary) e;
            return "(" + inline(b.left()) + " " + b.operator() + " " + inline(b.right()) + ")";
        }
    }

    private static void line(StringBuilder out, int depth, String text) {
        out.append("  ".repeat(depth)).append(text).append('\n');
    }

    private static void expr(StringBuilder out, Expr e, int depth) {
        if (e instanceof Expr.IntLiteral i) {
            line(out, depth, "Int " + i.value());
        } else if (e instanceof Expr.BoolLiteral b) {
            line(out, depth, "Bool " + b.value());
        } else if (e instanceof Expr.Variable v) {
            line(out, depth, "Var " + v.name());
        } else if (e instanceof Expr.Unary u) {
            line(out, depth, "Unary " + u.operator());
            expr(out, u.operand(), depth + 1);
        } else {
            Expr.Binary b = (Expr.Binary) e;
            line(out, depth, "Binary " + b.operator());
            expr(out, b.left(), depth + 1);
            expr(out, b.right(), depth + 1);
        }
    }

    private static void stmt(StringBuilder out, Stmt s, int depth) {
        if (s instanceof Stmt.VarDecl d) {
            line(out, depth, "VarDecl " + (d.isConst() ? "const " : "") + d.type().keyword() + " " + d.name());
            if (d.initializer() != null) {
                expr(out, d.initializer(), depth + 1);
            }
        } else if (s instanceof Stmt.Assign a) {
            line(out, depth, "Assign " + a.name());
            expr(out, a.value(), depth + 1);
        } else if (s instanceof Stmt.Print p) {
            line(out, depth, "Print");
            expr(out, p.value(), depth + 1);
        } else if (s instanceof Stmt.If i) {
            line(out, depth, "If");
            line(out, depth + 1, "Condition");
            expr(out, i.condition(), depth + 2);
            line(out, depth + 1, "Then");
            stmt(out, i.thenBranch(), depth + 2);
            if (i.elseBranch() != null) {
                line(out, depth + 1, "Else");
                stmt(out, i.elseBranch(), depth + 2);
            }
        } else if (s instanceof Stmt.While w) {
            line(out, depth, "While");
            line(out, depth + 1, "Condition");
            expr(out, w.condition(), depth + 2);
            line(out, depth + 1, "Body");
            stmt(out, w.body(), depth + 2);
        } else if (s instanceof Stmt.Break) {
            line(out, depth, "Break");
        } else if (s instanceof Stmt.Continue) {
            line(out, depth, "Continue");
        } else {
            Stmt.Block b = (Stmt.Block) s;
            line(out, depth, "Block");
            for (Stmt inner : b.statements()) {
                stmt(out, inner, depth + 1);
            }
        }
    }
}
