package rocks.tbog.tblauncher.utils;

public class ArrayHelper {
    public static boolean contains(int[] arr, int find) {
        for (int value : arr)
            if (value == find)
                return true;
        return false;
    }
}
