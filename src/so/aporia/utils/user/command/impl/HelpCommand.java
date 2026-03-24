package so.aporia.utils.user.command.impl;

import so.aporia.utils.user.command.Command;
import so.aporia.utils.user.command.CommandManager;

public class HelpCommand implements Command {

    @Override
    public String name() { return "help"; }

    @Override
    public String description() { return "Lists all available commands"; }

    @Override
    public void execute(String[] args) {
        CommandManager.chat("§6--- Aporia Commands ---");
        CommandManager.INSTANCE.getAll().stream()
            .sorted((a, b) -> a.name().compareTo(b.name()))
            .forEach(cmd -> CommandManager.chat("§e" + CommandManager.PREFIX + cmd.name() + " §7- " + cmd.description()));
    }
}
