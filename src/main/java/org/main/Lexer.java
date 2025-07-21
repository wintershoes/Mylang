package org.main;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lexer 类用于从输入文本中读取内容，并将其分割成一系列的Token。
 * <p>
 * 此类使用 GrammarRule 来匹配文本中的单词，识别出每个单词对应的Token类型。
 * 分析的结果是一个Token列表，每个Token包括类型和值。
 * </p>
 */
public class Lexer {
    GrammarRule grammarRule;
    List<Token> tokens; // 存储已经识别的token
    List<LexicalError> errors; // 用于存储错误信息
    String[] allLines;

    /**
     * 构造一个新的 Lexer 实例。
     * 初始化语法规则和Token列表。
     */
    public Lexer(String grammarFileName) {
        this.grammarRule = new GrammarRule();
        this.tokens = new ArrayList<>();
        this.errors = new ArrayList<>(); // 初始化错误列表
        grammarRule.createRuleFromFile(grammarFileName);
    }

    /**
     * 用于表示词法分析器的Token，包括token的类型、具体的值以及所在的行。
     */
    static class Token {
        public String[] lexeme;
        public int lineNumber;

        /**
         * 构造一个Token对象。
         *
         * @param lexeme     token的具体值
         * @param lineNumber token所在的行号
         */
        public Token(String[] lexeme, int lineNumber) {
            this.lexeme = lexeme;
            this.lineNumber = lineNumber;
        }

    }

    /**
     * 分析指定输入文件的内容。
     * <p>
     * 此方法首先从一个给定的语法规则文件中加载语法规则。
     * 然后，它读取输入文件的内容，并将其分割成Token，忽略每行末尾的分号。
     * 每个Token都是通过GrammarRule识别的，并添加到内部列表中。
     * </p>
     *
     * @param inputFileName 要分析的输入文件的名称。
     *
     */
    public void analyze(String inputFileName) {
        // 使用Scan类读取输入文件的内容
        Scan inputScan = new Scan(inputFileName);
        String[] inputLines = inputScan.readTextFromPath();
        allLines = inputLines;

        // 遍历每一行，进行词法分析
        for (int i = 0; i < inputLines.length; i++) {
            String line = inputLines[i]; // 获取当前行的内容
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue; // 忽略空行和纯注释行

            int commentIndex = line.indexOf("#");
            if (commentIndex >= 0) {
                // 只保留注释前的部分，同时去除注释前可能存在的多余空格
                line = line.substring(0, commentIndex).trim();
            }

            String modifiedLine = grammarRule.splitSpecialTokenValue(line);

            // 以空格分割单词
            String[] words = modifiedLine.split(" ");
            for (String word : words) {
                String sanitizedWord = word;//.replace(";", "");
                // 如果处理后的单词长度大于0，则继续匹配
                if (sanitizedWord.length() > 0) {
                    // 对每个单词使用语法规则进行匹配
                    Token currentToken = new Token(grammarRule.matchToken(sanitizedWord), i + 1);
                    if (currentToken.lexeme[0].equals("UNKNOWN")) { // 检查是否为未知Token
                        // 添加错误信息，包含行号
                        errors.add(new LexicalError(sanitizedWord ,i + 1));
                    } else {
                        Set<String> set = new HashSet<>(Arrays.asList("FORWARD", "BACKWARD", "TURNRIGHT","TURNLEFT","LOOKUP",
                                "LOOKDOWN","LOOKLEFT","LOOKRIGHT","GOTO","PERCEIVE","APPROACH","GRASP"));
                        if(set.contains(currentToken.lexeme[1])){
                            errors.add(new LexicalError(sanitizedWord ,i + 1));
                        }
                    }

                    tokens.add(currentToken);
                }
            }
        }
        Token endToken = new Token(new String[]{"$", "$"}, inputLines.length + 1);
        tokens.add(endToken);
        //加入一个结束标记，以便后续的语法分析
        ErrorHandler.handleError(errors);
    }

    /**
     * 获取所有识别的Token。
     *
     * @return 一个包含识别Token的列表。每个Token为一个字符串数组，包括类型和值。
     */
    public List<Token> getTokens() {
        return tokens;
    }

    /**
     * 获取一个迭代器，用于迭代tokens。
     *
     * @return 用于迭代tokens的Iterator对象
     */
    public TokenIterator getTokenIterator() {
        return new TokenIterator();
    }

    /**
     * 内部Token迭代器，用于迭代tokens。
     */
    public class TokenIterator implements Iterator<Token> {
        private int currentIndex; // 当前迭代的索引位置

