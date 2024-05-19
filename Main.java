import java.io.IOException;
import java.util.Scanner;


public class Main {
    public void fast() throws Exception {
        Lexer lexer = new Lexer();
        lexer.analyze("input/lexer_grammar.txt", "input.txt");
        lexer.printErrors();
        Parser parser = new Parser(lexer);
        parser.analyze("input/test_grammar.txt");
        parser.printErrors();
        //parser.printAST();
        SemanticsHandler semanticsHandler = new SemanticsHandler(parser.getRootNode());
        semanticsHandler.printAST();
        semanticsHandler.analyzeSemantics();
        semanticsHandler.printGeneratedCode();
    }

    public void testScan() throws Exception {
        Scan inputScan = new Scan("input/lexer_grammar.txt");
        String[] inputString = inputScan.readText();
        for (String str : inputString) {
            System.out.println(str);
        }

        GrammarRule g = new GrammarRule();
        g.createRuleFromFile("input/lexer_grammar.txt");
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

    public void testParserGrammar(String grammarFileName) throws Exception {
        Lexer lexer = new Lexer();
        // 调用analyze方法，输入语法规则文件名和要分析的文本文件名
        lexer.analyze("input/lexer_grammar.txt","input.txt");
        ParserGrammar g = new ParserGrammar(lexer.getAllTerminals());
        g.loadGrammarFromFile(grammarFileName);
        g.printProductions();
        System.out.println("\n");
        g.printFirstSets();
        System.out.println("\n");
        g.printPredictiveParsingTable();
    }

    public void testParser(String grammarFileName) throws Exception {
        Scanner scanner = new Scanner(System.in);

        // 提示用户输入一个数字选择对应的输入文件
        System.out.println("Enter a number between 1 and 8 to choose the input file:");
        int fileNumber = scanner.nextInt();

        // 根据输入选择文件名
        String inputFileName = "input" + fileNumber + ".txt";
        Lexer lexer = new Lexer();

        // 调用analyze方法，输入语法规则文件名和要分析的文本文件名
        lexer.analyze("input/lexer_grammar.txt", inputFileName);
        lexer.printErrors();
        Parser parser = new Parser(lexer);
        parser.analyze(grammarFileName);
        parser.printErrors();
        parser.printAST();

        scanner.close();  // 关闭Scanner
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in); // 创建 Scanner 实例以读取输入
        Main main = new Main(); // 创建 Main 类的实例

        System.out.println("Enter 1 to access the quick test mode, enter any other character to access specific test content.");
        int input = scanner.nextInt();
        scanner.nextLine();
        if (input == 1) {
            try {
                main.fast();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }else{
            try {
                System.out.println("Enter the test number:\n" +
                        "1.testScan: This test will verify whether the syntax rules are correctly read.\n" +
                        "2.testMatch: This test will provide a series of strings to identify whether they are identifiers or numbers.\n" +
                        "3.testLexer: This test will analyze the lexical results of the input file and print error messages.\n" +
                        "4.testParserGrammar: This test will read the grammar file and print the productions, first sets and the constructed predictive parsing table.\n"+
                        "5.testParser : This test will involve syntax analysis and will print errors as well as the generated syntax tree.");
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
                        main.testLexer("input/lexer_grammar.txt","input.txt");
                        break;
                    case 4:
                        main.testParserGrammar("input/parser_grammar.txt");
                        break;
                    case 5:
                        main.testParser("input/parser_grammar.txt");
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
}