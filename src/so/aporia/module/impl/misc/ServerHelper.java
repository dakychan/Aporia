package so.aporia.module.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.chat.Component;
import so.aporia.module.Category;
import so.aporia.module.Module;
import so.aporia.module.settings.BooleanSetting;
import so.aporia.utils.events.EventBus;
import so.aporia.utils.events.EventHandler;
import so.aporia.utils.events.impl.ChatHideEvent;
import so.aporia.utils.events.impl.PacketEvent;
import so.aporia.utils.events.impl.TickEvent;
import so.aporia.utils.user.logger.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ServerHelper — помощник для JeNr0.
 * 
 * Функции:
 * - AutoFlyMe: ловит падение и спамит /flyme до включения флая
 * - AutoExample: автоматически решает математические капчи (перехват пакетов)
 * - Скрывает сообщения о флае и капчи из чата
 */
public final class ServerHelper extends Module {

    private static final Minecraft mc = Minecraft.getInstance();
    
    private final BooleanSetting autoFlyMe = new BooleanSetting("AutoFlyMe", 
        "Автоматически активирует /flyme при падении", true);
    
    private final BooleanSetting mathResolver = new BooleanSetting("MathResolver",
        "Автоматически решает математические капчи", true);
    
    private final BooleanSetting hideFlyMessages = new BooleanSetting("Hide Fly Messages",
        "Скрывать сообщения о включении/выключении флая", true);
    
    private final BooleanSetting hideCaptcha = new BooleanSetting("Hide Captcha",
        "Скрывать капчи из чата", true);
    
    private boolean isFalling = false;
    private int ticksWithoutGround = 0;
    private int ticksSinceLastCommand = 0;
    private static final int COMMAND_COOLDOWN = 2;

    private static final Pattern SIMPLE_MATH =
            Pattern.compile(".*Решите\\s*:?\\s*(\\d+)\\s*\\+\\s*(\\d+).*");

    private static final Pattern COMPLEX_MATH =
            Pattern.compile(".*Решите\\s*:?\\s*([\\d+\\-*/()\\s]+).*");
    
    private static final Pattern CAPTCHA_SOLVED = 
        Pattern.compile("(\\w+)\\s+первым\\s+решил\\s+пример\\s+и\\s+победил");

    private long captchaStartTime = 0;
    private boolean captchaSolvedByUs = false;
    
    public ServerHelper() {
        super("ServerHelper", Category.MISC);
    }
    
    @Override
    protected void onEnable() {
        EventBus.INSTANCE.register(this);
        isFalling = false;
        ticksWithoutGround = 0;
        ticksSinceLastCommand = COMMAND_COOLDOWN;
    }

    @Override
    protected void onDisable() {
        EventBus.INSTANCE.unregister(this);
        isFalling = false;
        ticksWithoutGround = 0;
        ticksSinceLastCommand = 0;
    }

    /**
     * Перехватывает пакеты чата и решает капчи максимально быстро.
     */
    @EventHandler
    public void onPacketReceive(PacketEvent event) {
        if (event.packet() instanceof ClientboundSystemChatPacket packet) {
            String text = packet.content().getString();
            
            if (hideFlyMessages.isEnabled() && 
                (text.contains("Вы успешно включили себе флай") ||
                 text.contains("Вы успешно выключили себе флай"))) {
                event.cancel();
            }

            if (mathResolver.isEnabled() && text.contains("Решите")) {
                captchaStartTime = System.nanoTime();
                captchaSolvedByUs = false;
                solveCaptcha(text);
                event.cancel();
            }
            
            if (mathResolver.isEnabled() && captchaSolvedByUs) {
                Matcher solveMatcher = CAPTCHA_SOLVED.matcher(text);
                if (solveMatcher.find()) {
                    event.cancel();
                    captchaSolvedByUs = false;
                }
            }
        }
    }

