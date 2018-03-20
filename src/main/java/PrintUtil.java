package main.java;

public class PrintUtil {
    private PrintUtil() {} // this class cannot be instantiated

    public static String byteArrayToString(byte[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            sb.append((int) arr[i]);
            if (i < arr.length - 1) {
                sb.append("\t");
            }
        }
        return sb.toString();
    }

    public static String intArrayToString(int[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i < arr.length - 1) {
                sb.append("\t");
            }
        }
        return sb.toString();
    }
}
