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

    // ---- Stage 4: comments -------------------------------------------

    @Test
    void lineCommentIsSkipped() {
        assertEquals(List.of(PLUS, MINUS, EOF), types("+ // ignored ( ) \n-"));
    }

    @Test
    void lineCommentAtEndOfFileWithoutNewline() {
        assertEquals(List.of(EOF), types("// nothing else"));
    }

    @Test
    void blockCommentIsSkippedAndLinesStillCount() {
        List<Token> t = scan("/* one\n two\n three */ +");
        assertEquals(PLUS, t.get(0).type());
        assertEquals(3, t.get(0).line());
    }

    @Test
    void slashAloneIsStillDivision() {
        assertEquals(List.of(SLASH, SLASH, EOF), types("/ /"));
    }

    @Test
    void blockCommentsDoNotNest() {
        assertEquals(List.of(PLUS, EOF), types("/* a /* b */ +"));
    }

    @Test
    void unterminatedBlockCommentPointsAtItsStart() {
        List<LexicalError> errs = errorsOf("+\n  /* never closed");
        assertEquals(1, errs.size());
        assertEquals(2, errs.get(0).line());
        assertEquals(3, errs.get(0).column());
    }

    // ---- Stage 5: identifiers and keywords ---------------------------

    @Test
    void allNineKeywords() {
        assertEquals(
                List.of(KW_INT, KW_BOOL, KW_CONST, KW_IF, KW_ELSE, KW_WHILE,
                        KW_BREAK, KW_CONTINUE, KW_PRINT, EOF),
                types("int bool const if else while break continue print"));
    }

    @Test
    void keywordsAreCaseSensitive() {
        assertEquals(List.of(IDENTIFIER, IDENTIFIER, IDENTIFIER, EOF), types("INT Print While"));
    }

    @Test
    void keywordPrefixIsStillAnIdentifier() {
        assertEquals(List.of(KW_INT, IDENTIFIER, IDENTIFIER, EOF), types("int integer printer"));
    }

    @Test
    void identifiersWithUnderscoresAndDigits() {
        List<Token> t = scan("_count student_name score2");
        assertEquals(List.of(IDENTIFIER, IDENTIFIER, IDENTIFIER, EOF),
                t.stream().map(Token::type).toList());
        assertEquals("student_name", t.get(1).lexeme());
    }

    @Test
    void boolLiteralsKeepTheirText() {
        List<Token> t = scan("true false");
        assertEquals(BOOL_LITERAL, t.get(0).type());
        assertEquals("true", t.get(0).lexeme());
        assertEquals(BOOL_LITERAL, t.get(1).type());
        assertEquals("false", t.get(1).lexeme());
    }

    @Test
    void identifierPositionsAreTracked() {
        Token y = scan("int x;\n  y = z;").get(3);
        assertEquals("y", y.lexeme());
        assertEquals(2, y.line());
        assertEquals(3, y.column());
    }

    @Test
    void commentsMixedWithCode() {
        assertEquals(List.of(KW_INT, IDENTIFIER, SEMICOLON, EOF),
                types("// note\nint /* inline */ x; // trailing"));
    }

    @Test
    void wordsAreSplitByOperators() {
        assertEquals(List.of(IDENTIFIER, PLUS, IDENTIFIER, EOF), types("a+b"));
    }
}
