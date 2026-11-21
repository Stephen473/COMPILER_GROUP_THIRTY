package compiler.ir;

import compiler.ast.Type;

/**
 * The intermediate representation: a flat list of simple instructions ("three-address code").
 * Each instruction does one small thing, and control flow is made of labels and jumps.
 * This is easy for the code generator (Phase 4) to translate and for the optimizer (Phase 5) to improve.
 */
public sealed interface Instr {

    /** dest = src */
    record Copy(String dest, Operand src) implements Instr {}

    /** dest = left op right, where op is + - * / % &lt; &lt;= &gt; &gt;= == != */
    record Binary(String dest, String op, Operand left, Operand right) implements Instr {}

    /** dest = op operand, where op is - or ! */
    record Unary(String dest, String op, Operand operand) implements Instr {}

    /** A position in the code that a jump can go to. */
    record Label(String name) implements Instr {}

    /** Continue at the label. */
    record Jump(String target) implements Instr {}

    /** Continue at the label if the condition is 0 (false). Otherwise carry on with the next instruction. */
    record JumpIfFalse(Operand condition, String target) implements Instr {}

    /** Print a value. The type is kept so a bool prints as true/false and an int as a number. */
    record Print(Operand value, Type type) implements Instr {}
}
