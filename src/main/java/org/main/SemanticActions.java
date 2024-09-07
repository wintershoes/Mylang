package org.main;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//将需要定义的语义动作写在这个类里
//没有被定义语义动作的符号会运行defaultVisit函数,加进了ifAggregate集合里的节点出来访问孩子还会自动从子节点获取已经生成的代码,可以用visitChildren来访问孩子
//每个节点都有专门的变量用来存储生成的代码,可以使用aggregateCodeFromChildren从孩子节点获取代码
public class SemanticActions {
    public Set<String> ifAggregate;

    public SemanticActions() {
        ifAggregate = new HashSet<>();
        ifAggregate.addAll(List.of(new String[]{"ifCase" , "elseCase" , "condstatement" , "statement" ,"command",
        "loopstatement" , "switchCase" , "defaultCase"
        }));
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
        node.appendCodeLine("# python demo program");
        node.appendCodeLine("import sys");
        node.appendCodeLine("sys.path.insert(0, '/home/chenzhd/Project/Robot/src/tiago_demo/scripts')");
        node.appendCodeLine("import rospy");
        node.appendCodeLine("from hand import RobotHand");
        node.appendCodeLine("from head import RobotHead");
        node.appendCodeLine("from locate import RobotLocate");
        node.appendCodeLine("from move import RobotNav");
        node.appendCodeLine("from pick import RobotRecognition");
        node.appendCodeLine("from slam import RobotSLAM");
        node.appendCodeLine("from wheel import RobotWheel\n");
        node.appendCodeLine("rospy.init_node('robot', anonymous=True)");
        visitChildren(node);
        node.aggregateCodeFromChildren();
    }

//    public void visit_assignstatement(ExtendedASTNode node){
//        visitChildren(node);
//        ExtendedASTNode IdNode = node.getChildrenByType("ID");
//        ExtendedASTNode MathExpNode = node.getChildrenByType("mathExp");
//        SymbolInfo info = null;
//        if(ExtendedASTNode.getSymbol(IdNode.getChildrenByType("ID").getValue()) == null){
//            info = ExtendedASTNode.addSymbol(node.getChildrenByType("ID").getValue());
//        }
//        info.setAdditionalAttributes(MathExpNode.getAttribute("mathExpVal"));
//    }
//
//    public void visit_whileStatement(ExtendedASTNode node) {
//        // 获取布尔表达式节点
//        ExtendedASTNode boolExpNode = node.getChildrenByType("boolExp");
//
//        if (boolExpNode.isError()) {
//            node.appendCodeLine("Error: Invalid boolean expression in while loop.");
//            return;
//        }
//        node.increaseIndent();  // 增加缩进，适应 Python 的缩进语法
//        visitChildren(node);
//        node.decreaseIndent();  // 循环结束后减少缩进
//
//        // 首先生成 while 循环头部，boolExpNode.getCode() 应当包含该布尔表达式生成的代码
//        node.appendCodeLine("while " + boolExpNode.getCode().replace("\t","") + ":");
//
//        // 访问循环体内的命令
//
//        // 获取命令代码
//        int i = 0;
//        ExtendedASTNode commandNode = node.getChildrenByType("command",i);
//        while(!commandNode.isError()){
//            node.appendCodeLine(commandNode.getCode());
//            commandNode = node.getChildrenByType("command",++i);
//        }
//
//
//    }
//
//    public void visit_forStatement(ExtendedASTNode node) {
//        // 获取起始值和结束值
//        ExtendedASTNode startNode = node.getChildrenByType("NUMBER", 0);
//        ExtendedASTNode endNode = node.getChildrenByType("NUMBER", 1);
//
//        if (startNode.isError() || endNode.isError()) {
//            node.appendCodeLine("Error: Invalid loop bounds.");
//            return;
//        }
//
//        String startValue = startNode.getValue();
//        String endValue = endNode.getValue();
//
//        // 生成 Python 的 for 循环语句
//        node.appendCodeLine("for i in range(" + startValue + ", " + (Integer.parseInt(endValue) + 1) + "):");
//        node.increaseIndent();  // 增加缩进，以适应Python的缩进语法
//
//        // 访问循环体内的命令
//        visitChildren(node);
//
//        // 聚合子节点生成的代码
//        node.aggregateCodeFromChildren();
//
//        node.decreaseIndent();  // 循环结束后减少缩进
//    }
//
//    public void visit_switchStatement(ExtendedASTNode node) {
//        visitChildren(node);
//        double mathExpVal = (double) node.getChildrenByType("mathExp").getAttribute("mathExpVal");
//        int i = 0;
//        boolean ifPass = true;
//        ExtendedASTNode caseNode = node.getChildrenByType("switchCase",i);
//        while(!caseNode.isError()){
//            if(mathExpVal == Double.parseDouble(caseNode.getChildrenByType("NUMBER").getValue())){
//                node.appendCode(caseNode.getCode());
//                ifPass = false;
//                break;
//            }
//            i++;
//            caseNode = node.getChildrenByType("switchCase",i);
//        }
//
//        caseNode = node.getChildrenByType("defaultCase");
//        if(!caseNode.isError() && ifPass){
//            node.appendCode(caseNode.getCode());
//        }
//    }
//
//    public void visit_ifStatement(ExtendedASTNode node) {
//        try{
//            visitChildren(node);
//        }catch (Exception e){
//            int a = 0;
//        }
//
//        if((boolean) node.getChildrenByType("boolExp").getAttribute("boolExpVal")){
//            node.appendCode(node.getChildrenByType("ifCase").getCode());
//        }else{
//            ExtendedASTNode elseNode = node.getChildrenByType("elseCase");
//            if(!elseNode.isError()){
//                node.appendCode(node.getChildrenByType("elseCase").getCode());
//            }
//        }
//    }
//
//    public void visit_mathExp(ExtendedASTNode node) throws MultiException {
//        StringBuilder mathExp = new StringBuilder();
//        // 访问子节点以收集必要的数据
//        visitChildren(node);
//
//        // 获取左右操作数和操作符
//        ExtendedASTNode leftOperand = node.getChildren().get(1);  // 第一个操作数，跳过左括号
//        ExtendedASTNode operator = node.getChildren().get(2).getChildren().get(0);     // 操作符
//        ExtendedASTNode rightOperand = node.getChildren().get(3); // 第二个操作数
//        mathExp.append("( ").append(leftOperand.getValue()).append(" ").append(operator.getValue()).append(" ")
//                .append(rightOperand.getValue()).append(" )");
//
//
//        List<Exception> exceptions = new ArrayList<>();
//        double leftValue = 0;
//        double rightValue = 0;
//        try {
//            if (leftOperand != null) {
//                leftValue = getValueFromOperand(leftOperand);
//            }
//        } catch (Exception e) {
//            exceptions.add(e);
//        }
//
//        try {
//            if (rightOperand != null) {
//                rightValue = getValueFromOperand(rightOperand);
//            }
//        } catch (Exception e) {
//            exceptions.add(e);
//        }
//
//        if (!exceptions.isEmpty()) {
//            node.setAttribute("mathExpVal", 0.0); //返回一个任意结果
//            throw new MultiException("Multiple errors occurred", exceptions);
//        }
//
//        // 计算结果
//        double result = 0;
//        switch (operator.getType()) {
//            case "ADD":
//                result = leftValue + rightValue;
//                break;
//            case "SUBSTRACT":
//                result = leftValue - rightValue;
//                break;
//            case "MUTIPLE":
//                result = leftValue * rightValue;
//                break;
//            case "DIVIDE":
//                if (rightValue != 0) {
//                    result = leftValue / rightValue;
//                } else {
//                    node.appendCodeLine("Error: Division by zero.");
//                    return;
//                }
//                break;
//            default:
//                node.appendCodeLine("Error: Unsupported operation.");
//                return;
//        }
//        node.setAttribute("mathExpVal",result);
//        node.appendCode(mathExp.toString());
//    }
//
//    public void visit_boolExp(ExtendedASTNode node) throws MultiException {
//        StringBuilder boolExp = new StringBuilder();
//        boolean ifUnary = false;
//        visitChildren(node);
//
//        // 获取子节点列表
//        List<ExtendedASTNode> children = node.getChildren();
//        ExtendedASTNode operator = null, leftOperand = null, rightOperand = null;
//
//
//        // 解析不同结构的布尔表达式
//        if (children.size() == 5) {
//            // Structure: LPAREN (mathExp | NUMBER | ID) op (mathExp | NUMBER | ID) RPAREN
//            leftOperand = children.get(1);
//            operator = children.get(2).getChildren().get(0);
//            rightOperand = children.get(3);
//            boolExp.append("( ").append(leftOperand.getType().equals("mathExp")?leftOperand.getCode():leftOperand.getValue()).
//                    append(" ").append(operator.getValue())
//                    .append(" ").append(rightOperand.getType().equals("mathExp")?rightOperand.getCode():rightOperand.getValue()).
//                    append(" )");
//        } else if (children.size() == 4) {
//            // Structure: LPAREN unaryOp boolExp RPAREN
//            operator = children.get(1).getChildren().get(0);
//            rightOperand = children.get(2);
//            ifUnary = true;
//            boolExp.append("( ").append("not")
//                    .append(" ").append(rightOperand.getCode())
//                    .append(" )");
//        }
//
//        // 获取操作数值，可能为ID、NUMBER或mathExp的计算结果
//
//        List<Exception> exceptions = new ArrayList<>();
//        double leftValue = 0;
//        double rightValue = 0;
//        try {
//            if (leftOperand != null) {
//                leftValue = getValueFromOperand(leftOperand);
//            }
//        } catch (Exception e) {
//            exceptions.add(e);
//        }
//
//        try {
//            if (rightOperand != null) {
//                rightValue = getValueFromOperand(rightOperand);
//            }
//        } catch (Exception e) {
//            exceptions.add(e);
//        }
//
//        if (!exceptions.isEmpty()) {
//            node.setAttribute("boolExpVal", true); //返回一个任意结果
//            throw new MultiException("Multiple errors occurred", exceptions);
//        }
//
//
//
//        // 根据操作符进行布尔运算
//        boolean result = false;
//        String op = operator.getType();
//        switch (op) {
//            case "SMALL":
//                result = leftValue < rightValue;
//                break;
//            case "LARGE":
//                result = leftValue > rightValue;
//                break;
//            case "EQUAL":
//                result = leftValue == rightValue;
//                break;
//            case "NOTEQUAL":
//                result = leftValue != rightValue;
//                break;
//            case "AND":
//                result = leftValue != 0 && rightValue != 0;
//                break;
//            case "OR":
//                result = leftValue != 0 || rightValue != 0;
//                break;
//            case "NOT":
//                result = rightValue == 0;
//                break;
//            default:
//                node.appendCodeLine("Error: Unsupported boolean operation.");
//                return;
//        }
//
//        // 设置结果并追加代码
//        node.setAttribute("boolExpVal", result);
//        node.appendCode(boolExp.toString());
//    }
//
//    private double getValueFromOperand(ExtendedASTNode operand) {
//        if (operand.getType().equals("NUMBER")) {
//            return Double.parseDouble(operand.getValue());
//        } else if (operand.getType().equals("ID")) {
//            SymbolInfo info = ExtendedASTNode.getSymbol(operand.getValue());
//            if (info != null) {
//                return (double)info.getAdditionalAttributes();
//            } else {
//                throw new IllegalArgumentException("Identifier not defined: " + operand.getValue());
//            }
//        } else if (operand.getType().equals("mathExp") && operand.hasAttribute("mathExpVal")) {
//            return (double) operand.getAttribute("mathExpVal");
//        } else if (operand.hasAttribute("boolExpVal")) {
//            return (boolean) operand.getAttribute("boolExpVal") ? 1.0 : 0.0;
//        } else {
//            throw new IllegalArgumentException("Invalid operand type or missing computation: " + operand.getType());
//        }
//    }

