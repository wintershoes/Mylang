package org.main;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.*;

//将需要定义的语义动作写在这个类里
//没有被定义语义动作的符号会运行defaultVisit函数,加进了ifAggregate集合里的节点除了访问孩子还会自动从子节点获取已经生成的代码,可以用visitChildren来访问孩子
//每个节点都有专门的变量用来存储生成的代码,可以使用aggregateCodeFromChildren从孩子节点获取代码
public class SemanticActions {
    public static EntryNode entryNode = new EntryNode();
    public static ExitNode exitNode = new ExitNode();
    private static CFGNode currentCFGNode;

    private Map<String, Object> config;
    public CFG cfg;
    public Set<String> ifAggregate;

    public SemanticActions() {
        currentCFGNode = entryNode;
        cfg = new CFG();
        cfg.addNode(entryNode);
        cfg.addNode(exitNode);
        ifAggregate = new HashSet<>();
        ifAggregate.addAll(List.of(new String[]{"statement" ,"command","Case","loopstatement"
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

    public void visitChildrenWithCond(ExtendedASTNode node) {
        List<CFGNode> branchEndNodes = new ArrayList<>(); // 用于存储分支结束的节点
        String boolExp;
        CFGNode branchStartNode = currentCFGNode;
        boolean haselsestatement = false;

        for (ExtendedASTNode child : node.getChildren()) {
            if (child.getType().equals("boolExp")) {
                // 如果是布尔表达式，生成对应的条件节点并添加到CFG
                SemanticsHandler.visitNode(child);
                boolExp =  (String) child.getAttribute("boolExp");
                CFGNode conditionNode = new EmptyNode("conditionNode");
                cfg.addNode(conditionNode);

                // 从当前节点到条件节点添加边
                cfg.addEdge(branchStartNode,conditionNode, boolExp);
                currentCFGNode = conditionNode; // 更新当前节点为条件节点
            } else if (child.getType().equals("Case")) {
                // 如果是 Case 块，生成 then 分支的 CFG
                SemanticsHandler.visitNode(child);// 递归访问 Case 的子节点

                // Case 的代码生成完成后，记录结束节点
                branchEndNodes.add(currentCFGNode);

                // 将 currentCFGNode 重置为分支起点，方便接下来的分支处理
                currentCFGNode = branchStartNode;
            } else if (child.getType().equals("elseStatement")) {
                haselsestatement = true;
                // 如果是 elseStatement，则需要从其子节点出获取最终节点列表
                CFGNode branchEndNodeFromElse = visit_elseStatement(child);
                branchEndNodes.add(branchEndNodeFromElse);
            } else {
                // 默认访问子节点
                SemanticsHandler.visitNode(child);
            }
        }

        // 创建 MergeNode 并连接所有分支结束节点
        CFGNode mergeNode = new EmptyNode("mergeNode");
        cfg.addNode(mergeNode);
        for (CFGNode branchEndNode : branchEndNodes) {
            cfg.addEdge(branchEndNode, mergeNode, "True");
        }

        if(!haselsestatement) {
            cfg.addEdge(branchStartNode, mergeNode, "else");
        }

        // 更新 currentCFGNode 为 MergeNode
        currentCFGNode = mergeNode;
    }

    private void visitChildrenWithWhile(ExtendedASTNode node) {
        CFGNode LoopNode = new EmptyNode("LoopNode");
        cfg.addNode(LoopNode);
        cfg.addEdge(currentCFGNode, LoopNode, "True");
        String boolExp = null;

        for (ExtendedASTNode child : node.getChildren()) {
            if (child.getType().equals("boolExp")) {
                // 如果是布尔表达式，生成对应的条件节点并添加到CFG
                SemanticsHandler.visitNode(child);
                boolExp =  (String) child.getAttribute("boolExp");
                CFGNode conditionNode = new EmptyNode("conditionNode");
                cfg.addNode(conditionNode);

                // 从当前节点到条件节点添加边
                cfg.addEdge(LoopNode,conditionNode, boolExp);
                currentCFGNode = conditionNode; // 更新当前节点为条件节点
            } else if (child.getType().equals("Case")) {
                // 如果是 Case 块，生成 then 分支的 CFG
                SemanticsHandler.visitNode(child);// 递归访问 Case 的子节点
                cfg.addEdge(currentCFGNode, LoopNode, "True");
            } else {
                // 默认访问子节点
                SemanticsHandler.visitNode(child);
            }
        }

        CFGNode EndLoopNode = new EmptyNode("EndLoopNode");
        cfg.addNode(EndLoopNode);
        cfg.addEdge(LoopNode, EndLoopNode, "else");
        currentCFGNode = EndLoopNode;
    }

    private void visitChildrenWithFor(ExtendedASTNode node,int startValue,int endValue) {
        //定义一个节点用来初始化计数器变量
        CFGNode assignNode = new EmptyNode("ForLoopCount = " + Integer.toString(startValue));
        cfg.addNode(assignNode);
        cfg.addEdge(currentCFGNode, assignNode, "True");

        CFGNode LoopNode = new EmptyNode("LoopNode");
        cfg.addNode(LoopNode);
        cfg.addEdge(assignNode, LoopNode, "True");

        CFGNode conditionNode = new EmptyNode("conditionNode");
        cfg.addNode(conditionNode);

        // 从当前节点到条件节点添加边
        cfg.addEdge(LoopNode,conditionNode, "ForLoopCount <= " + Integer.toString(endValue));
        currentCFGNode = conditionNode; // 更新当前节点为条件节点


        for (ExtendedASTNode child : node.getChildren()) {
            if (child.getType().equals("Case")) {
                // 如果是 Case 块，生成 then 分支的 CFG
                SemanticsHandler.visitNode(child);// 递归访问 Case 的子节点

                CFGNode AddNode = new EmptyNode("ForLoopCount += 1");
                cfg.addNode(AddNode);
                cfg.addEdge(currentCFGNode, AddNode, "True");
                cfg.addEdge(AddNode, LoopNode, "True");
            } else {
                // 默认访问子节点
                SemanticsHandler.visitNode(child);
            }
        }

        CFGNode EndLoopNode = new EmptyNode("EndLoopNode");
        cfg.addNode(EndLoopNode);
        cfg.addEdge(LoopNode, EndLoopNode, "ForLoopCount > " + Integer.toString(endValue));
        currentCFGNode = EndLoopNode;
    }

    public void visit_program(ExtendedASTNode node) {
        // 从 config 中获取 program_code 的代码列表
        applyCodeFromConfig(node, "program_code");

        visitChildren(node);
        node.aggregateCodeFromChildren();
        node.appendCodeLine("");
        node.appendCodeLine("rospy.spin()");
        cfg.addEdge(currentCFGNode,exitNode,"True");
    }

    ///////////////////////////////////////赋值和表达式///////////////////////////////////////////////////
    //赋值语句
    public void visit_assignstatement(ExtendedASTNode node) {
        // 访问子节点以收集必要的数据
        visitChildren(node);

        String id = node.getChildrenByType("ID").getValue();
        String value = getValueFromOperand(node.getChildren().get(2));
        String operandType;
        ExtendedASTNode operandNode = node.getChildren().get(2);



        operandType = switch (node.getChildren().get(2).getType()) {
            case "NUMBER","mathExp" -> "mathExp";
            case "returnFunction" -> "returnFunction" ;
            case "string" -> "string"; // 假设 getValue 返回字符串值
            default -> throw new IllegalArgumentException("Unsupported operand type: " + operandNode.getType());
        };

        //查表,看是否定义了这个变量
        SymbolInfo info = ExtendedASTNode.getSymbol(id);

        //如果还没有定义
        if(info == null){
            //注册变量，每个变量都是以SymbolInfo的形式存储在Symbletable里的
            info = ExtendedASTNode.addSymbol(id);
        }

        info.setAdditionalAttributes(value);
        node.appendCodeLine(id + " = " + value);

        //处理CFG//
        //先将创建当前行代码对应的IR，并创建对应的节点和加到cfg中存储着
        if(!operandType.contains("returnFunction")){
            IR assignIR = new IR("ASSIGN",Arrays.asList(id , value));
            CFGNode assignNode = new BlockNode(Arrays.asList(assignIR));
            cfg.addNode(assignNode);

            //因为这里是顺序执行，所以将前一个node连接一条True边到当前节点
            cfg.addEdge(currentCFGNode,assignNode,"True");

            //将currentCFGNode设为当前节点
            currentCFGNode = assignNode;
        }else{
            //函数调用类型必须分成两个IR
            // 创建唯一的中间变量（mid_num）用于存储函数返回值
            String midVar = "mid_" + CFG.generateUniqueId(); // 每次调用生成唯一的标识符
            IR functionCallIR = new IR(node.getChildrenByType("returnFunction").getChildren().get(0).getType()
                    , Arrays.asList(midVar));
            // 创建第二个节点表示将 midVar 赋值给目标变量 id
            IR assignIR = new IR("ASSIGN", Arrays.asList(id , midVar));
            CFGNode assignNode = new BlockNode(Arrays.asList(functionCallIR,assignIR));
            cfg.addNode(assignNode);

            // 添加边连接 CFG 节点
            cfg.addEdge(currentCFGNode, assignNode, "True");
            // 更新当前 CFG 节点
            currentCFGNode = assignNode;
        }

        //处理CFG//
    }

    //数学表达式
    public void visit_mathExp(ExtendedASTNode node) {
        StringBuilder mathExp = new StringBuilder();

        // 获取左右操作数和操作符
        ExtendedASTNode leftOperand = node.getChildren().get(1);  // 第一个操作数，跳过左括号
        ExtendedASTNode operator = node.getChildren().get(2).getChildren().get(0);     // 操作符
        ExtendedASTNode rightOperand = node.getChildren().get(3); // 第二个操作数

        mathExp.append("( ")
                .append(getValueFromOperand(leftOperand))
                .append(" ")
                .append(operator.getValue())
                .append(" ")
                .append(getValueFromOperand(rightOperand))
                .append(" )");

        node.setAttribute("mathExp",mathExp.toString());
    }

    //布尔表达式
    public void visit_boolExp(ExtendedASTNode node) {
        // 访问子节点以收集必要的数据
        visitChildren(node);
        StringBuilder boolExp = new StringBuilder();

        // 获取第一个 comExp 的属性值
        ExtendedASTNode firstComExp = node.getChildren().get(0);
        String firstComExpStr = (String) firstComExp.getAttribute("comExp");
        boolExp.append(firstComExpStr);

        // 遍历后续的 boolop 和 comExp
        for (int i = 1; i < node.getChildren().size(); i += 2) {
            ExtendedASTNode boolOpNode = node.getChildren().get(i); // 获取 boolop 节点
            ExtendedASTNode comExpNode = node.getChildren().get(i + 1); // 获取 comExp 节点

            String boolOp = boolOpNode.getChildren().get(0).getValue();
            String comExpStr = (String) comExpNode.getAttribute("comExp"); // 获取 comExp 的属性值

            // 将 boolop 和 comExp 追加到布尔表达式中
            boolExp.append(" ").append(boolOp).append(" ").append(comExpStr);
        }

        // 将生成的布尔表达式存储在当前节点的属性中
        node.setAttribute("boolExp", boolExp.toString());
    }


    public void visit_comExp(ExtendedASTNode node) {
        // 访问子节点以收集必要的数据
        visitChildren(node);
        StringBuilder comExp = new StringBuilder();

        // 获取第一个操作数的值
        ExtendedASTNode leftOperand = node.getChildren().get(0);
        String leftOperandValue = getValueFromOperand(leftOperand);
        comExp.append(leftOperandValue);

        // 获取比较操作符的值
        ExtendedASTNode comOpNode = node.getChildren().get(1);
        String comOp = comOpNode.getChildren().get(0).getValue();
        if(comOp.equals("notin")){
            comOp = "not in";// 获取 boolop 的值
        }
        comExp.append(" ").append(comOp).append(" ");

        // 获取第二个操作数的值
        ExtendedASTNode rightOperand = node.getChildren().get(2);
        String rightOperandValue = getValueFromOperand(rightOperand);
        comExp.append(rightOperandValue);

        // 将生成的比较表达式存储在当前节点的属性中
        node.setAttribute("comExp", comExp.toString());
    }

    ////////////////////////////////////////分支语句//////////////////////////////////////////////////
    public void visit_condstatement(ExtendedASTNode node) {
        node.increaseIndent();
        // 访问子节点收集生成的代码
        visitChildrenWithCond(node);
        node.decreaseIndent();

        // 获取布尔表达式和case块代码的字符串
        StringBuilder condStatementCode = new StringBuilder();

        // 第一部分：处理主条件 (IF)
        ExtendedASTNode ifNode = node.getChildrenByType("boolExp", 0);
        String boolExp = (String) ifNode.getAttribute("boolExp");
        condStatementCode.append("if ").append(boolExp).append(":\n");

        // 获取第一个case块的代码
        ExtendedASTNode caseNode = node.getChildrenByType("Case", 0);
        condStatementCode.append(caseNode.getCode());

        // 其余部分：处理 ELSE IF 和 ELSE
        for (int i = 1; i < node.getChildren().size(); i++) {
            ExtendedASTNode elseNode = node.getChildren().get(i);

            if (elseNode.getType().equals("elseStatement")) {
                condStatementCode.append(elseNode.getCode());
            }
        }

        // 将生成的代码追加到当前节点
        node.appendCodeLine(condStatementCode.toString());
    }

    public CFGNode visit_elseStatement(ExtendedASTNode node) {
        CFGNode branchEndNode = null; // 用于存储分支结束的节点
        String boolExp = "else";
        CFGNode branchStartNode = currentCFGNode;

        // 访问子节点收集生成的代码,这里就直接手动visit吧
        for (ExtendedASTNode child : node.getChildren()) {
            if (child.getType().equals("boolExp")) {
                // 如果是布尔表达式，生成对应的条件节点并添加到CFG
                SemanticsHandler.visitNode(child);// 递归访问 Case
                boolExp = (String) child.getAttribute("boolExp");
            } else if (child.getType().equals("Case")) {
                CFGNode conditionNode = new EmptyNode("conditionNode");
                cfg.addNode(conditionNode);

                // 从当前节点到条件节点添加边
                cfg.addEdge(branchStartNode, conditionNode, boolExp);
                currentCFGNode = conditionNode; // 更新当前节点为条件节点

                SemanticsHandler.visitNode(child);// 递归访问 Case
                // Case 的代码生成完成后，记录结束节点
                branchEndNode = currentCFGNode;
                // 将 currentCFGNode 重置为分支起点，方便接下来的分支处理
                currentCFGNode = branchStartNode;
            }else{
                SemanticsHandler.visitNode(child);
            }
        }

        // elseStatement应该和父节点同级，elseStatement的子节点比父节点多缩进一级
        // 此时刚访问到elseStatement的时候，比父节点多缩进一级，所以访问子节点前不需要增加缩进。
        // 访问完子节点回来，此时还是比父节点多缩进一级，所以要减少一级
        node.decreaseIndent();
        StringBuilder elseStatementCode = new StringBuilder();

        // 判断是 "ELSE IF" 还是 "ELSE"
        if (!Objects.equals(node.getChildrenByType("boolExp").getType(), "ERROR")) {
            // ELSE IF 情况
            ExtendedASTNode elseIfNode = node.getChildrenByType("boolExp", 0);
            String elseIfBoolExp = (String) elseIfNode.getAttribute("boolExp");
            elseStatementCode.append("elif ").append(elseIfBoolExp).append(":\n");
        } else {
            // ELSE 情况
            elseStatementCode.append("else:\n");
        }

        ExtendedASTNode elseCaseNode = node.getChildrenByType("Case", 0);
        //注意获取子节点的代码不能用appendCode，必须直接字符串append或者其他，因为appendCode会进行缩进
        elseStatementCode.append(elseCaseNode.getCode());

        // 将生成的代码追加到当前节点
        node.appendCode(elseStatementCode.toString());
        // 退出之后，还有可能访问其他的elseStatement，所以必须还原访问elseStatement的状态，此处应该恢复比父节点多缩进一级
        node.increaseIndent();
        return branchEndNode;
    }

    ////////////////////////////////////////循环语句//////////////////////////////////////////////////

    public void visit_forStatement(ExtendedASTNode node) {
        // 获取循环的起始值和结束值，并将它们解析为整数
        int startValue = Integer.parseInt(node.getChildrenByType("NUMBER", 0).getValue());
        int endValue = Integer.parseInt(node.getChildrenByType("NUMBER", 1).getValue());

        // 生成 Python 的 for 循环语句（注意：结束值需要加 1 才能循环 endValue 次）
        StringBuilder forLoopCode = new StringBuilder();
        forLoopCode.append("for i in range(").append(startValue).append(", ").append(endValue + 1).append("):");
        node.appendCodeLine(forLoopCode.toString());

        // 增加缩进以适应 Python 的语法
        node.increaseIndent();
        // 获取循环体中的代码
        visitChildrenWithFor(node,startValue,endValue);
        node.decreaseIndent();  // 循环结束后减少缩进

        // 将生成的代码追加到节点
        for (ExtendedASTNode statement : node.getChildrenByType("Case").getChildren()) {
            if (statement.getType().equals("statement")) {
                node.appendCodeLineNoIndentation(statement.getCode());
            }
        }
    }


    public void visit_whileStatement(ExtendedASTNode node) {
        // 增加缩进以适应 Python 的语法
        node.increaseIndent();
        // 获取循环体中的代码
        visitChildrenWithWhile(node);
        node.decreaseIndent();  // 循环结束后减少缩进

        // 获取布尔表达式的值
        ExtendedASTNode boolExpNode = node.getChildrenByType("boolExp", 0);
        String boolExp = (String) boolExpNode.getAttribute("boolExp");

        // 生成 Python 的 while 循环语句
        StringBuilder whileLoopCode = new StringBuilder();
        whileLoopCode.append("while ").append(boolExp).append(":");
        node.appendCodeLine(whileLoopCode.toString());

        // 将生成的代码追加到节点
        for (ExtendedASTNode statement : node.getChildrenByType("Case").getChildren()) {
            if (statement.getType().equals("statement")) {
                String statement_code = statement.getCode();
                node.appendCodeLineNoIndentation(statement.getCode());
            }
        }

    }



    //////////////////////////////////////返回值函数//////////////////////////////////////////////////

    public void visit_returnFunction(ExtendedASTNode node) {
        String function = node.getChildren().get(0).getValue();
        node.setAttribute("returnFunction",function + "()");
    }


    //////////////////////////////////////有输入函数//////////////////////////////////////////////////

    public void visit_forwardCommand(ExtendedASTNode node) {
        String operand = getValueFromOperand(node.getChildren().get(1));
        applyCodeFromConfig(node, "forwardCommand", "NUMBER", operand);

        IR assignIR = new IR("FORWARD",Arrays.asList(operand));

//        node.appendCodeLine(String.valueOf(assignIR.hashCode()));
        CFGNode assignNode = new BlockNode(Arrays.asList(assignIR));
        cfg.addNode(assignNode);

        //因为这里是顺序执行，所以将前一个node连接一条True边到当前节点
        cfg.addEdge(currentCFGNode,assignNode,"True");

        //将currentCFGNode设为当前节点
        currentCFGNode = assignNode;
    }

    public void visit_backwardCommand(ExtendedASTNode node) {
        String operand = getValueFromOperand(node.getChildren().get(1));
        applyCodeFromConfig(node, "backwardCommand", "NUMBER", operand);

        IR assignIR = new IR("BACKWARD",Arrays.asList(operand));
        CFGNode assignNode = new BlockNode(Arrays.asList(assignIR));
        cfg.addNode(assignNode);

        //因为这里是顺序执行，所以将前一个node连接一条True边到当前节点
        cfg.addEdge(currentCFGNode,assignNode,"True");

        //将currentCFGNode设为当前节点
        currentCFGNode = assignNode;
    }

    public void visit_turnrightCommand(ExtendedASTNode node) {
        String operand = getValueFromOperand(node.getChildren().get(1));
        applyCodeFromConfig(node, "turnrightCommand", "NUMBER", operand);

        IR assignIR = new IR("TURNRIGHT",Arrays.asList(operand));
        CFGNode assignNode = new BlockNode(Arrays.asList(assignIR));
        cfg.addNode(assignNode);

        //因为这里是顺序执行，所以将前一个node连接一条True边到当前节点
        cfg.addEdge(currentCFGNode,assignNode,"True");

        //将currentCFGNode设为当前节点
        currentCFGNode = assignNode;
    }

    public void visit_turnleftCommand(ExtendedASTNode node) {
        String operand = getValueFromOperand(node.getChildren().get(1));
        applyCodeFromConfig(node, "turnleftCommand", "NUMBER", operand);

        IR assignIR = new IR("TURNLEFT",Arrays.asList(operand));
        CFGNode assignNode = new BlockNode(Arrays.asList(assignIR));
        cfg.addNode(assignNode);

        //因为这里是顺序执行，所以将前一个node连接一条True边到当前节点
        cfg.addEdge(currentCFGNode,assignNode,"True");

        //将currentCFGNode设为当前节点
        currentCFGNode = assignNode;
    }

    public void visit_gotoCommand(ExtendedASTNode node) {
        String number0 = getValueFromOperand(node.getChildren().get(1));
        String number1 = getValueFromOperand(node.getChildren().get(3));
        applyCodeFromConfig(node, "gotoCommand", new String[]{"NUMBER_0", "NUMBER_1"}, new String[]{number0, number1});
        IR assignIR = new IR("GOTO",Arrays.asList(number0 , number1));
        CFGNode assignNode = new BlockNode(Arrays.asList(assignIR));
        cfg.addNode(assignNode);

        //因为这里是顺序执行，所以将前一个node连接一条True边到当前节点
        cfg.addEdge(currentCFGNode,assignNode,"True");

        //将currentCFGNode设为当前节点
        currentCFGNode = assignNode;
    }

    public void visit_approachCommand(ExtendedASTNode node) {
        String s = node.getChildrenByType("string").getChildren().get(1).getValue();
        applyCodeFromConfig(node, "approachCommand", "string", s);

        IR assignIR = new IR("APPROACH",Arrays.asList(s));
        CFGNode assignNode = new BlockNode(Arrays.asList(assignIR));
        cfg.addNode(assignNode);

        //因为这里是顺序执行，所以将前一个node连接一条True边到当前节点
        cfg.addEdge(currentCFGNode,assignNode,"True");

        //将currentCFGNode设为当前节点
        currentCFGNode = assignNode;
    }

    public void visit_graspCommand(ExtendedASTNode node) {
        String s = node.getChildrenByType("string").getChildren().get(1).getValue();
        applyCodeFromConfig(node, "graspCommand", "string", "\"" + s + "\"");

        IR assignIR = new IR("GRASP",Arrays.asList(s));
        CFGNode assignNode = new BlockNode(Arrays.asList(assignIR));
        cfg.addNode(assignNode);

        //因为这里是顺序执行，所以将前一个node连接一条True边到当前节点
        cfg.addEdge(currentCFGNode,assignNode,"True");

        //将currentCFGNode设为当前节点
        currentCFGNode = assignNode;
    }

    //////////////////////////////////////无输入函数//////////////////////////////////////////////////
    public void visit_slamCommand(ExtendedASTNode node) {
        applyCodeFromConfig(node, "slamCommand");

        IR assignIR = new IR("SLAM",Arrays.asList());
        CFGNode assignNode = new BlockNode(Arrays.asList(assignIR));
        cfg.addNode(assignNode);

        //因为这里是顺序执行，所以将前一个node连接一条True边到当前节点
        cfg.addEdge(currentCFGNode,assignNode,"True");

        //将currentCFGNode设为当前节点
        currentCFGNode = assignNode;
    }

    public void visit_perceiveCommand(ExtendedASTNode node) {
        applyCodeFromConfig(node, "perceiveCommand");

        IR assignIR = new IR("PERCEIVE",Arrays.asList());
        CFGNode assignNode = new BlockNode(Arrays.asList(assignIR));
        cfg.addNode(assignNode);

        //因为这里是顺序执行，所以将前一个node连接一条True边到当前节点
        cfg.addEdge(currentCFGNode,assignNode,"True");

        //将currentCFGNode设为当前节点
        currentCFGNode = assignNode;
    }

    public void visit_releaseCommand(ExtendedASTNode node) {
        applyCodeFromConfig(node, "releaseCommand");

        IR assignIR = new IR("RELEASE",Arrays.asList());
        CFGNode assignNode = new BlockNode(Arrays.asList(assignIR));
        cfg.addNode(assignNode);

        //因为这里是顺序执行，所以将前一个node连接一条True边到当前节点
        cfg.addEdge(currentCFGNode,assignNode,"True");

        //将currentCFGNode设为当前节点
        currentCFGNode = assignNode;
    }

    public void visit_endCommand(ExtendedASTNode node) {
        List<String> numbers = new ArrayList<>();
        for (int i = 1; i <= 13; i += 2) {
            numbers.add(getValueFromOperand(node.getChildren().get(i)));
        }

        applyCodeFromConfig(node, "endCommand");

        IR assignIR = new IR("SET_END",numbers);
        CFGNode assignNode = new BlockNode(Arrays.asList(assignIR));
        cfg.addNode(assignNode);

        //因为这里是顺序执行，所以将前一个node连接一条True边到当前节点
        cfg.addEdge(currentCFGNode,assignNode,"True");

        //将currentCFGNode设为当前节点
        currentCFGNode = assignNode;
    }

    public void visit_gripperCommand(ExtendedASTNode node) {
        String number0 = getValueFromOperand(node.getChildren().get(1));
        applyCodeFromConfig(node, "gripperCommand");

        IR assignIR = new IR("SET_GRIP",Arrays.asList(number0));
        CFGNode assignNode = new BlockNode(Arrays.asList(assignIR));
        cfg.addNode(assignNode);

        //因为这里是顺序执行，所以将前一个node连接一条True边到当前节点
        cfg.addEdge(currentCFGNode,assignNode,"True");

        //将currentCFGNode设为当前节点
        currentCFGNode = assignNode;
    }

    //////////////////////////////////////////////工具型函数////////////////////////////////////////////////////
    // 用于解析ID | NUMBER | mathExp | returnFunction四种中的任意一种
    // 如果是NUMBER，就直接返回数字的字符串
    // 如果是字符串，直接返回字符串
    // 如果是ID mathExp returnFunction，就返回其属性，其属性保证这是一个字符串，带括号的，而且可以直接写进python的任意表达式的
    private String getValueFromOperand(ExtendedASTNode operand) {
        switch (operand.getType()) {
            case "NUMBER" -> {
                return operand.getValue();
            }
            case "string" -> {
                return "'" + operand.getChildren().get(1).getValue() + "'" ;
            }
            case "ID" -> {
                SymbolInfo info = ExtendedASTNode.getSymbol(operand.getValue());
                if (info != null) {
//                return "{" + (String) operand.getValue() + "}";
//                return (String) info.getAdditionalAttributes();
                    return operand.getValue();
                } else {
                    SemanticsHandler.semanticErrors.add(new SemanticsHandler.SemanticError(operand, "Identifier not defined: "
                            + operand.getValue()));
                }
                return "ERROR";
            }
            case "mathExp" -> {
                return (String) operand.getAttribute("mathExp");
            }
            case "returnFunction" -> {
                return (String) operand.getAttribute("returnFunction");
            }
            default -> {
                SemanticsHandler.semanticErrors.add(new SemanticsHandler.SemanticError(operand,
                        "Invalid operand type or missing computation: "
                        + operand.getValue()));
                return "ERROR";
            }
        }
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

/////////////////////////////////语义错误处理///////////////////////////////////////////

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