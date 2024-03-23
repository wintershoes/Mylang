import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Scan {
    private String fileName; // 要扫描的文件名

    /**
     * Scan 类的构造函数。
     * 
     * @param fileName 要扫描的文件名
     */
    public Scan(String fileName) {
        this.fileName = fileName; // 初始化 fileName
    }

    /**
     * 读取文件的文本内容并以字符串形式返回。
     * 
     * @return 文件的文本内容
     */
    public String[] readText() {
        List<String> contentList = new ArrayList<>(); // 用于存储文件内容的列表
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.trim().startsWith("//")) {
                    contentList.add(line); // 将非空行且不以"//"开头的内容添加到列表中
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // 如果发生 IOException，则打印堆栈跟踪信息
        }
        return contentList.toArray(new String[0]); // 将列表转换为字符串数组并返回
    }
}