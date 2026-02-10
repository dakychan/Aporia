# Saskkeee Simple Obfuscator

Простой обфускатор для Java байткода с поддержкой аннотаций.

## Поддерживаемые аннотации

### @CompileToNative
Помечает методы для нативной компиляции (повышенная защита).
```java
@CompileToNative
public void importantMethod() {
    // код
}
```

### @Entrypoint
Помечает класс как точку входа.
```java
@Entrypoint
public class MainClass {
    // код
}
```

### @VMProtect
Применяет виртуализацию/мутацию к методу.
```java
@VMProtect(type = CompileType.ULTRA)
public void secureMethod() {
    // код
}
```

Уровни защиты:
- `VIRTUALIZATION` - базовая виртуализация
- `MUTATION` - мутация кода
- `ULTRA` - максимальная защита

### @HttpStage
Помечает методы для HTTP проверок.
```java
@HttpStage(stage = 1)
public void checkLicense() {
    // код
}
```

## Использование

### Компиляция
```bash
./gradlew build
```

### Запуск обфускатора
```bash
java -cp build/classes/java/main by.saskkeee.compiler.Compiler <input_dir> <output_dir>
```

Пример:
```bash
java -cp build/classes/java/main by.saskkeee.compiler.Compiler build/classes/java/main build/obfuscated
```

## Что делает обфускатор

1. XOR шифрование байткода
2. Вставка мусорных байтов
3. Переименование классов/методов/полей (в разработке)
4. Обработка аннотаций защиты

## Примечания

- Обфускатор работает с скомпилированными .class файлами
- Не трогает конструкторы (<init>, <clinit>)
- Сохраняет структуру пакетов
