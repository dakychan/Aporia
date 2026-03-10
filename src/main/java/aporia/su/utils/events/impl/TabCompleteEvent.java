package aporia.su.utils.events.impl;

import aporia.su.utils.events.api.Event;

public class TabCompleteEvent extends Event {
    private final String prefix;
    public String[] completions;
    
    public TabCompleteEvent(String prefix) {
        this.prefix = prefix;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public void setCompletions(String[] completions) {
        this.completions = completions;
    }
}
