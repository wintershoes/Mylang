package org.main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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

        // 使用类加载器获取资源
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("input/" + fileName);

        if (inputStream == null) {
            System.err.println("File not found in resources/input directory.");
            return new String[0]; // 如果文件未找到，返回空数组
        }

        // 尝试读取文件
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                contentList.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading file from resources.");
            e.printStackTrace();
            return new String[0];
        }

        return contentList.toArray(new String[0]);
    }

    public String[] readTextFromPath() {
        List<String> contentList = new ArrayList<>();

        // 使用绝对路径读取文件
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                contentList.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading file from path: " + fileName);
            e.printStackTrace();
            return new String[0];
        }

        return contentList.toArray(new String[0]);
    }
}
