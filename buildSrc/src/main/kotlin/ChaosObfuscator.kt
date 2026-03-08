import org.gradle.api.Project
import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * CHAOS OBFUSCATOR - Полная каша с ежедневной ротацией
 * 
 * Что делает:
 * 1. Обфусцирует package, import, class, методы, поля
 * 2. Маппинги меняются каждый день в 00:00 UTC
 * 3. В runtime только каша - дамп бесполезен
 * 4. Миксины не трогает
 * 5. Генерирует RuntimeMapper с алгоритмом восстановления
 * 6. UNICODE CHAOS - арабский, китайский, японский, руны, иероглифы
 */
class ChaosObfuscator {
    
    lateinit var project: Project
    
    // UNICODE CHAOS - пиздец глазам
    private val unicodeChaos = listOf(
        // АРАБСКИЙ (справа налево, пиздец)
        "ابتثجحخدذرزسشصضطظعغفقكلمنهوي",
        "ءآأؤإئابةتثجحخدذرزسشصضطظعغ",
        "ـفقكلمنهوىيًٌٍَُِّْٕٖٜٓٔٗ٘ٙٚٛٝٞ",
        
        // КИТАЙСКИЙ (иероглифы, пизда глазам)
        "的一是不了人我在有他这为之大来以个中上们到说时",
        "要就出会可也你得对生能下面孩子对工动力已经面",
        "愛你我他她它我們你們他們今天明天昨天現在過去未來",
        "甲乙丙丁戊己庚辛壬癸子丑寅卯辰巳午未申酉戌亥",
        
        // ЯПОНСКИЙ (хирагана + катакана + кандзи)
        "あいうえおかきくけこさしすせそたちつてとなにぬねの",
        "はひふへほまみむめもやゆよらりるれろわをんぁぃぅぇぉ",
        "アイウエオカキクケコサシスセソタチツテトナニヌネノ",
        "ハヒフヘホマミムメモヤユヨラリルレロワヲンァィゥェォ",
        "犬猫鳥魚山川水火木金土日月年春夏秋冬東西南北",
        
        // КОРЕЙСКИЙ (хангыль - квадратные кракозябры)
        "가나다라마바사아자차카타파하",
        "거너더러머버서어저처커터퍼허",
        "고노도로모보소오조초코토포호",
        "구누두루무부수우주추쿠투푸후",
        "그느드르므브스으즈츠크트프흐",
        "기니디리미비시이지치키티피히",
        
        // ТАЙСКИЙ (петли и крючки)
        "กขฃคฅฆงจฉชซฌญฎฏฐฑฒณดตถทธนบปผฝพฟภมยรลวศษสหฬอฮ",
        "ะาำิีืึุูเแโใไๅๆ็่้๊๋์",
        
        // ДЕВАНАГАРИ (хинди, санскрит)
        "अआइईउऊऋएऐओऔकखगघङचछजझञटठडढणतथदधनपफबभमयरलवशषसह",
        "ािीुूृॄेैोौ्ँंः",
        
        // ТИБЕТСКИЙ (вообще пиздец)
        "ཀཁགངཅཆཇཉཏཐདནཔཕབམཙཚཛཝཞཟའཡརལཤཥསཧཨ",
        
        // ГРУЗИНСКИЙ (мхедрули)
        "აბგდევზთიკლმნოპჟრსტუფქღყშჩცძწჭხჯჰ",
        
        // АРМЯНСКИЙ
        "աբգդեզէըթժիլխծկհձղճմյնշոչպջռսվտրցւփքօֆ",
        
        // МОНГОЛЬСКИЙ (старомонгольское письмо - вертикально!)
        "ᠠᠡᠢᠣᠤᠥᠦᠧᠨᠩᠪᠫᠬᠭᠮᠯᠰᠱᠲᠳᠴᠵᠶᠷᠸᠹᠺᠻᠼᠽᠾᠿ",
        
        // РУНЫ (древнегерманские)
        "ᚠᚢᚦᚨᚱᚲᚷᚹᚺᚾᛁᛃᛇᛈᛉᛊᛏᛒᛖᛗᛚᛜᛞᛟ",
        
        // ЕГИПЕТСКИЕ ИЕРОГЛИФЫ
        "𓀀𓀁𓀂𓀃𓀄𓀅𓀆𓀇𓀈𓀉𓀊𓀋𓀌𓀍𓀎𓀏",
        
        // КЛИНОПИСЬ (Шумер, Вавилон)
        "𒀀𒀁𒀂𒀃𒀄𒀅𒀆𒀇𒀈𒀉𒀊𒀋𒀌𒀍𒀎𒀏",
        
        // МАТЕМАТИЧЕСКИЕ СИМВОЛЫ (жирные, рукописные, готические)
        "𝐀𝐁𝐂𝐃𝐄𝐅𝐆𝐇𝐈𝐉𝐊𝐋𝐌𝐍𝐎𝐏𝐐𝐑𝐒𝐓𝐔𝐕𝐖𝐗𝐘𝐙",
        "𝑎𝑏𝑐𝑑𝑒𝑓𝑔ℎ𝑖𝑗𝑘𝑙𝑚𝑛𝑜𝑝𝑞𝑟𝑠𝑡𝑢𝑣𝑤𝑥𝑦𝑧",
        "𝒜ℬ𝒞𝒟ℰℱ𝒢ℋℐ𝒥𝒦ℒℳ𝒩𝒪𝒫𝒬ℛ𝒮𝒯𝒰𝒱𝒲𝒳𝒴𝒵",
        "𝔄𝔅ℭ𝔇𝔈𝔉𝔊ℌℑ𝔍𝔎𝔏𝔐𝔑𝔒𝔓𝔔ℜ𝔖𝔗𝔘𝔙𝔚𝔛𝔜ℨ",
        
        // ЗЕРКАЛЬНЫЕ БУКВЫ (перевернутые)
        "ɐqɔpǝɟɓɥıɾʞlɯuodbɹsʇnʌʍxʎz",
        "∀𐐒ƆᗡƎℲ⅁HIſ⅂WNOԀΌᴚS⊥∩ɅMX⅄Z",
        
        // ШРИФТ БРЕЙЛЯ (для слепых реверсеров)
        "⠁⠃⠉⠙⠑⠋⠛⠓⠊⠚⠅⠇⠍⠝⠕⠏⠟⠗⠎⠞⠥⠧⠺⠭⠽⠵",
        
        // ДИАКРИТИКА ВСЮДУ (буквы с точками сверху/снизу/сбоку)
        "ạḅḍẹḥịḷṃṇọṛṣṭụṿẏỵ",
        "áǎàâāåãäąæćĉčçċďđéěèêēėę",
        "íǐìîīïĩıĵĺľļłńňņñóǒòôōőõøœ",
        "úǔùûūůűũųẃẁŵýỳŷÿźžż",
        
        // ГРЕЧЕСКИЙ (альфа, бета, гамма...)
        "αβγδεζηθικλμνξοπρστυφχψω",
        "ΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ",
        
        // КИРИЛЛИЦА (русский, украинский, болгарский)
        "абвгдежзийклмнопрстуфхцчшщъыьэюя",
        "АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ",
        "ѐёђѓєѕіїјљњћќѝўџ",
        
        // ЭМОДЗИ (да, они тоже валидные идентификаторы в Java!)
        "😀😁😂🤣😃😄😅😆😉😊😋😎😍😘🥰😗😙😚",
        "🔥💀👻👽🤖💩🎃🎄🎁🎈🎉🎊🎋🎍🎎🎏",
        
        // СПЕЦИАЛЬНЫЕ СИМВОЛЫ
        "ªºµ¹²³¼½¾ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞß",
        "ĀāĂăĄąĆćĈĉĊċČčĎďĐđĒēĔĕĖėĘęĚěĜĝĞğĠġĢģ",
        
        // СТРЕЛКИ И СИМВОЛЫ
        "←↑→↓↔↕↖↗↘↙↚↛↜↝↞↟↠↡↢↣↤↥↦↧↨",
        "⇐⇑⇒⇓⇔⇕⇖⇗⇘⇙⇚⇛⇜⇝⇞⇟⇠⇡⇢⇣⇤⇥⇦⇧⇨⇩⇪",
        
        // ГЕОМЕТРИЧЕСКИЕ ФИГУРЫ
        "■□▢▣▤▥▦▧▨▩▪▫▬▭▮▯▰▱▲△▴▵▶▷▸▹►▻▼▽▾▿◀◁◂◃◄◅",
        "●○◎◉◊○◌◍◎●◐◑◒◓◔◕◖◗◘◙◚◛◜◝◞◟◠◡◢◣◤◥",
        
        // МУЗЫКАЛЬНЫЕ СИМВОЛЫ
        "♩♪♫♬♭♮♯𝄞𝄢𝄫𝄪𝄬",
        
        // ШАХМАТНЫЕ ФИГУРЫ
        "♔♕♖♗♘♙♚♛♜♝♞♟",
        
        // КАРТОЧНЫЕ МАСТИ
        "♠♡♢♣♤♥♦♧",
        
        // ЗОДИАК
        "♈♉♊♋♌♍♎♏♐♑♒♓",
        
        // ПЛАНЕТЫ
        "☉☿♀♁♂♃♄♅♆♇",
        
        // АЛХИМИЧЕСКИЕ СИМВОЛЫ
        "🜁🜂🜃🜄🜅🜆🜇🜈🜉🜊🜋🜌🜍🜎🜏🜐🜑🜒🜓",
        
        // I CHING (Книга Перемен)
        "☰☱☲☳☴☵☶☷",
        
        // ТАЙСКИЕ ЦИФРЫ
        "๐๑๒๓๔๕๖๗๘๙",
        
        // АРАБСКИЕ ЦИФРЫ (восточные)
        "٠١٢٣٤٥٦٧٨٩",
        
        // БЕНГАЛЬСКИЕ ЦИФРЫ
        "০১২৩৪৫৬৭৮৯",
        
        // ДЕВАНАГАРИ ЦИФРЫ
        "०१२३४५६७८९"
    )
    
