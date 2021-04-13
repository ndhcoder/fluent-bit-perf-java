package com.viettel.app.dto;

import lombok.Data;

@Data
public class MonitorInfoBean {
    private int pid;

    /** Process name */
    private String processName;

    /** cpu usage rate */
    private double cpuUsage;

    /** Memory usage */
    private double memUsage;

    /** Memory usage in bytes */
    private double memUsageInBytes;

    /** Size of memory usage */
    private double memUseSize;

    private ProcessStat processStat;

    public String toString() {
        return "pid=" + pid + ", process=" + processStat.processName + ", cpu=" + cpuUsage + "%, mem="
                + memUsage + "%, memSize=" + memUseSize + "mb" + ", utime=" + processStat.getUTimeInMs() +"ms, stime=" + processStat.getSTimeInMs() +"ms";
    }
}
