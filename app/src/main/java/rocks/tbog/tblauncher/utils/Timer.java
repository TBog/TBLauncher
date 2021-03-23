package rocks.tbog.tblauncher.utils;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

public class Timer {
    protected final long mStart;
    protected long mStop;
    protected TimeUnit mUnit;

    protected Timer(long now, @NonNull TimeUnit unit) {
        mStart = now;
        mUnit = unit;
    }

    public static Timer startNano() {
        return new Timer(System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    public static Timer startMilli() {
        return new Timer(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
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
            long diff = mUnit.convert(mStop - mStart, TimeUnit.NANOSECONDS);
            long ms = TimeUnit.NANOSECONDS.toMillis(diff);
            long ns = diff - TimeUnit.MILLISECONDS.toNanos(ms);
            if (ns > 0)
                return ms + "ms " + ns + "ns";
            return ms + "ms";
        }
        return toStringSeconds();
    }

    @NonNull
    public String toStringSeconds() {
        long diff = mUnit.convert(mStop - mStart, TimeUnit.MILLISECONDS);
        long s = TimeUnit.MILLISECONDS.toSeconds(diff);
        long ms = diff - TimeUnit.SECONDS.toMillis(s);
        if (s == 0)
            return ms + "ms";
        if (ms > 0)
            return s + "sec " + ms + "ms";
        return s + "sec";
    }
}
