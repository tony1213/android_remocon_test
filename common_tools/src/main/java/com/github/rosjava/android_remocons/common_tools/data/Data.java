package com.github.rosjava.android_remocons.common_tools.data;

import com.github.rosjava.zeroconf_jmdns_suite.jmdns.DiscoveredService;

import java.util.ArrayList;

public class Data {
    private ArrayList<DiscoveredService> discoveredServices;
    private Object mutex;

    public Data() {
        mutex = new Object();
    }

    public void Wait() {
        synchronized (mutex) {
            try {
                mutex.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void Notify() {
        synchronized (mutex) {
            mutex.notify();
        }
    }

    public void setDiscoveredServices(ArrayList<DiscoveredService> discoveredServices) {
        this.discoveredServices = discoveredServices;
    }

    public ArrayList<DiscoveredService> getDiscoveredServices() {
        return this.discoveredServices;
    }

}