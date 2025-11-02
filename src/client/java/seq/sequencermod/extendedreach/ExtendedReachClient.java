package seq.sequencermod.extendedreach;

public final class ExtendedReachClient {
    private static volatile double reach = 5.0D;

    private ExtendedReachClient() {}

    public static void setReach(double r) { reach = r; }
    public static double getReach() { return reach; }
    public static float getReachF() { return (float) reach; }
}