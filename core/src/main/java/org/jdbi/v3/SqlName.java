package org.jdbi.v3;

public class SqlName {
    private final Object name;
    private final Object[] args;

    SqlName(Object name, Object... args) {
        this.name = name;
        this.args = args;
    }

    public Object getName() {
        return name;
    }

    public Object[] getArgs() {
        return args;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        for (Object arg : args) {
            sb.append(" ");
            sb.append(arg);
        }
        return sb.toString();
    }
}
