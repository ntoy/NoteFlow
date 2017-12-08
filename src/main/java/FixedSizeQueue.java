package main.java;

public class FixedSizeQueue<E> {
    private E[] contents;
    private int startIndex;
    private int size;

    public int getSize() {
        return size;
    }

    public FixedSizeQueue(int capacity) {
        contents = (E[]) new Object[capacity];
        startIndex = 0;
        size = 0;
    }

    public boolean enqueue(E e) {
        if (size >= contents.length) {
            return false;
        }
        else {
            contents[(startIndex + size) % contents.length] = e;
            size++;
            return true;
        }
    }

    public E dequeue() {
        if (size <= 0) {
            return null;
        } else {
            E e = contents[startIndex];
            startIndex = (startIndex + 1) % contents.length;
            size--;
            return e;
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == contents.length;
    }

    public static void main(String args[]) {
        FixedSizeQueue<String> q = new FixedSizeQueue<>(5);
        System.out.println(q.enqueue("one"));
        System.out.println(q.enqueue("two"));
        System.out.println(q.enqueue("three"));
        System.out.println(q.dequeue());
        System.out.println(q.dequeue());
        System.out.println(q.enqueue("four"));
        System.out.println(q.enqueue("five"));
        System.out.println(q.enqueue("six"));
        System.out.println(q.enqueue("seven"));
        System.out.println(q.enqueue("eight"));
    }
}
