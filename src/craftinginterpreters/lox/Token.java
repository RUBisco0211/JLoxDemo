package craftinginterpreters.lox;

public class Token {
    final TokenType type;
    final String lexeme; // 存放标识符的名字
    final Object literal; // 存放字面量值
    final int line;

    public Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }
    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
