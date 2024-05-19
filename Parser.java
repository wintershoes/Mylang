import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ParserGrammar类定义
class ParserGrammar {
    private Map<String, List<String[]>> productions; // 存储所有的产生式
    private Map<String, Set<String>> firstSets; // 存储每个非终结符的first集合
    private Map<String, Set<String>> followSets; // 存储每个非终结符的follow集合
    private Set<String> Terminals; // 存储终结符的集合
    private Map<String, Map<String, String[]>> predictiveTable; // 预测分析表
    private String startSymbol;  // 用于记录起始符号

    /**
     * 构造函数，初始化终结符集合。需要从lexer获取所有的终结符。
     *
     * @param TerminalSymbols 终结符字符串数组。
     */
    public ParserGrammar(String[] TerminalSymbols) {
        this.productions = new HashMap<>();
        this.firstSets = new HashMap<>();
        this.followSets = new HashMap<>();
        this.Terminals = new HashSet<>();
        this.Terminals.addAll(Arrays.asList(TerminalSymbols));
        this.startSymbol = "program"; //默认开始符号为program
    }

    /**
     * 添加产生式到parser的一个成员变量中，供后续语法分析使用。
     *
     * @param nonTerminal 非终结符。
     * @param production 非终结符展开后的产生式
     */
    public void addProduction(String nonTerminal, String[] production) {
        this.productions.computeIfAbsent(nonTerminal, k -> new ArrayList<>()).add(production);
    }

    /**
     * 获取给定非终结符的产生式。
     *
     * @param nonTerminal 要获取产生式的非终结符。
     * @return 表示指定非终结符产生式的字符串数组列表。
     */
    public List<String[]> getProductions(String nonTerminal) {
        return this.productions.getOrDefault(nonTerminal, new ArrayList<>());
    }

    /**
     * 获取文法的起始符号。
     *
     * @return 文法的起始符号。
     */
    public String getStartSymbol() {
        return startSymbol;
    }

