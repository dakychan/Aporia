package zenith.zov.base.comand.api;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;


import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import lombok.Getter;

import net.minecraft.command.CommandSource;
import org.jetbrains.annotations.NotNull;
import zenith.zov.utility.interfaces.IClient;

@Getter
public abstract class CommandAbstract implements IClient {
    private final String command;


    protected CommandAbstract(String command) {
       this.command = command;
    }

    public void register(CommandDispatcher<CommandSource> dispatcher) {

        LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder.literal(command);
        execute(builder);
        dispatcher.register(builder);

    }
    public abstract void execute(LiteralArgumentBuilder<CommandSource> builder);
    protected static <T> @NotNull RequiredArgumentBuilder<CommandSource, T> arg(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }
    protected static @NotNull LiteralArgumentBuilder<CommandSource> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }

}

