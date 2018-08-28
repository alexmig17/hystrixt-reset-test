package com.reset.test.demo.command.impl;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.reset.test.demo.command.AbstractHystrixCommandConfiguration;
import com.reset.test.demo.command.HystrixCommandConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service("hystrixCommandStandardConfiguration")
public class HystrixCommandStandardConfiguration extends AbstractHystrixCommandConfiguration {

    public static final String KEY = "HystrixCommandStandardConfiguration";
    @Autowired
    @Qualifier("mockedBehavior")
    private HystrixCommandConfiguration mockedBehavior;

    public HystrixCommandStandardConfiguration() {
        super(KEY);
    }

    @HystrixCommand(commandKey = KEY)
    @Override
    public void executeCommand(String testCase) {
        mockedBehavior.executeCommand(testCase);
    }
}