    /**
     * 从文件中加载语法规则。
     * 此方法首先添加初始产生式，然后读取指定的语法文件，并解析文件中的产生式规则。
     *
     * @param grammarFileName 语法文件的名称.
     */
    public void loadGrammarFromFile(String grammarFileName) {
        Scan scanner = new Scan(grammarFileName);
        String[] lines = scanner.readText();
        List<String[]> unhandledSyntaxRules = new ArrayList<>();

        for (String line : lines) {
            // 去除产生式末尾的分号
            line = line.trim().replaceAll(";$", "");
            String[] parts = line.split(":");

            if (parts.length == 2) {
                String nonTerminal = parts[0].trim();
                String production = parts[1].trim();
                unhandledSyntaxRules.add(new String[] {nonTerminal,production});
            }
        }
        GrammarRewriter rewriter = new GrammarRewriter();
        rewriter.rewriteQuantifier(unhandledSyntaxRules);
        rewriter.expandRules(unhandledSyntaxRules);

        for (String[] rule : unhandledSyntaxRules) {
            addProduction(rule[0], rule[1].split("\\s+"));
        }

        calculateFirstSets();
        calculateFollowSets();
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
                    boolean nullable = true;
                    for (String symbol : production) {
                        if (isTerminal(symbol)) {
                            changed |= firstSets.get(nonTerminal).add(symbol);
                            nullable = false;
                            break;
                        } else if (!symbol.equals(nonTerminal)) { // 不允许左递归
                            Set<String> firstSetOfSymbol = firstSets.get(symbol);
                            if (firstSetOfSymbol == null) {
                                throw new RuntimeException("请修改语法文件，非终结符：" + symbol + "至少要写一条表达式");
                            } else {
                                for (String firstSymbol : firstSetOfSymbol) {
                                    if (!firstSymbol.equals("ε")) {
                                        changed |= firstSets.get(nonTerminal).add(firstSymbol);
                                    }
                                }
                                if (!firstSetOfSymbol.contains("ε")) {
                                    nullable = false;
                                    break;
                                }
                            }
                        }
                    }
                    if (nullable) {
                        changed |= firstSets.get(nonTerminal).add("ε");
                    }
                }
            }
        }
    }

    /**
     * 计算每个非终结符的follow集合。
     */
    private void calculateFollowSets() {
        for (String nonTerminal : productions.keySet()) {
            followSets.put(nonTerminal, new HashSet<>());
        }

        // 将 $ 放入起始符号的 FOLLOW 集合
        followSets.get(startSymbol).add("$");

        boolean changed = true;
        while (changed) {
            changed = false;
            for (Map.Entry<String, List<String[]>> entry : productions.entrySet()) {
                String nonTerminal = entry.getKey();
                for (String[] production : entry.getValue()) {
                    Set<String> trailer = new HashSet<>(followSets.get(nonTerminal));
                    trailer.add("ε");

                    // 从右向左更新 FOLLOW 集
                    for (int i = production.length - 1; i >= 0; i--) {
                        String symbol = production[i];
                        if (isTerminal(symbol)) {
                            trailer.clear();
                            trailer.add(symbol);
                        } else { // symbol 是非终结符
                            int oldSize = followSets.get(symbol).size();

                            // Rule 2: 将 trailer 加到 FOLLOW(symbol) 中,除去空串
                            followSets.get(symbol).addAll(trailer);
                            followSets.get(symbol).remove("ε");

                            // Rule 3: 将 FOLLOW(nonTerminal) 添加到 FOLLOW(symbol) 中
                            if (i == production.length - 1 || trailer.contains("ε")) {
                                followSets.get(symbol).addAll(followSets.get(nonTerminal));
                            }

                            //更新trailer
                            Set<String> firstSetOfSymbol = new HashSet<>(firstSets.get(symbol));
                            if(!firstSetOfSymbol.contains("ε")){
                                trailer.clear();
                            }
                            firstSetOfSymbol.remove("ε");
                            trailer.addAll(firstSetOfSymbol);

                            if (followSets.get(symbol).size() != oldSize) changed = true;
                        }
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
                // 根据first集填写预测分析表
                for (String terminal : firstSet) {
                    if (!terminal.equals("ε")) { // 不管空串
                        predictiveTable.get(nonTerminal).put(terminal, rule);
                    }
                }

                // 如果产生式可以推出空串，则需要添加 FOLLOW(nonTerminal) 到预测分析表中
                if (firstSet.contains("ε")) {
                    Set<String> followSet = followSets.get(nonTerminal);
                    if (followSet != null) {
                        for (String followSymbol : followSet) {
                            predictiveTable.get(nonTerminal).put(followSymbol, rule);
                        }
                    }
                }
            }
        }
    }

    private Set<String> calculateFirstForRule(String[] rule) {
        Set<String> result = new HashSet<>();
        boolean nullable = true;

        for (String symbol : rule) {
            if (isTerminal(symbol)) {
                result.add(symbol);
                nullable = false;
                break;
            } else {
                Set<String> firstSetOfSymbol = firstSets.get(symbol);
                if (firstSetOfSymbol != null) {
                    for (String firstSymbol : firstSetOfSymbol) {
                        if (!firstSymbol.equals("ε")) {
                            result.add(firstSymbol);
                        }
                    }
                    if (!firstSetOfSymbol.contains("ε")) {
                        nullable = false;
                        break;
                    }
                } else {
                    throw new RuntimeException("请修改语法文件，符号：" + symbol + "至少要写一条表达式，因为没有在词法文件里找到该词汇，默认该词汇为非终结则应当写有对应的表达式");
                }
            }
        }

        if (nullable) {
            result.add("ε");
        }

        return result;
    }


    /**
     * 返回预测分析表。
     * @return 预测分析表，格式是Map<String, Map<String, String[]>>
     */
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

    /**
     * 打印所有的follow集合。
     */
    public void printFollowSets() {
        for (Map.Entry<String, Set<String>> entry : followSets.entrySet()) {
            System.out.println("Follow(" + entry.getKey() + ") = " + entry.getValue());
        }
    }
}

class ASTNode {
    private String type; // 节点类型，非终结符就是其名称，终结符是其词法分析器得到的类型。
    private String value; // 节点的值，例如具体的语句或变量名等。注意非终结符为空
    private List<ASTNode> children; // 用列表来存储树的子节点
    private int lineNumber; // 节点对应的源代码行号
    private boolean isTerminal; // 标记该节点是否是终结符

    public ASTNode(String type, String value, int lineNumber, boolean isTerminal) {
        this.type = type;
        this.value = value;
        this.children = new ArrayList<>();
        this.lineNumber = lineNumber;
        this.isTerminal = isTerminal;
    }

    /**
     * 添加子节点到当前节点。
     *
     * @param child 要添加的子节点。
     */
    public void addChild(ASTNode child) {
        this.children.add(child);
    }

    /**
     * 获取节点类型。
     *
     * @return 节点的类型。
     */
    public String getType() {
        return type;
    }

    /**
     * 获取节点值。
     *
     * @return 节点的值。
     */
    public String getValue() {
        return value;
    }

    /**
     * 获取所有孩子点。
     *
     * @return 当前节点的所有子节点。
     */
    public List<ASTNode> getChildren() {
        return children;
    }

    /**
     * 获取行号。
     *
     * @return 行号。
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * 判断节点是否是终结符。
     *
     * @return 如果节点是终结符，则返回true；否则返回false。
     */
    public boolean isTerminal() {
        return isTerminal;
    }

    /**
     * 打印语法树。
     *
     * @param level 当前节点的层级。
     */
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

    /**
     * 设置节点的值。
     *
     * @param value 要设置的节点值。
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * 设置行号。
     *
     * @param lineNumber 要设置的行号。
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
}
/**
 * Parser 类用于根据词法分析器的结果进行语法分析
 */
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
        this.stack.push(new ASTNode("$", null, -1, true)); // 结束符作为特殊节点
        this.stack.push(new ASTNode(grammar.getStartSymbol(), null, -1, false)); // 起始非终结符
        this.rootNode = this.stack.peek();
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
                }else if("ε".equals(topNode.getType())){
                    stack.pop(); // 遇到空串直接弹出即可
                }else {
                    StringBuilder sb = getStringError(topNode);
                    errors.add(sb.toString());
                    break;
                }
            } else {
                if (grammar.getPredictiveTable().containsKey(topNode.getType()) && grammar.getPredictiveTable().get(topNode.getType()).containsKey(currentToken.lexeme[0])) {
                    String[] production = grammar.getPredictiveTable().get(topNode.getType()).get(currentToken.lexeme[0]);
                    List<ASTNode> childrens = new ArrayList<>();
                    stack.pop(); // 移除栈顶非终结符
                    // 逆序将产生式的元素推入栈中，并作为子节点添加到当前节点
                    for (int i = production.length - 1; i >= 0; i--) {
                        ASTNode newNode = new ASTNode(production[i], null, -1, grammar.isTerminal(production[i]));
                        stack.push(newNode); // 同时推入栈中
                        childrens.add(newNode);
                    }
                    for (int i = childrens.size() - 1; i >= 0; i--) {
                        topNode.addChild(childrens.get(i)); // 将新节点添加为子节点
                    }

                } else {
                    errors.add("Parser error,There is no grammar that starts with the terminal: \"" + currentToken.lexeme[1] + "\" at line " + currentToken.lineNumber);//错误处理
                    break;
                }
            }
        }

    }

    private StringBuilder getStringError(ASTNode topNode) {
        StringBuilder sb = new StringBuilder();
        sb.append("Paser error");
        if(lastToken != null){
            sb.append(", at line: "+ currentToken.lineNumber +" There is no grammar where the: \""
                    + lastToken.lexeme[1] + "\" is followed by: \"" + currentToken.lexeme[1] + "\"\n");
        }
        if(topNode.isTerminal()){
            sb.append("Possibly correct grammar is, followed by a: " + topNode.getType());
        }
        return sb;
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

    /**
     * 判断是否存在语法错误
     * @return 是否有错误
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * 打印语法错误
     */
    public void printErrors() {
        if (hasErrors()) {
            for (String error : errors) {
                System.out.println(error);
            }
        }
    }

}
class GrammarRewriter {
    public void rewriteQuantifier(List<String[]> rules) {
        List<String[]> newRules = new ArrayList<>();
        // 匹配括号内的内容和后面的量词，允许嵌套括号
        Pattern pattern = Pattern.compile("(\\([^()]*?\\))([+*?])|(\\S+)([+*?])");
        int midNNonTerminalCnt = 0;
        String midNonTerminal = "mid";
        boolean ifChange;

        do {
            ifChange = false;
            for (String[] rule : rules) {
                String nonTerminal = rule[0];
                String production = rule[1];

                Matcher matcher = pattern.matcher(production);
                StringBuffer newProduction = new StringBuffer();

                // 处理所有找到的匹配项
                if (matcher.find()) {
                    ifChange = true;
                    String group = matcher.group(1) != null ? matcher.group(1) : matcher.group(3); // 量词前的内容
                    String quantifier = matcher.group(2) != null ? matcher.group(2) : matcher.group(4); // 量词

                    String newmidNonTerminal = midNonTerminal + "_" + midNNonTerminalCnt;
                    midNNonTerminalCnt++;

                    switch (quantifier) {
                        case "+":
                            matcher.appendReplacement(newProduction, group + ' ' + newmidNonTerminal);
                            matcher.appendTail(newProduction);
                            newRules.add(new String[] { nonTerminal, newProduction.toString() });
                            newRules.add(new String[] { newmidNonTerminal, group + ' ' + newmidNonTerminal });
                            newRules.add(new String[] { newmidNonTerminal, "ε" });
                            break;
                        case "?":
                            matcher.appendReplacement(newProduction, group);
                            matcher.appendTail(newProduction);
                            newRules.add(new String[] { nonTerminal, newProduction.toString() });

                            newProduction.setLength(0);
                            matcher.reset();
                            matcher.find();
                            matcher.appendReplacement(newProduction, "");
                            matcher.appendTail(newProduction);
                            newRules.add(new String[] { nonTerminal, newProduction.toString() });
                            break;
                        case "*":
                            matcher.appendReplacement(newProduction, newmidNonTerminal);
                            matcher.appendTail(newProduction);
                            newRules.add(new String[] { nonTerminal, newProduction.toString() });
                            newRules.add(new String[] { newmidNonTerminal, group + ' ' + newmidNonTerminal });
                            newRules.add(new String[] { newmidNonTerminal, "ε" });
                            break;
                    }

                } else {
                    newRules.add(rule);
                }

            }

            // 替换原始规则列表
            rules.clear();
            rules.addAll(newRules);
            newRules.clear();

        } while (ifChange);

    }

