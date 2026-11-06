package compiler.ast;

import java.util.List;

/** A statement node. Every statement remembers where it starts in the source. */
public sealed interface Stmt {

    int line();

    int column();

    /** {@code [const] type name [= initializer];} The initializer is null when omitted. */
    record VarDecl(Type type, String name, Expr initializer, boolean isConst, int line, int column)
            implements Stmt {}

    /** {@code name = value;} */
    record Assign(String name, Expr value, int line, int column) implements Stmt {}

    /** {@code print(value);} */
    record Print(Expr value, int line, int column) implements Stmt {}

    /**
     * {@code if (condition) { ... } [else ...]}.
     * {@code elseBranch} is null, a Block, or another If (an "else if").
     */
    record If(Expr condition, Block thenBranch, Stmt elseBranch, int line, int column) implements Stmt {}

    /** {@code while (condition) { ... }} */
    record While(Expr condition, Block body, int line, int column) implements Stmt {}

    record Break(int line, int column) implements Stmt {}

    record Continue(int line, int column) implements Stmt {}

    /** {@code { statements }} */
    record Block(List<Stmt> statements, int line, int column) implements Stmt {}
}
