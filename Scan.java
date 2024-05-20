import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件扫描类，用于读取文本文件内容。
 */
public class Scan {
    private String fileName; // 文件名

    /**
     * Scan 类的构造函数。
     *
     * @param fileName 要扫描的文件名。
     */
    public Scan(String fileName) {
        this.fileName = fileName;
    }

    /**
     * 读取文本文件内容并返回字符串数组。
     *
     * @return 包含文件每行内容的字符串数组。
     */
    public String[] readText() {
        List<String> contentList = new ArrayList<>();
        File file = new File(fileName).getAbsoluteFile();

        // 检查文件是否存在于当前目录
        if (!file.exists()) {
            // 尝试在当前目录的input文件夹中查找文件
            String basePath = new File("").getAbsolutePath();
            String inputPath = basePath + File.separator + "input" + File.separator + file.getName();
            file = new File(inputPath);

            if (!file.exists()) {
                // 尝试在与out文件夹同级的目录中查找文件
                String outSiblingPath = basePath + File.separator + ".." + File.separator + "input" + File.separator + file.getName();
                file = new File(outSiblingPath);

                if (!file.exists()) {
                    System.err.println("File not found at any given path.");
                    return new String[0]; // 如果文件在所有路径下都未找到，返回空数组
                }
            }
        }

        // 尝试读取文件
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.trim().startsWith("//")) {
                    contentList.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getPath());
            e.printStackTrace();
            return new String[0];
        }

        return contentList.toArray(new String[0]);
    }
}
