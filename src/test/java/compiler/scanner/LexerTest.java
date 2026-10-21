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
}