        /**
         * 构造一个TokenIterator对象。
         */
        public TokenIterator() {
            this.currentIndex = 0;
        }

        public int getcurrentIndex(){
            return  currentIndex;
        }

        public void backtrack(int backIndex){
            currentIndex = backIndex;
        }

        public Token lookbackK(int k){
            if(currentIndex - k - 1 >= 0){
                return tokens.get(currentIndex - k - 1);
            }else{
                throw new RuntimeException("越界");
            }
        }

        public Token lookaheadK(int k){
            if(currentIndex + k - 1 < tokens.size()){
                return tokens.get(currentIndex + k - 1);
            }else{
                throw new RuntimeException("越界");
            }
        }

        /**
         * 检查是否还有下一个Token。
         *
         * @return 如果还有下一个Token，则返回true；否则返回false。
         */
        @Override
        public boolean hasNext() {
            return currentIndex < tokens.size();
        }

        /**
         * 获取下一个Token。
         *
         * @return 下一个Token对象
         * @throws NoSuchElementException 如果没有更多的Token时抛出异常
         */
        @Override
        public Token next() {
            if (!hasNext()) {
                throw new RuntimeException("token访问越界");
            }
            return tokens.get(currentIndex++);
        }


    }

    /**
     * 打印所有识别的Token到标准输出。
     * <p>
     * 每个Token的类型和值将被格式化输出。
     * </p>
     */
    public void printTokens() {
        Iterator<Token> iterator = getTokenIterator();
        while (iterator.hasNext()) {
            Token currentToken = iterator.next();
            System.out.println("Token Type: " + currentToken.lexeme[0] + ", Value: " + currentToken.lexeme[1] + ", Line: " + currentToken.lineNumber);
        }
    }

    /**
     * 获取所有的终结符（与下一层语法分析器的沟通）
     */
    public String[] getAllTerminals(){
        return this.grammarRule.getAllTokenNames();
    }

    /**
     * 判断当前是否存在错误。
     * <p>
     * 该方法检查在词法分析过程中是否遇到了错误。如果存在一个或多个错误，
     * 方法将返回{@code true}；否则返回{@code false}。
     *
     * @return {@code true} 如果存在错误，否则 {@code false}。
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    // 词法错误类,用来处理语法错误
    class LexicalError extends CompilationError {
        static int noteTimes = 0;
        String message;
        int line;
        public LexicalError(String Message, int line) {
            this.message = Message;
            this.line = line;
        }

        @Override
        public boolean handle() {
            System.err.println("At line " + line + ", token " + message +":");
//            System.out.println("请不要在后续生成的代码里使用这个不符合词法的词汇");

            Set<String> set = new HashSet<>(Arrays.asList("FORWARD", "BACKWARD", "TURNRIGHT","TURNLEFT","LOOKUP",
                    "LOOKDOWN","LOOKLEFT","LOOKRIGHT","GOTO","PERCEIVE","APPROACH","GRASP"));
            if(set.contains(message)){
                System.err.println("keywords should be lower case.");
            }

            // 判断是否可能在尝试写一个标识符或者数字
            Pattern pattern = Pattern.compile("[^a-zA-Z0-9_.]");
            Matcher matcher = pattern.matcher(message);
            if(matcher.find() ){ //写了一个非法的字符
//              noteTimes += 1;
                String illegalChar = matcher.group();
                System.err.println("The " + illegalChar + " is an illegal character.");

//                if(noteTimes <= 1){
//                    System.out.println("后续的代码里只能使用以下正确的符号：");
//                    for(GrammarRule.Rule charac :grammarRule.getAllSpecialTokens()){
//                        System.out.println(charac.getTokenName() + " : " + charac.getPattern());
//                    }
//                }
            }else{
                pattern = Pattern.compile("\\d+(\\.\\d+)*");
                matcher = pattern.matcher(message);

                if (matcher.matches()) {
                    System.err.println("The number is illegal.");
                } else {
                    System.err.println("The identifier is illegal.");
                }
            }
            System.err.println();

            return true;
        }

    }

}

/**
 * <p>包含多个语法规则的类，每个规则由一个Token名称和一个匹配模式组成。</p>
 */
class GrammarRule {
    /**
     * 语法规则内部类，表示单个的语法规则。
     */
    static class Rule {
        private final String tokenName; // token名称
        private final String pattern; // 匹配模式

        /**
         * 构造函数，创建一个新的语法规则。
         *
         * @param tokenName 规则对应的Token名称。
         * @param pattern   用于匹配Token的模式。
         */
        public Rule(String tokenName, String pattern) {
            this.tokenName = tokenName;
            this.pattern = pattern;
        }

