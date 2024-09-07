package org.main;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parser 类用于根据词法分析器的结果进行语法分析
 */
public class Parser {
    Lexer lexer;
    Lexer.TokenIterator tokenIterator;
    Lexer.Token currentToken;
    String[] lastProduction; //存储最近希望扩展出来的表达式
    ParserGrammar grammar;
    CustomStack<ASTNode> stack; //栈存储的是ASTNode，会有利于语法树的构建
    List<ASTNode> NonTerminallog; //记录之前扩展的节点
    ASTNode lastNonTerminal; //记录最近希望扩展的非终结符
    ASTNode rootNode; //这是AST的根节点
    List<ParserError> errors; // 用于存储错误信息

    /**
     * 构造一个Parser实例，使用一个Lexer实例和一个ParserGrammar实例来提供Token和语法规则。
     *
     * @param lexer 用于词法分析的Lexer对象
     */
    public Parser(Lexer lexer) {
        this.lexer = lexer;//通过传递之前已经词法分析完毕的lexer实例来实现词法分析器和语法分析器之间的交流，我查阅资料后得知是传递的引用，不会造成空间浪费
        this.tokenIterator = this.lexer.getTokenIterator(); // 获取Token的迭代器
        this.grammar = new ParserGrammar(this.lexer.getAllTerminals()); // 初始化语法规则
        this.stack = new CustomStack<>(); // 初始化栈
        this.stack.push(new ASTNode("$", null, -1, true)); // 结束符作为特殊节点
        this.stack.push(new ASTNode(grammar.getStartSymbol(), null, -1, false)); // 起始非终结符
        this.rootNode = this.stack.peek();
        this.errors = new ArrayList<>(); // 初始化错误列表
        this.NonTerminallog = new ArrayList<>();
    }


    /**
     * 解析Token并构建语法树或其他语法结构。
     */
    public void analyze(String grammarFileName) {
//        if(lexer.hasErrors()){
//            System.out.println("The lexer has an error, please correct the syntax before proceeding with the parsing.");
//            return;
//        }
        grammar.loadGrammarFromFile(grammarFileName);
        currentToken = tokenIterator.next(); // 读取第一个Token

        while (!stack.isEmpty()) {
            ASTNode topNode = stack.peek(); // 检查栈顶ASTNode
            if (topNode.isTerminal()) {
                if (topNode.getType().equals(currentToken.lexeme[0])) {
                    //一定要注意！！！只有正式匹配的时候才能确定终结符的实际值和行号是多少！！在入栈的时候还不能确定。
                    topNode.setValue(currentToken.lexeme[1]); // 设置终结符的值
                    topNode.setLineNumber(currentToken.lineNumber); // 更新行号
                    NonTerminallog.add(topNode);
                    stack.pop(); // 栈顶符号与当前Token匹配，移出栈顶
                    if (tokenIterator.hasNext()) {
                        currentToken = tokenIterator.next(); // 读取下一个Token
                    }
                    if("$".equals(topNode.getType())){ //此时已经完成了语法分析
                        break;
                    }
                }else if("ε".equals(topNode.getType())){
                    stack.pop(); // 遇到空串直接弹出即可
                }else {
                    ParserError e = new ParserError();
                    errors.add(e);
                    if(!e.handle()){ //尝试处理和恢复错误，继续解析下面的代码，如果恢复失败了，直接结束语法分析
                        break;
                    }
                    int a = 0;
                }
            } else {
                if (grammar.getPredictiveTable().containsKey(topNode.getType()) &&
                        grammar.getPredictiveTable().get(topNode.getType()).containsKey(currentToken.lexeme[0])) {
                    String[] production = grammar.getPredictiveTable().get(topNode.getType()).get(currentToken.lexeme[0]);
                    if(Arrays.equals(production , grammar.getConflictSymbol())){
                        production = handleConflict(topNode.getType());
                    }
                    lastProduction = production;
                    List<ASTNode> childrens = new ArrayList<>();
                    lastNonTerminal = topNode;
                    NonTerminallog.add(topNode);
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
                    ParserError e = new ParserError();
                    errors.add(e);
                    if(!e.handle()){ //尝试处理和恢复错误，继续解析下面的代码，如果恢复失败了，直接结束语法分析
                        break;
                    }
                    int a = 0;
                }
            }
        }

    }

