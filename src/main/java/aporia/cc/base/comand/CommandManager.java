package aporia.cc.base.comand;

import com.mojang.brigadier.CommandDispatcher;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;
import aporia.cc.base.comand.api.CommandAbstract;
import aporia.cc.base.comand.impl.FriendCommand;
import aporia.cc.base.comand.impl.MacroCommand;
import aporia.cc.base.comand.impl.ClipCommand;
import aporia.cc.base.comand.impl.ConfigCommand;
import aporia.cc.base.comand.impl.RCTCommand;


import java.util.ArrayList;
import java.util.List;

@Getter
public class CommandManager {
    private String prefix = ".";


    private final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();

    private final CommandSource source = new ClientCommandSource(
            MinecraftClient.getInstance().getNetworkHandler(),
            MinecraftClient.getInstance(),
            false
    );

    private final List<CommandAbstract> commands = new ArrayList<>();

    public CommandManager() {


        registerCommand(new FriendCommand());
        registerCommand(new MacroCommand());
        registerCommand(new ClipCommand());
        registerCommand(new ConfigCommand());
        registerCommand(new RCTCommand());

    }


    public void registerCommand(CommandAbstract command) {
        if (command == null) return;

        command.register(dispatcher);
        this.commands.add(command);
    }
}

