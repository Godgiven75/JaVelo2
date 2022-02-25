package epfl.javelo;

public final class Bits {
     private Bits() {}

    public static int extractSigned(int value, int start, int length) {
        int rangeSize = start + length;
        int leftShift = value << 32 - rangeSize;
        return leftShift >> 32 - length;


    }

    public static int extractUnsigned(int value, int start, int length) {
        int rangeSize = start + length;
        int leftShift = value <<  32 - rangeSize;
        return leftShift >>> 32 - length;

    }

}
