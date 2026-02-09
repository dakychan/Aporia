package zenith.zov.base.comand.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import zenith.zov.base.comand.api.CommandAbstract;
import zenith.zov.base.comand.impl.args.MacroArgumentType;
import zenith.zov.base.comand.impl.args.CommandArgumentType;
import zenith.zov.utility.game.other.MessageUtil;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class MacroCommand extends CommandAbstract {
    public MacroCommand() {
        super("macro");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(arg("name", MacroArgumentType.create())
            .then(arg("bind", MacroArgumentType.create())
            .then(arg("command", CommandArgumentType.create())
            .executes(context -> {
                String name = context.getArgument("name", String.class);
                String bind = context.getArgument("bind", String.class);
                String command = context.getArgument("command", String.class);
                
                // Zenith.getInstance().getMacroManager().addMacro(name, bind, command);
                
                MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                    "§aМакрос " + name + " создан для клавиши " + bind + " с командой " + command);
                return SINGLE_SUCCESS;
            }))));

        builder.then(literal("help").executes(context -> {
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                "§6Использование: §r.macro <название> <бинд> <команда>");
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                "§6Примеры:§r");
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                "§e.macro hata h /home 1 §7- создать макрос hata для клавиши h с командой /home 1");
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                "§e.macro tp T /tp 100 64 100 §7- создать макрос tp для клавиши T с командой /tp 100 64 100");
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                "§e.macro list §7- показать все макросы");
            return SINGLE_SUCCESS;
        }));
    }
}
