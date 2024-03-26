import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public void testScan() throws Exception {
        Scan inputScan = new Scan("grammar.txt");
        String[] inputString = inputScan.readText();
        for (String str : inputString) {
            System.out.println(str);
        }

        GrammarRule g = new GrammarRule();
        g.createRuleFromFile("grammar.txt");
        g.printRules();
        g.matchToken("null");
    }

    public void testMatch() throws Exception {
        MatchIdentifierOrNumber match = new MatchIdentifierOrNumber();

        // Test strings
        String[] testStrings = {
                "identifier", "123", "_id123", "12.3", "-123", "0", "abc-123", "123abc", "**8",
                "12.-8","12.","12.89"
        };

        for (String s : testStrings) {
            int result = match.isIdentifierOrNumber(s);
            System.out.print("Input: \"" + s + "\" is ");
            switch (result) {
                case 1:
                    System.out.println("an identifier.");
                    break;
                case 2:
                    System.out.println("a number.");
                    break;
                default:
                    System.out.println("neither an identifier nor a number.");
                    break;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Main main = new Main(); // 创建 Main 类的实例
        try {
            main.testMatch();
        } catch (Exception e) {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            e.printStackTrace();
        }
    }
}