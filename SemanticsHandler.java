import java.util.*;

import java.lang.reflect.Method;

//将需要定义的语义动作写在这个类里
//没有被定义语义动作的符号会运行defaultVisit函数,可以用visitChildren来访问孩子
//会自动将子节点生成的代码汇总给父节点，每个节点只需生成好自己的代码即可
class SemanticActions {
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
    }

    public void visit_mathExp(ExtendedASTNode node){
        visitChildren(node);
        node.setAttribute("mathExpValue",node.getChildren().get(1).getAttribute("mathExpValue"));
    }

//    public void visit_addExp(ExtendedASTNode node){
//        double val1 = 0;
//        double val2 = 0;
//        if(node.getChildren().get(0).equals("ID")){
//            if(!ExtendedASTNode.symbolExists(node.getChildren().get(0).getValue())){
//                SemanticsHandler.semanticErrors.add("identifier:" + node.getChildren().get(0).getValue() + "is not defined.");
//            }else{
//                val1 = (double)ExtendedASTNode.getSymbol(node.getChildren().get(0).getValue()).getAdditionalAttributes();
//            }
//        }else{
//            try {
//                val1 = Double.parseDouble(node.getChildren().get(0).getValue());
//            } catch (NumberFormatException e) {
//                SemanticsHandler.semanticErrors.add("Error: " + node.getChildren().get(0).getValue() + " is not a valid double.");
//            }
//        }
//
//        if(node.getChildren().get(1).equals("ID")){
//            if(!ExtendedASTNode.symbolExists(node.getChildren().get(1).getValue())){
//                SemanticsHandler.semanticErrors.add("identifier:" + node.getChildren().get(1).getValue() + "is not defined.");
//            }else{
//                val2 = (double)ExtendedASTNode.getSymbol(node.getChildren().get(1).getValue()).getAdditionalAttributes();
//            }
//        }else{
//            try {
//                val2 = Double.parseDouble(node.getChildren().get(1).getValue());
//            } catch (NumberFormatException e) {
//                SemanticsHandler.semanticErrors.add("Error: " + node.getChildren().get(1).getValue() + " is not a valid double.");
//            }
//        }
//        double val = val1 + val2;
//        System.out.println(val);
//        node.setAttribute("mathExpValue",val);
//    }

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

    public void defaultVisit(ExtendedASTNode node) {
        visitChildren(node);
    }

    public void visitChildren(ExtendedASTNode node) {
        for (ExtendedASTNode child : node.getChildren()) {
            SemanticsHandler.visitNode(child);
        }
    }

}

public class SemanticsHandler {
    private ASTNode originalRootNode; // 原始的ASTNode
    private ExtendedASTNode simplifiedRootNode; // 化简后的ExtendedASTNode
    private static Map<String, Method> semanticRules;
    private static SemanticActions semanticActions;
    public static List<String> semanticErrors;

    public SemanticsHandler(ASTNode rootNode) {
        this.originalRootNode = rootNode;
        this.semanticActions = new SemanticActions();
        this.semanticRules = new HashMap<>();
        this.semanticErrors = new ArrayList<>();
        //initializeSemanticRules();
        this.simplifiedRootNode = simplifyAndConvertNode(rootNode); // 化简并转换
        initializeSemanticRules();
    }

    private void initializeSemanticRules() {
        // 使用反射机制获取SemanticActions类中所有的处理方法
        Method[] methods = SemanticActions.class.getMethods();
        for (Method method : methods) {
            if (method.getName().startsWith("visit_")) {
                String nodeType = method.getName().substring(6);
                semanticRules.put(nodeType, method);
            }
        }
    }

