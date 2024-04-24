import java.util.*;

// ParserGrammar类定义
class ParserGrammar {
    private Map<String, List<String[]>> productions; // 存储所有的产生式
    private Map<String, Set<String>> firstSets; // 存储每个非终结符的first集合
    private Set<String> Terminals; // 存储终结符的集合
    private Map<String, Map<String, String[]>> predictiveTable; // 预测分析表
    private String startSymbol;  // 用于记录起始符号

    /**
     * 构造函数，初始化终结符集合。
     *
     * @param TerminalSymbols 终结符数组。
     */
    public ParserGrammar(String[] TerminalSymbols) {
        this.productions = new HashMap<>();
        this.firstSets = new HashMap<>();
        this.Terminals = new HashSet<>();
        this.Terminals.addAll(Arrays.asList(TerminalSymbols));
        this.startSymbol = "start"; //默认开始符号为start，且可以推出start -> importStatement 和 start -> statement两个产生式
    }

    public void addProduction(String nonTerminal, String[] production) {
        this.productions.computeIfAbsent(nonTerminal, k -> new ArrayList<>()).add(production);
    }

    public List<String[]> getProductions(String nonTerminal) {
        return this.productions.getOrDefault(nonTerminal, new ArrayList<>());
    }

    public String getStartSymbol() {
        return startSymbol;
    }

    public void loadGrammarFromFile(String grammarFileName) {
        addProduction(startSymbol, new String[]{"importStatement"});
        addProduction(startSymbol, new String[]{"statement"});
        Scan scanner = new Scan(grammarFileName);
        String[] lines = scanner.readText();

        for (String line : lines) {
            // 去除产生式末尾的分号
            line = line.trim().replaceAll(";$", "");
            String[] parts = line.split(":");

            if (parts.length == 2) {
                String nonTerminal = parts[0].trim();
                // 使用“|”分割不同的选择
                String[] options = parts[1].trim().split("\\|");
                for (String option : options) {
                    String[] production = option.trim().split("\\s+");
                    addProduction(nonTerminal, production);
                }
            }
        }

        calculateFirstSets();
        buildPredictiveParsingTable();
    }

    /**
     * 打印所有的产生式规则。
     */
    public void printProductions() {
        for (Map.Entry<String, List<String[]>> entry : productions.entrySet()) {
            String nonTerminal = entry.getKey();
            List<String[]> productionList = entry.getValue();

            for (String[] production : productionList) {
                System.out.println(nonTerminal + " -> " + String.join(" ", production));
            }
        }
    }

