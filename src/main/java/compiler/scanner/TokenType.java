package compiler.scanner;

/**
 * Every kind of token the Eyra scanner can produce.
 * See docs/TOKEN_LIST.md for the full table.
 */
public enum TokenType {
    // Keywords
    KW_INT, KW_BOOL, KW_CONST, KW_IF, KW_ELSE, KW_WHILE,
    KW_BREAK, KW_CONTINUE, KW_PRINT,

    // Names and literals
    IDENTIFIER, INT_LITERAL, BOOL_LITERAL,

    // Arithmetic operators
    PLUS, MINUS, STAR, SLASH, PERCENT,

    // Assignment and comparison
    ASSIGN, EQ, NEQ, LT, LE, GT, GE,

    // Logical operators
    AND, OR, NOT,

    // Punctuation
    SEMICOLON, LPAREN, RPAREN, LBRACE, RBRACE,

    // End of input
    EOF
}
