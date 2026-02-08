package ru.render;

/**
 * Defines easing functions for animation curves.
 * Each easing function takes a normalized time value (0.0 to 1.0) and returns
 * a transformed value that determines the animation's progression.
 * 
 * Requirements: 5.1, 5.2, 5.3
 */
public enum EasingFunction {
    /**
     * Linear easing with no acceleration or deceleration.
     * Formula: f(t) = t
     */
    LINEAR {
        @Override
        public float apply(float t) {
            return t;
        }
    },
    
    /**
     * Cubic easing that accelerates in the first half and decelerates in the second half.
     * Creates a smooth, natural-feeling animation.
     * Formula: f(t) = t < 0.5 ? 4*t³ : 1 - pow(-2*t + 2, 3) / 2
     */
    EASE_IN_OUT_CUBIC {
        @Override
        public float apply(float t) {
            if (t < 0.5f) {
                return 4.0f * t * t * t;
            } else {
                float adjusted = -2.0f * t + 2.0f;
                return 1.0f - (adjusted * adjusted * adjusted) / 2.0f;
            }
        }
    },
    
    /**
     * Quadratic easing that accelerates in the first half and decelerates in the second half.
     * Creates a gentler curve than cubic easing.
     * Formula: f(t) = t < 0.5 ? 2*t² : 1 - pow(-2*t + 2, 2) / 2
     */
    EASE_IN_OUT_QUAD {
        @Override
        public float apply(float t) {
            if (t < 0.5f) {
                return 2.0f * t * t;
            } else {
                float adjusted = -2.0f * t + 2.0f;
                return 1.0f - (adjusted * adjusted) / 2.0f;
            }
        }
    };
    
    /**
     * Applies the easing function to the given normalized time value.
     * 
     * @param t The normalized time value (0.0 to 1.0)
     * @return The eased value
     */
    public abstract float apply(float t);
}
