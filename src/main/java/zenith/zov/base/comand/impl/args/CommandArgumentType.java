package zenith.zov.base.comand.impl.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CommandArgumentType implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = java.util.List.of("/home", "/events", "/pvp", "/call 1WantToFreak");

    public static CommandArgumentType create() {
        return new CommandArgumentType();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        StringBuilder command = new StringBuilder();
        while (reader.canRead()) {
            command.append(reader.read());
        }
        return command.toString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(EXAMPLES, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
