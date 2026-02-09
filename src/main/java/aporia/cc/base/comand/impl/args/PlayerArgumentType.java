package aporia.cc.base.comand.impl.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import aporia.cc.utility.interfaces.IMinecraft;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public class PlayerArgumentType implements ArgumentType<String>, IMinecraft {
    private static final Collection<String> EXAMPLES = List.of("Steve", "Alex", "Bogdan");

    public static PlayerArgumentType create() {
        return new PlayerArgumentType();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {

        return reader.readUnquotedString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(mc.getNetworkHandler().getPlayerList().stream().map(p -> p.getProfile().getName()), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}

