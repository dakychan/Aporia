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

public class CoordinateArgumentType implements ArgumentType<Double> {
    private static final Collection<String> EXAMPLES = java.util.List.of("-1", "10.5", "-5.2", "100");

    public static CoordinateArgumentType create() {
        return new CoordinateArgumentType();
    }

    @Override
    public Double parse(StringReader reader) throws CommandSyntaxException {
        try {
            return Double.parseDouble(reader.readString());
        } catch (NumberFormatException e) {
            throw new CommandSyntaxException(null, () -> "Не те циферки пишешь родной");
        }
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
