package craftinginterpreters.lox;

public enum TokenType {

    // 单字符token
    LEFT_PAREN, // 左括号 (
    RIGHT_PAREN, // 右括号 )
    LEFT_BRACE, // 左花括号 {
    RIGHT_BRACE, // 右花括号 }
    COMMA, // 逗号 ,
    DOT, // 句点 .
    MINUS, // 减号 -
    PLUS, // 加号 +
    SEMICOLON, // 分号 ;
    SLASH, // 左斜杠 /
    STAR, // 星号 *

    // 单或双字符token
    BANG, // 叹号 !
    BANG_EQUAL, // 不等于 !=
    EQUAL, // 等于 =
    EQUAL_EQUAL, // 等于等于 ==
    GREATER, // 大于 >
    GREATER_EQUAL, // 大于等于 >=
    LESS, // 小于 <
    LESS_EQUAL, // 小于等于 <=

    // 字面量token
    IDENTIFIER, // 标识符
    STRING, // 字符串
    NUMBER, // 数字

    // 关键字token
    AND,
    OR,
    CLASS,
    IF,
    ELSE,
    TRUE,
    FALSE,
    FUN, // 函数定义
    FOR,
    NIL, // null
    PRINT,
    RETURN,
    SUPER,
    THIS,
    VAR, // 变量定义
    WHILE,

    // 文件结束符
    EOF,
}