    // 树化简
    private ExtendedASTNode simplifyAndConvertNode(ASTNode node) {
        if (shouldRemoveNode(node)) {
            return null;
        }
        ExtendedASTNode extendedNode = new ExtendedASTNode(node.getType(), node.getValue(), node.getLineNumber(), node.isTerminal());
        List<ASTNode> oldChildren = node.getChildren();


        for (int i = 0; i < oldChildren.size(); i++) {
            ASTNode child = oldChildren.get(i);
            if (isIntermediateNode(child)) {
                List<ASTNode> children = child.getChildren();
                oldChildren.remove(i);
                oldChildren.addAll(i, children);
                i--;
            }else{
                ExtendedASTNode simplifychild = simplifyAndConvertNode(child);
                if(simplifychild != null){
                    extendedNode.addChild(simplifychild);
                }
            }
        }

        return extendedNode;
    }

    private boolean shouldRemoveNode(ASTNode node) {
        // 如果是中间节点且唯一子节点是ε则删除
        if (isIntermediateNode(node) && containsOnlyEpsilon(node.getChildren())) {
            return true;
        }

        return node.getType().equals("ε");
    }

    private boolean isIntermediateNode(ASTNode node) {
        return node.getType().startsWith("mid_");
    }

    private boolean containsOnlyEpsilon(List<ASTNode> children) {
        // 检查是否唯一子节点是ε
        return children.size() == 1 && children.get(0).getType().equals("ε");
    }

    // 语义分析，遍历语法树并执行对应的语义动作
    public void analyzeSemantics() {
        visitNode(simplifiedRootNode);
        if (!semanticErrors.isEmpty()) {
            System.out.println("Semantic errors found:");
            for (String error : semanticErrors) {
                System.out.println(error);
            }
        }
    }

    static public void visitNode(ExtendedASTNode node) {
        String nodeType = node.getType();
        Method method;

        // 检查是否有特定的访问方法
        if (semanticRules.containsKey(nodeType)) {
            method = semanticRules.get(nodeType);
        } else {
            // 没有为该类型定义特定的访问方法，使用默认方法
            try {
                method = SemanticActions.class.getMethod("defaultVisit", ExtendedASTNode.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("The default visit method is missing in SemanticActions class.", e);
            }
        }

        // 调用找到的方法
        try {
            method.invoke(semanticActions, node);
        } catch (Exception e) {
            semanticErrors.add("Error invoking semantic action for node type: " + nodeType + ". " + e.getMessage());
        }

        node.aggregateCodeFromChildren();
    }

    // 获取原始AST根节点
    ASTNode getOriginalRootNode() {
        return originalRootNode;
    }

    // 获取化简后的AST根节点
    ExtendedASTNode getSimplifiedRootNode() {
        return simplifiedRootNode;
    }

    public void printAST() {
        simplifiedRootNode.printTree(0);
    }

    // 方法来输出或处理生成的代码
    public void printGeneratedCode() {
        if (simplifiedRootNode != null) {
            System.out.println("Generated Code:");
            System.out.println(simplifiedRootNode.getCode());
        } else {
            System.out.println("No code generated.");
        }
    }
}

class SymbolInfo {
    private String identifier;
    private String type;
    private String scope;
    private Object additionalAttributes;

    public SymbolInfo(String identifier) {
        this.identifier = identifier;
        this.type = null;
        this.scope = null;
        this.additionalAttributes = null;
    }

    // Getter and Setter methods
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Object getAdditionalAttributes() {
        return additionalAttributes;
    }

    public void setAdditionalAttributes(Object additionalAttributes) {
        this.additionalAttributes = additionalAttributes;
    }
}


class ExtendedASTNode {
    //以下是静态成员，可以一些全局信息
    private static Map<String, Object> globalAttributes = new HashMap<>();
    private static Map<String, SymbolInfo> symbolTable = new HashMap<>(); // 全局符号表

    //以下是各个节点的私有信息
    private String type;
    private String value;
    private Map<String, Object> attributes;
    private List<ExtendedASTNode> children;
    private Map<String, List<ExtendedASTNode>> typeToChildrenMap;
    private int lineNumber;
    private boolean isTerminal;
    private StringBuilder code;
    private int indentationLevel;
    private ExtendedASTNode parent;  // 父节点的引用

