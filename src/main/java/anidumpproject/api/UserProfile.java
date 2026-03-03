package anidumpproject.api;

import anidumpproject.api.annotation.Native;
import aporia.cc.OsManager;
import aporia.cc.UserData;

import java.util.HashMap;
import java.util.Map;

/**
 * Профиль пользователя с интеграцией Kotlin UserData и OsManager.
 * <p>
 * Предоставляет единую точку доступа к информации о пользователе,
 * системе и аппаратном обеспечении. Использует Singleton паттерн.
 * </p>
 * 
 * <h3>Основные возможности:</h3>
 * <ul>
 *   <li>Получение имени пользователя из системы</li>
 *   <li>Генерация уникального Hardware ID</li>
 *   <li>Определение роли пользователя (USER, DEVELOPER, ADMIN)</li>
 *   <li>Генерация UUID на основе username</li>
 *   <li>Информация о системе и платформе</li>
 * </ul>
 * 
 * <h3>Примеры использования:</h3>
 * <pre>{@code
 * // Получить профиль
 * UserProfile profile = UserProfile.getInstance();
 * 
 * // Получить данные пользователя
 * String username = profile.getUsername();
 * String hwid = profile.getHwid();
 * String role = profile.getRole();
 * 
 * // Проверить роль
 * if (profile.isDeveloper()) {
 *     // Доступ к dev функциям
 * }
 * 
 * // Получить системную информацию
 * String sysInfo = profile.getSystemInfo();
 * }</pre>
 * 
 * <h3>Интеграция с Kotlin:</h3>
 * <p>
 * Класс использует Kotlin компоненты для генерации данных:
 * </p>
 * <ul>
 *   <li>{@link aporia.cc.UserData - генерация пользовательских данных</li>
 *   <li>{@link aporia.cc.UserGenerator} - генерация UUID и HWID</li>
 *   <li>{@link aporia.cc.OsManager} - информация о системе</li>
 * </ul>
 * 
 * @author Aporia Team
 * @version 2.0
 * @since 1.0
 * @see UserData
 * @see OsManager
 */
public class UserProfile {

    private static final UserProfile instance = new UserProfile();

    /**
     * Получить единственный экземпляр профиля пользователя.
     * 
     * @return экземпляр UserProfile
     */
    public static UserProfile getInstance() {
        return instance;
    }

    private final Map<String, String> cache = new HashMap<>();
    private final UserData.UserDataClass userData;

    /**
     * Приватный конструктор для Singleton паттерна.
     * <p>
     * Инициализирует профиль пользователя, получая данные из Kotlin UserData
     * и кэшируя их для быстрого доступа.
     * </p>
     */
    private UserProfile() {
        userData = UserData.getUserData();
        cache.put("username", userData.getUsername());
        cache.put("hwid", userData.getHardwareId());
        cache.put("role", userData.getRole().name());
        cache.put("uid", userData.getUuid());
        cache.put("subTime", "unlimited");
        cache.put("os", OsManager.getOsName());
        cache.put("platform", OsManager.getPlatform().name());
        cache.put("arch", OsManager.getOsArch());
    }

    /**
     * Получить имя пользователя.
     * <p>
     * Имя берется из системы (whoami на Linux/Mac, %USERNAME% на Windows).
     * Если системное имя недоступно или является служебным (root, admin),
     * генерируется случайное имя.
     * </p>
     * 
     * @return имя пользователя
     */
    @Native(type = Native.Type.STANDARD)
    public String getUsername() {
        return userData.getUsername();
    }

