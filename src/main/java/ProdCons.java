package main.java;

import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class ProdCons {
    // Create a list shared by producer and consumer
    // Size of list is 2.
    private FixedSizeQueue<Integer> queue;

    public ProdCons(int capacity) {
        queue = new FixedSizeQueue<>(capacity);
    }

    // Function called by producer thread
    public void produce(int value) throws InterruptedException
    {
        synchronized (this)
        {
            while (queue.isFull())
                wait();
            queue.enqueue(value);
            notify();
        }
    }

    // Function called by consumer thread
    public int consume() throws InterruptedException
    {
        synchronized (this)
        {
            while (queue.isEmpty())
                wait();
            int val = queue.dequeue();
            notify();
            return val;
        }
    }
}

