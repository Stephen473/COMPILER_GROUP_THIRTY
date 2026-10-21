package compiler.scanner;

/**
 * One problem found by the scanner. The scanner collects these instead of
 * stopping, so a single run can report every lexical mistake in a file.
 */
public record LexicalError(String message, int line, int column) {

    @Override
    public String toString() {
        return String.format("Lexical error at line %d, col %d: %s", line, column, message);
    }
}
