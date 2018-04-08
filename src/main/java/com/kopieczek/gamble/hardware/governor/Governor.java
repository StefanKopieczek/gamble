package com.kopieczek.gamble.hardware.governor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Governor {
    private static final Logger log = LogManager.getLogger(Governor.log);
    private static final double FREQUENCY_KHZ = 4194.304;
    private static final int MAXIMUM_SKEW_PERMITTED_MS = 10;
    private static final float SPEED_MULTIPLIER = 1.0f;
    private long lastSnapshotTime = -1;
    private long cyclesSinceLastSnapshot = -1;

    public void sleepIfNeeded(long cycleDelta) {
        long currentTime = System.currentTimeMillis();
        if (lastSnapshotTime == -1) {
            lastSnapshotTime = currentTime;
            cyclesSinceLastSnapshot = 0;
            return;
        }

        cyclesSinceLastSnapshot += cycleDelta;
        long timePeriodImpliedByCyclesMs = (long)(((double)cyclesSinceLastSnapshot) / (FREQUENCY_KHZ * SPEED_MULTIPLIER));
        long actualElapsedTimeMs = currentTime - lastSnapshotTime;
        long clockSkewMs = timePeriodImpliedByCyclesMs - actualElapsedTimeMs;


        if (clockSkewMs > MAXIMUM_SKEW_PERMITTED_MS) {
            log.debug("Governor sleeping for {}ms to adjust for clock skew", clockSkewMs);
            lastSnapshotTime = currentTime;
            cyclesSinceLastSnapshot = 0;

            try {
                Thread.sleep(clockSkewMs);
            } catch (InterruptedException e){
                log.warn("Governor interrupted while sleeping to fix clock skew");
                Thread.currentThread().interrupt();
            }
        }
    }
}
