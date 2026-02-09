package by.saskkeee.user;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Класс для работы с информацией о пользователе.
 * Предоставляет методы для получения имени пользователя, UID и роли.
 */
public final class UserInfo {

    private static final Set<String> SYSTEM_USERS = Set.of("sudo", "admin", "administrator", "root");
    private static final Set<String> DEVELOPERS = Set.of("daky_chan", "dusky2", "kotay");
    private static final int UID_RANDOM_MIN = 1;
    private static final int UID_RANDOM_MAX = 99999;
    private static final Random RANDOM = new SecureRandom();

    private UserInfo() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Получает имя пользователя системы.
     *
     * @return имя пользователя или "user", если имя системное/недоступно
     */
    public static String getUsername() {
        String systemUser = System.getProperty("user.name", "").trim();

        if (systemUser.isEmpty()) {
            return "user";
        }

        if (SYSTEM_USERS.contains(systemUser.toLowerCase(Locale.ROOT))) {
            return "user";
        }

        return systemUser;
    }

    /**
     * Вычисляет UID на основе имени пользователя.
     * UID основан на сумме позиций букв в алфавите.
     *
     * @return строковое представление UID
     */
    public static String getUID() {
        String username = getUsername();

        if (username.equals("user")) {
            return generateRandomUID();
        }

        long uid = 0L;
        String lowerUsername = username.toLowerCase(Locale.ROOT);

        for (char c : lowerUsername.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                uid += (c - 'a' + 1);
            } else if (c >= 'а' && c <= 'я') {
                uid += (c - 'а' + 1);
            } else if (c >= '0' && c <= '9') {
                uid += (c - '0');
            }
            if (uid > Integer.MAX_VALUE) {
                uid = uid % Integer.MAX_VALUE;
            }
        }
        if (uid == 0) {
            return generateRandomUID();
        }

        return String.valueOf(uid);
    }

    /**
     * Генерирует случайный UID в заданном диапазоне.
     *
     * @return случайный UID
     */
    private static String generateRandomUID() {
        int randomUid = UID_RANDOM_MIN + RANDOM.nextInt(UID_RANDOM_MAX - UID_RANDOM_MIN + 1);
        return String.valueOf(randomUid);
    }

    /**
     * Определяет роль пользователя.
     *
     * @return "Разработчик" или "Пользователь"
     */
    public static String getRole() {
        String username = getUsername().toLowerCase(Locale.ROOT);

        if (DEVELOPERS.contains(username)) {
            return "Разработчик";
        }

        return "Пользователь";
    }

    /**
     * Проверяет, является ли текущий пользователь разработчиком.
     *
     * @return true если пользователь разработчик
     */
    public static boolean isDeveloper() {
        return "Разработчик".equals(getRole());
    }

    /**
     * Получает полную информацию о пользователе в формате JSON.
     *
     * @return JSON строка с информацией о пользователе
     */
    public static String getUserInfoJson() {
        return String.format(
                "{\"username\": \"%s\", \"uid\": \"%s\", \"role\": \"%s\"}",
                getUsername(),
                getUID(),
                getRole()
        );
    }

    /**
     * Основной метод для тестирования (только для отладки).
     */
    public static void main(String[] args) {
        System.out.println("Имя пользователя: " + getUsername());
        System.out.println("UID: " + getUID());
        System.out.println("Роль: " + getRole());
        System.out.println("Полная информация: " + getUserInfoJson());
        System.out.println("Это разработчик? " + isDeveloper());
    }
}

