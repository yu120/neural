package org.micro.neural.circuitbreaker.service;

import java.util.UUID;

public class DemoServiceImpl implements DemoService {

    public String getUuid(int idx) {
        if (idx % 2 == 0) {
            throw new RuntimeException();
        }
        return UUID.randomUUID().toString() + idx;
    }

    public void illegalEx(int idx) {
        if (idx % 2 == 0) {
            throw new IllegalArgumentException();
        }
    }

}
