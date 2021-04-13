package com.viettel.app.task;

public interface WriterTaskCallback {
    void onWriteSuccess(long timestampDone, int bytes);
    void onFinish();
}
