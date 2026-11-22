package compiler.ir;

import compiler.ast.Expr;
import compiler.ast.Program;
import compiler.ast.Stmt;
import compiler.semantic.Analyzer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a program that passed semantic analysis into intermediate code.
 *
 * Expressions become instructions that store results in temporaries (%1, %2, ...).
 * Control flow becomes labels and jumps. Because the program is already known to be
 * correct, this class does no error checking.
 *
 * Shadowed variables get distinct names: the first "x" stays "x", and an inner
 * "x" becomes "x.2", so later phases never have to think about scopes.
 */
public class IrGenerator {

    private final Program program;
    private final Analyzer analysis;
    private final List<Instr> code = new ArrayList<>();

    private final Deque<Map<String, String>> scopes = new ArrayDeque<>();
    private final Map<String, Integer> declared = new HashMap<>();
    private int tempCount = 0;
    private int labelCount = 0;
    private final Deque<String> continueLabels = new ArrayDeque<>();
    private final Deque<String> breakLabels = new ArrayDeque<>();

    /** The analyzer must already have analyzed the program, and found no errors. */
    public IrGenerator(Program program, Analyzer analysis) {
        this.program = program;
        this.analysis = analysis;
    }

    public IrProgram generate() {
        scopes.push(new HashMap<>());
        for (Stmt statement : program.statements()) {
            statement(statement);
        }
        return new IrProgram(code);
    }

    // ------------------------------------------------------------------ statements

    private void statement(Stmt s) {
        if (s instanceof Stmt.VarDecl d) {
            Operand value = d.initializer() == null ? new Operand.Const(0) : expr(d.initializer());
            // declared after the initializer is generated, like in the analyzer
            code.add(new Instr.Copy(declare(d.name()), value));
        } else if (s instanceof Stmt.Assign a) {
            Operand value = expr(a.value());
            code.add(new Instr.Copy(resolve(a.name()), value));
        } else if (s instanceof Stmt.Print p) {
            Operand value = expr(p.value());
            code.add(new Instr.Print(value, analysis.typeOf(p.value())));
        } else if (s instanceof Stmt.Block b) {
            scopes.push(new HashMap<>());
            for (Stmt inner : b.statements()) {
                statement(inner);
            }
            scopes.pop();
        } else if (s instanceof Stmt.If i) {
            ifStatement(i);
        } else if (s instanceof Stmt.While w) {
            whileStatement(w);
        } else if (s instanceof Stmt.Break) {
            code.add(new Instr.Jump(breakLabels.peek()));
        } else if (s instanceof Stmt.Continue) {
            code.add(new Instr.Jump(continueLabels.peek()));
        }
    }

    /**
     * <pre>
     *   jumpIfFalse cond, Lelse
     *   ...then...
     *   jump Lend          (only when there is an else)
     * Lelse:
     *   ...else...
     * Lend:
     * </pre>
     */
    private void ifStatement(Stmt.If i) {
        Operand condition = expr(i.condition());
        String elseLabel = newLabel();
        code.add(new Instr.JumpIfFalse(condition, elseLabel));
        statement(i.thenBranch());
        if (i.elseBranch() == null) {
            code.add(new Instr.Label(elseLabel));
            return;
        }
        String endLabel = newLabel();
        code.add(new Instr.Jump(endLabel));
        code.add(new Instr.Label(elseLabel));
        statement(i.elseBranch());
        code.add(new Instr.Label(endLabel));
    }

    /**
     * <pre>
     * Lstart:
     *   jumpIfFalse cond, Lend
     *   ...body...
     *   jump Lstart
     * Lend:
     * </pre>
     * 'continue' jumps to Lstart and 'break' jumps to Lend.
     */
    private void whileStatement(Stmt.While w) {
        String start = newLabel();
        String end = newLabel();
        code.add(new Instr.Label(start));
        Operand condition = expr(w.condition());
        code.add(new Instr.JumpIfFalse(condition, end));
        continueLabels.push(start);
        breakLabels.push(end);
        statement(w.body());
        breakLabels.pop();
        continueLabels.pop();
        code.add(new Instr.Jump(start));
        code.add(new Instr.Label(end));
    }

    // ------------------------------------------------------------------ expressions

    /** Generates code for an expression and returns where its value will be. */
    private Operand expr(Expr e) {
        if (e instanceof Expr.IntLiteral i) {
            return new Operand.Const(i.value());
        } else if (e instanceof Expr.BoolLiteral b) {
            return new Operand.Const(b.value() ? 1 : 0);
        } else if (e instanceof Expr.Variable v) {
            return new Operand.Name(resolve(v.name()));
        } else if (e instanceof Expr.Unary u) {
            Operand operand = expr(u.operand());
            String dest = newTemp();
            code.add(new Instr.Unary(dest, u.operator(), operand));
            return new Operand.Name(dest);
        }
        Expr.Binary b = (Expr.Binary) e;
        if (b.operator().equals("&&") || b.operator().equals("||")) {
            return shortCircuit(b);
        }
        Operand left = expr(b.left());
        Operand right = expr(b.right());
        String dest = newTemp();
        code.add(new Instr.Binary(dest, b.operator(), left, right));
        return new Operand.Name(dest);
    }

    /**
     * && and || must not evaluate the right side when the left side decides the result.
     * <pre>
     *   a && b:   r = a ; jumpIfFalse r, Lend ; r = b ; Lend:
     *   a || b:   r = a ; n = !r ; jumpIfFalse n, Lend ; r = b ; Lend:
     * </pre>
     */
    private Operand shortCircuit(Expr.Binary b) {
        String result = newTemp();
        code.add(new Instr.Copy(result, expr(b.left())));
        Operand test = new Operand.Name(result);
        if (b.operator().equals("||")) {
            String negated = newTemp();
            code.add(new Instr.Unary(negated, "!", test));
            test = new Operand.Name(negated);
        }
        String end = newLabel();
        code.add(new Instr.JumpIfFalse(test, end));
        code.add(new Instr.Copy(result, expr(b.right())));
        code.add(new Instr.Label(end));
        return new Operand.Name(result);
    }

    // ------------------------------------------------------------------ names

    private String newTemp() {
        tempCount++;
        return "%" + tempCount;
    }

    private String newLabel() {
        labelCount++;
        return "L" + labelCount;
    }

    /** Declares a variable in the current scope and returns its unique IR name. */
    private String declare(String name) {
        int count = declared.merge(name, 1, Integer::sum);
        String unique = count == 1 ? name : name + "." + count;
        scopes.peek().put(name, unique);
        return unique;
    }

    /** The unique IR name of the nearest visible declaration of a name. */
    private String resolve(String name) {
        for (Map<String, String> scope : scopes) {
            String unique = scope.get(name);
            if (unique != null) {
                return unique;
            }
        }
        throw new IllegalStateException("Undeclared name '" + name + "': run the analyzer first");
    }
}
