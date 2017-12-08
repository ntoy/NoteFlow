package main.java;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ProdConsInterface {

    private final ProdCons pc;
    private boolean streamOver;
    private Thread producer;

    public ProdConsInterface(int capacity) {
        streamOver = false;
        pc = new ProdCons(capacity);
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
                    while((next = bufferedReader.read()) != -1) {
                        pc.produce(next);
                    }
                    pc.produce(-1);
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
        // Create consumer thread
        if (streamOver) {
            return -1;
        }
        else {
            int next = -1;
            try {
                next = pc.consume();
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

    public static void main(String[] args)
            throws InterruptedException
    {
        ProdConsInterface prodConsInterface = new ProdConsInterface(6);
        int next;

        while ((next = prodConsInterface.getNext()) != -1) {
            System.out.println((char) next);
        }
    }
}
