package org.main;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//将需要定义的语义动作写在这个类里
//没有被定义语义动作的符号会运行defaultVisit函数,加进了ifAggregate集合里的节点出来访问孩子还会自动从子节点获取已经生成的代码,可以用visitChildren来访问孩子
//每个节点都有专门的变量用来存储生成的代码,可以使用aggregateCodeFromChildren从孩子节点获取代码
public class SemanticActions {
    private Map<String, Object> config;
    public Set<String> ifAggregate;

    public SemanticActions() {
        ifAggregate = new HashSet<>();
        ifAggregate.addAll(List.of(new String[]{"ifCase" , "elseCase" , "condstatement" , "statement" ,"command",
        "loopstatement" , "switchCase" , "defaultCase"
        }));

        loadConfig();
    }

    private void loadConfig() {
        try {
            // 使用 ClassLoader 读取 resources 中的 JSON 配置文件
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream("input/TargetCode.json");

            if (inputStream == null) {
                throw new IllegalArgumentException("配置文件未找到！");
            }

            // 使用 Jackson 解析 JSON 文件
            ObjectMapper objectMapper = new ObjectMapper();
            config = objectMapper.readValue(inputStream, Map.class);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void defaultVisit(ExtendedASTNode node) {
        visitChildren(node);
    }

    public void aggregateVisit(ExtendedASTNode node) {
        visitChildren(node);
        node.aggregateCodeFromChildren();
    }

    public void visitChildren(ExtendedASTNode node) {
        for (ExtendedASTNode child : node.getChildren()) {
            SemanticsHandler.visitNode(child);
        }
    }

    public void visit_program(ExtendedASTNode node) {
//        node.appendCodeLine("import time");
//        node.appendCodeLine("from Rosmaster_Lib import Rosmaster");
//        node.newLine();
//        node.appendCodeLine("bot = Rosmaster()");

        // 从 config 中获取 program_code 的代码列表
        applyCodeFromConfig(node, "program_code");

        visitChildren(node);
        node.aggregateCodeFromChildren();
        node.appendCodeLine("");
        node.appendCodeLine("rospy.spin()");
    }

    public void visit_perceiveCommand(ExtendedASTNode node) {

        applyCodeFromConfig(node, "perceiveCommand");
    }

    public void visit_forwardCommand(ExtendedASTNode node) {
        String number = node.getChildrenByType("NUMBER").getValue();
        applyCodeFromConfig(node, "forwardCommand", "NUMBER", number);
    }

    public void visit_backwardCommand(ExtendedASTNode node) {
        String number = node.getChildrenByType("NUMBER").getValue();
        applyCodeFromConfig(node, "backwardCommand", "NUMBER", number);
    }

    public void visit_turnrightCommand(ExtendedASTNode node) {
        String number = node.getChildrenByType("NUMBER").getValue();
        applyCodeFromConfig(node, "turnrightCommand", "NUMBER", number);
    }

    public void visit_turnleftCommand(ExtendedASTNode node) {
        String number = node.getChildrenByType("NUMBER").getValue();
        applyCodeFromConfig(node, "turnleftCommand", "NUMBER", number);
    }

    public void visit_gotoCommand(ExtendedASTNode node) {
        String number0 = node.getChildrenByType("NUMBER", 0).getValue();
        String number1 = node.getChildrenByType("NUMBER", 1).getValue();
        applyCodeFromConfig(node, "gotoCommand", new String[]{"NUMBER_0", "NUMBER_1"}, new String[]{number0, number1});
    }

    public void visit_approachCommand(ExtendedASTNode node) {
        String id = node.getChildrenByType("ID").getValue();
        applyCodeFromConfig(node, "approachCommand", "ID", id);
    }

    public void visit_graspCommand(ExtendedASTNode node) {
        String id = node.getChildrenByType("ID").getValue();
        applyCodeFromConfig(node, "graspCommand", "ID", id);
    }

    public void visit_backCommand(ExtendedASTNode node) {
        applyCodeFromConfig(node, "backCommand");
    }

    // 用于从 config 中读取代码并应用
    private void applyCodeFromConfig(ExtendedASTNode node, String commandKey, String placeholder, String value) {
        List<String> commandLines = (List<String>) config.get(commandKey);
        if (commandLines != null) {
            for (String line : commandLines) {
                line = line.replace("{" + placeholder + "}", value);

                if (line.startsWith("*")) {
                    if(ExtendedASTNode.getGlobalAttribute(line) == null){
                        node.appendCodeLine(line.substring(1));
                        ExtendedASTNode.setGlobalAttribute(line, true);
                    }
                }else{
                    node.appendCodeLine(line);
                }
            }
        }
    }

    // 如果有多个替换项，例如多个占位符
    private void applyCodeFromConfig(ExtendedASTNode node, String commandKey, String[] placeholders, String[] values) {
        List<String> commandLines = (List<String>) config.get(commandKey);
        if (commandLines != null) {
            for (String line : commandLines) {
                for (int i = 0; i < placeholders.length; i++) {
                    line = line.replace("{" + placeholders[i] + "}", values[i]);
                }

                if (line.startsWith("*")) {
                    if(ExtendedASTNode.getGlobalAttribute(line) == null){
                        node.appendCodeLine(line.substring(1));
                        ExtendedASTNode.setGlobalAttribute(line, true);
                    }
                }else{
                    node.appendCodeLine(line);
                }
            }
        }
    }

    // 无需替换占位符时的通用方法
    private void applyCodeFromConfig(ExtendedASTNode node, String commandKey) {
        List<String> commandLines = (List<String>) config.get(commandKey);
        if (commandLines != null) {
            for (String line : commandLines) {
                if (line.startsWith("*")) {
                    if(ExtendedASTNode.getGlobalAttribute(line) == null){
                        node.appendCodeLine(line.substring(1));
                        ExtendedASTNode.setGlobalAttribute(line, true);
                    }
                }else{
                    node.appendCodeLine(line);
                }
            }
        }
    }


}

class MultiException extends Exception {
    private List<Exception> causes;

    public MultiException(String message, List<Exception> causes) {
        super(message);
        this.causes = causes;
    }

    public List<Exception> getCauses() {
        return causes;
    }
}