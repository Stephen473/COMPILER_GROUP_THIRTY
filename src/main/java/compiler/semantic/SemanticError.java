package compiler.semantic;

/** One semantic error: what is wrong and where. Collected, not thrown, like the other phases' errors. */
public record SemanticError(String message, int line, int column) {

    @Override
    public String toString() {
        return "Semantic error at line " + line + ", col " + column + ": " + message;
    }
}
