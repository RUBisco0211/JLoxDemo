package craftinginterpreters.lox;

import java.util.List;

public class LoxClass implements LoxCallable {
    final String name;

    public LoxClass(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        return null;
    }

    @Override
    public int arity() {
        return 0;
    }
}
