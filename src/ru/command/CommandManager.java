package ru.command;

import aporia.cc.Logger;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ru.command.commands.AliasCommand;
import ru.command.commands.ConfigCommand;
import ru.command.commands.FriendCommand;
import ru.command.commands.InfoCommand;
import ru.command.commands.PrefixCommand;

@Deprecated
public class CommandManager {
   private static CommandManager instance;
   private final CommandRegistry registry;
   private String prefix = ".";
   private final Map<String, String> aliases = new HashMap<>();

   private CommandManager() {
      this.registry = new CommandRegistry();
   }

   public static CommandManager getInstance() {
      if (instance == null) {
         instance = new CommandManager();
      }

      return instance;
   }

   public void initialize() {
      this.registerCommand(new ConfigCommand());
      this.registerCommand(new AliasCommand());
      this.registerCommand(new FriendCommand());
      this.registerCommand(new InfoCommand());
      this.registerCommand(new PrefixCommand());
      Logger.INSTANCE.info("CommandManager initialized with " + this.registry.getAllCommands().size() + " commands");
   }

   public boolean handleChatMessage(String message) {
      if (!message.startsWith(this.prefix)) {
         return false;
      } else {
         String commandText = message.substring(this.prefix.length()).trim();
         if (commandText.isEmpty()) {
            return true;
         } else {
            commandText = this.expandAlias(commandText);
            String[] parts = commandText.split("\\s+");
            String commandName = parts[0].toLowerCase();
            String[] args = new String[parts.length - 1];
            System.arraycopy(parts, 1, args, 0, args.length);
            Command command = this.registry.getCommand(commandName);
            if (command == null) {
               sendChatMessage("§cНеизвестная команда: " + commandName);
               return true;
            } else {
               try {
                  command.execute(args);
               } catch (Exception var8) {
                  sendChatMessage("§cОшибка выполнения команды: " + var8.getMessage());
                  var8.printStackTrace();
               }

               return true;
            }
         }
      }
   }

   public void registerCommand(Command command) {
      this.registry.register(command);
   }

   public void setPrefix(String prefix) {
      this.prefix = prefix;
   }

   public String getPrefix() {
      return this.prefix;
   }

   public void addAlias(String alias, String command) {
      this.aliases.put(alias.toLowerCase(), command);
   }

   public void removeAlias(String alias) {
      this.aliases.remove(alias.toLowerCase());
   }

   public Map<String, String> getAliases() {
      return new HashMap<>(this.aliases);
   }

   private String expandAlias(String input) {
      String[] parts = input.split("\\s+", 2);
      String firstWord = parts[0].toLowerCase();
      if (this.aliases.containsKey(firstWord)) {
         String expansion = this.aliases.get(firstWord);
         return parts.length > 1 ? expansion + " " + parts[1] : expansion;
      } else {
         return input;
      }
   }

   public static void sendChatMessage(String message) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.player.displayClientMessage(Component.literal(message), false);
      }
   }
}
