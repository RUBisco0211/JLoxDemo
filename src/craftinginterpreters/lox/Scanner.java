package craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import static craftinginterpreters.lox.TokenType.*;

/**
 * 扫描器 Scanner 或 词法分析器 Lexer
 */
public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    // start 用于记录 token 起始位置
    // current 为当前扫描位置
    // line 为行号
    private int start = 0, current = 0, line = 1;

    // 关键字对应的 token 类型
    private static final Map<String, TokenType> keywords = new HashMap<>();
    static {
        keywords.put("var", VAR);
        keywords.put("and", AND);
        keywords.put("or", OR);
        keywords.put("if", IF);
        keywords.put("else", ELSE);
        keywords.put("nil", NIL);
        keywords.put("true", TRUE);
        keywords.put("false", FALSE);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("while", WHILE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("class", CLASS);
        keywords.put("this", THIS);
        keywords.put("super", SUPER);
    }

    public Scanner(String source) {
        this.source = source;
    }

    /**
     * 扫描获取 token 列表
     */
    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            // start 始终位于某一 token 的第一个字符位置
            start = current;
            scanToken();
        }
        // 末尾添加文件结束符
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }
    private void scanToken() {
        char c = advance();
        // 此时 current 指向 c 后一位
        switch (c) {
            // 处理单字符token
            case '(' :addToken(LEFT_PAREN); break;
            case ')' :addToken(RIGHT_PAREN); break;
            case '{' :addToken(LEFT_BRACE); break;
            case '}' :addToken(RIGHT_BRACE); break;
            case ',' :addToken(COMMA); break;
            case '.' :addToken(DOT); break;
            case '-' :addToken(MINUS); break;
            case '+' :addToken(PLUS); break;
            case ';' :addToken(SEMICOLON); break;
            case '*' :addToken(STAR); break;
            // 左斜杠 / 可能为 SLASH token，也可能是双斜杠 // 注释
            case '/' :
                if (match('/')) {
                    // 双斜杠为注释
                    while (peek() != '\n' && !isAtEnd())
                        advance();
                    // 注释（包括双斜杠部分）不作为token
                } else {
                    addToken(SLASH);
                }
                break;

            // 处理双字符token
            case '!' :addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=' :addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<' :addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '>' :addToken(match('=') ? GREATER_EQUAL : GREATER); break;

            // 其他特定字符
            case ' ' :
            case '\r' :
            case '\t' :break; // 跳过
            case '\n' :line++; break; // 换行

            // 处理字符串字面量
            case '"' :string(); break;

            default:
                // 处理数字
                if (isDigit(c)) {
                    number();
                    break;
                }
                // 处理标识符或关键字 （标识符不能以数字开头）
                if (isAlpha(c)) {
                    identifier();
                    break;
                }
                Lox.error(line, "Unexpected character"); break;
        }
    }
    private boolean isAtEnd() {
        return current >= source.length();
    }

    /**
     * 返回当前字符并使 current 后移一位
     * current 始终指向返回的字符后一位，便于使用 substring 方法截取
     */
    private char advance() {
        return source.charAt(current++);
    }
    private void addToken(TokenType type) {
        addToken(type, null);
    }
    private void addToken(TokenType type, Object literal) {
        // 截取token值 lexeme
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    /**
     * 检查字符是否匹配，辅助处理双字符token
     * @param expected 期望匹配的字符
     */
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (peek() != expected) return false;

        // 匹配成功的情况下后移一位
        advance();
        return true;
    }

    /**
     * 获取 current 处字符
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    /**
     * 处理字符串字面量
     */
    private void string(){
        // current 处字符不为 " ，即字符串字面量还未结束
        while (peek() != '"' && !isAtEnd()) {
            // 支持多行字符串
            if (peek() == '\n') line++;
            advance();
        }
        if (isAtEnd()) {
            Lox.error(line, "Unexpected string");
            return;
        }
        // current 指向 "，字符串字面量结束，后移一位指向下一 token 开始位置
        advance();
        // start 为 ", current-1 为 "
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * 处理数字字面量
     */
    private void number() {
        // 注：带负号 - 的数字不被看作数字字面量
        while (isDigit(peek())) advance();
        // 遇到小数点
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) advance();
        }
        // 动态类型 所有数值都是 double 类型
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    /**
     * 获取 current 后一位字符
     */
    private char peekNext() {
        return current + 1 >= source.length() ? '\0' : source.charAt(current + 1);
    }

    /**
     * 处理标识符和关键字
     */
    private void identifier() {
        while (isAlphaOrNumeric(peek())) advance();
        // 最大匹配原则，优先检查是否为关键字
        String text = source.substring(start, current);
        TokenType type = keywords.getOrDefault(text, IDENTIFIER);
        addToken(type);
    }
    /**
     * 是否是字母（包括下划线）
     */
    private boolean isAlpha(char c) {
        // 大小写字母和下划线
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c == '_');
    }
    /**
     * 是否是字母、下划线或数字
     */
    private boolean isAlphaOrNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
}
