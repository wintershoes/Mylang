package org.main;

public class Main {
    public void fast() throws Exception {
        Lexer lexer = new Lexer("lexer_grammar.txt");
        lexer.analyze("input.txt");
        Parser parser = new Parser(lexer);
        parser.analyze("parser_grammar.txt");
        if(!parser.hasErrors()){
            SemanticsHandler semanticsHandler = new SemanticsHandler(parser.getRootNode());
            semanticsHandler.analyzeSemantics();
            semanticsHandler.printGeneratedCode();
        }
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.fast();
    }
}