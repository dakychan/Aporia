package ru.render;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Manages multiple animations and provides a centralized system for animation updates.
 * The AnimationSystem maintains a registry of active animations and updates them each frame.
 * 
 * Requirements: 5.1, 5.2, 5.3
 */
public class AnimationSystem {
    private final Map<String, Animation> activeAnimations;
    
    /**
     * Creates a new AnimationSystem with an empty animation registry.
     */
    public AnimationSystem() {
        this.activeAnimations = new HashMap<>();
    }
    
    /**
     * Creates a new animation and registers it with the given ID.
     * If an animation with the same ID already exists, it will be replaced.
     * 
     * @param id The unique identifier for this animation
     * @param duration The duration of the animation in seconds
     * @param easing The easing function to apply to the animation progress
     * @return The created Animation instance
     * @throws IllegalArgumentException if id is null or empty, or if duration is invalid
     */
    public Animation createAnimation(String id, float duration, EasingFunction easing) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Animation ID cannot be null or empty");
        }
        
        Animation animation = new Animation(duration);
        activeAnimations.put(id, animation);
        return animation;
    }
    
    /**
     * Updates all active animations by the given delta time.
     * Completed animations are automatically removed from the registry.
     * 
     * @param deltaTime The time elapsed since the last update in seconds
     */
    public void update(float deltaTime) {
        // Use iterator to safely remove completed animations while iterating
        Iterator<Map.Entry<String, Animation>> iterator = activeAnimations.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Animation> entry = iterator.next();
            Animation animation = entry.getValue();
            
            animation.update(deltaTime);
            
            // Remove completed animations
            if (animation.isComplete()) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Gets the progress of an animation by its ID.
     * 
     * @param id The unique identifier of the animation
     * @return The animation progress (0.0 to 1.0), or 0.0 if the animation doesn't exist
     */
    public float getProgress(String id) {
        Animation animation = activeAnimations.get(id);
        if (animation == null) {
            return 0.0f;
        }
        return animation.getProgress();
    }
    
    /**
     * Checks if an animation with the given ID exists and is active.
     * 
     * @param id The unique identifier of the animation
     * @return true if the animation exists and is active, false otherwise
     */
    public boolean hasAnimation(String id) {
        return activeAnimations.containsKey(id);
    }
    
    /**
     * Gets the number of active animations.
     * 
     * @return The count of active animations
     */
    public int getActiveAnimationCount() {
        return activeAnimations.size();
    }
    
    /**
     * Removes an animation from the registry.
     * 
     * @param id The unique identifier of the animation to remove
     * @return true if the animation was removed, false if it didn't exist
     */
    public boolean removeAnimation(String id) {
        return activeAnimations.remove(id) != null;
    }
    
    /**
     * Clears all active animations from the registry.
     */
    public void clear() {
        activeAnimations.clear();
    }
}
