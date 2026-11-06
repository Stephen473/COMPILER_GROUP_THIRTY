package compiler.parser;

/** One syntax error: what is wrong and where. Like LexicalError, it is collected, not thrown. */
public record ParseError(String message, int line, int column) {

    @Override
    public String toString() {
        return "Syntax error at line " + line + ", col " + column + ": " + message;
    }
}
