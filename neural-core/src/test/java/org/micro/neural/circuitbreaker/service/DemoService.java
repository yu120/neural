package org.micro.neural.circuitbreaker.service;

import org.micro.neural.circuitbreaker.annotation.GuardByCircuitBreaker;

public interface DemoService {

    @GuardByCircuitBreaker(noTripExceptions = {})
    String getUuid(int idx);

    @GuardByCircuitBreaker(noTripExceptions = {IllegalArgumentException.class})
    void illegalEx(int idx);

}
