package main.java;

public class Printer implements Runnable {
    Pipe.PipeSink inputPipe;

    public Printer(Pipe.PipeSink inputPipe) {
        this.inputPipe = inputPipe;
    }

    @Override
    public void run() {
        Object o;
        while ((o = inputPipe.read()) != null) {
            System.out.println(o);
        }
    }
}
