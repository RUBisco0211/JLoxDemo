package craftinginterpreters.lox;

import java.util.List;

public interface LoxCallable {
    // 调用方法
    Object call(Interpreter interpreter, List<Object> arguments);
    // 参数数量
    int arity();
}
