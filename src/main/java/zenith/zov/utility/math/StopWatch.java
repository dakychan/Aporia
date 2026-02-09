package zenith.zov.utility.math;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StopWatch {
    private long lastTime;

    public StopWatch() {
        reset();
    }

    public boolean every(long delay) {
        if (System.currentTimeMillis() - lastTime >= delay) {
            reset();
            return true;
        }
        return false;
    }

    public void reset() {
        this.lastTime = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - this.lastTime;
    }
}

