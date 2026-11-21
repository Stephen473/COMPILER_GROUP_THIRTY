package compiler.ir;

/**
 * Something an instruction can read: a constant or a named location.
 * Booleans are stored as ints: false is 0 and true is 1.
 */
public sealed interface Operand {

    record Const(int value) implements Operand {
        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    /**
     * A variable or a temporary. Variables keep their source name ("sum"), or a numbered
     * name when an inner scope reuses the name ("x.2"). Temporaries are "%1", "%2", ...
     * Neither form can clash with a name written in an Eyra program.
     */
    record Name(String id) implements Operand {
        @Override
        public String toString() {
            return id;
        }
    }
}
