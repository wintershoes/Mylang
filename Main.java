import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
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
}