    public void expandRules(List<String[]> rules) {
        Set<String> expandedRulesSet = new HashSet<>(); // 用于存储唯一的规则字符串
        List<String[]> expandedRules = new ArrayList<>(); // 最终的规则列表
        for (String[] rule : rules) {
            List<String[]> expanded = expandRule(rule[0], rule[1]);
            for (String[] expandedRule : expanded) {
                String uniqueRule = expandedRule[0] + " -> " + expandedRule[1]; // 创建一个唯一的规则字符串
                if (expandedRulesSet.add(uniqueRule)) { // 如果成功添加（即之前没有这个规则）
                    expandedRules.add(expandedRule); // 添加到结果列表
                }
            }
        }

        // 替换原始规则列表
        rules.clear();
        rules.addAll(expandedRules);
    }

    // 递归展开规则
    public List<String[]> expandRule(String nonTerminal, String production) {
        List<String[]> expanded = new ArrayList<>();

        int startIdx = production.indexOf('(');
        if (startIdx != -1) {
            int endIdx = findMatchingParenthesis(production, startIdx);
            if (endIdx != -1) {
                String prefix = production.substring(0, startIdx);
                String choices = production.substring(startIdx + 1, endIdx);
                String suffix = production.substring(endIdx + 1);

                // 分割选择并递归处理每个选择
                String[] splits = choices.split("\\|");
                for (String split : splits) {
                    expanded.addAll(expandRule(nonTerminal, prefix + split.trim() + suffix));
                }
            }
        } else {
            // 没有括号，检查是否有选择符
            if (production.contains("|")) {
                String[] splits = production.split("\\|");
                for (String split : splits) {
                    expanded.add(new String[] { nonTerminal, split.trim() });
                }
            } else {
                // 没有括号也没有选择符
                expanded.add(new String[] { nonTerminal, production });
            }
        }

        return expanded;
    }

    // 找到匹配的右括号
    public int findMatchingParenthesis(String production, int startIdx) {
        int count = 1;
        for (int i = startIdx + 1; i < production.length(); i++) {
            if (production.charAt(i) == '(') {
                count++;
            } else if (production.charAt(i) == ')') {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        return -1; // 未找到匹配的括号
    }

}