    public ExtendedASTNode(String type, String value, int lineNumber, boolean isTerminal) {
        this.type = type;
        this.value = value;
        this.lineNumber = lineNumber;
        this.isTerminal = isTerminal;
        this.attributes = new HashMap<>();
        this.children = new ArrayList<>();
        this.typeToChildrenMap = new HashMap<>();
        this.code = new StringBuilder();
        this.indentationLevel = 0;
        this.parent = null;  // 初始化时没有父节点
    }

    public void addChild(ExtendedASTNode child) {
        child.setParent(this);  // 设置子节点的父节点
        this.children.add(child);
        this.typeToChildrenMap.computeIfAbsent(child.getType(), k -> new ArrayList<>()).add(child);
    }

    public ExtendedASTNode getChildrenByType(String type) {
        return getChildrenByType(type, 0);
    }

    public ExtendedASTNode getChildrenByType(String type, int index) {
        return typeToChildrenMap.getOrDefault(type, new ArrayList<>()).get(index);
    }

    public ExtendedASTNode getParent() {
        return parent;
    }

    private void setParent(ExtendedASTNode parent) {
        this.parent = parent;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public List<ExtendedASTNode> getChildren() {
        return children;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public boolean isTerminal() {
        return isTerminal;
    }

    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    // 不会换行的
    public void appendCode(String line) {
        this.code.append("\t".repeat(Math.max(0, indentationLevel))).append(line);
    }

    // 会换行的
    public void appendCodeLine(String line) {
        this.code.append("\t".repeat(Math.max(0, indentationLevel))).append(line).append("\n");
    }

    // Add a new method for adding a newline
    public void newLine() {
        this.code.append("\n");
    }

    public void increaseIndent() {
        this.indentationLevel++;
    }

    public void decreaseIndent() {
        if (this.indentationLevel > 0) {
            this.indentationLevel--;
        }
    }

    public String getCode() {
        return code.toString();
    }

    public void aggregateCodeFromChildren() {
        for (ExtendedASTNode child : children) {
            this.code.append(child.getCode());  // 将子节点的生成代码追加到当前节点的代码中
        }
    }

    public void copyAttributesFromParent() {
        if (this.parent != null) {
            this.attributes.putAll(parent.attributes);
        }
    }

    public void copyAttributesFromPreviousSibling() {
        if (this.parent != null && !this.parent.getChildren().isEmpty()) {
            int index = this.parent.getChildren().indexOf(this);
            if (index > 0) {
                ExtendedASTNode previousSibling = this.parent.getChildren().get(index - 1);
                this.attributes.putAll(previousSibling.attributes);
            }
        }
    }

    // 设置全局属性
    public static void setGlobalAttribute(String key, Object value) {
        globalAttributes.put(key, value);
    }

    // 获取全局属性
    public static Object getGlobalAttribute(String key) {
        return globalAttributes.get(key);
    }

    // 方法来添加符号到符号表
    public static void addSymbol(String identifier) {
        symbolTable.put(identifier, new SymbolInfo(identifier));
    }

    // 方法来获取符号信息
    public static SymbolInfo getSymbol(String identifier) {
        return symbolTable.get(identifier);
    }

    // 方法来检查符号是否已存在
    public static boolean symbolExists(String identifier) {
        return symbolTable.containsKey(identifier);
    }

    public void printTree(int level) {
        StringBuilder sb = new StringBuilder();
        sb.append("  ".repeat(Math.max(0, level)));
        if(isTerminal) {
            sb.append(type).append(": ").append(value).append(" (Line: ").append(lineNumber).append(") ").append("[Terminal]");
        } else {
            sb.append(type).append(": ").append("[Non-terminal]");
        }
        System.out.println(sb.toString());
        for (ExtendedASTNode child : children) {
            child.printTree(level + 1);
        }
    }
}
