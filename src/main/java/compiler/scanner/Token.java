package compiler.scanner;

/**
 * A single lexical token: its type, the exact text matched, and where it began.
 */
public record Token(TokenType type, String lexeme, int line, int column) {

    @Override
    public String toString() {
        return String.format("%-15s %-20s (line %d, col %d)",
                type, "'" + lexeme + "'", line, column);
    }
}
