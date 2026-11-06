package compiler.ast;

import java.util.List;

/** The root of the syntax tree: the whole program is a list of statements. */
public record Program(List<Stmt> statements) {}
