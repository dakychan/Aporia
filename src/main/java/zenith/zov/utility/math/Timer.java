package zenith.zov.utility.math;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Timer {

    private long millis;

    public Timer() {
        reset();
    }
    public boolean finished(float delay) {
        return System.currentTimeMillis() - delay >= millis;
    }
    public boolean finished(long delay) {
        return System.currentTimeMillis() - delay >= millis;
    }

    public void reset() {
        this.millis = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - this.millis;
    }
}