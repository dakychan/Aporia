package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public class ColorArgument implements ArgumentType<net.minecraft.ChatFormatting> {
   private static final Collection<String> EXAMPLES = Arrays.asList("red", "green");
   public static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType(
      p_308345_ -> Component.translatableEscape("argument.color.invalid", p_308345_)
   );

   private ColorArgument() {
   }

   public static ColorArgument color() {
      return new ColorArgument();
   }

   public static net.minecraft.ChatFormatting getColor(CommandContext<CommandSourceStack> p_85467_, String p_85468_) {
      return (net.minecraft.ChatFormatting)p_85467_.getArgument(p_85468_, net.minecraft.ChatFormatting.class);
   }

   public net.minecraft.ChatFormatting parse(StringReader p_85465_) throws CommandSyntaxException {
      String s = p_85465_.readUnquotedString();
      net.minecraft.ChatFormatting chatformatting = net.minecraft.ChatFormatting.getByName(s);
      if (chatformatting != null && !chatformatting.isFormat()) {
         return chatformatting;
      } else {
         throw ERROR_INVALID_VALUE.createWithContext(p_85465_, s);
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_85473_, SuggestionsBuilder p_85474_) {
      return SharedSuggestionProvider.suggest(net.minecraft.ChatFormatting.getNames(true, false), p_85474_);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
