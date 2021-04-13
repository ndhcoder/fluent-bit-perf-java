package com.viettel.app.dto;

import lombok.Data;

@Data
public class SummaryMetrics {
    public double sumMem;
    public int sumMemInPercent;
    public long sumDuration;
    public long sumCpu;
    public long sumBytes;
    public long totalLines;
    public int count;

    public String toSummaryString() {
        return "AVG Mem: " + sumMem / count + "M (" + sumMemInPercent / count + "%)" +
                "\n" + "AVG Cpu: " + sumCpu / count + "%" +
                "\n" + "Estimated time: " + (sumDuration / 1000 - 3) + "s" +
                "\n" + "AVG Rate: " + (sumBytes / 1024 / 1024) / (sumDuration / 1000 - 3) + "M/sec" +
                "\n" + "TPS: " + (totalLines / (sumDuration / 1000 - 3)) + "";
    }
}
