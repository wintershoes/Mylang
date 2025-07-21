package org.main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;


class RobotState {
    private double x, y, z;    // 机器人三维坐标
    private double roll, pitch, yaw;  // 机器人位姿（滚转、俯仰、偏航角）

    public RobotState(double x, double y, double z, double roll, double pitch, double yaw) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.roll = roll;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    // 获取三维坐标
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    // 获取位姿
    public double getRoll() {
        return roll;
    }

    public double getPitch() {
        return pitch;
    }

    public double getYaw() {
        return yaw;
    }

    // 打印机器人状态
    @Override
    public String toString() {
        return String.format("Position: (%.2f, %.2f, %.2f), Orientation: (%.2f, %.2f, %.2f)",
                x, y, z, roll, pitch, yaw);
    }
}


public class IR {
    private String operation;       // 操作类型（operation）
    private List<String> operands; // 操作数（operands）
    private RobotState robotState;  // 机器人状态

    // 构造函数（默认无状态的构造函数）
    public IR(String operation, List<String> operands) {
        this.operation = operation;
        this.operands = operands;
        this.robotState = null; // 默认机器人状态为 null
    }

    // 构造函数（带机器人状态的构造函数）
    public IR(String operation, List<String> operands, RobotState robotState) {
        this.operation = operation;
        this.operands = operands;
        this.robotState = robotState;
    }

    // 获取操作类型
    public String getOperation() {
        return operation;
    }

    // 获取操作数
    public List<String> getOperands() {
        return operands;
    }

    // 设置机器人的状态
    public void setRobotState(RobotState newState) {
        this.robotState = newState;
    }

    // 获取机器人状态
    public RobotState getRobotState() {
        return robotState;
    }


