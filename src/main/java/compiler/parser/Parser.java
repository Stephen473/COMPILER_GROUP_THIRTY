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
        if (check(KW_INT) || check(KW_BOOL) || check(KW_CONST)) {
            return declaration();
        }
        if (check(KW_PRINT)) {
            return printStatement();
        }
        if (check(IDENTIFIER)) {
            return assignment();
        }
        if (check(KW_IF)) {
            return ifStatement();
        }
        if (check(KW_WHILE)) {
            return whileStatement();
        }
        if (check(KW_BREAK) || check(KW_CONTINUE)) {
            return jumpStatement();
        }
        if (check(LBRACE)) {
            return block();
        }
        if (check(RBRACE)) {
            throw error(peek(), "Unmatched '}': there is no open '{' for it");
        }
        if (check(KW_ELSE)) {
            throw error(peek(), "'else' without a matching 'if'");
        }
        throw error(peek(), "Expected a statement, found " + describe(peek()));
    }

    /** declaration -> ["const"] type IDENTIFIER ["=" expression] ";" */
    private Stmt declaration() {
        Token first = peek();
        boolean isConst = match(KW_CONST);
        if (!match(KW_INT, KW_BOOL)) {
            throw error(peek(), "Expected 'int' or 'bool' after 'const'");
        }
        Token typeToken = previous();
        Type type = typeToken.type() == KW_INT ? Type.INT : Type.BOOL;
        Token name = consume(IDENTIFIER, "Expected a variable name after '" + typeToken.lexeme() + "'");

        Expr initializer = null;
        if (match(ASSIGN)) {
            initializer = expression();
        } else if (isConst) {
            // Not fatal: record the error, keep parsing, so the semicolon is still consumed.
            errors.add(new ParseError(
                    "A const declaration must have an initializer: write 'const " + type.keyword()
                            + " " + name.lexeme() + " = value;'",
                    name.line(), name.column()));
        }
        expectSemicolon("variable declaration");
        return new Stmt.VarDecl(type, name.lexeme(), initializer, isConst, first.line(), first.column());
    }

    /** assignment -> IDENTIFIER "=" expression ";" */
    private Stmt assignment() {
        Token name = advance();
        consume(ASSIGN, "Expected '=' after '" + name.lexeme() + "'");
        Expr value = expression();
        expectSemicolon("assignment");
        return new Stmt.Assign(name.lexeme(), value, name.line(), name.column());
    }

    /** printStmt -> "print" "(" expression ")" ";" */
    private Stmt printStatement() {
        Token keyword = advance();
        consume(LPAREN, "Expected '(' after 'print'");
        Expr value = expression();
        consume(RPAREN, "Expected ')' after the expression");
        expectSemicolon("print statement");
        return new Stmt.Print(value, keyword.line(), keyword.column());
    }

    /** ifStmt -> "if" "(" expression ")" block ["else" (ifStmt | block)] */
    private Stmt.If ifStatement() {
        Token keyword = advance();
        consume(LPAREN, "Expected '(' after 'if'");
        Expr condition = expression();
        consume(RPAREN, "Expected ')' after the condition");
        if (!check(LBRACE)) {
            throw error(peek(), "Expected '{' to start the 'if' body");
        }
        Stmt.Block thenBranch = block();

        Stmt elseBranch = null;
        if (match(KW_ELSE)) {
            if (check(KW_IF)) {
                elseBranch = ifStatement();
            } else if (check(LBRACE)) {
                elseBranch = block();
            } else {
                throw error(peek(), "Expected '{' or 'if' after 'else'");
            }
        }
        return new Stmt.If(condition, thenBranch, elseBranch, keyword.line(), keyword.column());
    }

    /** whileStmt -> "while" "(" expression ")" block */
    private Stmt whileStatement() {
        Token keyword = advance();
        consume(LPAREN, "Expected '(' after 'while'");
        Expr condition = expression();
        consume(RPAREN, "Expected ')' after the condition");
        if (!check(LBRACE)) {
            throw error(peek(), "Expected '{' to start the 'while' body");
        }
        Stmt.Block body = block();
        return new Stmt.While(condition, body, keyword.line(), keyword.column());
    }

    /** "break" ";" | "continue" ";" */
    private Stmt jumpStatement() {
        Token keyword = advance();
        expectSemicolon("'" + keyword.lexeme() + "'");
        if (keyword.type() == KW_BREAK) {
            return new Stmt.Break(keyword.line(), keyword.column());
        }
        return new Stmt.Continue(keyword.line(), keyword.column());
    }

    /** block -> "{" statement* "}" */
    private Stmt.Block block() {
        Token open = advance(); // the '{'
        List<Stmt> statements = new ArrayList<>();
        while (!check(RBRACE) && !isAtEnd()) {
            Stmt statement = statementOrNull();
            if (statement != null) {
                statements.add(statement);
            }
        }
        if (isAtEnd()) {
            // Reported at the '{' that was never closed: that is where the programmer must look.
            errors.add(new ParseError("Unclosed '{': this block is never closed with '}'",
                    open.line(), open.column()));
        } else {
            advance(); // the '}'
        }
        return new Stmt.Block(statements, open.line(), open.column());
    }

    // ------------------------------------------------------------------ expressions

    /** expression -> or */
    Expr expression() {
        return or();
    }

    /** or -> and ("||" and)* */
    private Expr or() {
        Expr left = and();
        while (match(OR)) {
            Token op = previous();
            Expr right = and();
            left = new Expr.Binary(left, op.lexeme(), right, op.line(), op.column());
        }
        return left;
    }

    /** and -> equality ("&&" equality)* */
    private Expr and() {
        Expr left = equality();
        while (match(AND)) {
            Token op = previous();
            Expr right = equality();
            left = new Expr.Binary(left, op.lexeme(), right, op.line(), op.column());
        }
        return left;
    }

    /** equality -> comparison (("==" | "!=") comparison)* */
    private Expr equality() {
        Expr left = comparison();
        while (match(EQ, NEQ)) {
            Token op = previous();
            Expr right = comparison();
            left = new Expr.Binary(left, op.lexeme(), right, op.line(), op.column());
        }
        return left;
    }

    /** comparison -> term (("<" | "<=" | ">" | ">=") term)* */
    private Expr comparison() {
        Expr left = term();
        while (match(LT, LE, GT, GE)) {
            Token op = previous();
            Expr right = term();
            left = new Expr.Binary(left, op.lexeme(), right, op.line(), op.column());
        }
        return left;
    }

    /** term -> factor (("+" | "-") factor)* */
    private Expr term() {
        Expr left = factor();
        while (match(PLUS, MINUS)) {
            Token op = previous();
            Expr right = factor();
            left = new Expr.Binary(left, op.lexeme(), right, op.line(), op.column());
        }
        return left;
    }

    /** factor -> unary (("*" | "/" | "%") unary)* */
    private Expr factor() {
        Expr left = unary();
        while (match(STAR, SLASH, PERCENT)) {
            Token op = previous();
            Expr right = unary();
            left = new Expr.Binary(left, op.lexeme(), right, op.line(), op.column());
        }
        return left;
    }

    /** unary -> ("-" | "!") unary | primary */
    private Expr unary() {
        if (match(MINUS, NOT)) {
            Token op = previous();
            Expr operand = unary();
            return new Expr.Unary(op.lexeme(), operand, op.line(), op.column());
        }
        return primary();
    }

    /** primary -> INT_LITERAL | BOOL_LITERAL | IDENTIFIER | "(" expression ")" */
    private Expr primary() {
        if (match(INT_LITERAL)) {
            Token t = previous();
            // The scanner already rejected numbers that are too large, so this cannot overflow.
            return new Expr.IntLiteral(Integer.parseInt(t.lexeme()), t.line(), t.column());
        }
        if (match(BOOL_LITERAL)) {
            Token t = previous();
            return new Expr.BoolLiteral(t.lexeme().equals("true"), t.line(), t.column());
        }
        if (match(IDENTIFIER)) {
            Token t = previous();
            return new Expr.Variable(t.lexeme(), t.line(), t.column());
        }
        if (match(LPAREN)) {
            Expr inner = expression();
            consume(RPAREN, "Expected ')' after the expression");
            return inner; // no node for parentheses: the tree shape already shows the grouping
        }
        throw error(peek(), "Expected an expression, found " + describe(peek()));
    }

    // ------------------------------------------------------------------ errors and recovery

    /** Expects a ';'. */
    private void expectSemicolon(String what) {
        if (match(SEMICOLON)) {
            return;
        }
        // "Missing ;" is best pointed at the end of the previous token, not at the next line.
        throw errorAfter(previous(), "Expected ';' after " + what);
    }

    /**
     * Panic-mode recovery: after an error, skip tokens until a place where a new
     * statement can safely begin.
     */
    private void synchronize() {
        while (!isAtEnd()) {
            if (previous().type() == SEMICOLON) {
                return; // just finished a statement
            }
            switch (peek().type()) {
                case KW_INT, KW_BOOL, KW_CONST, KW_IF, KW_WHILE, KW_PRINT,
                     KW_BREAK, KW_CONTINUE, LBRACE, RBRACE:
                    return; // the next token starts a statement, or closes the current block
                default:
                    break;
            }
            advance();
        }
    }

    private ParseException error(Token at, String message) {
        return new ParseException(new ParseError(message, at.line(), at.column()));
    }

    /** An error placed just after the given token. */
    private ParseException errorAfter(Token token, String message) {
        return new ParseException(
                new ParseError(message, token.line(), token.column() + token.lexeme().length()));
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
