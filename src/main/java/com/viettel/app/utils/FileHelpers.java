package com.viettel.app.utils;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.log4j.lf5.util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class FileHelpers {
    public static List<String> readFirstNLine(String filePath, long numLine) {
        List<String> lines = new ArrayList<String>();
        File file = new File(filePath);
        LineIterator it = null;
        try {
            it = FileUtils.lineIterator(file, "UTF-8");
            while (it.hasNext() && numLine > 0) {
                String line = it.nextLine();
                lines.add(line);
                numLine--;
                // do something with line
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lines;
    }

    public static String readFile(String filePath) {
        try {
            return FileUtils.readFileToString(new File(filePath), Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static void appendToFile(File file, String content) {
        try {
            FileUtils.writeStringToFile(
                    file, content, Charsets.toCharset("UTF-8"), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
