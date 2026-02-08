package ru.render;

/**
 * Represents a single animation instance with time tracking and progress calculation.
 * Animations track elapsed time and provide normalized progress values (0.0 to 1.0).
 * 
 * Requirements: 5.1, 5.2, 5.3
 */
public class Animation {
    private float currentTime;
    private float duration;
    private boolean complete;
    
    /**
     * Creates a new animation with the specified duration.
     * 
     * @param duration The total duration of the animation in seconds
     * @throws IllegalArgumentException if duration is negative or zero
     */
    public Animation(float duration) {
        if (duration <= 0.0f) {
            throw new IllegalArgumentException("Animation duration must be positive, got: " + duration);
        }
        this.duration = duration;
        this.currentTime = 0.0f;
        this.complete = false;
    }
    
    /**
     * Updates the animation by the given delta time.
     * Once the animation completes, further updates have no effect.
     * 
     * @param deltaTime The time elapsed since the last update in seconds
     */
    public void update(float deltaTime) {
        if (complete) {
            return;
        }
        
        // Ignore negative delta times
        if (deltaTime < 0.0f) {
            return;
        }
        
        currentTime += deltaTime;
        
        if (currentTime >= duration) {
            currentTime = duration;
            complete = true;
        }
    }
    
    /**
     * Gets the current progress of the animation.
     * 
     * @return A value between 0.0 (start) and 1.0 (complete)
     */
    public float getProgress() {
        if (duration == 0.0f) {
            return 1.0f;
        }
        return Math.min(currentTime / duration, 1.0f);
    }
    
    /**
     * Checks if the animation has completed.
     * 
     * @return true if the animation has finished, false otherwise
     */
    public boolean isComplete() {
        return complete;
    }
    
    /**
     * Gets the current elapsed time.
     * 
     * @return The current time in seconds
     */
    public float getCurrentTime() {
        return currentTime;
    }
    
    /**
     * Gets the total duration of the animation.
     * 
     * @return The duration in seconds
     */
    public float getDuration() {
        return duration;
    }
    
    /**
     * Resets the animation to its initial state.
     */
    public void reset() {
        currentTime = 0.0f;
        complete = false;
    }
}
