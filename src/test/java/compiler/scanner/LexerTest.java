package compiler.scanner;

import org.junit.jupiter.api.Test;

import java.util.List;

import static compiler.scanner.TokenType.*;
import static org.junit.jupiter.api.Assertions.*;

class LexerTest {

    private static List<Token> scan(String src) {
        return new Lexer(src).scanTokens();
    }

    private static List<TokenType> types(String src) {
        return scan(src).stream().map(Token::type).toList();
    }

    private static List<LexicalError> errorsOf(String src) {
        Lexer lexer = new Lexer(src);
        lexer.scanTokens();
        return lexer.getErrors();
    }

    // ---- Stage 1: skeleton -------------------------------------------

    @Test
    void emptyInputGivesOnlyEof() {
        assertEquals(List.of(EOF), types(""));
    }

    @Test
    void whitespaceProducesNoTokens() {
        assertEquals(List.of(EOF), types(" \t\r\n  "));
    }

    @Test
    void eofPositionCountsLinesAndColumns() {
        Token eof = scan("\n\n  ").get(0);
        assertEquals(EOF, eof.type());
        assertEquals(3, eof.line());
        assertEquals(3, eof.column());
    }

    @Test
    void unknownCharacterIsReportedWithPosition() {
        List<LexicalError> errs = errorsOf("  @");
        assertEquals(1, errs.size());
        assertEquals(1, errs.get(0).line());
        assertEquals(3, errs.get(0).column());
        assertTrue(errs.get(0).message().contains("'@'"));
    }

    @Test
    void scanningContinuesAfterAnError() {
        Lexer lexer = new Lexer("@ #");
        List<Token> t = lexer.scanTokens();
        assertEquals(2, lexer.getErrors().size());
        assertEquals(EOF, t.get(t.size() - 1).type());
    }

    @Test
    void cleanInputHasNoErrors() {
        Lexer lexer = new Lexer("  \n ");
        lexer.scanTokens();
        assertFalse(lexer.hasErrors());
    }

    // ---- Stage 2: punctuation and single-character operators ---------

    @Test
    void punctuation() {
        assertEquals(List.of(SEMICOLON, LPAREN, RPAREN, LBRACE, RBRACE, EOF), types("; ( ) { }"));
    }

    @Test
    void singleCharacterOperators() {
        assertEquals(List.of(PLUS, MINUS, STAR, PERCENT, EOF), types("+ - * %"));
    }

    @Test
    void symbolsWithoutSpacesStillSplit() {
        assertEquals(List.of(LPAREN, PLUS, RPAREN, SEMICOLON, EOF), types("(+);"));
    }

    @Test
    void tokenPositionsAreRecorded() {
        Token plus = scan("(\n +").get(1);
        assertEquals("+", plus.lexeme());
        assertEquals(2, plus.line());
        assertEquals(2, plus.column());
    }

    // ---- Stage 3: multi-character operators --------------------------

    @Test
    void arithmeticOperators() {
        assertEquals(List.of(PLUS, MINUS, STAR, SLASH, PERCENT, EOF), types("+ - * / %"));
    }

    @Test
    void longestMatchOperators() {
        assertEquals(
                List.of(EQ, NEQ, LE, GE, LT, GT, ASSIGN, AND, OR, NOT, EOF),
                types("== != <= >= < > = && || !"));
    }

    @Test
    void adjacentOperatorsSplitGreedily() {
        assertEquals(List.of(EQ, ASSIGN, EOF), types("==="));
        assertEquals(List.of(LE, GT, EOF), types("<=>"));
        assertEquals(List.of(NEQ, ASSIGN, EOF), types("!=="));
    }

    @Test
    void loneAmpersandAndBarAreErrors() {
        List<LexicalError> amp = errorsOf("&");
        assertEquals(1, amp.size());
        assertTrue(amp.get(0).message().contains("&&"));
        List<LexicalError> bar = errorsOf("|");
        assertEquals(1, bar.size());
        assertTrue(bar.get(0).message().contains("||"));
    }
}
