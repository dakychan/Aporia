package aporia.su.utils.chat;

import java.util.List;

public abstract class Command {
    private final String name;
    private final String description;
    private final List<String> aliases;
    
    public Command(String name, String description) {
        this(name, description, List.of());
    }
    
    public Command(String name, String description, List<String> aliases) {
        this.name = name;
        this.description = description;
        this.aliases = aliases;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<String> getAliases() {
        return aliases;
    }
    
    public abstract void execute(List<String> args);
    
    public String[] getCompletions(List<String> args) {
        return new String[0];
    }
}
