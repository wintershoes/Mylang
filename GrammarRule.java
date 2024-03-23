import java.util.ArrayList;
import java.util.List;

class GrammarRule {
    // Rule类用于表示语法规则
    class Rule {
        private String tokenName; // token名称
        private String pattern; // 模式

        // 构造函数
        public Rule(String tokenName, String pattern) {
            this.tokenName = tokenName;
            this.pattern = pattern;
        }

        // 获取token名称
        public String getTokenName() {
            return tokenName;
        }

        // 获取模式
        public String getPattern() {
            return pattern;
        }
    }

    private List<Rule> rules; // 存储多个Rule类的变量

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