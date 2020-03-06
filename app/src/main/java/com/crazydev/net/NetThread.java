package com.crazydev.net;

public class NetThread extends Thread {
    public volatile boolean toFinish = true;
    public volatile int whatToS;

}