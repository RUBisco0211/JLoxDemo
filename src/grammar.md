### Lox 文法

#### 结合性

| Name          | Operators  | Associates |
|---------------|------------|------------|
| 等于 Equality   | == !=      | 左结合        |
| 比较 Comparison | \> >= < <= | 左结合        |
| 加减 Term       | - +        | 左结合        |
| 乘除 Factor     | / *        | 左结合        |
| 一元 Unary      | ! -        | 右结合        |

#### 表达式文法规则（数学运算和比较符号等）

- expression  ->  equality
- equality ->  comparison (( "!=" | "==" ) comparison ) *
- comparison ->  term ( ( ">" | ">=" | "<" | "<=" ) term ) *
- term ->  factor ( ( "-" | "+" ) factor ) *
- factor ->  unary ( ( "/" | "*" ) unary ) *
- unary -> ( "!" | "-" ) unary | primary  
- primary ->  NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER

#### 文法的递归下降分析

| 文法符号  | 代码表示         |
|-------|--------------|
| 终止符   | 匹配并消费一个token |
| 非终止符  | 调用规则对应的函数    |
| * 或 + | while 或 for  |
| ？     | if           |

#### 表达式求值

| Lox 类   | Java 表示 |
|---------|---------|
| 任何值     | Object  |
| Boolean | Boolean |
| nil     | null    |
| number  | Double  |
| string  | String  |

#### 语句文法规则
- program -> statement * EOF
- statement -> exprStmt | printStmt
- exprStmt -> expression ";"
- printStmt -> "print" expression ";"

##### 声明语句文法规则
- program -> declaration * EOF
- declaration -> varDecl | statement
- statement -> exprStmt | printStmt

##### 变量声明语句
- varDecl -> "var" IDENTIFIER ( "=" expression ) ? ";"

##### 赋值语句
- expression -> assignment
- assignment -> IDENTIFIER "=" assignment | equality

##### 块语法和语义
- statement -> exprStmt | printStmt | block
- block -> "{" declaration "}"

##### 条件分支语句
- statement -> exprStmt | ifStmt | printStmt | block
- ifStmt -> "if" "(" expression ")" statement ("else" statement) ?

##### 逻辑运算表达式
- expression -> assignment
- assignment -> IDENTIFIER "=" assignment | logic_or
- logic_or -> logic_and ( "or" logic_and ) *
- logic_and -> equality ( "and" equality ) *

##### while 循环语句
- statement -> exprStmt | ifStmt | printStmt | whileStmt | block
- whileStmt -> "while" "(" expression ")" statement

##### for 循环语句
- statement -> exprStmt | forStmt | ifStmt | printStmt | whileStmt | block 
- forStmt -> "for" "(" (varDecl | exprStmt | ";")  expression ? ";" expression ? ")" statement

##### 函数调用(函数的返回值可以是函数，故支持函数的连续调用)
- unary -> ( "!" | "-" ) unary | call
- call -> primary( "(" arguments ? ")" ) * 
- arguments -> expression ( "," expression ) *

##### 函数声明语句
- declaration -> funDecl | varDecl | statement
- funDecl -> "fun" function
- function -> IDENTIFIER "(" parameters ? ")" block
- parameters -> IDENTIFIER ( "," IDENTIFIER ) *

##### 返回语句
- statement -> exprStmt | forStmt | ifStmt | printStmt | returnStmt | whileStmt | block
- return -> "return" expression ? ";"

##### 类声明语句
- declaration -> classDecl | funDecl | varDecl | statement
- classDecl -> "class" IDENTIFIER "{" function* "}"

##### 扩充函数调用
- call -> primary ( "(" arguments? ")" | "." IDENTIFIER )* 