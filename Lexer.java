import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Lexer {

}

class GrammarRule {
    // Rule类用于表示语法规则
    static class Rule {
        private final String tokenName; // token名称
        private final String pattern; // 模式

        // 构造函数
        public Rule(String tokenName, String pattern) {
            this.tokenName = tokenName;
            this.pattern = pattern;
        }

        // 获取token名称
        public String getTokenName() {
            return tokenName;
        }

        // 获取用于匹配的Pattern
        public String getPattern() {
            return pattern;
        }
    }

    //用于匹配标识符和数字

    private final List<Rule> rules; // 存储多个Rule类的变量

    // 构造函数
    public GrammarRule() {
        this.rules = new ArrayList<>();
    }

    // 根据语法txt文件创建语法
    public void createRuleFromFile(String fileName) {
        Scan inputScan = new Scan("grammar.txt");
        String[] inputString = inputScan.readText();
        for (String str : inputString) {
            int colonIndex = str.indexOf(':');
            if (colonIndex != -1) {
                String newTokenName = str.substring(0, colonIndex).trim(); // 第一个字符串
                String newPattern = str.substring(colonIndex + 1).replace("'", ""); // 第二个字符串去除单引号
                Rule newRule = new Rule(newTokenName, newPattern);
                rules.add(newRule);
            }
        }
    }

    // 给Lexer提供的匹配函数
    public String[] matchToken(String text) {
        String[] token = new String[2];
        for (Rule rule : rules) {
            switch (rule.tokenName) {
                case "ID":
                    System.out.println("This is an ID");
                    break;
                default:
                    System.out.println("This is a key");
            }
        }
        return token;
    }

    // 打印所有的规则
    public void printRules() {
        for (Rule rule : rules) {
            System.out.println("Token Name: " + rule.getTokenName());
            System.out.println("Pattern: " + rule.getPattern());
        }
    }
}

/**
 * 用于处理文本文件扫描的类。
 */
class Scan {
    private String fileName; // 要扫描的文件名

    /**
     * Scan 类的构造函数。
     * 
     * @param fileName 要扫描的文件名
     */
    public Scan(String fileName) {
        this.fileName = fileName; // 初始化 fileName
    }

    /**
     * 读取文件的文本内容并以字符串形式返回。
     * 
     * @return 文件的文本内容
     */
    public String[] readText() {
        List<String> contentList = new ArrayList<>(); // 用于存储文件内容的列表
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.trim().startsWith("//")) {
                    contentList.add(line); // 将非空行且不以"//"开头的内容添加到列表中
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // 如果发生 IOException，则打印堆栈跟踪信息
        }
        return contentList.toArray(new String[0]); // 将列表转换为字符串数组并返回
    }
}

class MatchIdentifierOrNumber {
    // 定义状态枚举
    private enum State {
        START, IDENTIFIER, NUMBER1, NUMBER2, MID1, MID2, ERROR
    }

    // 当前状态
    private State currentState;

    // 构造函数，初始化当前状态为START
    public MatchIdentifierOrNumber() {
        currentState = State.START;
    }

    // 判断输入字符串是否为标识符或数字,标识符返回1，数字返回2,不接收0
    public int isIdentifierOrNumber(String input) {
        currentState = State.START;
        for (char c : input.toCharArray()) {
            currentState = nextState(currentState, c);
            if (currentState == State.ERROR) {
                break;
            }
        }
        if(currentState == State.IDENTIFIER){
            return 1;
        }else if(currentState == State.NUMBER1 || currentState == State.NUMBER2) {
            return 2;
        }else{
            return 0;
        }
    }

    // 根据当前状态和输入字符计算下一个状态
    private State nextState(State state, char inputChar) {
        switch (state) {
            case START:
                if (Character.isLetter(inputChar) || inputChar == '_') {
                    return State.IDENTIFIER;
                } else if (Character.isDigit(inputChar)) {
                    return State.NUMBER1;
                } else if (inputChar == '-'){
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
                }else if (Character.isDigit(inputChar)) {
                    return State.NUMBER1;
                } else {
                    return State.ERROR;
                }
            case NUMBER2:
                if (Character.isDigit(inputChar)) {
                    return State.NUMBER2;
                } else {
                    return State.ERROR;
                }
            case MID1: // 中间状态的逻辑
                if (Character.isDigit(inputChar)) {
                    return State.NUMBER1;
                }else {
                    return State.ERROR;
                }
            case MID2: // 中间状态的逻辑
                if (Character.isDigit(inputChar)) {
                    return State.NUMBER2;
                }else {
                    return State.ERROR;
                }
            default:
                return State.ERROR;
        }
    }
}