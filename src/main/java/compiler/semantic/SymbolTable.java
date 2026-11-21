package compiler.semantic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * The symbol table: a stack of scopes. Each block pushes a new scope and pops it
 * at the end. Looking a name up searches from the innermost scope outwards, which
 * is what makes an inner declaration shadow an outer one.
 */
public class SymbolTable {

    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();

    public SymbolTable() {
        enterScope(); // the global scope
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        if (scopes.size() == 1) {
            throw new IllegalStateException("Cannot leave the global scope");
        }
        scopes.pop();
    }

    /** How many scopes are open. The global scope counts as 1. */
    public int depth() {
        return scopes.size();
    }

    /** Declares a name in the innermost scope. Returns false if it is already declared there. */
    public boolean declare(Symbol symbol) {
        Map<String, Symbol> innermost = scopes.peek();
        if (innermost.containsKey(symbol.name())) {
            return false;
        }
        innermost.put(symbol.name(), symbol);
        return true;
    }

    /** Finds a name, innermost scope first. Returns null if it is not declared anywhere. */
    public Symbol lookup(String name) {
        for (Map<String, Symbol> scope : scopes) { // ArrayDeque iterates from the top of the stack
            Symbol found = scope.get(name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** Finds a name in the innermost scope only. Used to detect duplicate declarations. */
    public Symbol lookupInCurrentScope(String name) {
        return scopes.peek().get(name);
    }
}