        /**
         * 获取Token名称。
         *
         * @return Token名称字符串。
         */
        public String getTokenName() {
            return tokenName;
        }

        /**
         * 获取匹配模式字符串。
         *
         * @return 匹配模式字符串。
         */
        public String getPattern() {
            return pattern;
        }
    }

    // 存储多个Rule对象的列表。
    private final List<Rule> rules;
    //存储特殊标点符号，也就是lexer语法文件里SYMBOL开头的符号
    private final List<Rule> specialTokens;

    /**
     * 构造函数，初始化语法规则列表。
     */
    public GrammarRule() {
        this.rules = new ArrayList<>();
        this.specialTokens = new ArrayList<>();
    }

    /**
     * 从指定的文本文件加载语法规则。
     *
     * @param fileName 包含语法规则的文本文件名。
     */
    public void createRuleFromFile(String fileName) {
        Scan inputScan = new Scan(fileName);
        String[] inputString = inputScan.readText();
        StringBuilder currentStr = new StringBuilder();

        for (String str : inputString) {
            if (str.trim().isEmpty() || str.trim().startsWith("#")) continue;

            currentStr.append(str);
            if (currentStr.indexOf(";") >= 0) {
                int colonIndex = currentStr.indexOf(":");
                if (colonIndex != -1) {
                    // 找到冒号后的第一个单引号的位置
                    int startQuoteIndex = currentStr.indexOf("'", colonIndex + 1);
                    // 找到下一个单引号的位置
                    int endQuoteIndex = currentStr.indexOf("'", startQuoteIndex + 1);
                    // 确保两个单引号都找到了,单引号之间的内容是匹配规则
                    if (startQuoteIndex != -1 && endQuoteIndex != -1) {
                        String newTokenName = currentStr.substring(0, colonIndex).trim(); // 获取冒号前的token名称
                        // 提取两个单引号之间的字符串作为新的模式
                        String newPattern = currentStr.substring(startQuoteIndex + 1, endQuoteIndex);
                        //特殊字符单独存储
                        if(newTokenName.contains("SYMBOL")){
                            Rule newRule = new Rule(newTokenName.replace("SYMBOL","").trim(), newPattern);
                            specialTokens.add(newRule);
                        }else{
                            Rule newRule = new Rule(newTokenName, newPattern);
                            rules.add(newRule);
                        }
                    }else{
                        throw new RuntimeException("词法文件编写有误,匹配模式应当用两个单引号围住，请修改!");
                    }
                } else {
                    throw new RuntimeException("词法文件编写有误,每条词法规则应当由冒号':'连接，请修改!");
                }
                currentStr.setLength(0);
            }

        }
    }
    /**
     * 根据定义的specialTokens，对输入的一行进行预处理
     *
     * @param value 需要用特殊符号分割的字符串
     */
    public String splitSpecialTokenValue(String value) {
        // 先将所有的特殊符号排序
        List<Rule> sortedSpecialTokens = new ArrayList<>(specialTokens);
        sortedSpecialTokens.sort((a, b) -> b.getPattern().length() - a.getPattern().length());

        // 构建特殊字符的正则表达式，确保长的（如 "!="）优先被匹配
        StringBuilder regexBuilder = new StringBuilder();
        for (Rule token : sortedSpecialTokens) {
            if (regexBuilder.length() > 0) {
                regexBuilder.append("|");
            }
            regexBuilder.append(Pattern.quote(token.getPattern()));
        }
        String regex = regexBuilder.toString();

        // 编译正则表达式
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);

        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            // 添加前面的部分
            if (matcher.start() > lastEnd) {
                sb.append(value, lastEnd, matcher.start());
            }
            // 添加匹配到的特殊字符，前后加空格
            sb.append(" ").append(matcher.group()).append(" ");
            lastEnd = matcher.end();
        }
        // 添加最后的部分
        if (lastEnd < value.length()) {
            sb.append(value.substring(lastEnd));
        }

