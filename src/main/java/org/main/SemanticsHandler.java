package org.main;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import java.lang.reflect.Method;

public class SemanticsHandler {
    private ASTNode originalRootNode; // 原始的ASTNode
    private ExtendedASTNode simplifiedRootNode; // 化简后的ExtendedASTNode
    private static Map<String, Method> semanticRules;
    private static SemanticActions semanticActions;
    public static List<SemanticError> semanticErrors;

    public SemanticsHandler(ASTNode rootNode) {
        this.originalRootNode = rootNode;
        this.semanticActions = new SemanticActions();
        this.semanticRules = new HashMap<>();
        this.semanticErrors = new ArrayList<>();
        this.simplifiedRootNode = simplifyAndConvertNode(rootNode); // 化简并转换
        removeEpsilonAndEmptyNodes(simplifiedRootNode);
        initializeSemanticRules();
    }

    // Getter 方法，返回 SemanticActions 的 cfg
    public CFG getCfg() {
        return semanticActions.cfg;
    }

    // Getter 方法，返回 SemanticActions 的 entrynode
    public EntryNode getEntryNode() {
        return SemanticActions.entryNode;
    }

    // Getter 方法，返回 SemanticActions 的 exitnode
    public ExitNode getExitNode() {
        return SemanticActions.exitNode;
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
            //如果儿子节点是中间节点，那就直接把其孙子提上来，成为自己的儿子节点
            if (isIntermediateNode(child)) {
                List<ASTNode> children = child.getChildren();
                oldChildren.remove(i);
                oldChildren.addAll(i, children);
                i--; //需要对提上来的这些孙子节点逐个继续判断
            }else{
                ExtendedASTNode simplifyChild = simplifyAndConvertNode(child);
                if(simplifyChild != null){
                    extendedNode.addChild(simplifyChild);
                }
            }
        }

        if(!extendedNode.getChildren().isEmpty() && extendedNode.getChildren().get(extendedNode.getChildren().size() - 1)
                .getType().equals(node.getType() + "'")){
            extendedNode = simplifyRightRecursion(extendedNode);
        }