    public void visit_perceiveCommand(ExtendedASTNode node) {
        node.appendCodeLine("RobotSLAM().robot_slam()");
    }

    public void visit_forwardCommand(ExtendedASTNode node) {
//        String meter = node.getChildrenByType("NUMBER").getValue();
//        node.appendCodeLine("bot.set_car_motion(0.05, 0, 0)");
//        node.appendCodeLine("time.sleep(" + meter +" / 0.05)");
//        node.appendCodeLine("bot.set_car_motion(0, 0, 0)");
//        node.newLine();
        node.appendCodeLine("RobotWheel().robot_wheel(0, " + node.getChildrenByType("NUMBER").getValue() + ")" );
    }

    public void visit_backwardCommand(ExtendedASTNode node) {
//        String meter = node.getChildrenByType("NUMBER").getValue();
//        node.appendCodeLine("bot.set_car_motion(-0.05, 0, 0)");
//        node.appendCodeLine("time.sleep(" + meter +" / 0.05)");
//        node.appendCodeLine("bot.set_car_motion(0, 0, 0)");
//        node.newLine();
        node.appendCodeLine("RobotWheel().robot_wheel(0, -" + node.getChildrenByType("NUMBER").getValue() + ")");
    }

    public void visit_turnrightCommand(ExtendedASTNode node) {
//        String meter = node.getChildrenByType("NUMBER").getValue();
//        node.appendCodeLine("bot.set_car_motion(0, 0, -0.05)");
//        node.appendCodeLine("time.sleep(" + meter +" * (3.14159 / 180) / 0.05)");
//        node.appendCodeLine("bot.set_car_motion(0, 0, 0)");
//
//        node.newLine();
        node.appendCodeLine("RobotWheel().robot_wheel(" + node.getChildrenByType("NUMBER").getValue() + ", 0)");
    }

