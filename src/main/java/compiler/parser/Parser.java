package compiler.parser;

import compiler.ast.Expr;
import compiler.ast.Program;
import compiler.ast.Stmt;
import compiler.ast.Type;
import compiler.scanner.Token;
import compiler.scanner.TokenType;

import java.util.ArrayList;
import java.util.List;

import static compiler.scanner.TokenType.*;

/**
 * Recursive-descent parser for Eyra (grammar in docs/LANGUAGE_SPEC.md, section 15).
 *
 * There is one method per grammar rule. The parser reads the token list from the
 * scanner and builds a {@link Program}. Syntax errors are collected in a list, and
 * the parser recovers and keeps going, so one run reports many errors.
 */
public class Parser {

    /** Thrown inside the parser to jump out of a broken statement. Never escapes parse(). */
    static class ParseException extends RuntimeException {
        private final ParseError error;

        ParseException(ParseError error) {
            super(error.message());
            this.error = error;
        }

        ParseError error() {
            return error;
        }
    }

    private final List<Token> tokens;
    private final List<ParseError> errors = new ArrayList<>();
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // ------------------------------------------------------------------ public API

    /** Parses the whole token list. Check {@link #hasErrors()} afterwards. */
    public Program parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            Stmt statement = statementOrNull();
            if (statement != null) {
                statements.add(statement);
            }
        }
        return new Program(statements);
    }

    public List<ParseError> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    // ------------------------------------------------------------------ statements

    /** Parses one statement; on a syntax error records it, skips ahead, and returns null. */
    private Stmt statementOrNull() {
        int start = current;
        try {
            return statement();
        } catch (ParseException e) {
            errors.add(e.error());
            if (current == start) {
                advance(); // always make progress, or a bad token would loop forever
            }
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        throw error(peek(), "Expected a statement, found " + describe(peek()));
    }

    // ------------------------------------------------------------------ expressions

    /** expression -> or */
    Expr expression() {
        throw error(peek(), "Expected an expression, found " + describe(peek())); // filled in by stage 5
    }

    // ------------------------------------------------------------------ errors and recovery

    /**
     * Panic-mode recovery: after an error, skip tokens until a place where a new
     * statement can safely begin.
     */
    private void synchronize() {
        while (!isAtEnd()) {
            if (previous().type() == SEMICOLON) {
                return; // just finished a statement
            }
            advance();
        }
    }

    private ParseException error(Token at, String message) {
        return new ParseException(new ParseError(message, at.line(), at.column()));
    }

    private String describe(Token token) {
        return token.type() == EOF ? "end of input" : "'" + token.lexeme() + "'";
    }

    // ------------------------------------------------------------------ token cursor

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private boolean isAtEnd() {
        return peek().type() == EOF;
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    /** Consumes the next token if it has one of the given types. */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }
}
