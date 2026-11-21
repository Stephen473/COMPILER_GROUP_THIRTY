package compiler.ir;

import java.util.List;

/** The whole program as a list of instructions. */
public record IrProgram(List<Instr> instructions) {}
