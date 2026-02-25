package cc.apr.command.commands;

import aporia.cc.chat.ChatUtils;
import aporia.cc.chat.ChatUtils.MessageType;
import cc.apr.command.Command;
import cc.apr.command.CommandRegistry;

public class HelpCommand implements Command {
   private final CommandRegistry registry;

   public HelpCommand(CommandRegistry registry) {
      this.registry = registry;
   }

   @Override
   public String getName() {
      return "help";
   }

   @Override
   public String getDescription() {
      return "Показать список команд";
   }

   @Override
   public String getUsage() {
      return ChatUtils.INSTANCE.formatCommand("help") + " - Показать все команды";
   }

   @Override
   public void execute(String[] args) {
      ChatUtils.INSTANCE.sendMessage("=== Список команд ===", MessageType.WARNING);

      for (Command cmd : this.registry.getAllCommands()) {
         String line = String.format("%s - %s", cmd.getName(), cmd.getDescription());
         ChatUtils.INSTANCE.sendMessage(line, MessageType.WARNING);
      }
   }
}
