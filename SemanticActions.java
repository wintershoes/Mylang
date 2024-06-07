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
        "loopstatement"
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
        node.appendCodeLine("# python demo program");
        node.appendCodeLine("import sys");
        node.appendCodeLine("sys.path.insert(0, '/path_to_tiago_demo/tiago_demo/scripts')");
        node.appendCodeLine("import rospy");
        node.appendCodeLine("from hand import RobotHand");
        node.appendCodeLine("from head import RobotHead");
        node.appendCodeLine("from locate import RobotLocate");
        node.appendCodeLine("from move import RobotNav");
        node.appendCodeLine("from pick import RobotRecognition");
        node.appendCodeLine("from slam import RobotSLAM");
        node.appendCodeLine("from wheel import RobotWheel\n");
        node.newLine();
        node.appendCodeLine("rospy.init_node('robot', anonymous=True)");
        visitChildren(node);
        node.aggregateCodeFromChildren();
    }

    public void visit_switchStatement(ExtendedASTNode node) {
        visitChildren(node);
        node.
    }

    public void visit_ifStatement(ExtendedASTNode node) {
        visitChildren(node);

        if((boolean) node.getChildrenByType("boolExp").getAttribute("boolExpVal")){
            node.appendCode(node.getChildrenByType("ifCase").getCode());
        }else{
            ExtendedASTNode elseNode = node.getChildrenByType("elseCase");
            if(!elseNode.isError()){
                node.appendCode(node.getChildrenByType("elseCase").getCode());
            }
        }
    }

    public void visit_mathExp(ExtendedASTNode node) {
        // 访问子节点以收集必要的数据
        visitChildren(node);

        // 获取左右操作数和操作符
        ExtendedASTNode leftOperand = node.getChildren().get(1);  // 第一个操作数，跳过左括号
        ExtendedASTNode operator = node.getChildren().get(2).getChildren().get(0);     // 操作符
        ExtendedASTNode rightOperand = node.getChildren().get(3); // 第二个操作数

        double leftValue = getValueFromOperand(leftOperand);
        double rightValue = getValueFromOperand(rightOperand);

        // 计算结果
        double result = 0;
        switch (operator.getType()) {
            case "ADD":
                result = leftValue + rightValue;
                break;
            case "SUBSTRACT":
                result = leftValue - rightValue;
                break;
            case "MUTIPLE":
                result = leftValue * rightValue;
                break;
            case "DIVIDE":
                if (rightValue != 0) {
                    result = leftValue / rightValue;
                } else {
                    node.appendCodeLine("Error: Division by zero.");
                    return;
                }
                break;
            default:
                node.appendCodeLine("Error: Unsupported operation.");
                return;
        }
        node.setAttribute("mathExpVal",result);
        node.appendCode(String.valueOf(result));
    }

    public void visit_boolExp(ExtendedASTNode node) {
        visitChildren(node);

        // 获取子节点列表
        List<ExtendedASTNode> children = node.getChildren();
        ExtendedASTNode operator = null, leftOperand = null, rightOperand = null;

        // 解析不同结构的布尔表达式
        if (children.size() == 5) {
            // Structure: LPAREN ID op (mathExp | NUMBER) RPAREN 或者是 LPAREN mathExp op mathExp RPAREN
            leftOperand = children.get(1);
            operator = children.get(2).getChildren().get(0);
            rightOperand = children.get(3);
        } else if (children.size() == 4) {
            // Structure: LPAREN unaryOp boolExp RPAREN
            operator = children.get(1).getChildren().get(0);
            rightOperand = children.get(2);
        }

        // 获取操作数值，可能为ID、NUMBER或mathExp的计算结果
        double leftValue = 0;
        if (leftOperand != null) {
            leftValue = getValueFromOperand(leftOperand);
        }
        double rightValue = 0;
        if (rightOperand != null) {
            rightValue = getValueFromOperand(rightOperand);
        }

        // 根据操作符进行布尔运算
        boolean result = false;
        switch (operator.getType()) {
            case "SMALL":
                result = leftValue < rightValue;
                break;
            case "LARGE":
                result = leftValue > rightValue;
                break;
            case "EQUAL":
                result = leftValue == rightValue;
                break;
            case "NOTEQUAL":
                result = leftValue != rightValue;
                break;
            case "AND":
                result = leftValue != 0 && rightValue != 0;
                break;
            case "OR":
                result = leftValue != 0 || rightValue != 0;
                break;
            case "NOT":
                result = rightValue == 0;
                break;
            default:
                node.appendCodeLine("Error: Unsupported boolean operation.");
                return;
        }

        // 设置结果并追加代码
        node.setAttribute("boolExpVal", result);
        node.appendCode(String.valueOf(result));
    }

    private double getValueFromOperand(ExtendedASTNode operand) {
        if (operand.getType().equals("NUMBER")) {
            return Double.parseDouble(operand.getValue());
        } else if (operand.getType().equals("ID")) {
            SymbolInfo info = ExtendedASTNode.getSymbol(operand.getValue());
            if (info != null) {
                return Double.parseDouble((String) info.getAdditionalAttributes());
            } else {
                throw new IllegalArgumentException("Identifier not found in symbol table: " + operand.getValue());
            }
        } else if (operand.getType().equals("mathExp") && operand.hasAttribute("mathExpVal")) {
            return (double) operand.getAttribute("mathExpVal");
        } else if (operand.hasAttribute("boolExpVal")) {
            return (boolean) operand.getAttribute("boolExpVal") ? 1.0 : 0.0;
        } else {
            throw new IllegalArgumentException("Invalid operand type or missing computation: " + operand.getType());
        }
    }

    public void visit_perceiveCommand(ExtendedASTNode node) {
        if(ExtendedASTNode.getGlobalAttribute("if_SLAM") == null){
            node.appendCodeLine("SLAM = RobotSLAM()");
            ExtendedASTNode.setGlobalAttribute("if_SLAM",true);
        }
        node.appendCodeLine("SLAM.robot_slam()");
    }

    public void visit_forwardCommand(ExtendedASTNode node) {
        if(ExtendedASTNode.getGlobalAttribute("if_Wheel") == null){
            node.appendCodeLine("controller = RobotWheel()");
            ExtendedASTNode.setGlobalAttribute("if_Wheel",true);
        }
        node.appendCodeLine("controller.move(0," + node.getChildrenByType("NUMBER").getValue() + ")" );
    }

    public void visit_backwardCommand(ExtendedASTNode node) {
        if (ExtendedASTNode.getGlobalAttribute("if_Wheel") == null) {
            node.appendCodeLine("controller = RobotWheel()");
            ExtendedASTNode.setGlobalAttribute("if_Wheel", true);
        }
        node.appendCodeLine("controller.move(0, -" + node.getChildrenByType("NUMBER").getValue() + ")");
    }

    public void visit_turnrightCommand(ExtendedASTNode node) {
        if (ExtendedASTNode.getGlobalAttribute("if_Wheel") == null) {
            node.appendCodeLine("controller = RobotWheel()");
            ExtendedASTNode.setGlobalAttribute("if_Wheel", true);
        }
        node.appendCodeLine("controller.move(" + node.getChildrenByType("NUMBER").getValue() + ",0)");
    }

    public void visit_turnleftCommand(ExtendedASTNode node) {
        if (ExtendedASTNode.getGlobalAttribute("if_Wheel") == null) {
            node.appendCodeLine("controller = RobotWheel()");
            ExtendedASTNode.setGlobalAttribute("if_Wheel", true);
        }
        node.appendCodeLine("controller.move(-" + node.getChildrenByType("NUMBER").getValue() + ",0)");
    }

    public void visit_lookupCommand(ExtendedASTNode node) {
        if (ExtendedASTNode.getGlobalAttribute("if_Wheel") == null) {
            node.appendCodeLine("controller = RobotWheel()");
            ExtendedASTNode.setGlobalAttribute("if_Wheel", true);
        }
        node.appendCodeLine("controller.move(0,0.2)");
    }

    public void visit_lookdownCommand(ExtendedASTNode node) {
        if (ExtendedASTNode.getGlobalAttribute("if_Wheel") == null) {
            node.appendCodeLine("controller = RobotWheel()");
            ExtendedASTNode.setGlobalAttribute("if_Wheel", true);
        }
        node.appendCodeLine("controller.move(0,-0.2)");
    }

    public void visit_lookleftCommand(ExtendedASTNode node) {
        if (ExtendedASTNode.getGlobalAttribute("if_Wheel") == null) {
            node.appendCodeLine("controller = RobotWheel()");
            ExtendedASTNode.setGlobalAttribute("if_Wheel", true);
        }
        node.appendCodeLine("controller.move(-0.2,0)");
    }

    public void visit_lookrightCommand(ExtendedASTNode node) {
        if (ExtendedASTNode.getGlobalAttribute("if_Wheel") == null) {
            node.appendCodeLine("controller = RobotWheel()");
            ExtendedASTNode.setGlobalAttribute("if_Wheel", true);
        }
        node.appendCodeLine("controller.move(0.2,0)");
    }

    public void visit_gotoCommand(ExtendedASTNode node) {
        // Check if navigation system is initialized
        if (ExtendedASTNode.getGlobalAttribute("if_Navigation") == null) {
            node.appendCodeLine("Nav = RobotNav()");
            ExtendedASTNode.setGlobalAttribute("if_Navigation", true);
        }

        // Append the command to move to the specified coordinates
        node.appendCodeLine("Nav.robot_navigation(" + node.getChildrenByType("NUMBER",0).getValue() + ", "
                + node.getChildrenByType("NUMBER",1).getValue() + ")");
    }

    public void visit_approachCommand(ExtendedASTNode node) {
        if (ExtendedASTNode.getGlobalAttribute("if_Locate") == null) {
            node.appendCodeLine("locator = RobotLocate()");
            ExtendedASTNode.setGlobalAttribute("if_Locate", true);
        }
        if(ExtendedASTNode.getSymbol(node.getChildrenByType("ID").getValue()) == null){
            ExtendedASTNode.addSymbol(node.getChildrenByType("ID").getValue());
            node.appendCodeLine(node.getChildrenByType("ID").getValue() + "_label = " + "'"
                    + node.getChildrenByType("ID").getValue() + "'");
        }

        node.appendCodeLine("locator.locate_object(" + node.getChildrenByType("ID").getValue() + "_label" + ")");
    }

    public void visit_graspCommand(ExtendedASTNode node) {
        if (ExtendedASTNode.getGlobalAttribute("if_Hand") == null) {
            node.appendCodeLine("controller = RobotHand()");
            ExtendedASTNode.setGlobalAttribute("if_Hand", true);
        }
        if(ExtendedASTNode.getSymbol(node.getChildrenByType("ID").getValue()) == null){
            ExtendedASTNode.addSymbol(node.getChildrenByType("ID").getValue());
            node.appendCodeLine(node.getChildrenByType("ID").getValue() + "_label = " + "'"
                    + node.getChildrenByType("ID").getValue() + "'");
        }

        node.appendCodeLine("controller.grab(" + node.getChildrenByType("ID").getValue() + "_label" + ")");
    }


}