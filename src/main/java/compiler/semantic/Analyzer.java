package compiler.semantic;

import compiler.ast.Expr;
import compiler.ast.Program;
import compiler.ast.Stmt;
import compiler.ast.Type;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Semantic analysis: checks what the parser cannot, namely the meaning of the program.
 * It walks the syntax tree once and collects errors (it never throws on a bad program).
 *
 * Checks, by stage:
 *   2  names are declared before use, no duplicates in a scope, block scoping
 *   3  operand types of every operator
 *   4  initializer, assignment and condition types
 *   5  assignment to const, break and continue outside a loop
 *   6  a type for every expression, errors in source order
 *
 * A type of null means "unknown, an error was already reported". An operator with an
 * unknown operand also gives unknown, so one mistake does not produce a chain of
 * follow-up errors.
 */
public class Analyzer {

    private final SymbolTable symbols = new SymbolTable();
    private final List<SemanticError> errors = new ArrayList<>();
    private int loopDepth = 0;

    // ------------------------------------------------------------------ public API

    public void analyze(Program program) {
        for (Stmt statement : program.statements()) {
            statement(statement);
        }
    }

    public List<SemanticError> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    // ------------------------------------------------------------------ statements

    private void statement(Stmt s) {
        if (s instanceof Stmt.VarDecl d) {
            varDecl(d);
        } else if (s instanceof Stmt.Assign a) {
            assign(a);
        } else if (s instanceof Stmt.Print p) {
            expr(p.value());
        } else if (s instanceof Stmt.If i) {
            condition("if", i.condition());
            statement(i.thenBranch());
            if (i.elseBranch() != null) {
                statement(i.elseBranch());
            }
        } else if (s instanceof Stmt.While w) {
            condition("while", w.condition());
            loopDepth++;
            statement(w.body());
            loopDepth--;
        } else if (s instanceof Stmt.Block b) {
            symbols.enterScope();
            for (Stmt inner : b.statements()) {
                statement(inner);
            }
            symbols.exitScope();
        } else if (s instanceof Stmt.Break) {
            if (loopDepth == 0) {
                error("'break' can only be used inside a while loop", s.line(), s.column());
            }
        } else if (s instanceof Stmt.Continue) {
            if (loopDepth == 0) {
                error("'continue' can only be used inside a while loop", s.line(), s.column());
            }
        }
    }

    private void varDecl(Stmt.VarDecl d) {
        // The initializer is checked BEFORE the name is declared, so "int x = x;" is an error.
        Type initType = d.initializer() == null ? null : expr(d.initializer());
        if (initType != null && initType != d.type()) {
            error("Cannot initialize '" + d.name() + "' (" + d.type().keyword() + ") with "
                    + withArticle(initType) + " value", d.initializer().line(), d.initializer().column());
        }
        Symbol previous = symbols.lookupInCurrentScope(d.name());
        if (previous != null) {
            error("'" + d.name() + "' is already declared in this scope (first declared at line "
                    + previous.line() + ")", d.line(), d.column());
            return;
        }
        symbols.declare(new Symbol(d.name(), d.type(), d.isConst(), d.line(), d.column()));
    }

    private void assign(Stmt.Assign a) {
        Type valueType = expr(a.value());
        Symbol target = symbols.lookup(a.name());
        if (target == null) {
            error("Undeclared variable '" + a.name() + "'", a.line(), a.column());
            return;
        }
        if (target.isConst()) {
            error("Cannot assign to '" + a.name() + "': it is a const (declared at line "
                    + target.line() + ")", a.line(), a.column());
        }
        if (valueType != null && valueType != target.type()) {
            error("Cannot assign " + withArticle(valueType) + " value to '" + a.name() + "' ("
                    + target.type().keyword() + ")", a.value().line(), a.value().column());
        }
    }

    /** The condition of an if or while must be bool. */
    private void condition(String keyword, Expr condition) {
        Type t = expr(condition);
        if (t != null && t != Type.BOOL) {
            error("The condition of '" + keyword + "' must be bool, but found " + t.keyword(),
                    condition.line(), condition.column());
        }
    }

    // ------------------------------------------------------------------ expressions

    /** Checks an expression and returns its type, or null if it has an error. */
    private Type expr(Expr e) {
        if (e instanceof Expr.IntLiteral) {
            return Type.INT;
        } else if (e instanceof Expr.BoolLiteral) {
            return Type.BOOL;
        } else if (e instanceof Expr.Variable v) {
            Symbol symbol = symbols.lookup(v.name());
            if (symbol == null) {
                error("Undeclared variable '" + v.name() + "'", v.line(), v.column());
                return null;
            }
            return symbol.type();
        } else if (e instanceof Expr.Unary u) {
            return unary(u);
        } else {
            return binary((Expr.Binary) e);
        }
    }

    private Type unary(Expr.Unary u) {
        Type operand = expr(u.operand());
        if (operand == null) {
            return null; // already reported: do not add a follow-up error
        }
        Type expected = u.operator().equals("-") ? Type.INT : Type.BOOL;
        if (operand != expected) {
            error("Operator '" + u.operator() + "' needs " + withArticle(expected) + " operand, but found "
                    + operand.keyword(), u.line(), u.column());
        }
        return expected;
    }

    private Type binary(Expr.Binary b) {
        Type left = expr(b.left());
        Type right = expr(b.right());
        if (left == null || right == null) {
            return null; // an operand was already reported: do not add a follow-up error
        }
        String op = b.operator();
        switch (op) {
            case "+", "-", "*", "/", "%":
                requireOperands(b, left, right, Type.INT);
                return Type.INT;
            case "<", "<=", ">", ">=":
                requireOperands(b, left, right, Type.INT);
                return Type.BOOL;
            case "&&", "||":
                requireOperands(b, left, right, Type.BOOL);
                return Type.BOOL;
            default: // "==" and "!=": both int or both bool
                if (left != right) {
                    error("Operator '" + op + "' needs two operands of the same type, but found "
                            + left.keyword() + " and " + right.keyword(), b.line(), b.column());
                }
                return Type.BOOL;
        }
    }

    private void requireOperands(Expr.Binary b, Type left, Type right, Type expected) {
        if (left != expected || right != expected) {
            error("Operator '" + b.operator() + "' needs " + expected.keyword() + " operands, but found "
                    + left.keyword() + " and " + right.keyword(), b.line(), b.column());
        }
    }

    private String withArticle(Type t) {
        return t == Type.INT ? "an int" : "a bool";
    }

    // ------------------------------------------------------------------ errors

    private void error(String message, int line, int column) {
        errors.add(new SemanticError(message, line, column));
    }
}
