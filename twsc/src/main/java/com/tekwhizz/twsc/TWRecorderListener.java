package com.tekwhizz.twsc;


public interface TWRecorderListener {
    void TWRecorderOnComplete();
    void TWRecorderOnError(int errorCode, String reason);
}