    /**
     * Решает капчу из текста пакета.
     * aka
     * лучий парсер на районе
     */
    private void solveCaptcha(String text) {
        long answer = -1;
        Matcher simpleMatcher = SIMPLE_MATH.matcher(text);
        if (simpleMatcher.find()) {
            try {
                long a = Long.parseLong(simpleMatcher.group(1));
                long b = Long.parseLong(simpleMatcher.group(2));
                answer = a + b;
            } catch (Exception e) {
                return;
            }
        } else {
            Matcher complexMatcher = COMPLEX_MATH.matcher(text);
            if (complexMatcher.find()) {
                String expression = complexMatcher.group(1).trim();
                try {
                    answer = Math.round(evaluateExpression(expression));
                } catch (Exception e) {
                    return;
                }
            }
        }
        if (answer >= 0 && mc.player != null && mc.player.connection != null) {
            mc.player.connection.sendChat(String.valueOf(answer));
            captchaSolvedByUs = true;
            long endTime = System.nanoTime();
            long durationNanos = endTime - captchaStartTime;
            mc.execute(() -> {
                if (mc.player == null) return;
                String playerName = mc.player.getName().getString();
                double ms = durationNanos / 1_000_000.0;
                String timeStr = durationNanos + " ns";
                String msg = "§6Aporia.cc §f→ §a" + playerName + " решил капчу за §e" + timeStr + "§a!";
                mc.player.displayClientMessage(Component.literal(msg), false);
            });
        }
    }

    /**
     * Скрывает сообщения о флае и капчи из чата.
     */
    @EventHandler
    public void onChatHide(ChatHideEvent event) {
        String text = event.plainText();
        
        if (hideFlyMessages.isEnabled() && 
            (text.contains("Вы успешно включили себе флай") ||
             text.contains("Вы успешно выключили себе флай"))) {
            event.cancel();
        }
    }

    /**
     * Обнаруживает падение и спамит /flyme.
     */
    @EventHandler
    public void onTick(TickEvent event) {
        if (!autoFlyMe.isEnabled()) {
            return;
        }
        
        ticksSinceLastCommand++;
        
        if (mc.player == null) {
            isFalling = false;
            ticksWithoutGround = 0;
            return;
        }

        if (mc.player.getAbilities().flying) {
            isFalling = false;
            ticksWithoutGround = 0;
            return;
        }

        if (mc.player.onGround()) {
            isFalling = false;
            ticksWithoutGround = 0;
            return;
        }

        ticksWithoutGround++;

        double velocityY = mc.player.getDeltaMovement().y;
        if (ticksWithoutGround >= 3 && velocityY < -0.08) {
            if (!isFalling) {
                isFalling = true;
                ticksSinceLastCommand = COMMAND_COOLDOWN;
            }
        }

        if (isFalling && ticksSinceLastCommand >= COMMAND_COOLDOWN) {
            sendFlymeCommand();
            ticksSinceLastCommand = 0;
        }
    }
    
    /**
     * Отправляет команду /flyme на сервер.
     */
    private void sendFlymeCommand() {
        if (mc.player != null && mc.player.connection != null) {
            mc.player.connection.sendCommand("flyme");
        }
    }

    /**
     * Вычисляет математическое выражение с максимальной скоростью.
     */
    private double evaluateExpression(String expr) throws Exception {
        expr = expr.replaceAll("\\s+", "");
        if (expr.isEmpty()) throw new Exception("Empty expression");
        return evaluate(expr, new int[]{0});
    }

    private double evaluate(String expr, int[] pos) throws Exception {
        double result = parseTerm(expr, pos);
        
        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op != '+' && op != '-') break;
            pos[0]++;
            double right = parseTerm(expr, pos);
            result = op == '+' ? result + right : result - right;
        }
        
        return result;
    }

    private double parseTerm(String expr, int[] pos) throws Exception {
        double result = parseFactor(expr, pos);
        
        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op != '*' && op != '/') break;
            pos[0]++;
            double right = parseFactor(expr, pos);
            result = op == '*' ? result * right : result / right;
        }
        
        return result;
    }

    private double parseFactor(String expr, int[] pos) throws Exception {
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '(') {
            pos[0]++;
            double result = evaluate(expr, pos);
            if (pos[0] < expr.length()) pos[0]++;
            return result;
        }
        
        return parseNumber(expr, pos);
    }

    private double parseNumber(String expr, int[] pos) throws Exception {
        int start = pos[0];
        
        while (pos[0] < expr.length() && Character.isDigit(expr.charAt(pos[0]))) {
            pos[0]++;
        }
        
        if (pos[0] == start) {
            throw new Exception("Invalid number");
        }
        
        return Double.parseDouble(expr.substring(start, pos[0]));
    }
    
    public BooleanSetting getAutoFlyMe() { return autoFlyMe; }
    public BooleanSetting getMathResolver() { return mathResolver; }
    public BooleanSetting getHideFlyMessages() { return hideFlyMessages; }
    public BooleanSetting getHideCaptcha() { return hideCaptcha; }
}
