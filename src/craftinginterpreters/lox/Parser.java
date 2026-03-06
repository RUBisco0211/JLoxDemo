package craftinginterpreters.lox;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static craftinginterpreters.lox.TokenType.*;

/**
 * 解析器 把 token 列表转换为表达式 expression
 */
public class Parser {
    private static class ParseError extends RuntimeException{ }
    private final List<Token> tokens;
    // current 指向当前token
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }
    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    /**
     * 对应规则 declaration -> classDecl | funDecl | varDecl | statement
     */
    private Stmt declaration() {
        try {
            if (match(CLASS)) return classDeclaration();
            if (match(VAR)) return varDeclaration();
            if (match(FUN)) return function("function");
            return statement();
        } catch (ParseError error) {
            // WHY? 注意理解 “同步” 的错误恢复
            synchronize();
            return null;
        }
    }

    /**
     * 对应规则 classDecl -> "class" IDENTIFIER "{" function* "}"
     */
    private Stmt classDeclaration() {
        Token name = consume(IDENTIFIER, "Expect class name.");
        consume(LEFT_BRACE, "Expect '{' before class body.");
        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"));
        }
        consume(RIGHT_BRACE, "Expect '}' after class body.");

        return new Stmt.Class(name, methods);
    }

    /**
     * 对应规则 varDecl -> "var" IDENTIFIER ( "=" expression ) ? ";"
     */
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        Expr initializer = null;

        // 后方有初始化表达式
        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    /**
     * 对应规则
     * - function -> IDENTIFIER "(" parameters ? ")" block
     * - parameters -> IDENTIFIER ( "," IDENTIFIER ) *
     */
    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do{
                // 限制形参数量
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");

        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }


    /**
     * 对应规则 statement -> exprStmt | forStmt | ifStmt | printStmt | returnStmt | whileStmt | block
     */
    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(RETURN)) return returnStatement();
        // 块级作用域
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        if (match(WHILE)) return whileStatement();
        return expressionStatement();
    }

    /**
     * 对应规则 ifStmt -> "if" "(" expression ")" statement ("else" statement) ?
     */
    private Stmt ifStatement() {
        // 支持 if 或 else 后单语句不用加括号
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");
        Stmt thenBranch = statement();

        Stmt elseBranch = null;
        // IMPORTANT else 语句始终与前方最近的 if 语句配对，否则有歧义
        if (match(ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    /**
     * 对应规则 whileStmt -> "while" "(" expression ")" statement
     */
    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    /**
     * 对应规则 forStmt -> "for" "(" (varDecl | exprStmt | ";")  expression ? ";" expression ? ")" statement
     * 转化为 while 语句进行语法脱糖
     */
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        // 获取初始化语句
        Stmt initializer;
        if (match(SEMICOLON)) {
            // 无初始化语句
            initializer = null;
        } else if (match(VAR)) {
            // 初始化语句为变量定义
            initializer = varDeclaration();
        } else {
            // 初始化语句为表达式语句
            initializer = expressionStatement();
        }

        // 获取循环条件语句
        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        // 获取增量语句
        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        // 获取循环体
        Stmt body = statement();

        // 有增量语句
        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        // 无循环条件，转为死循环
        if (condition == null)
            condition = new Expr.Literal(true);

        body = new Stmt.While(condition, body);

        // 有初始化语句
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    /**
     * 对应规则 block -> "{" declaration "}"
     */
    private List<Stmt> block() {
        List<Stmt> stmts = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            stmts.add(declaration());
        }
        consume(RIGHT_BRACE, "Expect '}' after block.");
        return stmts;
    }

    /**
     * 对应规则 printStmt -> "print" expression ";"
     */
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    /**
     * 对应规则 exprStmt -> expression ";"
     */
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    /**
     * 对应规则 return -> "return" expression ? ";"
     */
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }


    /**
     * 对应规则 expression  ->  assignment
     */
    private Expr expression() {
        return assignment();
    }

    /**
     * 对应规则 assignment -> IDENTIFIER "=" assignment | logic_or
     */
    private Expr assignment() {
        Expr expr = or();

        // WHY? 没看太懂
        // 支持多级赋值 a = b = c = ...
        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    /**
     * 对应规则 logic_or -> logic_and ( "or" logic_and ) *
     */
    private Expr or() {
        Expr expr = and();
        while (match(OR)) {
            Token operator = previous(); // OR
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    /**
     *  对应规则 logic_and -> equality ( "and" equality ) *
     */
    private Expr and() {
        Expr expr = equality();
        while (match(AND)) {
            Token operator = previous(); // AND
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    /**
     * 对应规则 equality ->  comparison (( "!=" | "==" ) comparison ) *
     */
    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * 对应规则 comparison ->  term ( ( ">" | ">=" | "<" | "<=" ) term ) *
     */
    private Expr comparison() {
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * 对应规则 term ->  factor ( ( "-" | "+" ) factor ) *
     */
    private Expr term() {
        Expr expr = factor();
        while (match(PLUS, MINUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * 对应规则 factor ->  unary ( ( "/" | "*" ) unary ) *
     */
    private Expr factor() {
        Expr expr = unary();
        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * 对应规则 unary -> ( "!" | "-" ) unary | primary
     */
    private Expr unary() {
        // 不支持带正号的数字
        if (match(MINUS, BANG)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    /**
     * 对应规则 call -> primary( "(" arguments ? ")" ) *
     */
    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                Token name = consume(IDENTIFIER,"Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else break;
        }
        return expr;
    }
    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do{
                // 限制参数数量
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }

    /**
     * 对应规则 primary ->  NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER
     */
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);
        if (match(NUMBER, STRING)) return new Expr.Literal(previous().literal);
        if (match(IDENTIFIER)) return new Expr.Variable(previous());
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expected ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression.");
    }



    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }
    private boolean check(TokenType type) {
        return !isAtEnd() && peek().type == type;
    }
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }
    private Token previous() {
        return tokens.get(current - 1);
    }
    private Token peek() {
        return tokens.get(current);
    }
    private boolean isAtEnd() {
        return peek().type == EOF;
    }
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }
    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }
    private void synchronize() {
        if (previous().type == SEMICOLON) return;

        // 开启新语句的关键字
        switch (peek().type) {
            case CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> {
                return;
            }
        }
        advance();
    }
}
