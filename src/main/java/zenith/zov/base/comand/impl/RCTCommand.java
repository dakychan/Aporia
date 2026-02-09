package zenith.zov.base.comand.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;
import zenith.zov.Zenith;
import zenith.zov.base.comand.api.CommandAbstract;
import zenith.zov.base.notify.NotifyManager;
import zenith.zov.base.repository.RCTRepository;
import zenith.zov.utility.game.server.ServerHandler;
import zenith.zov.utility.interfaces.IClient;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class RCTCommand extends CommandAbstract implements IClient {
    private final RCTRepository repository;

    public RCTCommand() {
        super("rct");
        repository = Zenith.getInstance().getRCTRepository();
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            ServerHandler serverHandler = Zenith.getInstance().getServerHandler();
            
            if (!serverHandler.isHolyWorld()) {
                NotifyManager.getInstance().addNotification("[RCT]", net.minecraft.text.Text.literal(" Не работает на этом " + Formatting.RED + "сервере"));
                return SINGLE_SUCCESS;
            }

            if (serverHandler.isPvp()) {
                NotifyManager.getInstance().addNotification("️[RCT]", net.minecraft.text.Text.literal(" Вы находитесь в режиме " + Formatting.RED + "пвп"));
                return SINGLE_SUCCESS;
            }

            repository.reconnect(serverHandler.getAnarchy());
            return SINGLE_SUCCESS;
        });

        builder.then(CommandAbstract.arg("anarchy", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 63)).executes(context -> {
            ServerHandler serverHandler = Zenith.getInstance().getServerHandler();
            
            if (!serverHandler.isHolyWorld()) {
                NotifyManager.getInstance().addNotification("[RCT]", net.minecraft.text.Text.literal(" Не работает на этом " + Formatting.RED + "сервере"));
                return SINGLE_SUCCESS;
            }

            if (serverHandler.isPvp()) {
                NotifyManager.getInstance().addNotification("[RCT]️", net.minecraft.text.Text.literal(" Вы находитесь в режиме " + Formatting.RED + "пвп"));
                return SINGLE_SUCCESS;
            }

            int anarchy = context.getArgument("anarchy", Integer.class);
            repository.reconnect(anarchy);
            return SINGLE_SUCCESS;
        }));
    }
}
