import java.io.IOException;
import java.util.Scanner;


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
                "identifier__", "123", "_id123", "12.3", "-123", "0", "abc-123", "123abc", "**8",
                "12.-8", "12.", "12.89", "12.8.9","abc.cd_"
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

    public void testLexer(String grammarFileName,String inputFileName) throws Exception {
        Lexer lexer = new Lexer();

        // 调用analyze方法，输入语法规则文件名和要分析的文本文件名
        lexer.analyze(grammarFileName,inputFileName);
        lexer.printErrors();

        // 打印所有识别的Token
        lexer.printTokens();
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in); // 创建 Scanner 实例以读取输入

        System.out.println("Enter the name of the grammar rules file:");
        String grammarFileName = scanner.nextLine(); // 读取语法规则文件名

        System.out.println("Enter the name of the input file to analyze:");
        String inputFileName = scanner.nextLine(); // 读取要分析的输入文件名

        Main main = new Main(); // 创建 Main 类的实例
        try {
            System.out.println("Enter the test number:\n" +
                    "1.testScan: This test will verify whether the syntax rules are correctly read.\n" +
                    "2.testMatch: This test will provide a series of strings to identify whether they are identifiers or numbers.\n" +
                    "3.testLexer: This test will analyze the lexical results of the input file and print error messages.");
            int testNumber = 3;
            if(scanner.hasNextInt()) {
                testNumber = scanner.nextInt();
            }
            switch (testNumber){
                case 1:
                    main.testScan();
                    break;
                case 2:
                    main.testMatch();
                    break;
                case 3:
                    main.testLexer(grammarFileName,inputFileName);
                    break;
                default:
                    break;
            }

        } catch (Exception e) {
            System.out.println("error");
            e.printStackTrace();
        }
    }
}