    // 将 IR 列表转换为 Isabelle 实例代码
    public static String toIsabelleCode(List<IR> irList, String definitionName) {
        StringBuilder sb = new StringBuilder();
        sb.append("definition ").append(definitionName).append(" :: \"IR list\" where\n");
        sb.append("  \"").append(definitionName).append(" = [\n");

        for (int i = 0; i < irList.size(); i++) {
            IR item = irList.get(i);

            // 添加IR的操作类型
            sb.append("    (| ir_operation = ").append(item.getOperation()).append(", args = [");

            // 添加操作数，不加双引号
            List<String> operands = item.getOperands();
            for (int j = 0; j < operands.size(); j++) {
                sb.append(operands.get(j)); // 直接输出操作数，无需加双引号
                if (j < operands.size() - 1) {
                    sb.append(", "); // 多个操作数之间添加逗号
                }
            }
            sb.append("] |)");

            // 判断是否为最后一项，决定是否添加逗号
            if (i < irList.size() - 1) {
                sb.append(",\n");
            } else {
                sb.append("\n");
            }
        }

        sb.append("  ]\"\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(operation).append("   ");
        for (int i = 0; i < operands.size(); i++) {
            sb.append(operands.get(i));
            if (i < operands.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

}

abstract class CFGNode {
    // 抽象类，具体的类型由子类实现
}

// 代码块节点
class BlockNode extends CFGNode {
    private List<IR> irList; // IR 指令列表

    public BlockNode(List<IR> irList) {
        this.irList = irList;
    }

    public List<IR> getIrList() {
        return irList;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BlockNode\n{");
        for (int i = 0; i < irList.size(); i++) {
            sb.append(irList.get(i).toString());
            if (i < irList.size() - 1) {
                sb.append("\n");
            }
        }
        sb.append("}");
        return sb.toString();
    }

}

// 入口节点
class EntryNode extends CFGNode {
    @Override
    public String toString() {
        return "EntryNode";
    }
}

// 出口节点
class ExitNode extends CFGNode {
    @Override
    public String toString() {
        return "ExitNode";
    }
}

// 空节点
class EmptyNode extends CFGNode {
    String NodeName;

    public EmptyNode(String NodeName) {
        this.NodeName = NodeName;
    }

    @Override
    public String toString() {
        return NodeName;
    }
}

class CFGEdge {
    public  CFGNode src; // 源节点
    public  CFGNode dest; // 目标节点
    public  String condition; // 条件（布尔表达式）

    public CFGEdge(CFGNode src, CFGNode dest, String condition) {
        this.src = src;
        this.dest = dest;
        this.condition = condition;
    }

    public CFGNode getSrc() {
        return src;
    }

    public CFGNode getDest() {
        return dest;
    }

    public String getCondition() {
        return condition;
    }

    @Override
    public String toString() {
        return "CFGEdge{" +
                "src=" + src +
                ", dest=" + dest +
                ", condition='" + condition + '\'' +
                '}';
    }
}

class CFG {
    private Set<CFGNode> nodes; // 节点集合
    private Set<CFGEdge> edges; //
    private Map<CFGNode, List<CFGEdge>> sourceEdges; // 每个节点作为源点的边（出边）
    private Map<CFGNode, List<CFGEdge>> targetEdges; // 每个节点作为目标点的边（入边）// 边集合
    private static int uniqueIdCounter = 0; // 静态计数器，确保全局唯一性

    public CFG() {
        this.nodes = new HashSet<>();
        this.edges = new HashSet<>();
        this.sourceEdges = new HashMap<>();
        this.targetEdges = new HashMap<>();
    }

    // 添加节点
    public void addNode(CFGNode node) {
        nodes.add(node);
        // 初始化节点的出边和入边列表
        sourceEdges.putIfAbsent(node, new ArrayList<>());
        targetEdges.putIfAbsent(node, new ArrayList<>());
    }

    // 添加边
    public void addEdge(CFGNode src, CFGNode dest, String condition) {
        CFGEdge edge = new CFGEdge(src, dest, condition);
        edges.add(edge);

        // 为源点添加出边
        sourceEdges.putIfAbsent(src, new ArrayList<>());
        sourceEdges.get(src).add(edge);

        // 为目标点添加入边
        targetEdges.putIfAbsent(dest, new ArrayList<>());
        targetEdges.get(dest).add(edge);
    }

    // 获取节点集合
    public Set<CFGNode> getNodes() {
        return nodes;
    }

    // 获取边集合
    public Set<CFGEdge> getEdges() {
        return edges;
    }

    // 获取某节点的所有出边（源点的边）
    public List<CFGEdge> getOutgoingEdges(CFGNode node) {
        return sourceEdges.getOrDefault(node, Collections.emptyList());
    }

    // 获取某节点的所有入边（目标点的边）
    public List<CFGEdge> getIncomingEdges(CFGNode node) {
        return targetEdges.getOrDefault(node, Collections.emptyList());
    }

    // 移除边
    public void removeEdge(CFGEdge edge) {
        // 从全局边集合中移除
        edges.remove(edge);

        // 从 sourceEdges 中移除
        List<CFGEdge> srcEdges = sourceEdges.get(edge.src);
        if (srcEdges != null) {
            srcEdges.remove(edge);
            // 如果 srcEdges 为空，则移除该键值对
            if (srcEdges.isEmpty()) {
                sourceEdges.remove(edge.src);
            }
        }

        // 从 targetEdges 中移除
        List<CFGEdge> destEdges = targetEdges.get(edge.dest);
        if (destEdges != null) {
            destEdges.remove(edge);
            // 如果 destEdges 为空，则移除该键值对
            if (destEdges.isEmpty()) {
                targetEdges.remove(edge.dest);
            }
        }
    }

    // 移除节点
    public void removeNode(CFGNode node) {
        // 获取该节点的所有出边
        List<CFGEdge> outgoing = new ArrayList<>(getOutgoingEdges(node));
        for (CFGEdge edge : outgoing) {
            removeEdge(edge); // 删除每一条出边
        }

        // 获取该节点的所有入边
        List<CFGEdge> incoming = new ArrayList<>(getIncomingEdges(node));
        for (CFGEdge edge : incoming) {
            removeEdge(edge); // 删除每一条入边
        }

        // 从节点集合中移除该节点
        nodes.remove(node);

        // 从 sourceEdges 和 targetEdges 中移除与该节点相关的键值对（以防遗漏）
        sourceEdges.remove(node);
        targetEdges.remove(node);
    }

    // 导出为JSON文件
    public static void exportToJson(CFG cfg, String filename) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> graphData = new HashMap<>();

        List<Map<String, Object>> nodesData = new ArrayList<>();
        Map<CFGNode, String> nodeToId = new HashMap<>();
        Set<Integer> usedNodeHashes = new HashSet<>(); // 节点 hash 检查
        Set<Integer> usedIRHashes = new HashSet<>();   // IR hash 检查

        for (CFGNode node : cfg.getNodes()) {
            Map<String, Object> nodeData = new HashMap<>();

            int nodeHash = node.hashCode();
            String nodeId = "node_" + nodeHash;

            // 节点 hash 检查
            if (usedNodeHashes.contains(nodeHash)) {
                throw new RuntimeException("检测到 CFGNode hashCode 冲突：hash=" + nodeHash + "，冲突节点：" + node);
            }
            usedNodeHashes.add(nodeHash);
            nodeToId.put(node, nodeId);
            nodeData.put("id", nodeId);

            if (node instanceof EntryNode) {
                nodeData.put("type", "EntryNode");
            } else if (node instanceof ExitNode) {
                nodeData.put("type", "ExitNode");
            } else if (node instanceof BlockNode) {
                nodeData.put("type", "BlockNode");
            } else if (node instanceof EmptyNode) {
                nodeData.put("type", node.toString());
            } else {
                nodeData.put("type", "Unknown");
            }

            if (node instanceof BlockNode) {
                List<Map<String, Object>> irList = new ArrayList<>();
                for (IR ir : ((BlockNode) node).getIrList()) {
                    Map<String, Object> irData = new HashMap<>();
                    irData.put("operation", ir.getOperation());
                    irData.put("operands", ir.getOperands());

                    int irHash = ir.hashCode();
                    if (usedIRHashes.contains(irHash)) {
                        throw new RuntimeException("检测到 IR hashCode 冲突：hash=" + irHash + "，冲突 IR：" + ir);
                    }
                    usedIRHashes.add(irHash);
                    irData.put("irHash", irHash); //

                    if (ir.getRobotState() != null) {
                        Map<String, Object> robotStateData = new HashMap<>();
                        robotStateData.put("x", ir.getRobotState().getX());
                        robotStateData.put("y", ir.getRobotState().getY());
                        robotStateData.put("z", ir.getRobotState().getZ());
                        robotStateData.put("roll", ir.getRobotState().getRoll());
                        robotStateData.put("pitch", ir.getRobotState().getPitch());
                        robotStateData.put("yaw", ir.getRobotState().getYaw());
                        irData.put("robotState", robotStateData);
                    }

                    irList.add(irData);
                }
                nodeData.put("irList", irList);
            }

            nodesData.add(nodeData);
        }

        graphData.put("nodes", nodesData);

        // 构建边数据
        List<Map<String, Object>> edgesData = new ArrayList<>();
        for (CFGEdge edge : cfg.getEdges()) {
            Map<String, Object> edgeData = new HashMap<>();
            edgeData.put("src", nodeToId.get(edge.getSrc()));
            edgeData.put("dest", nodeToId.get(edge.getDest()));
            edgeData.put("condition", edge.getCondition());
            edgesData.add(edgeData);
        }

        graphData.put("edges", edgesData);

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), graphData);
    }



    @Override
    public String toString() {
        return "CFG{" +
                "nodes=" + nodes +
                ", edges=" + edges +
                '}';
    }

    // 静态方法：生成唯一的ID
    public static synchronized int generateUniqueId() {
        return uniqueIdCounter++;
    }

}

class CFGVisualizer {

    // 将 CFG 转换为 Graphviz 的 DOT 格式
    public static String toDotFormat(CFG cfg) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph CFG {\n");
        sb.append("  // 全局节点样式\n");
        sb.append("  node [shape=box, style=filled, color=lightblue, fontcolor=black];\n");
        sb.append("  // 全局边样式\n");
        sb.append("  edge [color=darkgreen, penwidth=2.0, fontcolor=blue, fontsize=12, arrowhead=vee];\n");
        sb.append("  ranksep=1.5;\n"); // 设置层次间距
        sb.append("  nodesep=0.5;\n"); // 设置节点间距
        sb.append("  size=\"20,20\";\n"); // 图像大小（宽度 x 高度） 调大可以更清晰
        sb.append("  dpi=1024;\n"); // 分辨率

        // 用于保存 rank=min 和 rank=max 的节点 ID
        StringBuilder rankMin = new StringBuilder("  {rank=min; ");
        StringBuilder rankMax = new StringBuilder("  {rank=max; ");

        // 遍历所有节点，同时处理 EntryNode、ExitNode 和普通节点
        for (CFGNode node : cfg.getNodes()) {
            String nodeId = getNodeId(node);

            // 处理 EntryNode
            if (node instanceof EntryNode) {
                rankMin.append(nodeId).append(" ");
                // 设置 EntryNode 的绿色样式
                sb.append("  ").append(nodeId).append(" [label=\"").append(node.toString())
                        .append("\", color=green, fontcolor=black];\n");
            }
            // 处理 ExitNode
            else if (node instanceof ExitNode) {
                rankMax.append(nodeId).append(" ");
                // 设置 ExitNode 的绿色样式
                sb.append("  ").append(nodeId).append(" [label=\"").append(node.toString())
                        .append("\", color=green, fontcolor=black];\n");
            }
            // 添加普通节点定义
            else {
                sb.append("  ").append(nodeId).append(" [label=\"").append(node.toString()).append("\"];\n");
            }
        }

        // 添加 rank 信息
        rankMin.append("}\n");
        rankMax.append("}\n");
        sb.append(rankMin);
        sb.append(rankMax);

        // 添加边
        for (CFGEdge edge : cfg.getEdges()) {
            sb.append("  ")
                    .append(getNodeId(edge.getSrc()))
                    .append(" -> ")
                    .append(getNodeId(edge.getDest()));

            // 如果边有条件，则显示条件
            if (edge.getCondition() != null && !edge.getCondition().isEmpty()) {
                sb.append(" [label=\"").append(edge.getCondition()).append("\"]");
            }
            sb.append(";\n");
        }

        sb.append("}");
        return sb.toString();
    }

    // 获取节点的唯一标识符
    private static String getNodeId(CFGNode node) {
        return "node_" + node.hashCode(); // 使用 hashCode 作为唯一 ID
    }

    // 将 DOT 格式保存为文件
    public static void saveDotToFile(String dot, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(dot);
        }
    }

    // 使用 Graphviz 渲染 DOT 文件为图像
    public static void renderDotFile(String dotFilePath, String outputImagePath) throws IOException, InterruptedException {
        // 可选: neato, twopi, circo, fdp
        ProcessBuilder processBuilder = new ProcessBuilder("dot", "-Tsvg:cairo", dotFilePath, "-o", outputImagePath);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        process.waitFor();
    }

    // 封装方法：将 CFG 转换为图像并保存
    public static void visualizeToFile(CFG cfg, String outputDirPath, String dotFileName, String imageFileName) {
        try {
            // 创建输出目录
            File outputDir = new File(outputDirPath);
            if (!outputDir.exists()) {
                outputDir.mkdirs(); // 如果目录不存在，则创建
            }

            // 设置文件路径
            String dotFilePath = outputDir.getAbsolutePath() + "/" + dotFileName;
            String outputImagePath = outputDir.getAbsolutePath() + "/" + imageFileName;

            // 转换为 DOT 格式
            String dotFormat = toDotFormat(cfg);

            // 保存 DOT 文件
            saveDotToFile(dotFormat, dotFilePath);

            // 渲染图像
            renderDotFile(dotFilePath, outputImagePath);

            System.out.println("CFG 已成功生成并保存为图像: " + outputImagePath);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
class CFGSimplifier {

    /**
     * 简化控制流图（CFG）的方法
     *
     * @param cfg       需要简化的 CFG 对象
     * @param entryNode 入口节点
     * @param exitNode  出口节点
     */
    public static void simplifyCFG(CFG cfg, CFGNode entryNode, CFGNode exitNode) {
        boolean changed;

        do {
            changed = false;
            List<CFGEdge> edges = new ArrayList<>(cfg.getEdges());

            for (CFGEdge edge : edges) {
                // 检查边的条件是否为 "True"
                if (!"True".equals(edge.condition)) {
                    continue;
                }

                CFGNode src = edge.src;
                CFGNode dest = edge.dest;

                // 排除入口节点和出口节点
                if (src.equals(entryNode) || dest.equals(exitNode)) {
                    continue;
                }

                // 排除包含 "Loop" 的空节点,以及是mergeNode的情况，否则会出现把多个分支节点融合的情况
                if (isLoopNode(src) || isLoopNode(dest) || isCondMergeNode(src) || isCondMergeNode(dest)) {
                    continue;
                }

                // 开始合并节点
                CFGNode mergedNode = mergeNodes(cfg, src, dest);
                if (mergedNode != null) {
                    changed = true;
                    break; // 由于 CFG 结构变化，重新开始遍历
                }
            }
        } while (changed);
    }

    /**
     * 判断节点是否是包含 "Loop" 的空节点
     *
     * @param node 需要判断的节点
     * @return 如果是包含 "Loop" 的空节点，返回 true；否则，返回 false
     */
    private static boolean isLoopNode(CFGNode node) {
        if (node instanceof EmptyNode) {
            String nodeName = ((EmptyNode) node).NodeName;
            return nodeName.contains("Loop");
        }
        return false;
    }

    /**
     * 判断节点是否是条件分支最后的Merge节点
     *
     * @param node 需要判断的节点
     * @return 如果是包含 "Loop" 的空节点，返回 true；否则，返回 false
     */
    private static boolean isCondMergeNode(CFGNode node) {
        if (node instanceof EmptyNode) {
            String nodeName = ((EmptyNode) node).NodeName;
            return nodeName.contains("merge");
        }
        return false;
    }

    /**
     * 合并两个节点并更新 CFG
     *
     * @param cfg  CFG 对象
     * @param src  源节点
     * @param dest 目标节点
     * @return 合并后的新节点，若无法合并则返回 null
     */
    private static CFGNode mergeNodes(CFG cfg, CFGNode src, CFGNode dest) {
        CFGNode mergedNode = null;

        // 合并逻辑：根据节点类型创建合并后的节点
        if (src instanceof EmptyNode && dest instanceof EmptyNode) {
            // 两个都是空节点，合并为目标节点的空节点
            String newNodeName = ((EmptyNode) dest).NodeName;
            mergedNode = new EmptyNode(newNodeName);
        } else if ((src instanceof EmptyNode && dest instanceof BlockNode) ||
                (src instanceof BlockNode && dest instanceof EmptyNode)) {
            // 一个是空节点，一个是 BlockNode，合并为 BlockNode
            if (src instanceof BlockNode) {
                mergedNode = new BlockNode(new ArrayList<>(((BlockNode) src).getIrList()));
            } else {
                mergedNode = new BlockNode(new ArrayList<>(((BlockNode) dest).getIrList()));
            }
        } else if (src instanceof BlockNode && dest instanceof BlockNode) {
            // 两个都是 BlockNode，合并 IR 列表
            List<IR> mergedIRList = new ArrayList<>(((BlockNode) src).getIrList());
            mergedIRList.addAll(((BlockNode) dest).getIrList());
            mergedNode = new BlockNode(mergedIRList);
        } else {
            // 不符合合并条件
            return null;
        }

        // 添加合并后的节点到 CFG
        cfg.addNode(mergedNode);

        // === 更新边逻辑 ===

        // 1. 将所有指向 src 的边改为指向 mergedNode
        List<CFGEdge> incomingEdgesSrc = new ArrayList<>(cfg.getIncomingEdges(src));
        for (CFGEdge edge : incomingEdgesSrc) {
            cfg.addEdge(edge.src, mergedNode, edge.condition);
        }

        // 2. 将所有指向 dest 的边改为指向 mergedNode
        List<CFGEdge> incomingEdgesDest = new ArrayList<>(cfg.getIncomingEdges(dest));
        for (CFGEdge edge : incomingEdgesDest) {
            if (edge.src.equals(src) && edge.dest.equals(dest)) {
                // 排除 src -> dest 的边
                continue;
            }
            cfg.addEdge(edge.src, mergedNode, edge.condition);
        }

        // 3. 将所有从 src 出发的边改为从 mergedNode 出发
        List<CFGEdge> outgoingEdgesSrc = new ArrayList<>(cfg.getOutgoingEdges(src));
        for (CFGEdge edge : outgoingEdgesSrc) {
            if (edge.src.equals(src) && edge.dest.equals(dest)) {
                // 排除 src -> dest 的边
                continue;
            }
            cfg.addEdge(mergedNode, edge.dest, edge.condition);
        }

        // 4. 将所有从 dest 出发的边改为从 mergedNode 出发
        List<CFGEdge> outgoingEdgesDest = new ArrayList<>(cfg.getOutgoingEdges(dest));
        for (CFGEdge edge : outgoingEdgesDest) {
            cfg.addEdge(mergedNode, edge.dest, edge.condition);
        }

        // 5. 移除 src 和 dest 节点
        cfg.removeNode(src);
        cfg.removeNode(dest);

        return mergedNode;
    }

}

class CFGAnalyzer {

    /**
     * 用来在路径中记录 (节点 + 该节点所处的所有条件)
     */
    public static class NodeWithConditions {
        private CFGNode node;
        private List<String> conditions; // 可以把每条条件存一个 List，也可以用逗号或 " ∧ " 拼成一个字符串

        public NodeWithConditions(CFGNode node, List<String> conditions) {
            this.node = node;
            // 这里要注意要拷贝一份，防止引用混淆
            this.conditions = new ArrayList<>(conditions);
        }

        public CFGNode getNode() {
            return node;
        }

        public List<String> getConditions() {
            return conditions;
        }

        @Override
        public String toString() {
            // 打印时，把条件用 " AND " 拼起来
            String condStr = String.join(" AND ", conditions);
            return "Node: " + node.toString() + ", Conditions: [" + condStr + "]";
        }
    }

    public static class IRWithBoolExpr {
        private String operation;    // 操作类型
        private List<String> operands;
        private String condition;    // 最终在Isabelle中输出  BoolTrue  或  Expression ''xxxx''

        public IRWithBoolExpr(String operation, List<String> operands, String condition) {
            this.operation = operation;
            this.operands = new ArrayList<>(operands);
            this.condition = condition;
        }

        public String getOperation() { return operation; }
        public List<String> getOperands() { return operands; }
        public String getCondition() { return condition; }
    }



    /**
     * 查找所有从 EntryNode 到 ExitNode 的路径（并记录沿途条件）
     *
     * @param cfg      控制流图 (CFG)
     * @param entry    入口节点
     * @param exit     出口节点
     * @return 从 EntryNode 到 ExitNode 的所有路径，每条路径是一个 List<NodeWithConditions>
     */
    public static List<List<NodeWithConditions>> findAllPaths(CFG cfg, EntryNode entry, ExitNode exit) {
        List<List<NodeWithConditions>> allPaths = new ArrayList<>(); // 保存所有路径
        // 我们的 “当前路径” 不再是单纯的 CFGNode 列表，而是 NodeWithConditions 列表
        List<NodeWithConditions> currentPath = new ArrayList<>();

        // visitedEdges 用于环路检测
        Set<CFGEdge> visitedEdges = new HashSet<>();

        // 我们在遍历时，还要维护一个当前条件列表，相当于条件栈
        // 每次走到一条边就 push，遇到 mergeNode/EndLoopNode 就 pop
        List<String> currentConditions = new ArrayList<>();

        // 启动深度优先搜索
        dfs(cfg, entry, exit, currentPath, allPaths, visitedEdges, currentConditions);

        return allPaths;
    }

    /**
     * 使用“传值拷贝”的 DFS 搜索所有从 current 到 exit 的路径，并记录沿途的条件。
     *
     * @param cfg               控制流图
     * @param current           当前节点
     * @param exit              出口节点
     * @param currentPath       当前搜索路径
     * @param allPaths          所有路径集合
     * @param visitedEdges      用于环检测的集合
     * @param parentConditions  父层传下来的条件列表
     */
    private static void dfs(CFG cfg,
                            CFGNode current,
                            ExitNode exit,
                            List<NodeWithConditions> currentPath,
                            List<List<NodeWithConditions>> allPaths,
                            Set<CFGEdge> visitedEdges,
                            List<String> parentConditions) {
        // 1) 先把父层的 conditions 拷贝一份给本层使用，互不影响
        List<String> localConditions = new ArrayList<>(parentConditions);

        // 2) 如果当前节点是 mergeNode 或 EndLoopNode，就在本层把栈顶弹掉
        if (isMergeNode(current) || isEndLoopNode(current)) {
            if (!localConditions.isEmpty()) {
                localConditions.remove(localConditions.size() - 1);
            }
        }

        // 3) 把 (当前节点, localConditions) 记录到路径里
        NodeWithConditions nodeWithConds = new NodeWithConditions(current, localConditions);
        currentPath.add(nodeWithConds);

        // 4) 如果到达出口节点，则将当前完整路径加入 allPaths
        if (current.equals(exit)) {
            allPaths.add(new ArrayList<>(currentPath));
            // 回溯：移除本节点
            currentPath.remove(currentPath.size() - 1);
            return;
        }

        // 5) 遍历当前节点的所有出边
        for (CFGEdge edge : cfg.getOutgoingEdges(current)) {
            // 如果边的条件为 "False"，直接跳过
            if ("False".equals(edge.getCondition())) {
                continue;
            }

            // 为避免环路，我们使用 visitedEdges 做检查
            if (visitedEdges.contains(edge)) {
                continue;
            }

            // 在本层针对这条出边也做一次本地拷贝
            List<String> edgeConditions = new ArrayList<>(localConditions);

            // 如果边的条件有效且 != "True"，就把它加入 edgeConditions
            String cond = edge.getCondition();
            if (cond != null && !cond.isEmpty() && !"True".equals(cond) && !"else".equals(cond)) {
                edgeConditions.add(cond);
            }

            // 标记这条边已访问，递归下去
            visitedEdges.add(edge);
            dfs(cfg, edge.getDest(), exit, currentPath, allPaths, visitedEdges, edgeConditions);
            visitedEdges.remove(edge);
        }

        // 6) 回溯：把本节点移出 currentPath
        currentPath.remove(currentPath.size() - 1);
    }

    // 判断是否是 mergeNode
    private static boolean isMergeNode(CFGNode node) {
        if (node instanceof EmptyNode) {
            String nodeName = ((EmptyNode) node).toString().toLowerCase();
            return nodeName.contains("merge");
        }
        return false;
    }

    // 判断是否是 EndLoopNode
    private static boolean isEndLoopNode(CFGNode node) {
        if (node instanceof EmptyNode) {
            String nodeName = ((EmptyNode) node).toString().toLowerCase();
            return nodeName.contains("endloop");
        }
        return false;
    }

    /**
     * 把一条路径（List<NodeWithConditions>）里的 IR 汇总起来。
     */
    public static List<IR> pathToIRList(List<NodeWithConditions> path) {
        List<IR> irList = new ArrayList<>();
        for (NodeWithConditions nc : path) {
            CFGNode node = nc.getNode();
            if (node instanceof BlockNode) {
                irList.addAll(((BlockNode) node).getIrList());
            }
        }
        return irList;
    }

    /**
     * 用于调试或直接打印所有路径
     */
    public static void printPaths(List<List<NodeWithConditions>> paths) {
        int pathCount = 1;
        for (List<NodeWithConditions> path : paths) {
            System.out.println("Path " + pathCount + ":");
            for (NodeWithConditions nc : path) {
                System.out.println("  " + nc.toString());
            }
            System.out.println();
            pathCount++;
        }
    }

    /**
     * 对于路径中的每个节点:
     * - 如果是 BlockNode，就把其中的 IR 取出来
     * - 给每条 IR 加一个 condition
     *   -> 若 conditions 列表为空 => "BoolTrue"
     *   -> 否则 => "Expression ''cond1 AND cond2''"
     * 最后返回 "IRWithBoolExpr" 列表
     */
    public static List<IRWithBoolExpr> toIRWithConditionList(List<NodeWithConditions> path) {
        List<IRWithBoolExpr> result = new ArrayList<>();

        for (NodeWithConditions nodeCond : path) {
            // 1) 拿到节点和它的条件列表
            CFGNode node = nodeCond.getNode();
            List<String> conds = nodeCond.getConditions();

            // 2) 先拼出 condition 的字符串
            //    若 conds.isEmpty(), condition = "BoolTrue"
            //    否则 condition = "Expression ''cond1 AND cond2''"
            String conditionStr;
            if (conds == null || conds.isEmpty()) {
                conditionStr = "BoolTrue";  // 枚举构造子 BoolTrue
            } else {
                // 用 AND 拼起来
                String joined = String.join(" AND ", conds);
                // 注意：Isabelle里要写两个单引号: ''  并在外面再加两个单引号会冲突
                // 正确写法: Expression ''xxxx''
                // 如果 joined = "d' in get_operable_objects()",
                // 就写: Expression ''d' in get_operable_objects()''
                // 这里简单处理一下:
                conditionStr = "Expression ''" + joined + "''";
            }

            // 3) 如果当前节点是 BlockNode，就把其 IR 内容拿出来
            if (node instanceof BlockNode) {
                for (IR ir : ((BlockNode) node).getIrList()) {
                    // 构造 IRWithBoolExpr
                    IRWithBoolExpr iwbe = new IRWithBoolExpr(
                            ir.getOperation(),
                            ir.getOperands(),
                            conditionStr
                    );
                    result.add(iwbe);
                }
            }
            // 如果是别的节点(EmptyNode等), 不产生 IR
        }

        return result;
    }


    /* ====================================================================
       将 IRWithBoolExpr 的列表, 输出为指定的 Isabelle 定义字符串
       ==================================================================== */

    /**
     * 生成形如：
     *
     * definition test5 :: "IR list" where
     *   "test5 = [
     *     (| ir_operation = FunctionCall GET_OBJS, args = [Identifier ''mid_0''], condition = BoolTrue |),
     *     (| ir_operation = ASSIGN, args = [Identifier ''obj_list'', Identifier ''mid_0''], condition = BoolTrue |),
     *     (| ir_operation = CustomOp APPROACH, args = [StringOperand ''a''], condition = Expression ''d' in get_operable_objects()'' |)
     *   ]"
     */
    public static String toIsabelleWithBoolExpr(List<IRWithBoolExpr> irList, String defName) {
        StringBuilder sb = new StringBuilder();
        sb.append("definition ").append(defName).append(" :: \"IR list\" where\n");
        sb.append("  \"").append(defName).append(" = [\n");

        for (int i = 0; i < irList.size(); i++) {
            IRWithBoolExpr item = irList.get(i);
            sb.append("    (| ir_operation = ").append(item.getOperation())
                    .append(", args = [");

            // 拼操作数
            List<String> operands = item.getOperands();
            for (int j = 0; j < operands.size(); j++) {
                sb.append(operands.get(j));
                if (j < operands.size() - 1) {
                    sb.append(", ");
                }
            }

            sb.append("], condition = ").append(item.getCondition()).append(" |)");

            if (i < irList.size() - 1) {
                sb.append(",\n");
            } else {
                sb.append("\n");
            }
        }

        sb.append("  ]\"\n");
        return sb.toString();
    }

    /**
     * 对多条路径分别生成对应的 definition。
     * 第 i 条路径 => definitionNamePrefix + i
     */
    public static void convertPathsToIsabelle(List<List<NodeWithConditions>> paths, String definitionNamePrefix) {
        int index = 1;
        for (List<NodeWithConditions> path : paths) {
            // 1) 转成带 condition 的 IR 列表
            List<IRWithBoolExpr> irList = toIRWithConditionList(path);

            // 2) 给它起个名字，比如 "test5_1"
            String defName = definitionNamePrefix + index;

            // 3) 生成Isabelle字符串
            String isabelleDef = toIsabelleWithBoolExpr(irList, defName);

            // 4) 打印或存文件
            System.out.println(isabelleDef);

            index++;
        }
    }
}
