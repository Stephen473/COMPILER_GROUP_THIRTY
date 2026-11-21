package compiler.semantic;

import compiler.ast.Type;

/** A declared name: its type, whether it is const, and where it was declared. */
public record Symbol(String name, Type type, boolean isConst, int line, int column) {}
