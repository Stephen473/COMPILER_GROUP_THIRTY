package compiler.ir;

/**
 * Prints the intermediate code as text. Labels start at the left edge and every other
 * instruction is indented by four spaces:
 *
 * <pre>
 *     x = 5
 *     %1 = x + 1
 * L1:
 *     jumpIfFalse %1, L1
 * </pre>
 */
public final class IrPrinter {

    private IrPrinter() {}

    public static String print(IrProgram program) {
        StringBuilder out = new StringBuilder();
        for (Instr instruction : program.instructions()) {
            out.append(format(instruction)).append('\n');
        }
        return out.toString();
    }

    public static String format(Instr instruction) {
        if (instruction instanceof Instr.Copy c) {
            return "    " + c.dest() + " = " + c.src();
        } else if (instruction instanceof Instr.Binary b) {
            return "    " + b.dest() + " = " + b.left() + " " + b.op() + " " + b.right();
        } else if (instruction instanceof Instr.Unary u) {
            return "    " + u.dest() + " = " + u.op() + u.operand();
        } else if (instruction instanceof Instr.Label l) {
            return l.name() + ":";
        } else if (instruction instanceof Instr.Jump j) {
            return "    jump " + j.target();
        } else if (instruction instanceof Instr.JumpIfFalse j) {
            return "    jumpIfFalse " + j.condition() + ", " + j.target();
        } else {
            Instr.Print p = (Instr.Print) instruction;
            return "    print " + p.type().keyword() + " " + p.value();
        }
    }
}
