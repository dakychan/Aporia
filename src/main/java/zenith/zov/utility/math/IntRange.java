package zenith.zov.utility.math;
import lombok.Getter;

import java.util.Random;

public class IntRange { //extends IntRange by kotlin
    @Getter
    private final int start;
    @Getter
    private final int endInclusive;
    private final Random random = new Random();

    public IntRange(int start, int endInclusive) {
        if (start > endInclusive) {
            throw new IllegalArgumentException("Start must be less than or equal to endInclusive");
        }
        this.start = start;
        this.endInclusive = endInclusive;
    }

    public int random() {
        return start + random.nextInt(endInclusive - start + 1);
    }

    @Override
    public String toString() {
        return start + ".." + endInclusive;
    }
}
