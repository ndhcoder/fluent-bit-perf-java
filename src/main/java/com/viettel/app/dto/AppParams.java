package com.viettel.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppParams {
    public int pid;
    public String inputFilePath;
    public String outputFolderPath;
    public String prefixDummy;
    public int seconds;
    public int numFiles;
    public int numLines;
    public boolean debug;
}
