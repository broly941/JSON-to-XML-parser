public class Token {
    private Type type;
    private Type innerType;
    private String str;
    private boolean opened;

    public Token(Type type, String str) {
        this.type = type;
        this.str = str;

        opened = true;
        innerType = null;
    }

    public Token(Type type, String str, boolean opened) {
        this.type = type;
        this.str = str;

        this.opened = opened;
        innerType = null;
    }

    public Type getType() {
        return type;
    }

    public boolean isInnerType(Type innerType) {
        return this.innerType == innerType ? true : false;
    }

    public void setInnerType(Type innerType) {
        this.innerType = innerType;
    }

    public String getStr() {
        return str;
    }

    public boolean isOpen() {
        return opened;
    }

    public void Close() {
        this.opened = false;
    }
}

