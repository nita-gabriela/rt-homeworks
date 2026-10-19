package ssatr.device;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

final class AlarmMetadata {
    private static final ConcurrentHashMap<String, BlockingQueue<Double>> THRESHOLDS =
            new ConcurrentHashMap<>();

    private AlarmMetadata() { }

    static void push(String deviceId, double threshold) {
        THRESHOLDS.computeIfAbsent(deviceId, id -> new LinkedBlockingQueue<>()).offer(threshold);
    }

    static double take(String deviceId) throws InterruptedException {
        return THRESHOLDS.computeIfAbsent(deviceId, id -> new LinkedBlockingQueue<>()).take();
    }
}

