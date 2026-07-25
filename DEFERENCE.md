# DEFERENCE.md — Изменения в Kotlin Compiler (obfuscation fork)

> Форк kompilyatora Kotlin s obfuskiey imen classov i metodov v JVM bytecode.

---

## 1. ObfuscationLowering — IR-level obfuscation

**Fayl:** `compiler/ir/backend.jvm/lower/src/org/jetbrains/kotlin/backend/jvm/lower/ObfuscationLowering.kt`

- `FileLoweringPass`, prinimaet `JvmBackendContext`
- Perebiraet vse `IrClass` v IrFile
- Dlya kazhdogo klassa: menyaet `IrClass.name` na sluchaynuyu stringu (20 simvolov, A-Z)
- Dlya kazhdogo `IrFunction`: menyaet imya (krome `<init>`, `<clinit>`, `main`, `access$*`)
- Dlya kazhdogo `IrProperty`: menyaet imya + backing field
- Dlya kazhdogo `IrField`: menyaet imya
- Vlozhennye klassy obfuskiruyutsya rekursivno

**Pochemu IR, a ne bytecode:** IrCall ssylaetsya na IrFunction po identichnosti, ne po imeni. Menyaem imya v Ir -> vse referentsii avtomaticheski aktualiziruyutsya.

---

## 2. ObfuscationContext — generator sluchaynyh imen

**Fayl:** `compiler/ir/backend.jvm/lower/src/org/jetbrains/kotlin/backend/jvm/lower/ObfuscationContext.kt`

- `object ObfuscationContext` (public)
- Хранит 3 mappa: `classNames`, `methodNames`, `propertyNames`
- Klyuch — polnoe FQ-imya (napr. `com.calc.Calculator`), znachenie — sluchaynaya stringa
- `getOrPutClassName(fqName)` — sozdayet ili vozvrashchaet obfuscirovanname imya
- `getOrPutMethodName(className, methodName)` — dlya metodov
- `getOrPutPropertyName(className, propertyName)` — dlya svoystv
- `getObfuscatedClassName(fqName)` — dlya poiska v manifeste (ispolzuetsya v CompileEnvironmentUtil)
- `clear()` — ochistka (dlya testov)

---

## 3. CompileEnvironmentUtil — obnovlenie Main-Class v manifeste

**Fayl:** `compiler/cli/src/org/jetbrains/kotlin/cli/jvm/compiler/CompileEnvironmentUtil.java`

**Bylo:**
```java
if (mainClass != null) {
    mainAttributes.putValue("Main-Class", mainClass.asString());
}
```

**Stalo:**
```java
if (mainClass != null) {
    String mainClassName = mainClass.asString();
    String obfuscated = ObfuscationContext.INSTANCE.getObfuscatedClassName(mainClassName);
    if (obfuscated != null) {
        String shortName = mainClass.shortName().asString();
        mainClassName = mainClassName.substring(0, mainClassName.length() - shortName.length()) + obfuscated;
    }
    mainAttributes.putValue("Main-Class", mainClassName);
}
```

**Pochemu:** pri kompilyacii s `-include-runtime -d app.jar` kompilyator sozdayet JAR s manifestom `Main-Class: com.calc.MainKt`. No nash obfusciator menyaet `MainKt` -> `YFTTEDAFCJNKHCRVQCYQ`. JVM ischet `main()` v orig. imeni -> ClassNotFound. Teper' manifest avtomaticheski poluchaet obfuscirovannoe imya.

---

## 4. Zavisimosti moduley

**`compiler/cli/build.gradle.kts`** — dobavlena:
```kotlin
implementation(project(":compiler:backend.jvm.lower"))
```
Nuzhna dlya dostupa k `ObfuscationContext` iz `CompileEnvironmentUtil.java`.

---

## 5. JvmLoweringPhases — registraciya passa

**Fayl:** `compiler/ir/backend.jvm/lower/src/org/jetbrains/kotlin/backend/jvm/JvmLoweringPhases.kt`

Dobavlen `::ObfuscationLowering` v spisok `jvmFilePhases` (v konce).

---

## 6. Pre-existing error fixes

### 6.1 CliDiagnosticReporting.kt
**Fayl:** `compiler/cli/cli-base/src/org/jetbrains/kotlin/cli/CliDiagnosticReporting.kt`
- Ubiraem neopr. funkciyu `context()` — zamenyaem na pryamoy vyzyv `factory.create()` + `diagnosticsCollector.report()`

