package com.viettel.app.utils;

import com.viettel.app.dto.MonitorInfoBean;
import com.viettel.app.dto.ProcessStat;
import com.viettel.app.dto.SysConf;
import lombok.extern.log4j.Log4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Log4j
public class ProcessUtils {
    final static boolean isNotWindows = System.getProperties().getProperty("os.name").toLowerCase().indexOf("windows") < 0;
    final static BigDecimal DIVISOR = BigDecimal.valueOf(1024);
    static int SC_LK;

    static {
        SC_LK = Integer.parseInt(getSystemConfig(SysConf._SC_CLK_TCK));
    }

    public static void main(String[] args) {
        System.out.println(getProcessStat(75358));
    }

    public static MonitorInfoBean getMonitorProcessBean(int pid) {
        MonitorInfoBean monitorInfo = new MonitorInfoBean();
        monitorInfo.setPid(pid);

        if(!isNotWindows){
            monitorInfo.setMemUsage(500);
            return monitorInfo;
        }

        Runtime rt = Runtime.getRuntime();
        BufferedReader in = null;
        try {
            String[] cmd = {
                    "/bin/sh",
                    "-c",
                    "top -b -n 1 | grep " + pid
            };
            Process p = rt.exec(cmd);
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String str = null;
            String[] strArray = null;
            while ((str = in.readLine()) != null) {
                log.debug("top: " + str);
                int m = 0;

                strArray = str.split(" ");
                for (int i = 0; i < strArray.length; i++) {
                    String info = strArray[i];
                    if (info.trim().length() == 0){
                        continue;
                    }
                    if (m == 5) {//The fifth column is the physical memory value occupied by the process
                        String unit = info.substring(info.length() - 1);
                        if(unit.equalsIgnoreCase("g")) {
                            BigDecimal memUseSize = new BigDecimal(info.substring(0, info.length() - 1));
                            monitorInfo.setMemUseSize(memUseSize.doubleValue() * 1024);
                        } else if(unit.equalsIgnoreCase("m")) {
                            BigDecimal memUseSize = new BigDecimal(info.substring(0, info.length() - 1));
                            monitorInfo.setMemUseSize(memUseSize.doubleValue());
                        } else {
                            BigDecimal memUseSize = new BigDecimal(info);
                            monitorInfo.setMemUseSize(memUseSize.divide(DIVISOR, 2, RoundingMode.HALF_UP).doubleValue());
                        }
                    }

                    if (m == 8) {//The 9th column is the percentage of CPU usage
                        monitorInfo.setCpuUsage(Double.parseDouble(info));
                    }

                    if (m == 9) {//The 10th column is the percentage of memory used.
                        monitorInfo.setMemUsage(Double.parseDouble(info));
                    }

                    if (m == 11) {//The 10th column is the process name
                        monitorInfo.setProcessName(info);
                    }

                    m++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (monitorInfo.getProcessName() != null) {
            monitorInfo.setProcessStat(getProcessStat(pid));
        }
        return monitorInfo;
    }

    public static String getSystemConfig(String config) {
        Runtime rt = Runtime.getRuntime();
        BufferedReader in = null;
        String result = "";
        try {
            String[] cmd = {
                    "/bin/sh",
                    "-c",
                    "getconf " + config
            };
            Process p = rt.exec(cmd);
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String str = null;
            String[] strArray = null;
            while ((str = in.readLine()) != null) {
                result = str;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

        return result;
    }

    public static ProcessStat getProcessStat(int pid) {
        String content = FileHelpers.readFile("/proc/" + pid + "/stat");
        if (content.isEmpty()) {
            throw new RuntimeException("can not read stat proc " + pid);
        }

        int cpuHz = SC_LK;
        ProcessStat stat = new ProcessStat();
        String[] contents = content.split("\\s");
        stat.setProcessName(contents[1]);
        stat.setUTime(Long.parseLong(contents[13]));
        stat.setSTime(Long.parseLong(contents[14]));
        stat.setMem(Long.parseLong(contents[23]));
        stat.setUTimeInS(stat.getUTime() / cpuHz);
        stat.setUTimeInMs(stat.getUTime() * 1000 / cpuHz);
        stat.setSTimeInS(stat.getSTime() / cpuHz);
        stat.setSTimeInMs(stat.getSTime() * 1000 / cpuHz);

        return stat;
    }
}
