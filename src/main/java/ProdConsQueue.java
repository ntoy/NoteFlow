package main.java;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ArrayBlockingQueue;

public class ProdConsQueue {
    private ArrayBlockingQueue<Integer> queue;
    private Boolean streamOver;
    private Thread producer;
    volatile boolean keepGoing;

    public ProdConsQueue(int capacity) {
        streamOver = false;
        keepGoing = true;
        queue = new ArrayBlockingQueue<>(capacity);
        // Create producer thread
        producer = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                BufferedReader bufferedReader = null;
                try {
                    bufferedReader = Files.newBufferedReader((new File("/Users/Nico/dummy.txt")).toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int next = -1;
                try {
                    while((next = bufferedReader.read()) != -1 && keepGoing) {
                        queue.put(next);
                    }
                    if (keepGoing) {
                        queue.put(-1);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        producer.start();
    }

    public int getNext() {
        if (keepGoing) {
            // Create consumer thread
            if (streamOver) {
                return -1;
            } else {
                int next = -1;
                try {
                    next = queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (next == -1) {
                    streamOver = true;
                    try {
                        producer.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return next;
            }
        }
        else {
            throw new RuntimeException("Cannot get next: producer is dead");
        }
    }

    public void close() {
        keepGoing = false;
        queue.clear();
        try {
            producer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        queue = null;
    }

    public static void main(String[] args)
            throws InterruptedException
    {
        ProdConsQueue prodConsQueue = new ProdConsQueue(1);
        int next;

        for (int i = 0; i < 100; i++) {
            next = prodConsQueue.getNext();
            if (next >= 0)
                System.out.println((char) next);
            else System.out.println(".-.");
        }
        prodConsQueue.close();
    }
}
