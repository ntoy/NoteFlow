package main.java;

import java.util.ArrayList;
import java.util.Collection;

// An ArrayList that allows us to remove chunks instead
// of forcing us to inefficiently remove them one by one and shift
// everything each time
public class DecentArrayList<E> extends ArrayList<E> {
    public DecentArrayList() {
        super();
    }

    public DecentArrayList(Collection<? extends E> c) {
        super(c);
    }

    public DecentArrayList(int initialCapacity) {
        super(initialCapacity);
    }

    public void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
    }

    public static void main(String[] args) {
        DecentArrayList<String> list = new DecentArrayList<>(5);
        list.add("one");
        list.add("two");
        list.add("three");
        list.add("four");
        list.removeRange(1, 3);
        list.add("five");
        System.out.println(list);
    }
}