        return extendedNode;
    }

    //化简引入的右递归非终结符
    private ExtendedASTNode simplifyRightRecursion(ExtendedASTNode node){
        if(node.isTerminal()){
            return  node;
        }

        //判断是否有右递归形式的中间非终结符
        ExtendedASTNode intermediateChild = node.getChildren().get(node.getChildren().size() - 1);
        intermediateChild.setType(node.getType());
        Collections.rotate(node.getChildren(), 1);

        if(intermediateChild.getChildren().isEmpty()){
            return node;
        }

        ExtendedASTNode lastChild = intermediateChild;

        if(!intermediateChild.getChildren().isEmpty() && ( intermediateChild.getChildren().size() != 1 ||
                !intermediateChild.getChildren().get(0).getType().equals("ε") ) ) {

            lastChild = intermediateChild.getChildren().get(intermediateChild.getChildren().size() - 1);
            lastChild.setType(node.getType());
            Collections.rotate(lastChild.getParent().getChildren(), 1);

            while(!lastChild.getChildren().isEmpty() && ( lastChild.getChildren().size() != 1 || !lastChild.getChildren().get(0).getType().equals("ε") ) )
            {
                lastChild = lastChild.getChildren().get(lastChild.getChildren().size() - 1);
                lastChild.setType(node.getType());
                Collections.rotate(lastChild.getParent().getChildren(), 1);
            }
        }

        for(int i = 1;i<node.getChildren().size();i++)
        {
            lastChild.addChild(node.getChildren().get(i));
        }

        return intermediateChild;
    }

    private static boolean removeEpsilonAndEmptyNodes(ExtendedASTNode node) {
        if (node == null) {
            return false; // 如果节点为null，直接返回false
        }

        boolean shouldRemove = false;
        Iterator<ExtendedASTNode> iterator = node.getChildren().iterator();
        while (iterator.hasNext()) {
            ExtendedASTNode child = iterator.next();
            // 递归调用来决定是否删除子节点
            boolean removeChild = removeEpsilonAndEmptyNodes(child);
            if (removeChild) {
                iterator.remove(); // 删除子节点
            }
        }

        // 检查当前节点是否为ε节点或是否变成了空节点
        if ("ε".equals(node.getType()) || (node.getChildren().isEmpty() && !node.isTerminal())) {
            shouldRemove = true; // 标记当前节点需要被删除
        }

        return shouldRemove;
    }

    private boolean shouldRemoveNode(ASTNode node) {
        // 如果是中间节点且唯一子节点是ε则删除
        if (isIntermediateNode(node) && containsOnlyEpsilon(node.getChildren())) {
            return true;
        }
        return false;
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
        ErrorHandler.handleError(semanticErrors);
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
                if(semanticActions.ifAggregate.contains(nodeType)){
                    method = SemanticActions.class.getMethod("aggregateVisit", ExtendedASTNode.class);
                }else{
                    method = SemanticActions.class.getMethod("defaultVisit", ExtendedASTNode.class);
                }
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("The default visit method is missing in SemanticActions class.", e);
            }
        }

        // 调用找到的方法

        try {
            method.invoke(semanticActions, node);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }


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
        if(!semanticErrors.isEmpty())  return;
        if (simplifiedRootNode != null) {
//            System.out.println("Generated Code:");
            System.out.println(simplifiedRootNode.getCode());
        } else {
            System.err.println("No code generated.");
        }
    }

    //语义错误类
    static class SemanticError extends CompilationError {
        private ExtendedASTNode errorNode; // 错误发生的节点
        private String errorMessage;       // 错误消息

        public SemanticError(ExtendedASTNode errorNode, String errorMessage) {
            this.errorNode = errorNode;
            this.errorMessage = errorMessage;
        }

        @Override
        public boolean handle() {
            int line = errorNode.getLineNumber();
            System.err.println("\nInvalid syntax at line " + (line) + "' :");
            System.err.println("语义错误: " + errorMessage + " | 节点: " + errorNode.getType());
            return true;
        }

        // Getter 方法
        public ExtendedASTNode getErrorNode() {
            return errorNode;
        }

        public String getErrorMessage() {
            return errorMessage;
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
    private static ExtendedASTNode errorNode = new ExtendedASTNode("ERROR","ERROR",-1,true);
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
    private static int indentationLevel = 0;
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
        this.parent = null;  // 初始化时没有父节点
    }

    public void setType(String type){
        this.type = type;
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
        if(typeToChildrenMap.containsKey(type)){
            List<ExtendedASTNode> children = typeToChildrenMap.getOrDefault(type, new ArrayList<>());
            if (children.isEmpty() || index >= children.size()){
                return errorNode;
            }else{
                return typeToChildrenMap.getOrDefault(type, new ArrayList<>()).get(index);
            }
        }else{
            return errorNode;
        }

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

    public boolean isError(){
        return type.equals("ERROR");
    }

    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }
    public boolean hasAttribute(String key) { return this.attributes.containsKey(key); }

    // 不会换行的
    public void appendCode(String line) {
        this.code.append("\t".repeat(Math.max(0, indentationLevel))).append(line);
    }

    // 会换行的
    public void appendCodeLine(String line) {
        this.code.append("\t".repeat(Math.max(0, indentationLevel))).append(line).append("\n");
    }

    public void appendCodeLineNoIndentation(String line) {
        this.code.append(line).append("\n");
    }

    // Add a new method for adding a newline
    public void newLine() {
        this.code.append("\n");
    }

    public void increaseIndent() {
        indentationLevel++;
    }

    public void decreaseIndent() {
        if (indentationLevel > 0) {
            indentationLevel--;
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
    public static SymbolInfo addSymbol(String identifier) {
        symbolTable.put(identifier, new SymbolInfo(identifier));
        return symbolTable.get(identifier);
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
