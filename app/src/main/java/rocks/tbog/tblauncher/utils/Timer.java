package rocks.tbog.tblauncher.utils;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

public class Timer {
    protected long mStart;
    protected long mStop;
    protected TimeUnit mUnit;

    public static final StopTimeComparator STOP_TIME_COMPARATOR = new StopTimeComparator();

    public Timer() {
        this(0, TimeUnit.MILLISECONDS);
    }

    protected Timer(long now, @NonNull TimeUnit unit) {
        mStart = now;
        mStop = now;
        mUnit = unit;
    }

    public static Timer startNano() {
        return new Timer(System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    public static Timer startMilli() {
        return new Timer(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    public void start() {
        if (mUnit == TimeUnit.NANOSECONDS) {
            mStart = System.nanoTime();
        } else {
            mStart = System.currentTimeMillis();
        }
        mStop = mStart;
    }

    public void stop() {
        if (mUnit == TimeUnit.NANOSECONDS) {
            mStop = System.nanoTime();
        } else {
            mStop = System.currentTimeMillis();
        }
        //return mStop - mStart;
    }

    @NonNull
    @Override
    public String toString() {
        if (mUnit == TimeUnit.NANOSECONDS) {
            long deltaTime = mUnit.convert(mStop - mStart, TimeUnit.NANOSECONDS);
            long ms = TimeUnit.NANOSECONDS.toMillis(deltaTime);
            if (ms == 0)
                return deltaTime + "ns";
            long ns = deltaTime - TimeUnit.MILLISECONDS.toNanos(ms);
            if (ns > 0)
                return ms + "ms " + ns + "ns";
            return ms + "ms";
        }
        return toStringSeconds();
    }

    @NonNull
    public String toStringSeconds() {
        long deltaTime = mUnit.convert(mStop - mStart, TimeUnit.MILLISECONDS);
        long s = TimeUnit.MILLISECONDS.toSeconds(deltaTime);
        long ms = deltaTime - TimeUnit.SECONDS.toMillis(s);
        if (s == 0)
            return ms + "ms";
        if (ms > 0)
            return s + "sec " + ms + "ms";
        return s + "sec";
    }

    public static class StopTimeComparator implements java.util.Comparator<Timer> {
        @Override
        public int compare(Timer o1, Timer o2) {
            if (o1 == o2)
                return 0;
            if (o1 == null)
                return -1;
            if (o2 == null)
                return 1;
            return (int) (o1.mStop - o2.mStop);
        }
    }
}
