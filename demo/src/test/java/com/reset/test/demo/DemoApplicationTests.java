package com.reset.test.demo;

import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.metric.consumer.HealthCountsStream;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.reset.test.demo.command.AbstractCommandExceptionCatcher;
import com.reset.test.demo.command.HystrixCommandConfiguration;
import com.reset.test.demo.command.HystrixCommandExceptionCatcher;
import com.reset.test.demo.command.exception.ExecuteCommandTestException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static com.reset.test.demo.command.impl.HystrixCommandStandardConfiguration.KEY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DemoApplicationTests {

    private static final Boolean PRINT_STATISTICS = true;
    private static final String SUCCESS_CASE = "success";
    private static final String FAILURE_CASE = "failureCase";
    @Autowired
    @Qualifier("hystrixCommandStandardConfiguration")
    private HystrixCommandConfiguration hystrixCommandStandardConfiguration;
    @MockBean(name = "mockedBehavior")
    HystrixCommandConfiguration mockedBehavior;
    private HystrixCommandExceptionCatcher hystrixCommandWrapper;

    @Before
    public void setUp() {
        doNothing().when(mockedBehavior).executeCommand(SUCCESS_CASE);
        doThrow(new ExecuteCommandTestException()).when(mockedBehavior).executeCommand(FAILURE_CASE);
        hystrixCommandWrapper = new AbstractCommandExceptionCatcher(hystrixCommandStandardConfiguration,
                Collections.singletonList(ExecuteCommandTestException.class), PRINT_STATISTICS) {
        };
        Hystrix.reset();
        HystrixPlugins.reset();
        HealthCountsStream.reset();
        circuitBreakerAssertNull();

    }

    /*
    docs:
    https://github.com/Netflix/Hystrix/wiki/How-it-Works#flow2
    see 'Circuit Breaker' block

    The precise way that the circuit opening and closing occurs is as follows:

    1) Assuming the volume across a circuit meets a certain threshold (HystrixCommandProperties.circuitBreakerRequestVolumeThreshold())...
    2) And assuming that the error percentage exceeds the threshold error percentage (HystrixCommandProperties.circuitBreakerErrorThresholdPercentage())...
    3) Then the circuit-breaker transitions from CLOSED to OPEN.
    4) While it is open, it short-circuits all requests made against that circuit-breaker.
    5) After some amount of time (HystrixCommandProperties.circuitBreakerSleepWindowInMilliseconds()), the next single request is let through (this is the HALF-OPEN state). If the request fails, the circuit-breaker returns to the OPEN state for the duration of the sleep window. If the request succeeds, the circuit-breaker transitions to CLOSED and the logic in 1. takes over again.
     */


    /**
     * basket is new, First result is failure, check does hystrixCircuitBreaker open
     * expected result is true.
     * input data:
     * threshold == 1,  percentage = 50, basket total count = 1, success = 0, failure = 1
     * Step by step:
     * 1) step. basket total count => threshold. it mean that logic will calculate percentage
     * 1 >= 1
     * 2) step. (failure)/(success +failure) = % of errors if % > threshold percentage - open
     * 1/(0+1) = 100%; 100% >= 50% - open
     */
    @Test
    public void test_CircuitOpen_When_Threshold_Is_One_FirstCommand_Is_FailureCase() {
        hystrixSetUp(1, 50);
        hystrixCommandWrapper.executeCommand(FAILURE_CASE);
        HystrixCircuitBreaker hystrixCircuitBreaker = getCircuitBreakerAssertNotNull();
        sleep();
        Assert.assertTrue(hystrixCircuitBreaker.isOpen());
    }

    /**
     * basket is new, First result is failure, check does hystrixCircuitBreaker open
     * expected result is true.
     * input data:
     * thresholdVolume == 1,  thresholdPercentage = 100, basket total count = 1, success = 0, failure = 1
     * Step by step:
     * 1) step. basket total count => threshold. it mean that logic will calculate percentage
     * 1 >= 1
     * 2) step. (failure)/(success +failure) = % of errors if % > threshold percentage - open
     * 1/(0+1) = 100%; 100% >= 100% - open
     */
    @Test
    public void test_CircuitOpen_When_Threshold_Is_One_FirstCommand_Is_FailureCase2() {
        hystrixSetUp(1, 100);
        hystrixCommandWrapper.executeCommand(FAILURE_CASE);
        HystrixCircuitBreaker hystrixCircuitBreaker = getCircuitBreakerAssertNotNull();
        sleep();
        Assert.assertTrue(hystrixCircuitBreaker.isOpen());
    }

    /**
     * basket is new, First result is failure, check does hystrixCircuitBreaker open
     * expected result is true.
     * input data:
     * thresholdVolume == 1,  thresholdPercentage = 11, basket total count = 1, success = 0, failure = 1
     * Step by step:
     * 1) step. basket total count => threshold. it mean that logic will calculate percentage
     * 1 >= 1
     * 2) step. (failure)/(success +failure) = % of errors if % > threshold percentage - open
     * 1/(0+1) = 100%; 100% >= 1% - open
     */
    @Test
    public void test_CircuitOpen_When_Threshold_Is_One_FirstCommand_Is_FailureCase3() {
        hystrixSetUp(1, 1);
        hystrixCommandWrapper.executeCommand(FAILURE_CASE);
        HystrixCircuitBreaker hystrixCircuitBreaker = getCircuitBreakerAssertNotNull();
        sleep();
        Assert.assertTrue(hystrixCircuitBreaker.isOpen());
    }

    /**
     * basket is new, First result is success, second is failure check does hystrixCircuitBreaker open
     * expected result is true.
     * input data:
     * thresholdVolume == 1,  thresholdPercentage = 50, basket total count = 2, success = 1, failure = 1
     * Step by step:
     * 1) step. basket total count => threshold. it mean that logic will calculate percentage
     * 2 >= 1
     * 2) step. (failure)/(success +failure) = % of errors if % > threshold percentage - open
     * 1/(1+1) = 50%; 50% >= 50% - open
     */
    @Test
    public void test_CircuitOpen_When_Threshold_Is_One_FirstCommand_Is_SuccessCase() {
        hystrixSetUp(1, 50);
        hystrixCommandWrapper.executeCommand(SUCCESS_CASE);
        hystrixCommandWrapper.executeCommand(FAILURE_CASE);
        HystrixCircuitBreaker hystrixCircuitBreaker = getCircuitBreakerAssertNotNull();
        sleep();
        Assert.assertTrue(hystrixCircuitBreaker.isOpen());
    }

    /*

     */

    /**
     * basket is new, 1,2 result is success, 3 is failure check does hystrixCircuitBreaker open
     * expected result is true.
     * input data:
     * thresholdVolume == 1,  thresholdPercentage = 30, basket total count = 3, success = 2, failure = 1
     * Step by step:
     * 1) step. basket total count => threshold. it mean that logic will calculate percentage
     * 3 >= 1
     * 2) step. (failure)/(success +failure) = % of errors if % > threshold percentage - open
     * 1/(2+1) = 33,33%; 33,33% >= 30% ? true. - will open
     */
    @Test
    public void test_CircuitOpen_When_Threshold_Is_One_FirstCommand_Is_SuccessCase2() {
        hystrixSetUp(1, 30);
        hystrixCommandWrapper.executeCommand(SUCCESS_CASE);
        hystrixCommandWrapper.executeCommand(SUCCESS_CASE);
        hystrixCommandWrapper.executeCommand(FAILURE_CASE);
        HystrixCircuitBreaker hystrixCircuitBreaker = getCircuitBreakerAssertNotNull();
        sleep();
        Assert.assertTrue(hystrixCircuitBreaker.isOpen());
    }

    /**
     * basket is new, 1,2 result is success, 3 is failure. C
     * heck does hystrixCircuitBreaker open. expected result is true.
     * input data:
     * thresholdVolume == 1,  thresholdPercentage = 34, basket total count = 3, success = 2, failure = 1
     * Step by step:
     * 1) step. basket total count => threshold.
     * 3 >= 1 it mean that logic will calculate percentage
     * 2) step. (failure)/(success +failure) = % of errors if % > threshold percentage - open
     * 1/(2+1) = 33,33%; 33,33% >= 34% ? false. - will not open
     */
    @Test
    public void test_CircuitClose_When_Threshold_Is_One_FirstCommand_Is_SuccessCase3() {
        hystrixSetUp(1, 34);
        hystrixCommandWrapper.executeCommand(SUCCESS_CASE);
        hystrixCommandWrapper.executeCommand(SUCCESS_CASE);
        hystrixCommandWrapper.executeCommand(FAILURE_CASE);
        HystrixCircuitBreaker hystrixCircuitBreaker = getCircuitBreakerAssertNotNull();
        sleep();
        Assert.assertFalse(hystrixCircuitBreaker.isOpen());
    }

    /**
     * basket is new, 1,2 result is success, 3 is failure.
     * Check does hystrixCircuitBreaker open, expected result is false.
     * input data:
     * thresholdVolume == 4,  thresholdPercentage = 30, basket total count = 3, success = 2, failure = 1
     * Step by step:
     * 1) step. basket total count => threshold. it mean that logic will calculate percentage
     * 3 >= 4 ? false
     * 2) step. will not check => will not open
     */
    @Test
    public void test_CircuitClose_When_Threshold_Is_Four_First_Command_Is_SuccessCase() {
        hystrixSetUp(4, 30);
        hystrixCommandWrapper.executeCommand(SUCCESS_CASE);
        HystrixCircuitBreaker hystrixCircuitBreaker = getCircuitBreakerAssertNotNull();
        hystrixCommandWrapper.executeCommand(SUCCESS_CASE);
        hystrixCommandWrapper.executeCommand(FAILURE_CASE);
        sleep();
        Assert.assertFalse(hystrixCircuitBreaker.isOpen());
    }

    /**
     * basket is new, 1,2 result is success, 3 is failure.
     * Check does hystrixCircuitBreaker open, expected result is true.
     * input data:
     * thresholdVolume == 3,  thresholdPercentage = 30, basket total count = 3, success = 2, failure = 1
     * Step by step:
     * 1) step. basket total count => threshold. it mean that logic will calculate percentage
     * 3 >= 3 ? true
     * 2) step. (failure)/(success +failure) = % of errors if % > threshold percentage - open
     * 1/(2+1) = 33,33%; 33,33% >= 30% ? true. - will open
     */
    @Test
    public void test_When_Threshold_Is_Three_First_Command_Is_SuccessCase() {
        hystrixSetUp(3, 30);

        hystrixCommandWrapper.executeCommand(SUCCESS_CASE);
        hystrixCommandWrapper.executeCommand(SUCCESS_CASE);
        hystrixCommandWrapper.executeCommand(FAILURE_CASE);
        HystrixCircuitBreaker hystrixCircuitBreaker = getCircuitBreakerAssertNotNull();
        sleep();
        Assert.assertTrue(hystrixCircuitBreaker.isOpen());
    }


    private void hystrixSetUp(int thresholdVolume, int thresholdPercentage) {
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command." + KEY +
                ".circuitBreaker.requestVolumeThreshold", thresholdVolume);
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command." + KEY +
                ".circuitBreaker.errorThresholdPercentage", thresholdPercentage);
    }

    private HystrixCircuitBreaker getCircuitBreaker() {
        return hystrixCommandWrapper.getCircuitBreaker();
    }

    private void circuitBreakerAssertNull() {
        HystrixCircuitBreaker hystrixCircuitBreaker = getCircuitBreaker();
        assertNull(hystrixCircuitBreaker);
    }

    private HystrixCircuitBreaker getCircuitBreakerAssertNotNull() {
        HystrixCircuitBreaker hystrixCircuitBreaker = getCircuitBreaker();
        assertNotNull(hystrixCircuitBreaker);
        return hystrixCircuitBreaker;
    }

    private HystrixCommandMetrics getHystrixMetrics() {
        return hystrixCommandWrapper.getHystrixMetrics();
    }

    private void sleep() {
        if (!PRINT_STATISTICS) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
