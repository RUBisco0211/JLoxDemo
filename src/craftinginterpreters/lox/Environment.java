package craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    // IMPORTANT 直接外部作用域的环境
    final Environment enclosing;

    // 无参构造函数用于构造全局作用域环境（环境链的结尾）
    Environment() {
        enclosing = null;
    }
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    // 变量表
    private final Map<String, Object> values = new HashMap<>();
    void define(String name, Object value) {
        // IMPORTANT 允许变量重新定义 (便于REPL交互模式下使用)
        values.put(name, value);
    }
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }
        // 本作用域环境中没有该变量时，依次查找其直接外部作用域的环境
        if (enclosing != null) {
            return enclosing.get(name);
        }
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }
        // 本作用域环境中没有该变量时，依次查找其直接外部作用域的环境
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }
        throw new RuntimeError(name,"Undefined variable '" + name.lexeme + "'.");
    }

    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }

    // 明确知道外层中哪个环境中包含变量
    Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }
        return environment;
    }
}
