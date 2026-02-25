package cc.apr.command;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {
   private final Map<String, Command> commands = new HashMap<>();

   public void register(Command command) {
      this.commands.put(command.getName().toLowerCase(), command);
   }

   public void unregister(String name) {
      this.commands.remove(name.toLowerCase());
   }

   public Command getCommand(String name) {
      return this.commands.get(name.toLowerCase());
   }

   public Collection<Command> getAllCommands() {
      return this.commands.values();
   }
}
