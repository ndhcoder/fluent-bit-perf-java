package com.viettel.app.task;

import com.viettel.app.dto.AppParams;
import com.viettel.app.utils.FileHelpers;
import com.viettel.app.utils.SystemUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;
import java.io.File;
import java.util.List;

@Data
@Log4j
@NoArgsConstructor
public class TailWriterTask implements Runnable {
    int idTask;
    AppParams params;
    List<String> inputLines;
    File outputFile;
    WriterTaskCallback taskCallback;
    long startTime;

    public TailWriterTask(int idTask, AppParams params, List<String> inputLines, File outputFile, WriterTaskCallback taskCallback) {
        this.idTask = idTask;
        this.params = params;
        this.inputLines = inputLines;
        this.outputFile = outputFile;
        this.taskCallback = taskCallback;
    }

    private void logMessage(String message, boolean forceLog) {
        if (params.debug || forceLog) {
            log.info("Task #" + idTask + ": " + message);
        }
    }

    private void logMessageWithTime(String message, boolean forceLog) {
        if (params.debug || forceLog) {
            log.info("Task #" + idTask + " - " + ((System.currentTimeMillis() - startTime) / 1000) + ": " + message);
        }
    }

    public void run() {
        logMessage("start writer task", false);

        try {
            runTask();
        } catch (Exception e) {
            log.error(e);
        } finally {
            logMessage("stop writer task", false);
            taskCallback.onFinish();
        }
    }

    private void runTask() {
        String content = String.join("\n", inputLines);

        int cnt = 0;
        startTime = System.currentTimeMillis();

        while (cnt < params.seconds) {
            FileHelpers.appendToFile(outputFile, content + "\n");
            cnt++;

            logMessageWithTime("write " + inputLines.size() + " success to " + outputFile.getName(), false);
            long currentTime = System.currentTimeMillis();
            SystemUtils.sleep(1000);
            taskCallback.onWriteSuccess(currentTime, content.getBytes().length);
        }
    }
}
