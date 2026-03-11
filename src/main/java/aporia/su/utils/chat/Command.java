package aporia.su.utils.chat;

import java.util.List;

/**
 * Базовый абстрактный класс для всех команд в системе.
 * Каждая команда должна наследоваться от этого класса и реализовывать метод execute.
 *
 * @author Aporia
 */
public abstract class Command {
    private final String name;
    private final String description;
    private final List<String> aliases;

    /**
     * Создает команду без алиасов.
     *
     * @param name имя команды (без префикса)
     * @param description описание команды
     */
    public Command(String name, String description) {
        this(name, description, List.of());
    }

    /**
     * Создает команду с алиасами.
     *
     * @param name имя команды (без префикса)
     * @param description описание команды
     * @param aliases список альтернативных имен команды
     */
    public Command(String name, String description, List<String> aliases) {
        this.name = name;
        this.description = description;
        this.aliases = aliases;
    }

    /**
     * @return имя команды
     */
    public String getName() {
        return name;
    }

    /**
     * @return описание команды
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return список алиасов команды
     */
    public List<String> getAliases() {
        return aliases;
    }

    /**
     * Выполняет команду с заданными аргументами.
     *
     * @param args список аргументов команды (без имени команды)
     */
    public abstract void execute(List<String> args);

    /**
     * Возвращает список автодополнений для текущих аргументов.
     * По умолчанию возвращает пустой массив.
     *
     * @param args текущие аргументы команды
     * @return массив возможных автодополнений
     */
    public String[] getCompletions(List<String> args) {
        return new String[0];
    }

    // ========================================================================
    // Режим ожидания ввода (для интерактивных команд)
    // ========================================================================

    /**
     * @return количество шагов ожидания ввода (0 = режим ожидания отключен)
     */
    public int getExpectedSteps() {
        return 0;
    }

    /**
     * Вызывается после каждого шага ввода.
     *
     * @param currentStep текущий шаг (начиная с 1)
     * @param buffer собранные данные
     */
    public void onStepComplete(int currentStep, List<String> buffer) {}

    /**
     * Выполняет команду с буфером введенных данных.
     * Вызывается после завершения всех шагов ожидания.
     *
     * @param buffer список введенных данных по шагам
     */
    public void executeWithBuffer(List<String> buffer) {
        execute(buffer);
    }

    /**
     * @return сообщение для отображения на текущем шаге ожидания
     */
    public String getStepPrompt(int currentStep) {
        return "";
    }
}