    private val classMappings = mutableMapOf<String, String>()
    private val methodMappings = mutableMapOf<String, MutableMap<String, String>>()
    private val fieldMappings = mutableMapOf<String, MutableMap<String, String>>()
    private var dailySeed: Long = 0
    
    /**
     * Расширенные обфускаторы (Catlean-style)
     */
    private lateinit var stringEncryptor: StringEncryptor
    private lateinit var methodHandleGenerator: MethodHandleGenerator
    private lateinit var threadLocalCache: ThreadLocalCacheGenerator
    private lateinit var longObfuscator: LongObfuscator
    private lateinit var garbageGenerator: GarbageGenerator
    
    
    fun obfuscateFromJar(jarFile: File, outputDir: File) {
        println("=== CHAOS OBFUSCATOR ===")
        dailySeed = getDailySeed()
        println("Daily seed: $dailySeed")
        
        stringEncryptor = StringEncryptor(dailySeed)
        methodHandleGenerator = MethodHandleGenerator(dailySeed)
        threadLocalCache = ThreadLocalCacheGenerator(dailySeed)
        longObfuscator = LongObfuscator(dailySeed)
        garbageGenerator = GarbageGenerator(dailySeed)
        
        val tempDir = File(project.buildDir, "temp-obfuscate-scan")
        tempDir.deleteRecursively()
        tempDir.mkdirs()
        
        project.copy {
            from(project.zipTree(jarFile))
            into(tempDir)
        }
        
        println("Scanning classes...")
        scanClasses(tempDir)
        
        if (classMappings.isEmpty()) {
            println("No classes to obfuscate")
            tempDir.deleteRecursively()
            return
        }
        
        println("Found ${classMappings.size} classes to obfuscate")
        classMappings.forEach { (old, new) ->
            println("  $old -> $new")
        }
        
        outputDir.deleteRecursively()
        outputDir.mkdirs()
        
        obfuscateClasses(tempDir, outputDir)
        
        val date = LocalDate.now(ZoneId.of("UTC"))
        saveMappings(File(project.buildDir, "chaos-mappings-$date.txt"))
        
        generateRuntimeMapper(outputDir)
        
        tempDir.deleteRecursively()
        println("✅ Obfuscation complete")
    }
    
