package aporia.cc.base.comand.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import aporia.cc.Aporia;
import aporia.cc.base.comand.api.CommandAbstract;
import aporia.cc.base.comand.impl.args.FriendArgumentType;
import aporia.cc.base.comand.impl.args.PlayerArgumentType;
import aporia.cc.utility.game.other.MessageUtil;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class FriendCommand extends CommandAbstract {
    public FriendCommand() {
        super("friend");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add").then(arg("player", PlayerArgumentType.create()).executes(context -> {
            String name = context.getArgument("player", String.class);
            if(Aporia.getInstance().getFriendManager().getItems().contains(name)) {
                MessageUtil.displayMessage(MessageUtil.LogLevel.WARN, "Уже добавлен " + name);
                return SINGLE_SUCCESS;
            }
            Aporia.getInstance().getFriendManager().add(name);
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "Добавили " + name);
            return SINGLE_SUCCESS;
        })));


        builder.then(literal("remove").then(arg("player", FriendArgumentType.create()).executes(context -> {
            String nickname = context.getArgument("player", String.class);

           Aporia.getInstance().getFriendManager().removeFriend(nickname);
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,nickname + " удален из друзей");
            return SINGLE_SUCCESS;
        })));
        builder.then(literal("list").executes(commandContext -> {



                MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,Aporia.getInstance().getFriendManager().getItems().toString() );



            return SINGLE_SUCCESS;
        }));
    }

}

