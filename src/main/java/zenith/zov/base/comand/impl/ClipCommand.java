package zenith.zov.base.comand.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import zenith.zov.base.comand.api.CommandAbstract;
import zenith.zov.base.comand.impl.args.CoordinateArgumentType;
import zenith.zov.utility.game.other.MessageUtil;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ClipCommand extends CommandAbstract {
    public ClipCommand() {
        super("clip");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("vclip")
            .then(arg("distance", CoordinateArgumentType.create())
            .executes(context -> {
                double distance = context.getArgument("distance", Double.class);
                PlayerEntity player = MinecraftClient.getInstance().player;
                if (player != null) {
                    player.setPosition(player.getX(), player.getY() + distance + 0.1, player.getZ());
                    
                    MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                        "§aВертикальный вклип на " + distance + " блоков");
                }
                return SINGLE_SUCCESS;
            })));
        builder.then(literal("hclip")
            .then(arg("distance", CoordinateArgumentType.create())
            .executes(context -> {
                double distance = context.getArgument("distance", Double.class);
                PlayerEntity player = MinecraftClient.getInstance().player;
                
                if (player != null) {
                    double yaw = Math.toRadians(player.getYaw());
                    player.setPosition(player.getX() - Math.sin(yaw) * distance, player.getY() + 0.1, player.getZ() + Math.cos(yaw) * distance);
                    MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                        "§aГоризонтальный вклип на " + distance + " блоков");
                }
                return SINGLE_SUCCESS;
            })));
        builder.then(literal("up")
            .then(arg("distance", CoordinateArgumentType.create())
            .executes(context -> {
                double distance = context.getArgument("distance", Double.class);
                PlayerEntity player = MinecraftClient.getInstance().player;
                
                if (player != null) {
                    player.setPosition(player.getX(), player.getY() + distance + 0.1, player.getZ());
                    MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                        "§aВклип вверх на " + distance + " блоков");
                }
                return SINGLE_SUCCESS;
            })));

        builder.then(literal("down")
            .then(arg("distance", CoordinateArgumentType.create())
            .executes(context -> {
                double distance = context.getArgument("distance", Double.class);
                PlayerEntity player = MinecraftClient.getInstance().player;
                
                if (player != null) {
                    player.setPosition(player.getX(), player.getY() - distance + 0.1, player.getZ());
                    MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                        "§aВклип вниз на " + distance + " блоков");
                }
                return SINGLE_SUCCESS;
            })));

        builder.then(literal("forward")
            .then(arg("distance", CoordinateArgumentType.create())
            .executes(context -> {
                double distance = context.getArgument("distance", Double.class);
                PlayerEntity player = MinecraftClient.getInstance().player;
                
                if (player != null) {
                    double yaw = Math.toRadians(player.getYaw());
                    player.setPosition(player.getX() - Math.sin(yaw) * distance, player.getY() + 0.1, player.getZ() + Math.cos(yaw) * distance);
                    MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                        "§aВклип вперед на " + distance + " блоков");
                }
                return SINGLE_SUCCESS;
            })));

        builder.then(literal("back")
            .then(arg("distance", CoordinateArgumentType.create())
            .executes(context -> {
                double distance = context.getArgument("distance", Double.class);
                PlayerEntity player = MinecraftClient.getInstance().player;
                
                if (player != null) {
                    double yaw = Math.toRadians(player.getYaw());
                    player.setPosition(player.getX() + Math.sin(yaw) * distance, player.getY() + 0.1, player.getZ() - Math.cos(yaw) * distance);
                    MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                        "§aВклип назад на " + distance + " блоков");
                }
                return SINGLE_SUCCESS;
            })));

        builder.then(literal("help").executes(context -> {
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                "§6Использование: §r.clip <vclip/hclip/up/down/forward/back/help> [расстояние]");
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                "§6Примеры:§r");
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                "§e.clip vclip 10 §7- вклип вверх на 10 блоков");
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                "§e.clip hclip 5 §7- горизонтальный вклип на 5 блоков");
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                "§e.clip up 3 §7- вклип вверх на 3 блока");
            return SINGLE_SUCCESS;
        }));
    }
}
