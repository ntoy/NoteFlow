package main.java;

public class ListMap<K, V> {
    private class Node {
        private K key;
        private V value;
        private Node next;

        private Node(K key, V value, Node next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private Node first;

    public ListMap() {
        first = null;
    }

    public boolean add (K key, V value) {
        Node last = null;
        for (Node n = first; n != null; n = n.next) {
            if (n.key.equals(key))
                return false;
            last = n;
        }
        if (last == null)
            first = new Node(key, value, null);
        else
            last.next = new Node(key, value, null);
        return true;
    }

    public boolean remove(K key) {
        Node prev = null;
        for (Node n = first; n != null; n = n.next) {
            if (n.key.equals(key)) {
                if (prev == null)
                    first = n.next;
                else
                    prev.next = n.next;
                return true;
            }
            prev = n;
        }
        return false;
    }

    public boolean contains(E e) {
        for (Node n = first; n != null; n = n.next) {
            if (n.content.equals(e))
                return true;
        }
        return false;
    }
}
