import java.util.*;



// ParserGrammar类定义
class ParserGrammar_k {
    public int k; //确定一次要解析多少个符号
    private Map<String, List<String[]>> productions; // 存储所有的产生式
    private Map<String, Set<List<String>>> firstkSets; // 用列表存储k个符号的序列
    private Map<String, Set<List<String>>> followkSets; // 存储每个非终结符的follow集合
    private Set<String> Terminals; // 存储终结符的集合a
    private Map<String, Map<String, String[]>> predictiveTable; // 预测分析表,映射前必须展平k个符号，提高哈希表的性能
    private String startSymbol;  // 用于记录起始符号

    /**
     * 构造函数，初始化终结符集合。需要从lexer获取所有的终结符。
     *
     * @param TerminalSymbols 终结符字符串数组。
     */
    public ParserGrammar_k(String[] TerminalSymbols,int k) {
        this.k = k;
        this.productions = new HashMap<>();
        this.firstkSets = new HashMap<>();
        this.followkSets = new HashMap<>();
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
            if(Objects.equals(rule[1], "")){  //这个是针对s+这种情况专门写的，因为s:p+,s的一个产生式中可能会什么都没有
                continue;
            }
            addProduction(rule[0], rule[1].split("\\s+"));
        }

        calculateFirstkSets();
        calculateFollowkSets();
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
     * 计算每个非终结符的FIRSTk集合。
     */
    public void calculateFirstkSets() {
        // 初始化 FIRSTk 集合
        for (String nonTerminal : productions.keySet()) {
            firstkSets.put(nonTerminal, new HashSet<>());
        }
        boolean changed = true;
        while (changed) {
            changed = false;

            // 遍历所有产生式
            for (Map.Entry<String, List<String[]>> entry : productions.entrySet()) {
                String nonTerminal = entry.getKey();

                for (String[] production : entry.getValue()) {
                    Set<List<String>> newFirstkSet = calculateFirstkForProduction(production);

                    // 将新计算出的 FIRSTk 集合合并到当前非终结符的 FIRSTk 集合中
                    if (!newFirstkSet.isEmpty() && firstkSets.get(nonTerminal).addAll(newFirstkSet)) {
                        changed = true;
                    }
                }
            }
        }
    }

    /**
     * 计算给定产生式的FIRSTk集合。
     * @param production 产生式的右侧符号序列。
     * @return 产生式的FIRSTk集合。
     */
    private Set<List<String>> calculateFirstkForProduction(String[] production) {
        Set<List<String>> currentFirstk = new HashSet<>();
        boolean nullable = true;

        for (String symbol : production) {
            if (isTerminal(symbol)) {
                // 是终结符，直接添加到当前已经计算的First集
                currentFirstk = concatenate(currentFirstk, Collections.singletonList(symbol));
                if(!symbol.equals("ε")){
                    nullable = false;
                }
            } else {
                Set<List<String>> firstkOfSymbol = firstkSets.get(symbol);
                if (firstkOfSymbol == null) {
                    firstkOfSymbol = new HashSet<>();
                }

                Set<List<String>> newFirstk = new HashSet<>();
                for (List<String> seq : firstkOfSymbol) {
                    newFirstk.addAll(concatenate(currentFirstk, seq));
                }
                currentFirstk = newFirstk;

                // 检查是否包含空串
                if (!containsEmpty(firstkOfSymbol)) {
                    nullable = false;
                }
            }
        }
        if(nullable){
            currentFirstk.add(Collections.singletonList("ε"));
        }
        return currentFirstk;
    }



    /**
     * 检查FIRSTk集合中是否包含空串。
     * @param firstkSet FIRSTk集合。
     * @return 如果包含空串，则返回true；否则返回false。
     */
    private boolean containsEmpty(Set<List<String>> firstkSet) {
        for (List<String> seq : firstkSet) {
            if (seq.contains("ε")) return true;
        }
        return false;
    }

    private Set<List<String>> concatenate(Set<List<String>> setOfLists, List<String> singleList) {
        if(singleList.size() == 1 && singleList.get(0).equals("ε")){
            return setOfLists;
        }
        Set<List<String>> result = new HashSet<>();

        for (List<String> existingList : setOfLists) {
            List<String> concatenatedList;
            if(existingList.size() == 1 && existingList.get(0).equals("ε")){
                concatenatedList = new ArrayList<>(); //能推出空串的情况下，后面拼接东西之前应当为空
            }else{
                concatenatedList = new ArrayList<>(existingList);
            }

            for (String symbol : singleList) {
                if (concatenatedList.size() < k) {
                    concatenatedList.add(symbol);
                } else {
                    break;
                }
            }

            result.add(concatenatedList);
        }

        if (setOfLists.isEmpty() && !singleList.isEmpty()) {
            List<String> truncatedList = singleList.size() <= k ? new ArrayList<>(singleList) : new ArrayList<>(singleList.subList(0, k));
            result.add(truncatedList);
        }

        return result;
    }

