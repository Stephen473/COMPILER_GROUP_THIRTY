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
        assertEquals(Type.INT, t.lookup("x").type());
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
