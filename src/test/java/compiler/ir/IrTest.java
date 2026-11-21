package compiler.ir;

import compiler.ast.Type;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IrTest {

    private static Operand.Name name(String id) {
        return new Operand.Name(id);
    }

    private static Operand.Const num(int v) {
        return new Operand.Const(v);
    }

    // ---- Stage 7: instructions and the printer

    @Test
    void operandsPrintAsTheirValue() {
        assertEquals("5", num(5).toString());
        assertEquals("-3", num(-3).toString());
        assertEquals("%1", name("%1").toString());
        assertEquals("sum", name("sum").toString());
    }

    @Test
    void copy() {
        assertEquals("    x = 5", IrPrinter.format(new Instr.Copy("x", num(5))));
        assertEquals("    y = x", IrPrinter.format(new Instr.Copy("y", name("x"))));
    }

    @Test
    void binary() {
        assertEquals("    %1 = a + 1", IrPrinter.format(new Instr.Binary("%1", "+", name("a"), num(1))));
        assertEquals("    %2 = %1 <= b", IrPrinter.format(new Instr.Binary("%2", "<=", name("%1"), name("b"))));
    }

    @Test
    void unary() {
        assertEquals("    %1 = -a", IrPrinter.format(new Instr.Unary("%1", "-", name("a"))));
        assertEquals("    %2 = !b", IrPrinter.format(new Instr.Unary("%2", "!", name("b"))));
    }

    @Test
    void labelsStartAtTheLeftEdge() {
        assertEquals("L1:", IrPrinter.format(new Instr.Label("L1")));
    }

    @Test
    void jumps() {
        assertEquals("    jump L3", IrPrinter.format(new Instr.Jump("L3")));
        assertEquals("    jumpIfFalse %1, L2", IrPrinter.format(new Instr.JumpIfFalse(name("%1"), "L2")));
    }

    @Test
    void printShowsTheType() {
        assertEquals("    print int x", IrPrinter.format(new Instr.Print(name("x"), Type.INT)));
        assertEquals("    print bool %1", IrPrinter.format(new Instr.Print(name("%1"), Type.BOOL)));
    }

    @Test
    void aWholeProgramIsOneInstructionPerLine() {
        IrProgram p = new IrProgram(List.of(
                new Instr.Copy("i", num(0)),
                new Instr.Label("L1"),
                new Instr.Binary("%1", "<", name("i"), num(3)),
                new Instr.JumpIfFalse(name("%1"), "L2"),
                new Instr.Jump("L1"),
                new Instr.Label("L2")));
        String expected = """
                    i = 0
                L1:
                    %1 = i < 3
                    jumpIfFalse %1, L2
                    jump L1
                L2:
                """;
        assertEquals(expected, IrPrinter.print(p));
    }

    @Test
    void anEmptyProgramPrintsNothing() {
        assertEquals("", IrPrinter.print(new IrProgram(List.of())));
    }

    @Test
    void instructionsWithTheSameContentAreEqual() {
        assertEquals(new Instr.Copy("x", num(1)), new Instr.Copy("x", num(1)));
        assertNotEquals(new Instr.Copy("x", num(1)), new Instr.Copy("x", num(2)));
    }
}
