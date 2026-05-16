/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.module.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import so.aporia.module.Category;
import so.aporia.module.Module;
import so.aporia.module.settings.BooleanSetting;
import so.aporia.module.settings.NumberSetting;
import so.aporia.module.settings.TextSetting;
import so.aporia.utils.events.EventBus;
import so.aporia.utils.events.EventHandler;
import so.aporia.utils.events.impl.ChatMessageEvent;
import so.aporia.utils.events.impl.PlayerDeathEvent;
import so.aporia.utils.events.impl.TickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AutoEZ — автоматически отправляет сообщение в чат после убийства игрока.
 * <p>
 * Детект:
 * 1. Парсит чат на "Вы убили &lt;ник&gt;" или аналогичные паттерны
 * 2. Если чат не дал результат — хукает PlayerDeathEvent напрямую
 */
public final class AutoEZ extends Module {

    private static final Minecraft mc = Minecraft.getInstance();

    private static final Pattern KILL_CHAT_RU = Pattern.compile(
        "Вы\\s+убили\\s+([^\\s.!?]+)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern KILL_CHAT_EN = Pattern.compile(
        "You\\s+killed\\s+([^\\s.!?]+)",
        Pattern.CASE_INSENSITIVE
    );

    private final Set<String> killedPlayers = new HashSet<>();
    private String lastKilledName = null;
    private int delayTicks = 0;

    public final BooleanSetting chatMode = new BooleanSetting("Chat Detect", "", true);
    public final BooleanSetting eventMode = new BooleanSetting("Event Detect", "", true);
    public final NumberSetting delay = new NumberSetting("Delay", "", 0.5, 0.0, 5.0, 0.5);
    public final TextSetting message = new TextSetting("Message", "", "ez");

    public AutoEZ() {
        super("AutoEZ", Category.MISC);
    }

    @Override
    protected void onEnable() {
        EventBus.INSTANCE.register(this);
        killedPlayers.clear();
        lastKilledName = null;
        delayTicks = 0;
    }

    @Override
    protected void onDisable() {
        EventBus.INSTANCE.unregister(this);
        killedPlayers.clear();
        lastKilledName = null;
    }

    @EventHandler
    public void onChat(ChatMessageEvent event) {
        if (!chatMode.isEnabled()) return;
        if (mc.player == null) return;

        String text = event.plainText();

        Matcher ru = KILL_CHAT_RU.matcher(text);
        if (ru.find()) {
            onKill(ru.group(1));
            return;
        }

        Matcher en = KILL_CHAT_EN.matcher(text);
        if (en.find()) {
            onKill(en.group(1));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!eventMode.isEnabled()) return;
        if (mc.player == null) return;

        Player dead = event.player();
        if (dead == null) return;

        if (dead == mc.player) return;

        onKill(event.playerName());
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (lastKilledName == null) return;

        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        sendEz(lastKilledName);
        lastKilledName = null;
    }

    private void onKill(String playerName) {
        String name = playerName.replaceAll("[^a-zA-Z0-9_\\u0400-\\u04FF]", "");
        if (name.isEmpty()) return;
        if (killedPlayers.contains(name)) return;

        killedPlayers.add(name);
        lastKilledName = name;
        delayTicks = (int) (delay.get() * 20);
    }

    private void sendEz(String target) {
        if (mc.player == null || mc.player.connection == null) return;

        String msg = message.get().replace("{player}", target);
        if (msg.isEmpty()) msg = "ez";

        mc.player.connection.sendChat(msg);
    }
}