    /**
     * Получить Hardware ID (HWID).
     * <p>
     * Уникальный идентификатор на основе аппаратного обеспечения.
     * Используется для привязки лицензий к конкретному компьютеру.
     * </p>
     * <p>
     * HWID генерируется на основе:
     * </p>
     * <ul>
     *   <li>UUID материнской платы (Windows: wmic, Linux: /etc/machine-id)</li>
     *   <li>Имени пользователя</li>
     *   <li>Названия ОС</li>
     *   <li>Идентификатора процессора</li>
     *   <li>Количества ядер процессора</li>
     * </ul>
     * 
     * @return Hardware ID в формате UUID
     */
    @Native(type = Native.Type.STANDARD)
    public String getHwid() {
        return userData.getHardwareId();
    }

    /**
     * Получить роль пользователя.
     * <p>
     * Роль определяется автоматически на основе имени пользователя.
     * Разработчики (daky_chan, dusky2, kotay) получают роль DEVELOPER.
     * </p>
     * 
     * @return роль пользователя (USER, DEVELOPER, ADMIN, CONTRIBUTOR)
     * @see UserData.UserRole
     */
    @Native(type = Native.Type.STANDARD)
    public String getRole() {
        return userData.getRole().name();
    }

    /**
     * Получить UUID пользователя.
     * <p>
     * Уникальный идентификатор, генерируемый на основе имени пользователя.
     * Используется для идентификации пользователя в системе.
     * </p>
     * <p>
     * UUID генерируется путем преобразования каждого символа username
     * в числовое значение и сжатия результата.
     * </p>
     * 
     * @return UUID пользователя (числовая строка)
     */
    @Native(type = Native.Type.STANDARD)
    public String getUid() {
        return userData.getUuid();
    }

    /**
     * Получить время подписки.
     * <p>
     * В текущей версии возвращает "unlimited".
     * В будущем будет реализована система подписок с проверкой срока действия.
     * </p>
     * 
     * @return время подписки
     * @deprecated Будет заменено на реальную систему подписок
     */
    @Native(type = Native.Type.STANDARD)
    public String getSubsTime() {
        // TODO: Реализовать систему подписок
        return "unlimited";
    }

    /**
     * Проверить, является ли пользователь разработчиком.
     * <p>
     * Разработчики имеют доступ к дополнительным функциям и отладочной информации.
     * </p>
     * 
     * @return true если пользователь - разработчик
     */
    public boolean isDeveloper() {
        return userData.getRole() == UserData.UserRole.DEVELOPER;
    }

    /**
     * Получить информацию о системе.
     * <p>
     * Возвращает строку с названием ОС, версией и архитектурой.
     * </p>
     * 
     * @return информация о системе в формате "OS Version (Arch)"
     * @see OsManager
     */
    public String getSystemInfo() {
        return String.format("%s %s (%s)", 
            OsManager.getOsName(),
            OsManager.getOsVersion(),
            OsManager.getOsArch()
        );
    }

    /**
     * Получить значение профиля по ключу.
     * <p>
     * Универсальный метод для получения любых данных профиля из кэша.
     * </p>
     * 
     * <h3>Доступные ключи:</h3>
     * <ul>
     *   <li><b>username</b> - имя пользователя</li>
     *   <li><b>hwid</b> - Hardware ID</li>
     *   <li><b>role</b> - роль пользователя</li>
     *   <li><b>uid</b> - UUID пользователя</li>
     *   <li><b>subTime</b> - время подписки</li>
     *   <li><b>os</b> - название ОС</li>
     *   <li><b>platform</b> - платформа (WINDOWS, LINUX, MAC)</li>
     *   <li><b>arch</b> - архитектура процессора</li>
     * </ul>
     * 
     * @param profile ключ профиля
     * @return значение или пустая строка если ключ не найден
     */
    public String profile(String profile) {
        return cache.getOrDefault(profile, "");
    }

    /**
     * Получить полные данные пользователя.
     * <p>
     * Возвращает объект с полной информацией о пользователе из Kotlin UserData.
     * </p>
     *
     * @return объект UserDataClass с данными пользователя
     * @see UserData.UserDataClass
     */
    public UserData.UserDataClass getUserData() {
        return userData;
    }
}
