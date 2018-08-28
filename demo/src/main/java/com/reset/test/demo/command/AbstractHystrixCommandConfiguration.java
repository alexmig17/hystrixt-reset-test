package com.reset.test.demo.command;

import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;

public abstract class AbstractHystrixCommandConfiguration implements HystrixCommandConfiguration {

    private final String commandKey;

    public AbstractHystrixCommandConfiguration(String commandKey) {
        this.commandKey = commandKey;
    }

    @Override
    public HystrixCircuitBreaker getCircuitBreaker() {
        return HystrixCircuitBreaker.Factory.getInstance(HystrixCommandKey.Factory.asKey(getCommandKey()));
    }

    @Override
    public HystrixCommandMetrics getHystrixMetrics() {
        return HystrixCommandMetrics.getInstance(HystrixCommandKey.Factory.asKey(getCommandKey()));
    }

    public String getCommandKey() {
        return commandKey;
    }
}
