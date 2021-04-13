package com.viettel.app.dto;

import lombok.Data;

@Data
public class ProcessStat {
    long uTime;
    long uTimeInMs;
    long uTimeInS;
    long sTime;
    long sTimeInMs;
    long sTimeInS;
    String processName;
    long mem;
}
