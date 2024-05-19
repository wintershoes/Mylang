import java.util.*;

import java.lang.reflect.Method;

//将需要定义的语义动作写在这个类里
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

    public void visit_perceiveCommand(ExtendedASTNode node) {

        node.appendCodeLine("SLAM = RobotSLAM()");
        node.appendCodeLine("SLAM.robot_slam()");
    }

    public void visit_forwardCommand(ExtendedASTNode node) {
        node.appendCodeLine("controller = RobotWheel()");
        node.appendCodeLine("controller.robot_wheel(0," + node.getChildrenByType("NUMBER").getValue() + ")" );
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
    private static List<String> semanticErrors;

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

class ExtendedASTNode {
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