    private String[] handleConflict(String nonTerminal){
        int bacKIndex = tokenIterator.getcurrentIndex();
        String[] result = null;
        Lexer.Token lookaheadToken;
        switch (nonTerminal){
            case "statement":
                break;
            default:
                throw new RuntimeException("还有未能处理的语法冲突！该非终结符为" + nonTerminal);
        };

        tokenIterator.backtrack(bacKIndex);
        return result;
    }

    /**
     * 打印解析后的语法分析表
     */
    public void printAST(){
        if (hasErrors()){
            System.out.println("\nThere is an parsing error; No AST can be printed.");
            return;
        }
        this.rootNode.printTree(0);
    }

    public ASTNode getRootNode(){
        return rootNode;
    }

    /**
     * 判断是否存在语法错误
     * @return 是否有错误
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }


    // 语法错误类
    class ParserError extends CompilationError {
        public ParserError() {

        }

        @Override
        public boolean handle() {
            if(stack.peek() == null){
                return false;
            }
            System.err.println("At line " + currentToken.lineNumber + ":");
            if(stack.peek().isTerminal){
                if(lastNonTerminal.getType().contains("Command")){
                    return handle_command();
                }else{
                    return handle_terminal();
                }
            }else{
                String topType = stack.peek().getType();
                switch (topType){
                    case "mid_0":
                    case "program":
                        return handle_program();
                }
                return handle_nonterminal();
            }
        }

        public boolean handle_terminal(){
            return false;
        }

        public boolean handle_nonterminal() {
            return false;
        }

        public boolean handle_program() {
            System.err.println("The command (keyword) is illegal.");

            return skipSemi();
        }

        public boolean handle_command() {
            ASTNode topNode = stack.peek();
            if (topNode.getType().equals("SEMI")) {
                if (currentToken.lexeme[0].equals("COMMA") | currentToken.lexeme[0].equals("NUMBER") ) {
                    System.err.println("The number of parameters (identifiers) is illegal.");
                    popSemi();
                    return skipSemi();
                } else {
                    System.err.println("Last sentence must end with the semi symbol.");
                    popSemi();
                    return true;
                }
            } else if ((currentToken.lexeme[0].equals("ID") && topNode.getType().equals("NUMBER")) |
            (currentToken.lexeme[0].equals("NUMBER") && topNode.getType().equals("ID"))) {
                System.err.println("Parameter types of the command are invalid.");
                popSemi();
                return skipSemi();
            } else if (currentToken.lexeme[0].equals("SEMI") && (topNode.getType().equals("COMMA") | topNode.getType().equals("NUMBER")
                    | topNode.getType().equals("ID"))) {
                System.err.println("The number of parameters (identifiers) is illegal.");
                popSemi();
                return skipSemi();
            }else{
                System.err.println("This statement exists errors.");
                popSemi();
                return skipSemi();
            }
        }

        boolean skipSemi(){
            Lexer.Token followToken;
            while(tokenIterator.hasNext())
            {
                followToken = tokenIterator.next();
                if(followToken.lexeme[1].equals(";")){
                    break;
                }
                if(followToken.lexeme[1].equals("$")){
                    return false;
                }
            }

            if(tokenIterator.hasNext()){
                currentToken = tokenIterator.next();
                return true;
            }
            return false;
        }

        void popSemi(){
            while(!stack.isEmpty()){
                ASTNode topNode = stack.peek();
                if(topNode.getType().equals("SEMI")){
                    stack.pop();
                    break;
                }else if(topNode.getType().equals("$")){
                    break;
                }else {
                    stack.pop();
                }
            }
        }

    }

    //class ParserError extends CompilationError {
//
//        public ParserError() {
//
//        }
//
//        @Override
//        public void handle() {
//            System.out.println("At line " + currentToken.lineNumber + ":");
//            if(stack.peek() == null){
//                return;
//            }
//            if(stack.peek().isTerminal){
//                if(lastNonTerminal.getType().contains("Command")){
//                    handle_command();
//                } else if (stack.peek().getType().equals("ASSIGN")) {
//                    handle_assign();
//                } else{
//                    handle_terminal();
//                }
//            }else{
//                String topType = stack.peek().getType();
//                switch (topType){
//                    case "mid_0":
//                    case "program":
//                        handle_program();
//                        return;
//                    case "assignstatement":
//                        handle_assign();
//                        return;
//                }
//                if(topType.contains("Exp")){
//                    handle_Exp();
//                } else if (topType.contains("op")) {
//                    handle_op();
//                }else{
//                    handle_nonterminal();
//                }
//            }
//        }
//
//        public void handle_terminal(){
//            System.out.println("目前期望" + tokenIterator.lookbackK(1).lexeme[1] + "后紧跟一个" + stack.peek().getType() +
//                    " 但看到的是 \"" + currentToken.lexeme[1] + "\"");
//        }
//
//        public void handle_nonterminal() {
//            if(!Objects.equals(currentToken.lexeme[0], "$")){
//                System.out.println("出现了不期望的词汇: " + currentToken.lexeme[0] + ", 值为 \"" + currentToken.lexeme[1] + "\"");
//            }else{
//                System.out.println("语句意外地结束了，缺少必要的部分");
//            }
//
//            Map<String, String[]> predictiveTable = grammar.getPredictiveTable().get(stack.peek().getType());
//            boolean ifFirst = true;
//            for (Map.Entry<String, String[]> entry : predictiveTable.entrySet()) {
//                String key = entry.getKey(); // 键，通常是一个词法单元
//                String[] production = entry.getValue(); // 对应的产生式规则，是一个字符串数组
//
//                if(!production[0].equals("ε") && grammar.isTerminal(production[0])){
//                    if(ifFirst){
//                        System.out.println("此处可能期望的词汇有:");
//                        ifFirst = false;
//                    }else {
//                        System.out.print("或: ");
//                    }
//                    System.out.println(production[0]);
//                }
//            }
//
//        }
//
//        public void handle_program() {
//            System.out.println("不存在以类型为: " + currentToken.lexeme[0] + ", 值为 \"" + currentToken.lexeme[1] + "\" 的词汇开头的语句");
//        }
//
//        public void handle_command() {
//            for (String word:lastProduction) {
//                if(word.equals(currentToken.lexeme[0])){
//                    System.out.println("你写的" + lastNonTerminal.getType() + "型语句, 在 \"" + currentToken.lexeme[1] +
//                            "\" 前，缺失了部分词汇");
//                    System.out.println("此处希望符合的表示式为 " + Arrays.toString(lastProduction));
//                    return;
//                }
//            }
//
//            ASTNode topNode = stack.peek();
//            if(!Objects.equals(currentToken.lexeme[0], "$")){
//                System.out.println("你写的" + lastNonTerminal.getType() + "型语句里, 不应出现类型为: " + currentToken.lexeme[0] +
//                        ", 值为 \"" + currentToken.lexeme[1] + "\" 的词汇");
//            }else {
//                System.out.println("你写的" + lastNonTerminal.getType() + "型语句此处缺少了: " + topNode.getType() + " 词汇");
//            }
//            if(topNode.getType().equals("SEMI")){
//                System.out.println("此处应当用分号: “;” 结束该" + lastNonTerminal.getType() + "类型的命令");
//            }else{
//                System.out.println("此处希望符合的表示式为 " + Arrays.toString(lastProduction));
//            }
//
//        }
//
//        public void handle_Exp(){
//            if(stack.peek().getType().contains("bool")){
//                System.out.println("此处布尔表达式有误");
//                System.out.println("正确的表达式应为:[LPAREN , (mathExp | NUMBER | ID) , boolop , (mathExp | NUMBER | ID) , RPAREN]");
//            }else if(stack.peek().getType().contains("math")){
//                System.out.println("此处数学表达式有误");
//                System.out.println("正确的表达式应为:[LPAREN , (NUMBER | ID) , mathop , (NUMBER | ID) , RPAREN]");
//            }
//
//        }
//
//        public void handle_op(){
//            System.out.println("表达式缺少正确的操作符:" + stack.peek().getType());
//        }
//
//        public void handle_assign(){
//            System.out.println("你是否在写一个赋值语句? 若是，你的赋值语句不对，正确形式应为" + "[ID , ASSIGN , mathExp , SEMI]");
//            System.out.println("若你写的不是，则检查" + tokenIterator.lookbackK(1).lexeme[1] + "的拼写是否正确，可能没有填写正确的关键字");
//        }
//
//    }


}

// ParserGrammar类定义
class ParserGrammar {
    Map<String, List<String[]>> productions; // 存储所有的产生式
    Map<String, Set<String>> firstSets; // 存储每个非终结符的first集合
    Map<String, Set<String>> followSets; // 存储每个非终结符的follow集合
    Set<String> Terminals; // 存储终结符的集合a
    Map<String, Map<String, String[]>> predictiveTable; // 预测分析表
    String startSymbol;  // 用于记录起始符号
    String[] conflictSymbol;  // 用于记录预测表是否存在冲突
    List<String> conflicts;  // 用于记录存在的冲突

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
        this.conflictSymbol = new String[]{"???"}; //默认冲突符号为???
        this.conflicts = new ArrayList<>();
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

    public String[] getConflictSymbol(){ return conflictSymbol;}

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
        StringBuilder currentProduction = new StringBuilder();

        for (String line : lines) {
            // 忽略空行和注释
            if (line.trim().isEmpty() || line.trim().startsWith("//")) continue;
            // 这里处理了分行写的情况
            if (line.contains(";")) {
                // 根据分号进行分割
                int semicolonIndex = line.indexOf(';');
                currentProduction.append(line.substring(0, semicolonIndex));

                // 根据冒号分割
                String completeProduction = currentProduction.toString().trim();
                String[] parts = completeProduction.split(":");

                if (parts.length == 2) {
                    String nonTerminal = parts[0].trim();
                    if(isTerminal(nonTerminal)){
                        throw new RuntimeException("请检查语法文件格式，产生式左侧不能出现非终结符");
                    }
                    String production = parts[1].trim();
                    unhandledSyntaxRules.add(new String[] {nonTerminal, production});
                } else if(parts.length > 2) {
                    throw new RuntimeException("请检查语法文件格式，以分号结束的一个产生式里，不要有多个冒号");
                } else {
                    throw new RuntimeException("请检查语法文件格式，每个产生式应当由分号连接");
                }

                // 重置累加
                currentProduction = new StringBuilder();

                if (semicolonIndex + 1 < line.length()) {
                    String Str = line.substring(semicolonIndex + 1);
                    if(!Str.startsWith("//")){
                        throw new RuntimeException("请检查语法文件格式，在分号后，请另起一行再写新的表达式");
                    }
                }
            } else {
                currentProduction.append(line);
            }
        }
        GrammarRewriter rewriter = new GrammarRewriter();
        rewriter.rewriteQuantifier(unhandledSyntaxRules);
        rewriter.expandRules(unhandledSyntaxRules);

        for (String[] rule : unhandledSyntaxRules) {
            addProduction(rule[0], rule[1].split("\\s+"));
        }
        eliminateDirectLeftRecursion();
        eliminateLeftCommonFactors();
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

    public void printConflicts(){
        for(String conflict : conflicts){
            System.out.println(conflict);
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
                            if(!symbol.equals("ε")){
                                changed |= firstSets.get(nonTerminal).add(symbol);
                                nullable = false;
                                break;
                            }
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

    //提取左公因子要记录下来，方便后续还原成原表达式，这样利于语义分析
    public void eliminateLeftCommonFactors() {
        Map<String, List<String[]>> newProductions = new HashMap<>();

        for (String nonTerminal : productions.keySet()) {
            List<String[]> currentProductions = new ArrayList<>(productions.get(nonTerminal));
            boolean changed;

            do {
                Map<String, List<String[]>> prefixMap = new HashMap<>();
                changed = false;

                for (String[] production : currentProductions) {
                    for (int length = 1; length <= production.length; length++) {
                        String prefix = String.join(" ", Arrays.copyOf(production, length));
                        prefixMap.computeIfAbsent(prefix, k -> new ArrayList<>()).add(production);
                    }
                }

                String longestCommonPrefix = "";
                List<String[]> longestGroup = null;

                for (Map.Entry<String, List<String[]>> entry : prefixMap.entrySet()) {
                    if (entry.getValue().size() > 1 && entry.getKey().length() > longestCommonPrefix.length()) {
                        longestCommonPrefix = entry.getKey();
                        longestGroup = entry.getValue();
                    }
                }

                // 以上计算出了最长的公共前缀longestCommonPrefix，以及longestGroup存了该前缀对应的产生式有哪些

                if (longestCommonPrefix.length() > 0) {
                    // 确定新非终结符
                    String[] commonPrefixArray = longestCommonPrefix.split(" ");
                    String newNonTerminal = "mid_" + nonTerminal + "_ext";
                    int index = 1;
                    while (productions.containsKey(newNonTerminal) || newProductions.containsKey(newNonTerminal)) {
                        newNonTerminal = "mid_" + nonTerminal + "_ext" + index++;
                    }

                    // newGroupProductions:新的中间非终结符可以推出的产生式，也就是公因子后面的部分
                    List<String[]> newGroupProductions = new ArrayList<>();
                    for (String[] prod : longestGroup) {
                        if (prod.length > commonPrefixArray.length) {
                            newGroupProductions.add(Arrays.copyOfRange(prod, commonPrefixArray.length, prod.length));
                        } else {
                            newGroupProductions.add(new String[] {"ε"});
                        }
                    }

                    // 先将newProductions中含有最长前缀的表达式给删去
                    List<String[]> finalLongestGroup = longestGroup;

                    List<String[]> existingProductions = newProductions.get(nonTerminal); // 获取当前非终结符对应的产生式列表
                    if (existingProductions != null) {
                        existingProductions.removeIf(production -> finalLongestGroup.stream()
                                .anyMatch(groupProd -> Arrays.equals(production, groupProd) // 检查是否与longestGroup中的任何一个产生式相等
                                ));
                    }

                    // 确定所有新的产生式：

                    // 原非终结符可以推出公因子+中间终结符
                    newProductions.computeIfAbsent(nonTerminal, k -> new ArrayList<>())
                            .add(Arrays.copyOf(commonPrefixArray, commonPrefixArray.length + 1));
                    newProductions.get(nonTerminal)
                            .get(newProductions.get(nonTerminal).size() - 1)[commonPrefixArray.length] = newNonTerminal;

                    // 新终结符推出公因子后面的部分
                    newProductions.put(newNonTerminal, newGroupProductions);

                    // longestGroup为含有最长前缀的表达式，这里把他们从currentProductions中删去

                    currentProductions = currentProductions.stream()
                            .filter(p -> !finalLongestGroup.contains(p))
                            .collect(Collectors.toList());

                    // 把公因子+中间终结符这个新的产生式放回currentProductions，因为还可能会有更短的公共前缀但是重合
                    String[] newProduction = Arrays.copyOf(commonPrefixArray, commonPrefixArray.length + 1);
                    newProduction[commonPrefixArray.length] = newNonTerminal;
                    currentProductions.add(newProduction);

                    changed = true;
                }
            } while (changed);

            for (String[] remainingProduction : currentProductions) {
                List<String[]> productionsList = newProductions.computeIfAbsent(nonTerminal, k -> new ArrayList<>());
                if (productionsList.stream().noneMatch(p -> Arrays.equals(p, remainingProduction))) {
                    productionsList.add(remainingProduction);
                }
            }

        }
        productions = newProductions;
    }

    //TODO:消除左递归，还没有完善，可能存在很多bug
    public void eliminateDirectLeftRecursion() {
        Map<String, List<String[]>> newProductions = new HashMap<>();

        for (String nonTerminal : productions.keySet()) {
            //首先需要将递归的产生式和非递归的产生式分开记录下来
            List<String[]> recursiveProductions = new ArrayList<>();
            List<String[]> nonRecursiveProductions = new ArrayList<>();

            for (String[] production : productions.get(nonTerminal)) {
                if (production.length > 0 && production[0].equals(nonTerminal)) {
                    recursiveProductions.add(production);
                } else {
                    nonRecursiveProductions.add(production);
                }
            }

            if (!recursiveProductions.isEmpty()) {
                String newNonTerminal = nonTerminal + "'";
                List<String[]> newNonTerminalProductions = new ArrayList<>();

                //A->AB | c
                //改写成 A->CA' A'->BA'

                //下面是先增加A->CA'的部分
                for (String[] nonRecursiveProduction : nonRecursiveProductions) {
                    String[] newProduction = Arrays.copyOf(nonRecursiveProduction, nonRecursiveProduction.length + 1);
                    newProduction[nonRecursiveProduction.length] = newNonTerminal;
                    newProductions.computeIfAbsent(nonTerminal, k -> new ArrayList<>()).add(newProduction);
                }

                //再增加A'->BA'的部分
                for (String[] recursiveProduction : recursiveProductions) {
                    //注意原本的第一个非终结符是不需要拷贝的
                    String[] newProduction = Arrays.copyOfRange(recursiveProduction, 1, recursiveProduction.length + 1);
                    newProduction[recursiveProduction.length - 1] = newNonTerminal;
                    newNonTerminalProductions.add(newProduction);
                }

                //最后还要保证引入的新非终结符可以推出空串
                newNonTerminalProductions.add(new String[] { "ε" });
                newProductions.put(newNonTerminal, newNonTerminalProductions);
            } else {
                newProductions.put(nonTerminal, nonRecursiveProductions);
            }
        }

        productions = newProductions;
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
                        Map<String, String[]> innerMap = predictiveTable.get(nonTerminal);
                        if (!innerMap.containsKey(terminal)) {
                            innerMap.put(terminal, rule);
                        } else {
                            conflicts.add("对于" + nonTerminal + "存在冲突，只往前看一项:" + terminal +
                                    "，既可以选择" + Arrays.toString(innerMap.get(terminal)));
                            conflicts.add("又可以选择：" + Arrays.toString(rule));
                            innerMap.put(terminal, conflictSymbol);
                       }
                    }
                }

                // 如果产生式可以推出空串，则需要添加 FOLLOW(nonTerminal) 到预测分析表中
                if (firstSet.contains("ε")) {
                    Set<String> followSet = followSets.get(nonTerminal);
                    if (followSet != null) {
                        for (String followSymbol : followSet) {
                            Map<String, String[]> innerMap = predictiveTable.get(nonTerminal);
                            if (!innerMap.containsKey(followSymbol)) {
                                innerMap.put(followSymbol, rule);
                            } else {
                                throw new RuntimeException("存在first和follow冲突,该文法无法用LL1文法解析");
                            }
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
                if (!symbol.equals("ε")){
                    result.add(symbol);
                    nullable = false;
                    break;
                }
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
    public String type; // 节点类型，非终结符就是其名称，终结符是其词法分析器得到的类型。
    public String value; // 节点的值，例如具体的语句或变量名等。注意非终结符为空
    public List<ASTNode> children; // 用列表来存储树的子节点
    public int lineNumber; // 节点对应的源代码行号
    public boolean isTerminal; // 标记该节点是否是终结符

    public ASTNode(String type, String value, int lineNumber, boolean isTerminal) {
        this.type = type;
        this.value = value;
        this.children = new ArrayList<>();
        this.lineNumber = lineNumber;
        this.isTerminal = isTerminal;
    }

    public void addChild(ASTNode child) {
        this.children.add(child);
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public List<ASTNode> getChildren() {
        return children;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public boolean isTerminal() {
        return isTerminal;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public void printTree(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
        if(isTerminal) {
            sb.append(type).append(": ").append(value).append(" (Line: ").append(lineNumber).append(") ").append("[Terminal]");
        }else{
            sb.append(type).append(": ").append("[Non-terminal]");
        }
        System.out.println(sb.toString());
        for (ASTNode child : children) {
            child.printTree(level + 1);
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
                            matcher.appendReplacement(newProduction, "ε");
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

class CustomStack<T> {
    public List<T> stack;

    public CustomStack() {
        this.stack = new ArrayList<>();
    }

    public void push(T item) {
        stack.add(item); // 将元素添加到列表末尾
    }

    public T pop() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Stack is empty");
        }
        return stack.remove(stack.size() - 1); // 移除并返回列表末尾的元素
    }

    public T peek() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Stack is empty");
        }
        return stack.get(stack.size() - 1); // 返回列表末尾的元素，但不移除
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public List<T> getStack() {
        return Collections.unmodifiableList(stack);
    }
}