package util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RateLimiter {
    private static final int MAX_REQUESTS = 10;
    private static final long TIME_WINDOW_MS = 60 * 1000;


    private static final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> requestLogs = new ConcurrentHashMap<>();

    public static boolean isAllowed(String ip) {
        long now = System.currentTimeMillis();
        requestLogs.putIfAbsent(ip, new ConcurrentLinkedQueue<>());
        ConcurrentLinkedQueue<Long> timestamps = requestLogs.get(ip);

        synchronized (timestamps) {

            while (!timestamps.isEmpty() && (now - timestamps.peek()) > TIME_WINDOW_MS) {
                timestamps.poll();
            }

            if (timestamps.size() >= MAX_REQUESTS) {
                return false; // Too many requests
            } else {
                timestamps.add(now);
                return true; // Allowed
            }
        }
    }
}
