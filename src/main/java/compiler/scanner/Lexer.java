package compiler.scanner;

import java.util.ArrayList;
import java.util.List;

import static compiler.scanner.TokenType.*;

/**
 * Phase 1: the Eyra scanner (lexical analyzer).
 *
 * Turns source text into a list of {@link Token}s. Whitespace and comments are
 * skipped. Problems are recorded as {@link LexicalError}s and scanning
 * continues, so one run reports every lexical mistake.
 *
 * Named Lexer to avoid confusion with java.util.Scanner.
 * Design: docs/SCANNER_DESIGN.md
 */
public class Lexer {

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private final List<LexicalError> errors = new ArrayList<>();

    private boolean scanned = false;
    private int pos = 0;      // index of the next unread character
    private int line = 1;
    private int column = 1;

    // Where the lexeme currently being scanned began
    private int startLine;
    private int startColumn;

    public Lexer(String source) {
        this.source = source;
    }

    /** Scans the whole input. The returned list always ends with EOF. */
    public List<Token> scanTokens() {
        if (scanned) {
            return tokens;
        }
        scanned = true;
        while (!isAtEnd()) {
            startLine = line;
            startColumn = column;
            scanToken();
        }
        tokens.add(new Token(EOF, "", line, column));
        return tokens;
    }

    /** Every lexical error found (valid after {@link #scanTokens()}). */
    public List<LexicalError> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    // ---- Main dispatch -----------------------------------------------------

    private void scanToken() {
        char c = advance();
        switch (c) {
            case ' ', '\t', '\r', '\n' -> { /* whitespace: advance() already tracked the line */ }
            case '(' -> add(LPAREN, "(");
            case ')' -> add(RPAREN, ")");
            case '{' -> add(LBRACE, "{");
            case '}' -> add(RBRACE, "}");
            case ';' -> add(SEMICOLON, ";");
            case '+' -> add(PLUS, "+");
            case '-' -> add(MINUS, "-");
            case '*' -> add(STAR, "*");
            case '%' -> add(PERCENT, "%");
            case '/' -> add(SLASH, "/");
            case '=' -> { if (match('=')) add(EQ, "=="); else add(ASSIGN, "="); }
            case '!' -> { if (match('=')) add(NEQ, "!="); else add(NOT, "!"); }
            case '<' -> { if (match('=')) add(LE, "<="); else add(LT, "<"); }
            case '>' -> { if (match('=')) add(GE, ">="); else add(GT, ">"); }
            case '&' -> {
                if (match('&')) add(AND, "&&");
                else report("Unexpected '&' (did you mean '&&'?)");
            }
            case '|' -> {
                if (match('|')) add(OR, "||");
                else report("Unexpected '|' (did you mean '||'?)");
            }
            default -> report("Unexpected character " + describe(c));
        }
    }

    // ---- Low-level helpers -------------------------------------------------

    private boolean isAtEnd() {
        return pos >= source.length();
    }

    private char advance() {
        char c = source.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(pos) != expected) {
            return false;
        }
        advance();
        return true;
    }

    /** Printable ASCII shows as 'c'; anything else shows as U+XXXX. */
    private static String describe(char c) {
        return (c >= 32 && c < 127) ? "'" + c + "'" : String.format("U+%04X", (int) c);
    }

    private void add(TokenType type, String lexeme) {
        tokens.add(new Token(type, lexeme, startLine, startColumn));
    }

    private void report(String message) {
        errors.add(new LexicalError(message, startLine, startColumn));
    }
}