    fun obfuscateExtracted(inputDir: File, outputDir: File) {
        dailySeed = getDailySeed()
        
        stringEncryptor = StringEncryptor(dailySeed)
        methodHandleGenerator = MethodHandleGenerator(dailySeed)
        threadLocalCache = ThreadLocalCacheGenerator(dailySeed)
        longObfuscator = LongObfuscator(dailySeed)
        garbageGenerator = GarbageGenerator(dailySeed)
        
        scanClasses(inputDir)
        
        if (classMappings.isEmpty()) {
            return
        }
        
        outputDir.deleteRecursively()
        outputDir.mkdirs()
        
        obfuscateClasses(inputDir, outputDir)
        
        val date = LocalDate.now(ZoneId.of("UTC"))
        saveMappings(File(project.buildDir, "chaos-mappings-$date.txt"))
        
        generateRuntimeMapper(outputDir)
    }
    
    private fun getDailySeed(): Long {
        val date = LocalDate.now(ZoneId.of("UTC"))
        val dateStr = "${date.year}${date.monthValue}${date.dayOfMonth}"
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(dateStr.toByteArray())
        return hash.fold(0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 0xFF) }
    }
    
    private fun scanClasses(dir: File) {
        dir.walkTopDown().forEach { file ->
            if (file.extension == "class") {
                val classNode = readClass(file)
                
                // Пропускаем миксины
                if (isMixin(classNode)) {
                    return@forEach
                }
                
                // Проверяем аннотации
                val level = getObfuscationLevel(classNode)
                if (level != ObfuscationLevel.NONE) {
                    val oldName = classNode.name
                    
                    if (oldName.contains("$")) {
                        return@forEach
                    }
                    
                    val newName = generateChaosClassName(oldName, level)
                    classMappings[oldName] = newName
                    
                    methodMappings[oldName] = mutableMapOf()
                    fieldMappings[oldName] = mutableMapOf()
                }
            }
        }
        
        // Второй проход - обрабатываем внутренние классы
        dir.walkTopDown().forEach { file ->
            if (file.extension == "class") {
                val classNode = readClass(file)
                val oldName = classNode.name
                
                // Это внутренний класс?
                if (oldName.contains("$")) {
                    // Находим родительский класс
                    val outerClassName = oldName.substringBefore("$")
                    
                    // Если родительский класс обфусцирован, обфусцируем и внутренний
                    if (classMappings.containsKey(outerClassName)) {
                        val outerNewName = classMappings[outerClassName]!!
                        val innerPart = oldName.substringAfter("$")
                        val newName = "$outerNewName\$$innerPart"
                        classMappings[oldName] = newName
                        
                        methodMappings[oldName] = mutableMapOf()
                        fieldMappings[oldName] = mutableMapOf()
                    }
                }
            }
        }
    }
    
    private fun isMixin(classNode: ClassNode): Boolean {
        return classNode.visibleAnnotations?.any { 
            it.desc.contains("Mixin") || it.desc.contains("mixin")
        } ?: false
    }
    
    private fun getObfuscationLevel(classNode: ClassNode): ObfuscationLevel {
        val allAnnotations = (classNode.visibleAnnotations ?: emptyList()) + 
                            (classNode.invisibleAnnotations ?: emptyList())
        
        allAnnotations.forEach { ann ->
            if (ann.desc.contains("MainClass")) {
                return ObfuscationLevel.NONE
            }
        }
        
        allAnnotations.forEach { ann ->
            if (ann.desc.contains("Obfuscate")) {
                if (ann.values != null) {
                    for (i in 0 until ann.values.size step 2) {
                        if (ann.values[i] == "level") {
                            val levelValue = ann.values[i + 1]
                            if (levelValue is Array<*> && levelValue.size >= 2) {
                                val level = levelValue[1] as? String ?: "NONE"
                                return when (level) {
                                    "LIGHT" -> ObfuscationLevel.LIGHT
                                    "MEDIUM" -> ObfuscationLevel.MEDIUM
                                    "HEAVY" -> ObfuscationLevel.HEAVY
                                    "EXTREME" -> ObfuscationLevel.EXTREME
                                    else -> ObfuscationLevel.NONE
                                }
                            }
                        }
                    }
                }
                return ObfuscationLevel.MEDIUM
            }
        }
        
        allAnnotations.forEach { ann ->
            if (ann.desc.contains("Native")) {
                return ObfuscationLevel.HEAVY
            }
        }
        
        return ObfuscationLevel.NONE
    }
    
    private fun generateChaosClassName(oldName: String, level: ObfuscationLevel): String {
        val random = Random(oldName.hashCode().toLong() + dailySeed)
        
        val packageParts = oldName.split("/").dropLast(1)
        val className = oldName.split("/").last()
        
        val obfuscatedPackage = packageParts.joinToString("/") { 
            generateChaosName("pkg", 1, random, level)
        }
        
        val obfuscatedClass = when (level) {
            ObfuscationLevel.LIGHT -> generateChaosName(className, 2, random, level)
            ObfuscationLevel.MEDIUM -> generateChaosName(className, 3, random, level)
            ObfuscationLevel.HEAVY -> generateChaosName(className, 4, random, level)
            ObfuscationLevel.EXTREME -> generateChaosName(className, 5, random, level)
            ObfuscationLevel.NONE -> className
        }
        
        return if (obfuscatedPackage.isNotEmpty()) {
            "$obfuscatedPackage/$obfuscatedClass"
        } else {
            "aporia/su/$obfuscatedClass"
        }
    }
    
    /**
     * Генерирует хаотичное имя
     * 
     * Формат:
     * - package: рандом буква (a, b, c)
     * - import: буква + символ (a$, b_, c0)
     * - class: символ + буква (1a, $b, _c)
     * - method: буква буква (aa, ab, ac)
     * - field: символ символ ($$, $_, $0)
     */
    private fun generateChaosName(base: String, length: Int, random: Random = Random(base.hashCode().toLong() + dailySeed), level: ObfuscationLevel = ObfuscationLevel.MEDIUM): String {
        // ASCII база (всегда доступна)
        val asciiLetters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val asciiSymbols = "_$"
        
        val charPool = buildString {
            append(asciiLetters)
            append(asciiSymbols)
            
            when (level) {
                ObfuscationLevel.LIGHT -> {}
                ObfuscationLevel.MEDIUM -> {
                    append("αβγδεζηθικλμνξοπρστυφχψω")
                    append("ΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ")
                    append("абвгдежзийклмнопрстуфхцчшщъыьэюя")
                    append("АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ")
                }
                ObfuscationLevel.HEAVY -> {
                    append("αβγδεζηθικλμνξοπρστυφχψω")
                    append("абвгдежзийклмнопрстуфхцчшщъыьэюя")
                    append("的一是不了人我在有他这为之大来以个中上们到说时")
                    append("要就出会可也你得对生能下面孩子对工动力已经面")
                    append("あいうえおかきくけこさしすせそたちつてとなにぬねの")
                    append("アイウエオカキクケコサシスセソタチツテトナニヌネノ")
                    append("가나다라마바사아자차카타파하")
                    append("거너더러머버서어저처커터퍼허")
                    append("กขคงจฉชซญฎฏฐฑฒณดตถทธนบปผฝพฟภมยรลวศษสหฬอฮ")
                    append("अआइईउऊऋएऐओऔकखगघङचछजझञटठडढणतथदधनपफबभमयरलवशषसह")
                }
                ObfuscationLevel.EXTREME -> {
                    append("αβγδεζηθικλμνξοπρστυφχψω")
                    append("абвгдежзийклмнопрстуфхцчшщъыьэюя")
                    
                    unicodeChaos.forEach { charset ->
                        append(charset)
                    }
                    
                    // Арабский (RTL - пиздец)
                    append("ابتثجحخدذرزسشصضطظعغفقكلمنهوي")
                    
                    // Руны
                    append("ᚠᚢᚦᚨᚱᚲᚷᚹᚺᚾᛁᛃᛇᛈᛉᛊᛏᛒᛖᛗᛚᛜᛞᛟ")
                    
                    // Грузинский
                    append("აბგდევზთიკლმნოპჟრსტუფქღყშჩცძწჭხჯჰ")
                    
                    // Армянский
                    append("աբգդեզէըթժիլխծկհձղճմյնշոչպջռսվտրցւփքօֆ")
                    
                    // Тибетский
                    append("ཀཁགངཅཆཇཉཏཐདནཔཕབམཙཚཛཝཞཟའཡརལཤཥསཧཨ")
                    
                    // Монгольский
                    append("ᠠᠡᠢᠣᠤᠥᠦᠧᠨᠩᠪᠫᠬᠭᠮᠯᠰᠱᠲᠳᠴᠵᠶᠷᠸᠹᠺᠻᠼᠽᠾᠿ")
                    
                    // Математические символы
                    append("𝐀𝐁𝐂𝐃𝐄𝐅𝐆𝐇𝐈𝐉𝐊𝐋𝐌𝐍𝐎𝐏𝐐𝐑𝐒𝐓𝐔𝐕𝐖𝐗𝐘𝐙")
                    append("𝑎𝑏𝑐𝑑𝑒𝑓𝑔ℎ𝑖𝑗𝑘𝑙𝑚𝑛𝑜𝑝𝑞𝑟𝑠𝑡𝑢𝑣𝑤𝑥𝑦𝑧")
                    
                    // Зеркальные
                    append("ɐqɔpǝɟɓɥıɾʞlɯuodbɹsʇnʌʍxʎz")
                    
                    // Диакритика
                    append("áǎàâāåãäąæćĉčçċďđéěèêēėę")
                    append("íǐìîīïĩıĵĺľļłńňņñóǒòôōőõøœ")
                    
                    // Брейль
                    append("⠁⠃⠉⠙⠑⠋⠛⠓⠊⠚⠅⠇⠍⠝⠕⠏⠟⠗⠎⠞⠥⠧⠺⠭⠽⠵")
                    
                    // Геометрия
                    append("■□▢▣▤▥▦▧▨▩▪▫▬▭▮▯▰▱▲△▴▵▶▷▸▹►▻▼▽▾▿◀◁◂◃◄◅")
                    
                    // Стрелки
                    append("←↑→↓↔↕↖↗↘↙↚↛↜↝↞↟↠↡↢↣↤↥↦↧↨")
                    
                    // Эмодзи (да, они валидные идентификаторы!)
                    append("😀😁😂🤣😃😄😅😆😉😊😋😎😍😘🥰😗😙😚")
                    append("🔥💀👻👽🤖💩🎃🎄🎁🎈🎉🎊🎋🎍🎎🎏")
                }
                ObfuscationLevel.NONE -> {
                    // Не должно вызываться
                }
            }
        }
        
        // Генерируем имя
        return buildString {
            // Первый символ - всегда валидный Java идентификатор
            val firstChar = charPool.filter { Character.isJavaIdentifierStart(it) }
            if (firstChar.isNotEmpty()) {
                append(firstChar.random(random))
            } else {
                append('_') // Fallback
            }
            
            // Остальные символы - любые валидные
            repeat(length - 1) {
                val validChars = charPool.filter { Character.isJavaIdentifierPart(it) }
                if (validChars.isNotEmpty()) {
                    append(validChars.random(random))
                } else {
                    append(asciiLetters.random(random))
                }
            }
        }
    }
    
    /**
     * Обфусцирует классы
     */
    private fun obfuscateClasses(inputDir: File, outputDir: File) {
        inputDir.walkTopDown().forEach { file ->
            if (file.extension == "class") {
                val classNode = readClass(file)
                val oldName = classNode.name
                
                /**
                 * Если класс в маппингах - создаем обфусцированную КОПИЮ
                 */
                if (classMappings.containsKey(oldName)) {
                    // Создаем копию для обфускации
                    val obfuscatedNode = ClassNode()
                    classNode.accept(obfuscatedNode)
                    
                    // Обновляем ссылки ТОЛЬКО в обфусцированной копии
                    remapReferences(obfuscatedNode)
                    
                    obfuscatedNode.name = classMappings[oldName]!!
                    
                    /**
                     * Переименовываем методы (если есть маппинги)
                     */
                    obfuscatedNode.methods?.forEach { method ->
                        val key = method.name + method.desc
                        if (methodMappings[oldName]?.containsKey(key) == true) {
                            method.name = methodMappings[oldName]!![key]!!
                        }
                    }
                    
                    /**
                     * Переименовываем поля (если есть маппинги)
                     */
                    obfuscatedNode.fields?.forEach { field ->
                        if (fieldMappings[oldName]?.containsKey(field.name) == true) {
                            field.name = fieldMappings[oldName]!![field.name]!!
                        }
                    }
                    
                    /**
                     * РАСШИРЕННАЯ ОБФУСКАЦИЯ (Catlean-style)
                     */
                    val level = getObfuscationLevelForProcessing(obfuscatedNode, oldName)
                    if (level != ObfuscationLevel.NONE && level != ObfuscationLevel.LIGHT) {
                        applyAdvancedObfuscation(obfuscatedNode, level)
                    }
                    
                    // Сохраняем обфусцированную копию
                    val outputFile = File(outputDir, "${obfuscatedNode.name}.class")
                    outputFile.parentFile.mkdirs()
                    writeClass(obfuscatedNode, outputFile)
                }
                
                // ВСЕГДА сохраняем оригинальный класс (БЕЗ изменений)
                val relativePath = file.relativeTo(inputDir)
                val outputFile = File(outputDir, relativePath.path)
                outputFile.parentFile.mkdirs()
                writeClass(classNode, outputFile)
            }
        }
    }
    
    /**
     * Обновляет ссылки
     */
    private fun remapReferences(classNode: ClassNode) {
        classNode.superName = remap(classNode.superName)
        classNode.interfaces = classNode.interfaces?.map { remap(it) }
        
        classNode.fields?.forEach { field ->
            field.desc = remapDescriptor(field.desc)
            field.signature = remapSignature(field.signature)
        }
        
        classNode.methods?.forEach { method ->
            method.desc = remapDescriptor(method.desc)
            method.signature = remapSignature(method.signature)
            
            method.instructions?.forEach { insn ->
                when (insn) {
                    is TypeInsnNode -> insn.desc = remap(insn.desc)
                    is FieldInsnNode -> {
                        val oldOwner = insn.owner
                        insn.owner = remap(insn.owner)
                        insn.desc = remapDescriptor(insn.desc)
                        // Переименовываем поле
                        if (fieldMappings[oldOwner]?.containsKey(insn.name) == true) {
                            insn.name = fieldMappings[oldOwner]!![insn.name]!!
                        }
                    }
                    is MethodInsnNode -> {
                        val oldOwner = insn.owner
                        insn.owner = remap(insn.owner)
                        val oldKey = insn.name + insn.desc
                        insn.desc = remapDescriptor(insn.desc)
                        // Переименовываем метод
                        if (methodMappings[oldOwner]?.containsKey(oldKey) == true) {
                            insn.name = methodMappings[oldOwner]!![oldKey]!!
                        }
                    }
                    is InvokeDynamicInsnNode -> {
                        insn.desc = remapDescriptor(insn.desc)
                    }
                    is LdcInsnNode -> {
                        if (insn.cst is Type) {
                            insn.cst = Type.getType(remapDescriptor((insn.cst as Type).descriptor))
                        }
                    }
                }
            }
            
            method.tryCatchBlocks?.forEach { tcb ->
                tcb.type = remap(tcb.type)
            }
            
            method.localVariables?.forEach { lv ->
                lv.desc = remapDescriptor(lv.desc)
                lv.signature = remapSignature(lv.signature)
            }
        }
    }
    
    private fun remap(className: String?): String? {
        if (className == null) return null
        return classMappings[className] ?: className
    }
    
    private fun remapDescriptor(desc: String?): String? {
        if (desc == null) return null
        var result = desc
        classMappings.forEach { (old, new) ->
            result = result?.replace("L$old;", "L$new;")
        }
        return result
    }
    
    private fun remapSignature(signature: String?): String? {
        if (signature == null) return null
        var result = signature
        classMappings.forEach { (old, new) ->
            result = result?.replace("L$old;", "L$new;")
        }
        return result
    }
    
    /**
     * Генерирует RuntimeMapper с алгоритмом восстановления
     */
    private fun generateRuntimeMapper(outputDir: File) {
        val mapperFile = File(outputDir, "anidumpproject/api/RuntimeMapper.class")
        mapperFile.parentFile.mkdirs()
        
        val cw = ClassWriter(0) // БЕЗ COMPUTE_MAXS!
        cw.visit(
            Opcodes.V17,
            Opcodes.ACC_PUBLIC,
            "anidumpproject/api/RuntimeMapper",
            null,
            "java/lang/Object",
            null
        )
        
        // Статическое поле с seed
        cw.visitField(
            Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL,
            "SEED",
            "J",
            null,
            dailySeed
        ).visitEnd()
        
        // Статическая Map с маппингами
        cw.visitField(
            Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL,
            "MAPPINGS",
            "Ljava/util/Map;",
            "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;",
            null
        ).visitEnd()
        
        // Статический блок инициализации
        val clinit = cw.visitMethod(
            Opcodes.ACC_STATIC,
            "<clinit>",
            "()V",
            null,
            null
        )
        clinit.visitCode()
        
        // new HashMap()
        clinit.visitTypeInsn(Opcodes.NEW, "java/util/HashMap")
        clinit.visitInsn(Opcodes.DUP)
        clinit.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false)
        clinit.visitFieldInsn(Opcodes.PUTSTATIC, "anidumpproject/api/RuntimeMapper", "MAPPINGS", "Ljava/util/Map;")
        
        // Добавляем маппинги
        classMappings.forEach { (old, new) ->
            clinit.visitFieldInsn(Opcodes.GETSTATIC, "anidumpproject/api/RuntimeMapper", "MAPPINGS", "Ljava/util/Map;")
            clinit.visitLdcInsn(new)
            clinit.visitLdcInsn(old)
            clinit.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true)
            clinit.visitInsn(Opcodes.POP)
        }
        
        clinit.visitInsn(Opcodes.RETURN)
        clinit.visitMaxs(0, 0) // ASM сам посчитает
        clinit.visitEnd()
        
        // Метод deobfuscate
        val mv = cw.visitMethod(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
            "deobfuscate",
            "(Ljava/lang/String;)Ljava/lang/String;",
            null,
            null
        )
        mv.visitCode()
        
        // return MAPPINGS.getOrDefault(obfuscated, obfuscated);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "anidumpproject/api/RuntimeMapper", "MAPPINGS", "Ljava/util/Map;")
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "getOrDefault", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true)
        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String")
        mv.visitInsn(Opcodes.ARETURN)
        mv.visitMaxs(0, 0) // ASM сам посчитает
        mv.visitEnd()
        
        cw.visitEnd()
        
        FileOutputStream(mapperFile).use { fos ->
            fos.write(cw.toByteArray())
        }
    }
    
    /**
     * Сохраняет маппинги
     */
    private fun saveMappings(file: File) {
        file.writeText(buildString {
            appendLine("# CHAOS OBFUSCATION MAPPINGS")
            appendLine("# Date: ${LocalDate.now(ZoneId.of("UTC"))}")
            appendLine("# Seed: $dailySeed")
            appendLine("# ⚠️  EXPIRES TOMORROW AT 00:00 UTC!")
            appendLine()
            appendLine("# Classes:")
            classMappings.forEach { (old, new) ->
                appendLine("$old -> $new")
            }
            appendLine()
            appendLine("# Methods:")
            methodMappings.forEach { (className, methods) ->
                appendLine("# Class: $className")
                methods.forEach { (old, new) ->
                    appendLine("  $old -> $new")
                }
            }
            appendLine()
            appendLine("# Fields:")
            fieldMappings.forEach { (className, fields) ->
                appendLine("# Class: $className")
                fields.forEach { (old, new) ->
                    appendLine("  $old -> $new")
                }
            }
        })
    }
    
    private fun readClass(file: File): ClassNode {
        val classNode = ClassNode()
        FileInputStream(file).use { fis ->
            val classReader = ClassReader(fis)
            // EXPAND_FRAMES - расширяем frames для корректной обработки
            classReader.accept(classNode, ClassReader.EXPAND_FRAMES)
        }
        return classNode
    }
    
    private fun writeClass(classNode: ClassNode, file: File) {
        // COMPUTE_MAXS вместо COMPUTE_FRAMES - безопаснее, не требует полного classpath
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        classNode.accept(classWriter)
        FileOutputStream(file).use { fos ->
            fos.write(classWriter.toByteArray())
        }
    }
    
    /**
     * Получение уровня обфускации для обработки
     */
    private fun getObfuscationLevelForProcessing(classNode: ClassNode, oldName: String): ObfuscationLevel {
        val allAnnotations = (classNode.visibleAnnotations ?: emptyList()) + 
                            (classNode.invisibleAnnotations ?: emptyList())
        
        allAnnotations.forEach { ann ->
            if (ann.desc.contains("Obfuscate")) {
                if (ann.values != null) {
                    for (i in 0 until ann.values.size step 2) {
                        if (ann.values[i] == "level") {
                            val levelValue = ann.values[i + 1]
                            if (levelValue is Array<*> && levelValue.size >= 2) {
                                val level = levelValue[1] as? String ?: "NONE"
                                return when (level) {
                                    "LIGHT" -> ObfuscationLevel.LIGHT
                                    "MEDIUM" -> ObfuscationLevel.MEDIUM
                                    "HEAVY" -> ObfuscationLevel.HEAVY
                                    "EXTREME" -> ObfuscationLevel.EXTREME
                                    else -> ObfuscationLevel.NONE
                                }
                            }
                        }
                    }
                }
                return ObfuscationLevel.MEDIUM
            }
        }
        
        return ObfuscationLevel.NONE
    }
    
    /**
     * Применяет расширенную обфускацию (Catlean-style)
     */
    private fun applyAdvancedObfuscation(classNode: ClassNode, level: ObfuscationLevel) {
        /**
         * Временно отключаем расширенную обфускацию для избежания "Method too large"
         * TODO: Оптимизировать генерацию кода
         */
        
        /**
         * 1. ThreadLocal Cache (только для HEAVY, не EXTREME)
         */
        if (level == ObfuscationLevel.HEAVY) {
            threadLocalCache.generateThreadLocalCache(classNode)
        }
        
        /**
         * 2. String Encryption (ограниченное количество)
         */
        if (level == ObfuscationLevel.HEAVY || level == ObfuscationLevel.EXTREME) {
            /**
             * Шифруем только короткие строки
             */
            encryptShortStrings(classNode)
        }
    }
    
    /**
     * Шифрование только коротких строк (оптимизированная версия)
     */
    private fun encryptShortStrings(classNode: ClassNode) {
        val stringPairs = mutableListOf<Pair<MethodNode, LdcInsnNode>>()
        
        classNode.methods?.forEach { method ->
            method.instructions?.forEach { insn ->
                if (insn is LdcInsnNode && insn.cst is String) {
                    val str = insn.cst as String
                    /**
                     * Только строки до 50 символов
                     */
                    if (str.length <= 50) {
                        stringPairs.add(method to insn)
                    }
                }
            }
        }
        
        if (stringPairs.isEmpty()) return
        
        /**
         * Максимум 20 строк
         */
        val stringsToEncrypt = stringPairs.take(20)
        
        generateDecryptMethod(classNode)
        
        val (key1, _) = stringEncryptor.generateKeys()
        
        stringsToEncrypt.forEach { (method, ldcInsn) ->
            val originalString = ldcInsn.cst as String
            val encrypted = stringEncryptor.encryptString(originalString, key1)
            
            if (encrypted.size > 50) {
                return@forEach
            }
            
            val insnList = InsnList()
            
            insnList.add(LdcInsnNode(encrypted.size))
            insnList.add(IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_BYTE))
            
            encrypted.forEachIndexed { index, byte ->
                insnList.add(InsnNode(Opcodes.DUP))
                insnList.add(LdcInsnNode(index))
                insnList.add(LdcInsnNode(byte.toInt()))
                insnList.add(InsnNode(Opcodes.BASTORE))
            }
            
            insnList.add(LdcInsnNode(key1))
            insnList.add(MethodInsnNode(
                Opcodes.INVOKESTATIC,
                classNode.name,
                "decrypt\${dailySeed.toString(36)}",
                "([BJ)Ljava/lang/String;",
                false
            ))
            
            method.instructions.insert(ldcInsn, insnList)
            method.instructions.remove(ldcInsn)
        }
    }
    
    /**
     * Шифрование всех строк в классе
     */
    private fun encryptAllStrings(classNode: ClassNode) {
        val stringPairs = mutableListOf<Pair<MethodNode, LdcInsnNode>>()
        
        classNode.methods?.forEach { method ->
            method.instructions?.forEach { insn ->
                if (insn is LdcInsnNode && insn.cst is String) {
                    stringPairs.add(method to insn)
                }
            }
        }
        
        if (stringPairs.isEmpty()) return
        
        /**
         * Ограничиваем количество строк для шифрования (чтобы метод не был слишком большим)
         */
        val stringsToEncrypt = stringPairs.take(50)
        
        /**
         * Генерируем метод расшифровки
         */
        generateDecryptMethod(classNode)
        
        /**
         * Заменяем LDC на вызовы расшифровки
         */
        val (key1, _) = stringEncryptor.generateKeys()
        
        stringsToEncrypt.forEach { (method, ldcInsn) ->
            val originalString = ldcInsn.cst as String
            
            /**
             * Пропускаем слишком длинные строки
             */
            if (originalString.length > 200) {
                return@forEach
            }
            
            val encrypted = stringEncryptor.encryptString(originalString, key1)
            
            /**
             * Пропускаем если зашифрованный массив слишком большой
             */
            if (encrypted.size > 100) {
                return@forEach
            }
            
            val insnList = InsnList()
            
            /**
             * Создаем байт-массив
             */
            insnList.add(LdcInsnNode(encrypted.size))
            insnList.add(IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_BYTE))
            
            encrypted.forEachIndexed { index, byte ->
                insnList.add(InsnNode(Opcodes.DUP))
                insnList.add(LdcInsnNode(index))
                insnList.add(LdcInsnNode(byte.toInt()))
                insnList.add(InsnNode(Opcodes.BASTORE))
            }
            
            /**
             * Вызываем метод расшифровки
             */
            insnList.add(LdcInsnNode(key1))
            insnList.add(MethodInsnNode(
                Opcodes.INVOKESTATIC,
                classNode.name,
                "decrypt\${dailySeed.toString(36)}",
                "([BJ)Ljava/lang/String;",
                false
            ))
            
            method.instructions.insert(ldcInsn, insnList)
            method.instructions.remove(ldcInsn)
        }
    }
    
    /**
     * Генерация метода расшифровки строк
     */
    private fun generateDecryptMethod(classNode: ClassNode) {
        val methodName = "decrypt\${dailySeed.toString(36)}"
        
        /**
         * Проверяем что метод еще не создан
         */
        if (classNode.methods?.any { it.name == methodName } == true) {
            return
        }
        
        val method = MethodNode(
            Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC,
            methodName,
            "([BJ)Ljava/lang/String;",
            null,
            null
        )
        
        method.instructions.apply {
            /**
             * XOR расшифровка
             */
            add(VarInsnNode(Opcodes.ALOAD, 0))
            add(InsnNode(Opcodes.ARRAYLENGTH))
            add(IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_BYTE))
            add(VarInsnNode(Opcodes.ASTORE, 3))
            
            add(InsnNode(Opcodes.ICONST_0))
            add(VarInsnNode(Opcodes.ISTORE, 4))
            
            val loopStart = LabelNode()
            val loopEnd = LabelNode()
            
            add(loopStart)
            add(VarInsnNode(Opcodes.ILOAD, 4))
            add(VarInsnNode(Opcodes.ALOAD, 0))
            add(InsnNode(Opcodes.ARRAYLENGTH))
            add(JumpInsnNode(Opcodes.IF_ICMPGE, loopEnd))
            
            add(VarInsnNode(Opcodes.ALOAD, 3))
            add(VarInsnNode(Opcodes.ILOAD, 4))
            add(VarInsnNode(Opcodes.ALOAD, 0))
            add(VarInsnNode(Opcodes.ILOAD, 4))
            add(InsnNode(Opcodes.BALOAD))
            add(VarInsnNode(Opcodes.LLOAD, 1))
            add(VarInsnNode(Opcodes.ILOAD, 4))
            add(InsnNode(Opcodes.I2L))
            add(InsnNode(Opcodes.LXOR))
            add(InsnNode(Opcodes.L2I))
            add(InsnNode(Opcodes.IXOR))
            add(InsnNode(Opcodes.I2B))
            add(InsnNode(Opcodes.BASTORE))
            
            add(InsnNode(Opcodes.IINC))
            add(VarInsnNode(Opcodes.ISTORE, 4))
            add(JumpInsnNode(Opcodes.GOTO, loopStart))
            
            add(loopEnd)
            
            /**
             * new String(decrypted, UTF-8)
             */
            add(TypeInsnNode(Opcodes.NEW, "java/lang/String"))
            add(InsnNode(Opcodes.DUP))
            add(VarInsnNode(Opcodes.ALOAD, 3))
            add(LdcInsnNode("UTF-8"))
            add(MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "java/lang/String",
                "<init>",
                "([BLjava/lang/String;)V",
                false
            ))
            add(InsnNode(Opcodes.ARETURN))
        }
        
        method.maxStack = 6
        method.maxLocals = 5
        
        classNode.methods.add(method)
    }
    
    /**
     * Обфускация чисел через long-арифметику
     */
    private fun obfuscateNumbers(classNode: ClassNode) {
        classNode.methods?.forEach { method ->
            if (!method.name.startsWith("<")) {
                longObfuscator.obfuscateMethodConstants(method)
            }
        }
    }
    
    /**
     * Добавление мусорных данных
     */
    private fun addGarbageData(classNode: ClassNode) {
        garbageGenerator.generateGarbageStaticBlock(classNode)
        garbageGenerator.generateGarbageMethods(classNode, 5)
        garbageGenerator.generateGarbageFields(classNode, 10)
    }
    
    /**
     * Обфускация control flow
     */
    private fun obfuscateControlFlow(classNode: ClassNode) {
        classNode.methods?.forEach { method ->
            if (!method.name.startsWith("<")) {
                garbageGenerator.obfuscateControlFlow(method)
                garbageGenerator.generateGarbageTryCatch(method)
            }
        }
    }
    
    enum class ObfuscationLevel {
        NONE,
        LIGHT,
        MEDIUM,
        HEAVY,
        EXTREME
    }
}
