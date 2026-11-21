package compiler.semantic;

import compiler.ast.Type;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SymbolTableTest {

    private static Symbol sym(String name, Type type) {
        return new Symbol(name, type, false, 1, 1);
    }

    @Test
    void startsWithOneGlobalScope() {
        assertEquals(1, new SymbolTable().depth());
    }

    @Test
    void declaredNameCanBeFound() {
        SymbolTable t = new SymbolTable();
        assertTrue(t.declare(sym("x", Type.INT)));
        assertEquals(Type.INT, t.lookup("x").type());
    }

    @Test
    void unknownNameIsNull() {
        assertNull(new SymbolTable().lookup("nothing"));
    }

    @Test
    void sameNameTwiceInOneScopeIsRejected() {
        SymbolTable t = new SymbolTable();
        assertTrue(t.declare(sym("x", Type.INT)));
        assertFalse(t.declare(sym("x", Type.BOOL)));
        assertEquals(Type.INT, t.lookup("x").type()); // the first one is kept
    }

    @Test
    void innerScopeCanSeeOuterNames() {
        SymbolTable t = new SymbolTable();
        t.declare(sym("x", Type.INT));
        t.enterScope();
        assertEquals(Type.INT, t.lookup("x").type());
        assertEquals(2, t.depth());
    }

    @Test
    void innerNameShadowsOuterName() {
        SymbolTable t = new SymbolTable();
        t.declare(sym("x", Type.INT));
        t.enterScope();
        assertTrue(t.declare(sym("x", Type.BOOL)));
        assertEquals(Type.BOOL, t.lookup("x").type());
    }

    @Test
    void outerNameReturnsWhenTheScopeEnds() {
        SymbolTable t = new SymbolTable();
        t.declare(sym("x", Type.INT));
        t.enterScope();
        t.declare(sym("x", Type.BOOL));
        t.exitScope();
        assertEquals(Type.INT, t.lookup("x").type());
    }

    @Test
    void innerNamesDisappearWhenTheScopeEnds() {
        SymbolTable t = new SymbolTable();
        t.enterScope();
        t.declare(sym("temp", Type.INT));
        t.exitScope();
        assertNull(t.lookup("temp"));
    }

    @Test
    void lookupInCurrentScopeIgnoresOuterScopes() {
        SymbolTable t = new SymbolTable();
        t.declare(sym("x", Type.INT));
        t.enterScope();
        assertNull(t.lookupInCurrentScope("x"));
        assertNotNull(t.lookup("x"));
    }

    @Test
    void theGlobalScopeCannotBeLeft() {
        SymbolTable t = new SymbolTable();
        assertThrows(IllegalStateException.class, t::exitScope);
    }

    @Test
    void symbolKeepsConstAndPosition() {
        Symbol s = new Symbol("LIMIT", Type.INT, true, 3, 7);
        assertTrue(s.isConst());
        assertEquals(3, s.line());
        assertEquals(7, s.column());
    }

    @Test
    void errorToStringIsReadable() {
        assertEquals("Semantic error at line 2, col 5: Undeclared variable 'y'",
                new SemanticError("Undeclared variable 'y'", 2, 5).toString());
    }
}
