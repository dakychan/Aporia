package aporia.su.util.events.impl;


import aporia.su.util.events.api.events.Event;

public class WorldChangeEvent implements Event {
    
    private static final WorldChangeEvent INSTANCE = new WorldChangeEvent();
    
    public static WorldChangeEvent get() {
        return INSTANCE;
    }
}