    /**
     * 计算每个非终结符的first集合。
     */
    private void calculateFirstSets() {
        // 初始时，为每个非终结符创建空集合
        for (String nonTerminal : productions.keySet()) {
            firstSets.put(nonTerminal, new HashSet<>());
        }

        // 标记是否发生变化，用于迭代计算
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Map.Entry<String, List<String[]>> entry : productions.entrySet()) {
                String nonTerminal = entry.getKey();
                for (String[] production : entry.getValue()) {
                    // 还没有实现考虑空串的情况，所以永远都只需要考虑第一个终结符和非终结符就行了
                    if (isTerminal(production[0])) {
                        changed |= firstSets.get(nonTerminal).add(production[0]);
                    } else if (!production[0].equals(nonTerminal)) { // 这种是左递归的情况，因为这里没有涉及左递归，所以这一判断其实可以省略
                        // 如果产生式以非终结符开始，将其first集合合并到当前非终结符的first集合中
                        Set<String> firstSetOfProduction = firstSets.get(production[0]);
                        changed |= firstSets.get(nonTerminal).addAll(firstSetOfProduction);
                    }
                }
            }
        }
    }

    /**
     * 构建预测分析表。
     */
    public void buildPredictiveParsingTable() {
        predictiveTable = new HashMap<>();

        for (Map.Entry<String, List<String[]>> entry : productions.entrySet()) {
            String nonTerminal = entry.getKey();
            List<String[]> rules = entry.getValue();

            predictiveTable.put(nonTerminal, new HashMap<>());

            for (String[] rule : rules) {
                Set<String> firstSet = calculateFirstForRule(rule);
                //根据first集填写预测分析表
                for (String terminal : firstSet) {
                    predictiveTable.get(nonTerminal).put(terminal, rule);
                }
            }
        }
    }

    /**
     * 计算一个产生式规则的FIRST集合。
     *
     * @param rule 产生式规则。
     * @return 产生式的FIRST集合。
     */
    private Set<String> calculateFirstForRule(String[] rule) {
        Set<String> result = new HashSet<>();
        String firstSymbol = rule[0];

        if (isTerminal(firstSymbol)) {
            result.add(firstSymbol);
        } else {
            result.addAll(firstSets.get(firstSymbol));
        }

        return result;
    }

    public Map<String, Map<String, String[]>> getPredictiveTable() {
        return predictiveTable;
    }

    /**
     * 打印预测分析表。
     */
    public void printPredictiveParsingTable() {
        for (Map.Entry<String, Map<String, String[]>> entry : predictiveTable.entrySet()) {
            String nonTerminal = entry.getKey();
            System.out.println("Predictive Parsing Table for " + nonTerminal + ":");
            Map<String, String[]> tableEntry = entry.getValue();
            for (Map.Entry<String, String[]> subEntry : tableEntry.entrySet()) {
                System.out.println("  On input '" + subEntry.getKey() + "': " + String.join(" ", subEntry.getValue()));
            }
        }
    }

    /**
     * 检查给定的符号是否是终结符。
     *
     * @param symbol 要检查的符号。
     * @return 如果符号是终结符，则返回true；否则返回false。
     */
    public boolean isTerminal(String symbol) {
        return this.Terminals.contains(symbol);
    }

    /**
     * 打印所有的first集合。
     */
    public void printFirstSets() {
        for (Map.Entry<String, Set<String>> entry : firstSets.entrySet()) {
            System.out.println("First(" + entry.getKey() + ") = " + entry.getValue());
        }
    }
}

class ASTNode {
    private String type; // 节点类型，例如 'importStatement', 'statement', 等。
    private String value; // 节点的值，例如具体的语句或变量名等。
    private List<ASTNode> children; // 子节点列表
    private int lineNumber; // 节点对应的源代码行号
    private boolean isTerminal; // 标记该节点是否是终结符

    public ASTNode(String type, String value, int lineNumber, boolean isTerminal) {
        this.type = type;
        this.value = value;
        this.children = new ArrayList<>();
        this.lineNumber = lineNumber;
        this.isTerminal = isTerminal;
    }

    // 添加子节点
    public void addChild(ASTNode child) {
        this.children.add(child);
    }

    // 获取节点类型
    public String getType() {
        return type;
    }

    // 获取节点值
    public String getValue() {
        return value;
    }

    // 获取所有子节点
    public List<ASTNode> getChildren() {
        return children;
    }

    // 获取行号
    public int getLineNumber() {
        return lineNumber;
    }

    // 判断是否是终结符
    public boolean isTerminal() {
        return isTerminal;
    }

    // 打印语法树（简单的深度优先遍历）
    public void printTree(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
        sb.append(type).append(": ").append(value).append(" (Line: ").append(lineNumber).append(") ").append(isTerminal ? "[Terminal]" : "[Non-terminal]");
        System.out.println(sb.toString());
        for (ASTNode child : children) {
            child.printTree(level + 1);
        }
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
}

public class Parser {
    private Lexer lexer;
    private Iterator<Lexer.Token> tokenIterator;
    private Lexer.Token currentToken;
    private Lexer.Token lastToken;
    private ParserGrammar grammar;
    private Deque<ASTNode> stack; //栈存储的是ASTNode，会有利于语法树的构建
    private ASTNode rootNode; //这是AST的根节点
    private List<String> errors; // 用于存储错误信息

