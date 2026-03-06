package craftinginterpreters.lox;

// IMPORTANT: 使用异常实现返回机制
public class Return extends RuntimeException{
    final Object value;

    public Return(Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}
