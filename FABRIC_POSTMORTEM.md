# Fabric Port Attempt — Post-Mortem

## Вердикт: Fabric — хуйня для клиент-модов

### Что было
Попытка портировать Aporia Client (NeoForge 1.21.x) на Fabric 1.21.11 с Mojang mappings.

### Чего добились за вечер
- Базовый build system (Gradle + Loom + Kotlin)
- ~12 миксинов работали
- Рендер (drawRect), шрифты MSDF, title screen замена
- Модули, бинды, ClickGui работали
- Ассеты из внешней папки резолвились

### Чего НЕ добились
- **Blur** — recursion в рендере из-за timing миксинов
- **Прозрачность (drawRectBlurred)** — RenderPipeline API не даёт нормальный blend
- **3D рендер** — depth buffer не чистится, нет proper render order
- **~20 миксинов** из ref/ не сделаны (Camera, Entity, Fog, Sky, Disabler, и т.д.)
- **Config loading** — баг с Range bounds

### Почему Fabric хуйня для этого проекта

1. **Mixin = bytecode lottery.** Ошибка только при runtime, нет компиляторной проверки
2. **Нет контроля над render order.** Mid-method injection не работает надёжно
3. **RenderPipeline в 1.21.5+** — новый GPU API без документации
4. **ClassTweaker** вместо AccessWidener — формат не описан нигде
5. **Kotlin + Mixin = страдание** ($ ломает, @get: не работает)
6. **Каждый миксин зависит от порядка другого** — хуйня на хуйне

### Для кого Fabric норм
- Маленькие моды (5-10 миксинов)
- API моды (добавить предмет/рецепт)
- Не требующие контроля над render pipeline

### Для кого Fabric — страдание
- Клиент-читы с кастомным рендером
- Blur/прозрачность/post-processing
- Моды с 20+ injection points
- Anything where render timing matters

### NeoForge лучше потому что
- Events с приоритетами (контроль порядка)
- Access Transformer (документированный)
- Хорошая документация API
- Kotlin работает из коробки

---

*Создано после 4+ часов попыток заставить Fabric работать с клиент-читом.*
*Рекомендация: не трогать Fabric для серьёзных клиент-модов.*
