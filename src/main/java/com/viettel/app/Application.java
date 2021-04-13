package com.viettel.app;

import com.viettel.app.dto.AppParams;
import com.viettel.app.service.TailWriterService;

import java.util.Arrays;

public class Application {
    public static void main(String[] args) {
        String inputFile = System.getProperty("user.dir") + "/data/input.txt";
        String outputFolder = System.getProperty("user.dir") + "/data/output";

        System.out.println("params: " + Arrays.toString(args));
        if (args.length != 7) {
            System.out.println("please enter params: <input_sample_file> <output_folder_path> " +
                    "<output_number_files> <output_number_lines_per_seconds> <output_prefix_file> <duration_test_in_seconds> <pid_fluent_bit>");

            return;
        }

        AppParams params = AppParams.builder()
                .inputFilePath(args[0])
                .outputFolderPath(args[1])
                .numFiles(Integer.parseInt(args[2]))
                .numLines(Integer.parseInt(args[3]))
                .prefixDummy(args[4])
                .seconds(Integer.parseInt(args[5]))
                .pid(Integer.parseInt(args[6]))
                .build();

        System.out.println("init success app params: " + params);
        TailWriterService service = new TailWriterService();
        service.start(params);
    }

}
