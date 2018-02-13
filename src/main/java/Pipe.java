package main.java;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;

public class Pipe<T> {
    private ArrayBlockingQueue<Optional<T>> queue;
    private boolean closed;

    public PipeSource source;
    public PipeSink sink;

    public Pipe(int capacity) {
        queue = new ArrayBlockingQueue<>(capacity);
        closed = false;
        source = new PipeSource();
        sink = new PipeSink();
    }

    public class PipeSource {
        public void write(T e){
            try {
                queue.put(Optional.of(e));
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
        public T read(){
            if (closed) return null;
            Optional<T> optional = null;
            try {
                optional = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (optional.isPresent()) {
                return optional.get();
            }
            else {
                closed = true;
                return null;
            }
        }
    }
}
