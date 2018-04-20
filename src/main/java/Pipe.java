package main.java;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;

public class Pipe<T> {
    private ArrayBlockingQueue<Optional<T>> queue;
    private boolean closed;

    public PipeSource source;
    public PipeSink sink;
    public int inCount;
    public int outCount;

    public Pipe(int capacity) {
        queue = new ArrayBlockingQueue<>(capacity);
        closed = false;
        source = new PipeSource();
        sink = new PipeSink();
    }

    public int getInCount() {
        return inCount;
    }

    public int getOutCount() {
        return outCount;
    }

    public class PipeSource {
        // cannot be instantiated directly
        private PipeSource() {}

        public void write(T e){
            try {
                queue.put(Optional.of(e));
                inCount++;
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }

        public void close(){
            try {
                queue.put(Optional.empty());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public class PipeSink {
        // cannot be instantiated directly
        private PipeSink() {}

        public T read(){
            if (closed) return null;
            Optional<T> optional = null;
            try {
                optional = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (optional.isPresent()) {
                outCount++;
                return optional.get();
            }
            else {
                closed = true;
                return null;
            }
        }
    }
}