    /**
     * 计算每个非终结符的follow集合。
     */
    private void calculateFollowkSets() {
        // 初始化每个非终结符的FOLLOWk集合为空
        for (String nonTerminal : productions.keySet()) {
            followkSets.put(nonTerminal, new HashSet<>());
        }

        // 起始符号的FOLLOW集应包含一个特殊结束符号，这里假定为 "$"
        followkSets.get(startSymbol).add(Collections.singletonList("$"));

        boolean changed = true;
        while (changed) {
            changed = false;

            // 遍历所有产生式
            for (Map.Entry<String, List<String[]>> entry : productions.entrySet()) {
                String lhs = entry.getKey(); // 左边的非终结符

                for (String[] production : entry.getValue()) {
                    List<String> trailer = new ArrayList<>();

                    // 从右至左遍历产生式
                    for (int i = production.length - 1; i >= 0; i--) {
                        String symbol = production[i];
                        if (!isTerminal(symbol)) {
                            // 是非终结符，更新其FOLLOW集
                            Set<List<String>> followSetOfSymbol = followkSets.get(symbol);
                            //先计算trailer的first集
                            Set<List<String>> firstkSetsOfTrailer = calculateFirstkForProduction(trailer.toArray(new String[0]));
                            Set<List<String>> followSetOfLHS = followkSets.get(lhs);

                            for (List<String> lhsFollow : followSetOfLHS) {
                                Set<List<String>> concatenatedResult = concatenate(firstkSetsOfTrailer, lhsFollow);
                                if (followSetOfSymbol.addAll(concatenatedResult)) {
                                    changed = true;
                                }
                            }
                        }
                        trailer.add(0,symbol);
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
        // 初始化预测分析表
        productions.keySet().forEach(nonTerminal -> predictiveTable.put(nonTerminal, new HashMap<>()));

        // 遍历每个非终结符的产生式
        for (String nonTerminal : productions.keySet()) {
            List<String[]> productionList = productions.get(nonTerminal);
            for (String[] production : productionList) {
                Set<List<String>> firstk = calculateFirstkForProduction(production);
                Set<List<String>> followk = followkSets.get(nonTerminal);
                Set<List<String>> concatSet = new HashSet<>();

                // 遍历 followk 集合中的每个符号串
                for (List<String> followSeq : followk) {
                    // 计算 First_k(a) ⊕_k Follow_k(A)
                    concatSet.addAll(concatenate(firstk, followSeq));
                }
                for (List<String> sequence : concatSet) {
                    if (sequence.size() <= k) {
                        // 转换序列为字符串表示形式
                        String key = String.join(" ", sequence);
                        Map<String, String[]> innerMap = predictiveTable.get(nonTerminal);
                        if (!innerMap.containsKey(key)) {
                            innerMap.put(key, production);
                        } else {
                            throw new RuntimeException("该文法无法用LL" + k + "文法解析");
                        }
                    }
                }
            }
        }
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
     * 打印所有的FIRSTk集合。
     */
    public void printFirstSets() {
        int maxLineLength = 100; // 设定每行最大长度

        for (Map.Entry<String, Set<List<String>>> entry : firstkSets.entrySet()) {
            Set<String> printableSets = new HashSet<>();
            for (List<String> symbolList : entry.getValue()) {
                printableSets.add(String.join(" ", symbolList));
            }
            String completeSet = "{" + String.join(", ", printableSets) + "}";

            System.out.print("Firstk(" + entry.getKey() + ") = ");

            if (completeSet.length() > maxLineLength) {
                System.out.println(completeSet.substring(0, 1));
                completeSet = completeSet.substring(1, completeSet.length() - 1).trim();

                while (completeSet.length() > maxLineLength) {
                    int lastSpaceIndex = completeSet.substring(0, maxLineLength).lastIndexOf(", ");
                    if (lastSpaceIndex == -1) {
                        lastSpaceIndex = maxLineLength;
                    }
                    System.out.println("    " + completeSet.substring(0, lastSpaceIndex));
                    completeSet = completeSet.substring(lastSpaceIndex + 2).trim();
                }
                System.out.println("    " + completeSet);
                System.out.println("}");
            } else {
                System.out.println(completeSet);
            }
        }
    }


    /**
     * 打印所有的follow集合。
     */
    public void printFollowkSets() {
        int maxLineLength = 100;

        for (Map.Entry<String, Set<List<String>>> entry : followkSets.entrySet()) {
            Set<String> printableSets = new HashSet<>();
            for (List<String> symbolList : entry.getValue()) {
                printableSets.add(String.join(" ", symbolList));
            }
            String completeSet = "{" + String.join(", ", printableSets) + "}";

            System.out.print("Followk(" + entry.getKey() + ") = ");

            if (completeSet.length() > maxLineLength) {
                System.out.println(completeSet.substring(0, 1));
                completeSet = completeSet.substring(1, completeSet.length() - 1).trim();

                while (completeSet.length() > maxLineLength) {
                    int lastSpaceIndex = completeSet.substring(0, maxLineLength).lastIndexOf(", ");
                    if (lastSpaceIndex == -1) {
                        lastSpaceIndex = maxLineLength;
                    }
                    System.out.println("    " + completeSet.substring(0, lastSpaceIndex));
                    completeSet = completeSet.substring(lastSpaceIndex + 2).trim();
                }
                System.out.println("    " + completeSet);
                System.out.println("}");
            } else {
                System.out.println(completeSet);
            }
        }
    }
}


/**
 * Parser 类用于根据词法分析器的结果进行语法分析
 */
public class Parser_k {
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
    public Parser_k(Lexer lexer) {
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

    /*todo*/
    public void visualizeAST(){
        if (hasErrors()){
            System.out.println("\nThere is an parsing error; only the recognized AST can be printed.");
        }
        System.out.println("The value of non-terminals is null, while the value of terminals is the specific value from lexical analysis." );
        //this.rootNode.visualizeTree();
    }

    ASTNode getRootNode(){
        return rootNode;
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
