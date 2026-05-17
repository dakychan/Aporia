/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.module.settings;

import java.util.function.Supplier;

/**
 * Range slider setting with min/max bounds and a center value.
 * Renders as: |----*----| where | are bounds and * is current value.
 */
public class RangeSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double increment;

    public RangeSetting(String name, String description, double defaultValue, double min, double max, double increment) {
        this(name, description, defaultValue, min, max, increment, null);
    }

    public RangeSetting(String name, String description, double defaultValue, double min, double max, double increment, Supplier<Boolean> visible) {
        super(name, description, defaultValue, visible);
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public double getMin()       { return min; }
    public double getMax()       { return max; }
    public double getIncrement() { return increment; }

    public void setValue(double v) {
        this.value = Math.max(min, Math.min(max, Math.round(v / increment) * increment));
    }

    public float getFloat() { return value.floatValue(); }
    public int   getInt()   { return value.intValue(); }

    public void increase() { setValue(value + increment); }
    public void decrease() { setValue(value - increment); }

    public double getPercentage() { return (value - min) / (max - min); }
}
