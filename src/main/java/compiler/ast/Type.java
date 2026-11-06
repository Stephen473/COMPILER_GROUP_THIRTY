package compiler.ast;

/** The two types of Eyra. */
public enum Type {
    INT("int"),
    BOOL("bool");

    private final String keyword;

    Type(String keyword) {
        this.keyword = keyword;
    }

    /** The word used for this type in source code. */
    public String keyword() {
        return keyword;
    }
}
