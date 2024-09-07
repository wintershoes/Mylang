package org.main;

public class Main {

    public void fast(String inputFile) throws Exception {
//        System.out.println("Current working directory: " + System.getProperty("user.dir"));
        Lexer lexer = new Lexer("lexer_grammar.txt");
        lexer.analyze(inputFile);  // 使用命令行传入的输入文件
        Parser parser = new Parser(lexer);
        parser.analyze("parser_grammar.txt");
        if (!lexer.hasErrors() & !parser.hasErrors()) {
            SemanticsHandler semanticsHandler = new SemanticsHandler(parser.getRootNode());
            semanticsHandler.analyzeSemantics();
            semanticsHandler.printGeneratedCode();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Please provide the input file as a command line argument.");
            System.exit(1);
        }

        String inputFile = args[0];  // 获取命令行参数
        Main main = new Main();
        main.fast(inputFile);
    }
}
