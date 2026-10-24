package compiler.scanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private static final int MAX_INT = Integer.MAX_VALUE;

    private static final Map<String, TokenType> WORDS = Map.ofEntries(
            Map.entry("int", KW_INT),
            Map.entry("bool", KW_BOOL),
            Map.entry("const", KW_CONST),
            Map.entry("if", KW_IF),
            Map.entry("else", KW_ELSE),
            Map.entry("while", KW_WHILE),
            Map.entry("break", KW_BREAK),
            Map.entry("continue", KW_CONTINUE),
            Map.entry("print", KW_PRINT),
            Map.entry("true", BOOL_LITERAL),
            Map.entry("false", BOOL_LITERAL));

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
            case '/' -> {
                if (match('/')) {
                    skipLineComment();
                } else if (match('*')) {
                    skipBlockComment();
                } else {
                    add(SLASH, "/");
                }
            }
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
            default -> {
                if (isDigit(c)) {
                    scanNumber(c);
                } else if (isWordStart(c)) {
                    scanWord(c);
                } else {
                    report("Unexpected character " + describe(c));
                }
            }
        }
    }

    // ---- Words and numbers -------------------------------------------------

    private void scanWord(char first) {
        StringBuilder sb = new StringBuilder().append(first);
        while (!isAtEnd() && isWordPart(peek())) {
            sb.append(advance());
        }
        String text = sb.toString();
        add(WORDS.getOrDefault(text, IDENTIFIER), text);
    }

    private void scanNumber(char first) {
        StringBuilder sb = new StringBuilder().append(first);
        while (!isAtEnd() && isDigit(peek())) {
            sb.append(advance());
        }

        // A letter or underscore right after digits, e.g. 2age: take the whole word
        if (!isAtEnd() && isWordStart(peek())) {
            while (!isAtEnd() && isWordPart(peek())) {
                sb.append(advance());
            }
            report("Invalid token '" + sb + "': identifiers cannot start with a digit");
            return;
        }

        String text = sb.toString();
        if (text.length() > 1 && text.charAt(0) == '0') {
            report("Leading zeros are not allowed in '" + text + "'");
            return;
        }
        try {
            Integer.parseInt(text);
        } catch (NumberFormatException e) {
            report("Integer literal '" + text + "' is too large (maximum " + MAX_INT + ")");
            return;
        }
        add(INT_LITERAL, text);
    }

    // ---- Comments ----------------------------------------------------------

    private void skipLineComment() {
        while (!isAtEnd() && peek() != '\n') {
            advance();
        }
    }

    private void skipBlockComment() {
        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                advance();
                advance();
                return;
            }
            advance();
        }
        // Reported at the position where the comment started
        report("Unterminated block comment");
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

    private char peek() {
        return isAtEnd() ? '\0' : source.charAt(pos);
    }

    private char peekNext() {
        return pos + 1 >= source.length() ? '\0' : source.charAt(pos + 1);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isWordStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private static boolean isWordPart(char c) {
        return isWordStart(c) || isDigit(c);
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
