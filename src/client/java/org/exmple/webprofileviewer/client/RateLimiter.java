package org.exmple.webprofileviewer.client;
//全是AI写的，因为目前我还没学会，别介意，等我学会了会回来优化的:)
/**
 * Token bucket rate limiter to prevent API requests from being throttled.
 * Allows brief bursts while enforcing a maximum sustained request rate.
 */
public class RateLimiter {
    private final double capacity;
    private final double refillPerSecond;
    private double tokens;
    private long lastRefillNanos;

    /**
     * Creates a rate limiter with the specified capacity and refill rate.
     *
     * @param capacity the maximum number of tokens
     * @param refillPerSecond the rate at which tokens are replenished (tokens per second)
     */
    public RateLimiter(double capacity, double refillPerSecond) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.tokens = capacity; // start full so first few requests can go fast
        this.lastRefillNanos = System.nanoTime();
    }

    /**
     * Refills the token bucket based on elapsed time since last refill.
     */
    private void refill() {
        long now = System.nanoTime();
        double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
        if (elapsedSeconds <= 0) return;
        tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
        lastRefillNanos = now;
    }

    /**
     * Consumes one token, blocking until a token is available or the thread is interrupted.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public synchronized void consume() throws InterruptedException {
        while (true) {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return;
            }
            // compute how long until next token is available (seconds)
            double needed = 1.0 - tokens;
            long waitNanos = (long) Math.ceil((needed / refillPerSecond) * 1_000_000_000.0);
            // convert to millis for Thread.sleep, but keep at least 1ms to avoid tight-loop
            long waitMillis = Math.max(1, waitNanos / 1_000_000);
            this.wait(waitMillis);
        }
    }

    /**
     * Notifies all waiting threads that the state may have changed.
     * Can be called after waiting to wake up potential waiters.
     */
    public synchronized void wake() {
        this.notifyAll();
    }
}