        return sb.toString();
    }

    /**
     * 根据定义的规则匹配输入文本中的Token。
     *
     * @param word 输入文本。
     * @return 与文本匹配的Token数组。
     */
    public String[] matchToken(String word) {
        // 优先匹配关键字
        for (Rule rule : rules) {
            if (word.equals(rule.getPattern())) {
                // 如果找到匹配，返回规则的tokenName和单词
                return new String[]{rule.getTokenName(), word};
            }
        }

        // 然后匹配特殊符号
        for (Rule rule : specialTokens) {
            if (word.equals(rule.getPattern())) {
                // 如果找到匹配，返回规则的tokenName和单词
                return new String[]{rule.getTokenName(), word};
            }
        }

        // 最后再检查是否为标识符或数字
        MatchIdentifierOrNumber identifierOrNumberMatcher = new MatchIdentifierOrNumber();
        int result = identifierOrNumberMatcher.isIdentifierOrNumber(word);
        String tokenType;
        switch (result) {
            case 1:
                tokenType = "ID";  //IDENTIFIER
                break;
            case 2:
                tokenType = "NUMBER";
                break;
            default:
                tokenType = "UNKNOWN";
                break;
        }

        return new String[]{tokenType, word};
    }

    /**
     * 打印出所有的语法规则。
     */
    public void printRules() {
        for (Rule rule : rules) {
            System.out.println("Token Name: " + rule.getTokenName());
            System.out.println("Pattern: " + rule.getPattern());
        }

    }

    /**
     * 返回所有rules和specialTokens组成的String[]。
     *
     * @return 包含所有规则的String数组。
     */
    public String[] getAllTokenNames() {
        List<String> tokenNames = new ArrayList<>();

        // 遍历rules列表，添加每个Rule的tokenName到列表中
        for (Rule rule : rules) {
            tokenNames.add(rule.getTokenName());
        }
        // 遍历specialTokens列表，也添加每个Rule的tokenName到列表中
        for (Rule token : specialTokens) {
            tokenNames.add(token.getTokenName());
        }
        tokenNames.add("ID");
        tokenNames.add("NUMBER");
        tokenNames.add("ε");  //引入空串终结符

        // 将List转换为String数组并返回
        return tokenNames.toArray(new String[0]);
    }

    public List<Rule> getAllSpecialTokens() {
        List<Rule> SpecialTokensList = new ArrayList<>();

        SpecialTokensList.addAll(specialTokens);

        return SpecialTokensList;
    }

}



/**
 * MatchIdentifierOrNumber 类使用有限状态自动机（DFA）来判断字符串是否为合法的标识符或数字。
 */
class MatchIdentifierOrNumber {
    /**
     * 状态枚举，定义了DFA中的所有状态。
     */
    private enum State {
        START, IDENTIFIER, NUMBER1, NUMBER2, MID1, MID2, ERROR
    }

    // 当前状态变量，初始状态为START。
    private State currentState;

    /**
     * 构造函数，初始化当前状态为START。
     */
    public MatchIdentifierOrNumber() {
        currentState = State.START;
    }

    /**
     * 根据输入的字符串判断其是否为标识符或数字。
     * 标识符返回1，数字返回2，如果既不是标识符也不是数字则返回0。
     *
     * @param input 要判断的字符串
     * @return 返回判断结果，1表示标识符，2表示数字，0表示错误或不接受的输入。
     */
    public int isIdentifierOrNumber(String input) {
        currentState = State.START;
        for (char c : input.toCharArray()) {
            currentState = nextState(currentState, c);
            if (currentState == State.ERROR) {
                break;
            }
        }
        if (currentState == State.IDENTIFIER) {
            return 1;
        } else if (currentState == State.NUMBER1 || currentState == State.NUMBER2) {
            return 2;
        } else {
            return 0;
        }
    }

    /**
     * 根据当前状态和输入字符确定DFA的下一个状态。
     *
     * @param state 当前状态
     * @param inputChar 输入字符
     * @return 返回下一个状态
     */
    private State nextState(State state, char inputChar) {
        switch (state) {
            case START:
                if (Character.isLetter(inputChar) || inputChar == '_') {
                    return State.IDENTIFIER;
                } else if (Character.isDigit(inputChar)) {
                    return State.NUMBER1;
                } else if (inputChar == '-') {
                    return State.MID1;
                } else {
                    return State.ERROR;
                }
            case IDENTIFIER:
                if (Character.isLetterOrDigit(inputChar) || inputChar == '_') {
                    return State.IDENTIFIER;
                } else {
                    return State.ERROR;
                }
            case NUMBER1:
                if (inputChar == '.') {
                    return State.MID2;
                } else if (Character.isDigit(inputChar)) {
                    return State.NUMBER1;
                } else {
                    return State.ERROR;
                }
            case NUMBER2, MID2:
                if (Character.isDigit(inputChar)) {
                    return State.NUMBER2;
                } else {
                    return State.ERROR;
                }
            case MID1:
                if (Character.isDigit(inputChar)) {
                    return State.NUMBER1;
                } else {
                    return State.ERROR;
                }
            default:
                return State.ERROR;
        }
    }
}