    public void visit_turnleftCommand(ExtendedASTNode node) {
//        String meter = node.getChildrenByType("NUMBER").getValue();
//        node.appendCodeLine("bot.set_car_motion(0, 0, 0.05)");
//        node.appendCodeLine("time.sleep(" + meter +" * (3.14159 / 180) / 0.05)");
//        node.appendCodeLine("bot.set_car_motion(0, 0, 0)");
//
//        node.newLine();
        node.appendCodeLine("RobotWheel().robot_wheel(-" + node.getChildrenByType("NUMBER").getValue() + ", 0)");
    }

    public void visit_lookupCommand(ExtendedASTNode node) {
        node.appendCodeLine("RobotHead().robot_head(0,0.2)");
    }

    public void visit_lookdownCommand(ExtendedASTNode node) {
        node.appendCodeLine("RobotHead().robot_head(0,-0.2)");
    }

    public void visit_lookleftCommand(ExtendedASTNode node) {
        node.appendCodeLine("RobotHead().robot_head(-0.2,0)");
    }

    public void visit_lookrightCommand(ExtendedASTNode node) {
        node.appendCodeLine("RobotHead().robot_head(0.2,0)");
    }

    public void visit_gotoCommand(ExtendedASTNode node) {
        // Append the command to move to the specified coordinates
        node.appendCodeLine("RobotNav().robot_navigation(" + node.getChildrenByType("NUMBER",0).getValue() + ", "
                + node.getChildrenByType("NUMBER",1).getValue() + ")");
    }

    public void visit_approachCommand(ExtendedASTNode node) {
        node.appendCodeLine("x, y = RobotLocate().locate_object(" + node.getChildrenByType("ID").getValue() + ")");
        node.appendCodeLine("RobotNav().robot_navigation(x, y)");
    }

    public void visit_graspCommand(ExtendedASTNode node) {
        node.appendCodeLine("x, y, z = RobotRecognition().recognize_object(" + node.getChildrenByType("ID").getValue() + ")");
        node.appendCodeLine("RobotHand().robot_hand(x, y, z)");
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