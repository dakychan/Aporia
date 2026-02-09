package zenith.zov.base.comand.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import zenith.zov.Zenith;
import zenith.zov.base.comand.api.CommandAbstract;
import zenith.zov.base.comand.impl.args.FriendArgumentType;
import zenith.zov.base.comand.impl.args.PlayerArgumentType;
import zenith.zov.utility.game.other.MessageUtil;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class StaffCommand extends CommandAbstract {
    public StaffCommand() {
        super("friend");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add").then(arg("player", PlayerArgumentType.create()).executes(context -> {
            String name = context.getArgument("player", String.class);
            if(Zenith.getInstance().getStaffManager().getItems().contains(name)) {
                MessageUtil.displayMessage(MessageUtil.LogLevel.WARN, "Уже добавлен " + name);
                return SINGLE_SUCCESS;
            }
            Zenith.getInstance().getStaffManager().add(name);
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "Добавили " + name);
            return SINGLE_SUCCESS;
        })));


        builder.then(literal("remove").then(arg("player", FriendArgumentType.create()).executes(context -> {
            String nickname = context.getArgument("player", String.class);

            Zenith.getInstance().getStaffManager().remove(nickname);
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,nickname + " удален из стаффа");
            return SINGLE_SUCCESS;
        })));
        builder.then(literal("list").executes(commandContext -> {



            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,Zenith.getInstance().getStaffManager().getItems().toString() );



            return SINGLE_SUCCESS;
        }));
    }

}
