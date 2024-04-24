import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件扫描类，用于读取文本文件内容。
 */
public class Scan {
    private String fileName; // 文件名
    /*
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
        // 构建当前文件的绝对路径
        File file = new File(fileName).getAbsoluteFile();

        // 检查文件是否存在
        if (!file.exists()) {
            // 如果文件不存在，则尝试回到out文件夹同级的目录下找input文件夹
            Path currentDirectory = Paths.get("").toAbsolutePath();
            Path outDirectory = currentDirectory.getParent(); // 获取out目录的父目录，即项目根目录
            Path inputDirectory = outDirectory.resolve("input"); // 构建指向同级input目录的路径
            Path newFilePath = inputDirectory.resolve(fileName); // 构建最终的文件路径
            file = newFilePath.toFile(); // 更新文件路径
            if (!file.exists()) {
                // 则在当前目录下找input文件夹
                Path currentInputDirectory = currentDirectory.resolve("input");
                newFilePath = currentInputDirectory.resolve(fileName);
                file = newFilePath.toFile(); // 更新文件路径
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
        }

        return contentList.toArray(new String[0]);
    }
}