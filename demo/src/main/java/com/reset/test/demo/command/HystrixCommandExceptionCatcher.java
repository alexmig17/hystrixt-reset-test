package com.reset.test.demo.command;

public interface HystrixCommandExceptionCatcher extends HystrixCommandConfiguration{

    void catchException(RuntimeException e);
}
