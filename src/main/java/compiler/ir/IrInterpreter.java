package compiler.ir;

import compiler.ast.Type;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs intermediate code directly. It is not the real back end (that is Phase 4), but it
 * lets us run Eyra programs now, and later it lets us check that the optimizer
 * (Phase 5) did not change what a program prints.
 *
 * Runtime rules from the language spec: 32-bit wrap-around arithmetic, division and
 * remainder by zero are errors, and a step limit stops endless loops.
 */
public class IrInterpreter {

    /** A problem that can only be found while the program runs. */
    public static class RuntimeError extends RuntimeException {
        public RuntimeError(String message) {
            super(message);
        }
    }

    public static final long STEP_LIMIT = 10_000_000L;

    private final List<Instr> code;
    private final Map<String, Integer> labels = new HashMap<>();
    private final Map<String, Integer> memory = new HashMap<>();

    public IrInterpreter(IrProgram program) {
        this.code = program.instructions();
        for (int i = 0; i < code.size(); i++) {
            if (code.get(i) instanceof Instr.Label l) {
                labels.put(l.name(), i);
            }
        }
    }

    /** Runs the program, writing what it prints to {@code out}. Throws RuntimeError on a runtime error. */
    public void run(PrintStream out) {
        int pc = 0;
        long steps = 0;
        while (pc < code.size()) {
            if (++steps > STEP_LIMIT) {
                throw new RuntimeError("Step limit exceeded (" + STEP_LIMIT + " steps): is there an endless loop?");
            }
            Instr instruction = code.get(pc);
            pc++;
            if (instruction instanceof Instr.Copy c) {
                memory.put(c.dest(), value(c.src()));
            } else if (instruction instanceof Instr.Binary b) {
                memory.put(b.dest(), compute(b.op(), value(b.left()), value(b.right())));
            } else if (instruction instanceof Instr.Unary u) {
                int v = value(u.operand());
                memory.put(u.dest(), u.op().equals("-") ? -v : (v == 0 ? 1 : 0));
            } else if (instruction instanceof Instr.Jump j) {
                pc = labels.get(j.target());
            } else if (instruction instanceof Instr.JumpIfFalse j) {
                if (value(j.condition()) == 0) {
                    pc = labels.get(j.target());
                }
            } else if (instruction instanceof Instr.Print p) {
                int v = value(p.value());
                out.println(p.type() == Type.BOOL ? (v != 0 ? "true" : "false") : String.valueOf(v));
            }
            // labels do nothing when reached
        }
    }

    private int value(Operand operand) {
        if (operand instanceof Operand.Const c) {
            return c.value();
        }
        Integer stored = memory.get(((Operand.Name) operand).id());
        return stored == null ? 0 : stored; // declared variables always have a value; 0 is the safe default
    }

    private int compute(String op, int a, int b) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/":
                if (b == 0) {
                    throw new RuntimeError("Division by zero");
                }
                return a / b;
            case "%":
                if (b == 0) {
                    throw new RuntimeError("Remainder by zero");
                }
                return a % b;
            case "<": return a < b ? 1 : 0;
            case "<=": return a <= b ? 1 : 0;
            case ">": return a > b ? 1 : 0;
            case ">=": return a >= b ? 1 : 0;
            case "==": return a == b ? 1 : 0;
            case "!=": return a != b ? 1 : 0;
            default: throw new IllegalStateException("Unknown operator " + op);
        }
    }
}