### 6.2 ImplicitReceiverUtils.kt
**Fayl:** `compiler/fir/semantics/src/org/jetbrains/kotlin/fir/declarations/ImplicitReceiverUtils.kt`
- Ubiraem annotaciyu `@ConsistentCopyVisibility` (net v classpath stdlib)

### 6.3 fir:semantics/build.gradle.kts
```kotlin
tasks.withType<KotlinJvmCompile> {
    compilerOptions.freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
}
```

### 6.4 fir:resolve + fir:checkers
Dobavlena zavisimost` na `kotlin-stdlib-bootstrap` (2.4.20-dev-4664) — reshaet otsutstvie funkcii `context()` (SinceKotlin 2.2).

### 6.5 gradle/verification-metadata.xml
Dobavleny v trusted artifacts: `kotlin-stdlib`, `kotlin-stdlib-common`.

---

## 7. Primer obfuskirovannogo vyvoda

### Iskhodnyy kod:
```kotlin
package com.calc
object Calculator {
    fun add(a: Int, b: Int): Int = a + b
    fun describe(): String = "calc v1.0"
}
fun main() { println(Calculator.add(5, 3)) }
```

### Posle kompilyacii:
| Original | Obfuscated |
|---|---|
| `Calculator.class` | `CBDLCOIIEGDHHLNDEVBE.class` |
| `MainKt.class` | `RQINLOXJSDBDFZNZCCTX.class` |
| `add(Int, Int)` | `JCUXHQIRIRLBKIBZYONL(Int, Int)` |
| `describe()` | `BFYWEMDCWIJWRMABTAIU()` |
| `main(String[])` | `main(String[])` (NE obfuskiruetsya!) |
| `Main-Class:` v manifeste | `com.calc.CBDLCOIIEGDHHLNDEVBE` |

### Klassy v bytecode:
```
public final class com.calc.CBDLCOIIEGDHHLNDEVBE {
    public static final void main();
    public static void main(java.lang.String[]);
}

public final class com.calc.RQINLOXJSDBDFZNZCCTX {
    public static final com.calc.RQINLOXJSDBDFZNZCCTX YPQPBDTAELOBEADTCWPB;
    public final int JCUXHQIRIRLBKIBZYONL(int, int);
    public final int WWRUPMDWRWXSZWWFOEEX(int, int);
    public final java.lang.String BFYWEMDCWIJWRMABTAIU();
}
```

---

## 8. Pol'zovanie

### Rezhim 1: IR-level (tol'ko Kotlin)
```bash
# Obfuskaciya avtomaticheski na IR urovne
kotlinc.bat -d out.jar Main.kt Calculator.kt -nowarn
java -jar out.jar
```
Tol'ko Kotlin klassy. Java ssylki NE rabotayut.

### Rezhim 2: Bytecode-level (Kotlin + Java)
```bash
# Step 1: Compile Kotlin (bez IR obfuski)
kotlinc.bat -Xafter-compile -d classes src/com/calc/Calculator.kt src/com/calc/Main.kt -nowarn

# Step 2: Compile Java (vidit orig. Kotlin imena)
javac -d classes -cp classes src/com/calc/Main.java

# Step 3: Bytecode obfuscation (vse klassy: Kotlin + Java)
java -cp compiler.jar org.jetbrains.kotlin.cli.jvm.compiler.BytecodeObfuscator classes

# Step 4: Run
java -cp classes;kotlin-stdlib.jar com.calc.Main
```
VSE klassy obfuskiruyutsya vmeste. Java ssylki na Kotlin obnovlyayutsya.

### Standalone obfuscator
```bash
# Obfuscirovat' vse .class v papke
java -cp compiler.jar org.jetbrains.kotlin.cli.jvm.compiler.BytecodeObfuscator <directory>
```

---

## 9. Ogranicheniya

- Imya `main()` NE obfuskiruetsya ( JVM trebuet `public static void main(String[])` )
- Klassy s `main()` NE obfuskiruyutsya (entry point)
- Imena paketov SOHRANYAYUTSYA ( tol'ko imena klassov/metodov )
- Imena vnutrennikh anonimnykh klassov obfuskiruyutsya
- `compiler.version` v JAR nuzhno patchit' vruchnuyu (menyaet LanguageVersion)
- Enum funktsii (`values`, `entries`, `valueOf`) NE obfuskiruyutsya
- `@ChaosNative` klassy NE obfuskiruyutsya
- Java -> Kotlin ssylki RABOTAYUT tol'ko v rezhime 2 (bytecode-level)
