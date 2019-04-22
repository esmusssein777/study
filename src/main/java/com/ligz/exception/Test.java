package com.ligz.exception;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * author:ligz
 */
public class Test {
    BlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(5);
    BlockingQueue<String> queue2 = new LinkedBlockingDeque<>(10);
}
