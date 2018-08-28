package com.reset.test.demo.command;

import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandMetrics;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

import static com.reset.test.demo.command.impl.HystrixCommandStandardConfiguration.KEY;

public abstract class AbstractCommandExceptionCatcher implements HystrixCommandExceptionCatcher{

    private final HystrixCommandConfiguration hystrixCommandConfiguration;
    private final List<Class<? extends RuntimeException>> excludeExceptions;
    private final Boolean printStatistics;

    protected AbstractCommandExceptionCatcher(HystrixCommandConfiguration hystrixCommandConfiguration,
                                              List<Class<? extends RuntimeException>> excludeExceptions, Boolean printStatistics) {
        this.hystrixCommandConfiguration = hystrixCommandConfiguration;
        this.excludeExceptions = excludeExceptions;
        this.printStatistics = printStatistics;
    }

    @Override
    public void executeCommand(String testCase) {
        try {
            hystrixCommandConfiguration.executeCommand(testCase);
        } catch (RuntimeException e) {
            catchException(e);
        }
        printStatistic();
    }

    @Override
    public void catchException(RuntimeException e) {
        if (CollectionUtils.isNotEmpty(getExcludeExceptions()) ) {
            for (Class<? extends RuntimeException> exception: getExcludeExceptions()) {
                if (e.getClass().isAssignableFrom(exception)) {
                    return;
                }
            }
        }
        throw e;
    }

    @Override
    public HystrixCircuitBreaker getCircuitBreaker() {
        return hystrixCommandConfiguration.getCircuitBreaker();
    }

    @Override
    public HystrixCommandMetrics getHystrixMetrics() {
        return hystrixCommandConfiguration.getHystrixMetrics();
    }

   public List<Class<? extends RuntimeException>> getExcludeExceptions() {
        return excludeExceptions;
    }

    private void printStatistic() {

        if (printStatistics) {
            sleep();
            HystrixCommandMetrics.HealthCounts healthCounts = getHystrixMetrics().getHealthCounts();
            System.out.println();
            System.out.println("************************ start ************************");
            System.out.println(String.format("Error count - %s", healthCounts.getErrorCount()));
            System.out.println(String.format("Error percentage - %s", healthCounts.getErrorPercentage()));
            System.out.println(String.format("Total requests - %s", healthCounts.getTotalRequests()));
            System.out.println(String.format("Is open - %s", getCircuitBreaker().isOpen()));
            System.out.println("************************ end ************************");
            System.out.println();
            System.out.println();
        }
    }

    private void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
