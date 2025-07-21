package org.main;

import java.util.List;

public class Main {

    public void fast(String inputFile) throws Exception {
//      System.out.println("Current working directory: " + System.getProperty("user.dir"));
        Lexer lexer = new Lexer("lexer_grammar.txt");
        lexer.analyze(inputFile);  // 使用命令行传入的输入文件
        if(!lexer.hasErrors()){
            Parser parser = new Parser(lexer);
            parser.analyze("parser_grammar.txt");
            if (!parser.hasErrors()) {
                SemanticsHandler semanticsHandler = new SemanticsHandler(parser.getRootNode());
                semanticsHandler.analyzeSemantics();
                semanticsHandler.printGeneratedCode();

                CFGVisualizer.visualizeToFile(semanticsHandler.getCfg(), "src/main/resources/output", "cfg.dot", "cfg.svg");
                CFGSimplifier.simplifyCFG(semanticsHandler.getCfg(), semanticsHandler.getEntryNode(), semanticsHandler.getExitNode());
                CFGVisualizer.visualizeToFile(semanticsHandler.getCfg(), "src/main/resources/output", "cfg_sim.dot", "cfg_sim.svg");


                CFG.exportToJson(semanticsHandler.getCfg(), "src/main/resources/output/cfg_output.json");
                System.out.println("CFG 导出为 JSON 文件成功！");




//                List<List<CFGAnalyzer.NodeWithConditions>> paths = CFGAnalyzer.findAllPaths(semanticsHandler.getCfg(), semanticsHandler.getEntryNode(), semanticsHandler.getExitNode());
////                CFGAnalyzer.printPaths(paths);
//                CFGAnalyzer.convertPathsToIsabelle(paths,"test");
            }
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
