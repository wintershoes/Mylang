lexer_grammar规则

未实现自定义正则表达式

将keywords和特殊符号按下面形式写：

IMPORT      :'import';

LPAREN      : '(';
RPAREN      : ')';
EQUAL       : '=';
COMMA       : ',';
SEMI        : ';';


parser_grammar规则：
不能嵌套括号
不能跨行写

只有importStatement和statement两个开始符
小写单词是非终结符
大写单词是终结符
按照importStatement : fromImport | singleImport SEMI;
fromImport      : FROM ID IMPORT ID;
写出所有的生成式即可