    /**
     * 构造一个Parser实例，使用一个Lexer实例和一个ParserGrammar实例来提供Token和语法规则。
     *
     * @param lexer 用于词法分析的Lexer对象
     */
    public Parser(Lexer lexer) {
        this.lexer = lexer;//通过传递之前已经词法分析完毕的lexer实例来实现词法分析器和语法分析器之间的交流，我查阅资料后得知是传递的引用，不会造成空间浪费
        this.tokenIterator = this.lexer.getTokenIterator(); // 获取Token的迭代器，这里不知道是写this.lexer好一点还是lexer好点......
        this.grammar = new ParserGrammar(this.lexer.getAllTerminals()); // 初始化语法规则
        this.stack = new ArrayDeque<>(); // 初始化栈
        this.rootNode = new ASTNode("root", "Program", 0, false);
        this.stack.push(new ASTNode("$", null, -1, true)); // 结束符作为特殊节点
        this.stack.push(new ASTNode(grammar.getStartSymbol(), null, -1, false)); // 起始非终结符
        this.rootNode.addChild(stack.peek());
        this.errors = new ArrayList<>(); // 初始化错误列表
    }


    /**
     * 解析Token并构建语法树或其他语法结构。
     */
    public void analyze(String grammarFileName) {
        if(lexer.hasErrors()){
            System.out.println("The lexer has an error, please correct the syntax before proceeding with the parsing.");
            return;
        }
        grammar.loadGrammarFromFile(grammarFileName);
        currentToken = tokenIterator.next(); // 读取第一个Token
        lastToken = null;

        while (!stack.isEmpty()) {
            ASTNode topNode = stack.peek(); // 检查栈顶ASTNode
            if (topNode.isTerminal()) {
                if (topNode.getType().equals(currentToken.lexeme[0])) {
                    //一定要注意！！！只有正式匹配的时候才能确定终结符的实际值和行号是多少！！在入栈的时候还不能确定。
                    topNode.setValue(currentToken.lexeme[1]); // 设置终结符的值
                    topNode.setLineNumber(currentToken.lineNumber); // 更新行号
                    stack.pop(); // 栈顶符号与当前Token匹配，移出栈顶
                    if (tokenIterator.hasNext()) {
                        lastToken = currentToken;
                        currentToken = tokenIterator.next(); // 读取下一个Token
                    }
                    if("$".equals(topNode.getType())){ //此时已经完成了语法分析
                        break;
                    }
                } else if("$".equals(topNode.getType())){ //如果栈顶符号为结束符，但是tokens还没有分析到结束符，此时需要重新加入一个开始符号来分析接下来的符号
                    this.stack.push(new ASTNode(grammar.getStartSymbol(), null, -1, false)); // 起始非终结符
                    this.rootNode.addChild(stack.peek());
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Paser error");
                    if(lastToken != null){
                        sb.append(", at line: "+ currentToken.lineNumber +" There is no grammar where the: \""
                                + lastToken.lexeme[1] + "\" is followed by: \"" + currentToken.lexeme[1] + "\"\n");
                    }
                    if(topNode.isTerminal()){
                        sb.append("Possibly correct grammar is, followed by a: " + topNode.getType());
                    }
                    errors.add(sb.toString());
                    break;
                }
            } else {
                if (grammar.getPredictiveTable().containsKey(topNode.getType()) && grammar.getPredictiveTable().get(topNode.getType()).containsKey(currentToken.lexeme[0])) {
                    String[] production = grammar.getPredictiveTable().get(topNode.getType()).get(currentToken.lexeme[0]);
                    stack.pop(); // 移除栈顶非终结符
                    // 逆序将产生式的元素推入栈中，并作为子节点添加到当前节点
                    for (int i = production.length - 1; i >= 0; i--) {
                        ASTNode newNode = new ASTNode(production[i], null, -1, grammar.isTerminal(production[i]));
                        topNode.addChild(newNode); // 将新节点添加为子节点
                        stack.push(newNode); // 同时推入栈中
                    }
                } else {
                    errors.add("Parser error,There is no grammar that starts with the terminal: \"" + currentToken.lexeme[1] + "\" at line " + currentToken.lineNumber);//错误处理
                    break;
                }
            }
        }

    }

    /**
     * 打印解析后的语法分析表
     */
    public void printAST(){
        if (hasErrors()){
            System.out.println("\nThere is an parsing error; only the recognized AST can be printed.");
        }
        System.out.println("The value of non-terminals is null, while the value of terminals is the specific value from lexical analysis." );
        this.rootNode.printTree(0);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void printErrors() {
        if (hasErrors()) {
            for (String error : errors) {
                System.out.println(error);
            }
        }
    }

}
