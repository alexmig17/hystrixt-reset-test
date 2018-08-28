package com.reset.test.demo.command;

import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandMetrics;

public interface HystrixCommandConfiguration {

    void executeCommand(String testCase);
    HystrixCircuitBreaker getCircuitBreaker();
    HystrixCommandMetrics getHystrixMetrics();

}
