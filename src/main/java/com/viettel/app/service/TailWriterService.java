package com.viettel.app.service;

import com.viettel.app.dto.AppParams;
import com.viettel.app.dto.MonitorInfoBean;
import com.viettel.app.dto.ProcessStat;
import com.viettel.app.dto.SummaryMetrics;
import com.viettel.app.task.TailWriterTask;
import com.viettel.app.task.WriterTaskCallback;
import com.viettel.app.utils.FileHelpers;
import com.viettel.app.exception.TailWriteException;
import com.viettel.app.utils.ProcessUtils;
import com.viettel.app.utils.SystemUtils;
import lombok.extern.log4j.Log4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Log4j
public class TailWriterService {

    public TailWriterService() {

    }

    private List<String> getInputLines(String inputFilePath, int numLines) {
        List<String> lines = FileHelpers.readFirstNLine(inputFilePath, numLines);

        if (lines.isEmpty()) {
            throw new TailWriteException("input file is empty");
        }

        while (lines.size() < numLines) {
            lines.addAll(lines.subList(0, Math.min(numLines - lines.size(), lines.size())));
        }

        return lines;
    }

    private List<File> getDummyFiles(String outputFileFolderPath, int numFiles, String prefixDummyFile) {
        File folder = new File(outputFileFolderPath);
        if (!folder.exists() || folder.isFile()) {
            throw new TailWriteException("output path must be a folder");
        }

        List<File> dummyFiles = new ArrayList<File>();
        for (int i = 0; i < numFiles; i++) {
            File file = new File(outputFileFolderPath, prefixDummyFile + "_" + i + ".log");
            if (file.isDirectory()) {
                throw new TailWriteException("dummy path must be a file");
            }

            if (!file.exists()) {
                try {
                    if (!file.createNewFile()) {
                        throw new TailWriteException("cannot create dummy file");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new TailWriteException("cannot create dummy file");
                }
            }

            dummyFiles.add(file);
        }

        return dummyFiles;
    }

    private void reportProcess(AppParams params, int records, int bytes, int countTimeWriter, SummaryMetrics summaryMetrics, long time) {
        new Thread(() -> {
            MonitorInfoBean monitorInfoBean = ProcessUtils.getMonitorProcessBean(params.getPid());
            log.info(String.format("write done #%s: seconds=%s, records=%s, write (b)=%s, write=%sM, %s",
                    countTimeWriter, time, records, bytes, (bytes/1024/1024), monitorInfoBean));

            synchronized (summaryMetrics) {
                summaryMetrics.sumMemInPercent += monitorInfoBean.getMemUsage();
                summaryMetrics.sumMem += monitorInfoBean.getMemUseSize();
                summaryMetrics.sumCpu += monitorInfoBean.getCpuUsage();
                summaryMetrics.count++;
            }
        }).start();
    }

    private void reportSummary(AppParams params, SummaryMetrics metrics) {
        MonitorInfoBean monitorInfoBean = ProcessUtils.getMonitorProcessBean(params.pid);
        log.info(String.format("\n===== SUMMARY =====\n%s\n%s", monitorInfoBean, metrics.toSummaryString()));
    }

    public void start(AppParams params) {
        SummaryMetrics summaryMetrics = new SummaryMetrics();

        int waitTime = 3;
        AtomicInteger countFinishTask = new AtomicInteger();
        AtomicInteger countTimeWrite = new AtomicInteger();
        AtomicInteger countWriterSuccessTask = new AtomicInteger();
        AtomicLong lastTime = new AtomicLong(System.currentTimeMillis());
        long startTime = lastTime.get();

        List<String> inputLines = getInputLines(params.inputFilePath, params.numLines);
        List<File> dummyFiles = getDummyFiles(params.outputFolderPath, params.numFiles, params.prefixDummy);
        ExecutorService executor = Executors.newFixedThreadPool(params.numFiles);

        MonitorInfoBean monitorInfoBean = ProcessUtils.getMonitorProcessBean(params.pid);
        if (monitorInfoBean.getProcessName() == null) {
            throw new TailWriteException("process with pid " + params.pid + " not found");
        }

        WriterTaskCallback taskCallback = new WriterTaskCallback() {
            @Override
            public synchronized void onWriteSuccess(long timestampDone, int bytes) {
                countWriterSuccessTask.incrementAndGet();
                summaryMetrics.sumBytes += bytes;
                summaryMetrics.totalLines += params.numLines;

                if (countWriterSuccessTask.get() == params.numFiles) {
                    countTimeWrite.addAndGet(1);
                    reportProcess(params, params.numLines, bytes, countTimeWrite.get(), summaryMetrics, (timestampDone - startTime) / 1000);
                    countWriterSuccessTask.set(0);
                    summaryMetrics.sumDuration += (timestampDone - lastTime.get());
                    lastTime.set(timestampDone);
                }
            }

            @Override
            public void onFinish() {
                countFinishTask.incrementAndGet();

                if (countFinishTask.get() == params.numFiles) {

                    int cnt = 0;
                    do {
                        ProcessStat stat1 = ProcessUtils.getProcessStat(params.pid);
                        SystemUtils.sleep(1000);
                        ProcessStat stat2 = ProcessUtils.getProcessStat(params.pid);

                        long currentTime = System.currentTimeMillis();
                        summaryMetrics.sumDuration += (currentTime - lastTime.get());
                        reportProcess(params, 0, 0, 0, summaryMetrics, (currentTime - startTime) / 1000);
                        lastTime.set(currentTime);

                        if (stat2.getUTimeInMs() - stat1.getUTimeInMs() == 0) {
                            cnt++;
                        } else {
                            if (cnt > 0) {
                                cnt = 0;
                            }
                        }
                    } while (cnt < waitTime);

                    log.info("stop service. Done task");
                    reportSummary(params, summaryMetrics);
                }
            }
        };

        try {
            for (int i = 0; i < params.numFiles; i++) {
                TailWriterTask task = new TailWriterTask(i, params, inputLines, dummyFiles.get(i), taskCallback);
                executor.execute(task);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.info("shut down executor");
            executor.shutdown();
        }
    }
}
