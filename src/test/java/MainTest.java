import org.junit.jupiter.api.Test;
import org.main.*;

public class MainTest {

    //能正常编译的测试案例
    @Test
    public void testNormal() throws Exception {
        Main main = new Main();
        Lexer lexer = new Lexer("lexer_grammar.txt");
        lexer.analyze("input.txt");
        Parser parser = new Parser(lexer);
        parser.analyze("parser_grammar.txt");

        if (!lexer.hasErrors() && !parser.hasErrors()) {
            SemanticsHandler semanticsHandler = new SemanticsHandler(parser.getRootNode());
            semanticsHandler.analyzeSemantics();
            semanticsHandler.printGeneratedCode();
        }
    }

    //未定义标识符就引用
    @Test
    public void testInput1() throws Exception {
        Main main = new Main();
        Lexer lexer = new Lexer("lexer_grammar.txt");
        lexer.analyze("input1.txt");
        Parser parser = new Parser(lexer);
        parser.analyze("parser_grammar.txt");

        if (!lexer.hasErrors() && !parser.hasErrors()) {
            SemanticsHandler semanticsHandler = new SemanticsHandler(parser.getRootNode());
            semanticsHandler.analyzeSemantics();
            semanticsHandler.printGeneratedCode();
        }
    }

    //未写分号结尾
    @Test
    public void testInput2() throws Exception {
        Main main = new Main();
        Lexer lexer = new Lexer("lexer_grammar.txt");
        lexer.analyze("input2.txt");
        Parser parser = new Parser(lexer);
        parser.analyze("parser_grammar.txt");

        if (!lexer.hasErrors() && !parser.hasErrors()) {
            SemanticsHandler semanticsHandler = new SemanticsHandler(parser.getRootNode());
            semanticsHandler.analyzeSemantics();
            semanticsHandler.printGeneratedCode();
        }
    }

    //goto语句的参数数量缺少
    @Test
    public void testInput3() throws Exception {
        Main main = new Main();
        Lexer lexer = new Lexer("lexer_grammar.txt");
        lexer.analyze("input3.txt");
        Parser parser = new Parser(lexer);
        parser.analyze("parser_grammar.txt");

        if (!lexer.hasErrors() && !parser.hasErrors()) {
            SemanticsHandler semanticsHandler = new SemanticsHandler(parser.getRootNode());
            semanticsHandler.analyzeSemantics();
            semanticsHandler.printGeneratedCode();
        }
    }

    //表达式有误
    @Test
    public void testInput4() throws Exception {
        Main main = new Main();
        Lexer lexer = new Lexer("lexer_grammar.txt");
        lexer.analyze("input4.txt");
        Parser parser = new Parser(lexer);
        parser.analyze("parser_grammar.txt");

        if (!lexer.hasErrors() && !parser.hasErrors()) {
            SemanticsHandler semanticsHandler = new SemanticsHandler(parser.getRootNode());
            semanticsHandler.analyzeSemantics();
            semanticsHandler.printGeneratedCode();
        }
    }

    //出现非法词汇
    @Test
    public void testInput5() throws Exception {
        Main main = new Main();
        Lexer lexer = new Lexer("lexer_grammar.txt");
        lexer.analyze("input5.txt");
        Parser parser = new Parser(lexer);
        parser.analyze("parser_grammar.txt");

        if (!lexer.hasErrors() && !parser.hasErrors()) {
            SemanticsHandler semanticsHandler = new SemanticsHandler(parser.getRootNode());
            semanticsHandler.analyzeSemantics();
            semanticsHandler.printGeneratedCode();
        }
    }
}
