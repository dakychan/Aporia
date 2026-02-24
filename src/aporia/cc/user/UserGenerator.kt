package aporia.cc.user

import org.lwjgl.system.Platform
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.MessageDigest
import java.util.*
import java.io.File
import kotlin.random.Random as KtRandom

object UserGenerator {
    
    fun generateRandomUsername(): String {
        val prefixes = listOf(
            "ProstoFilya", "User", "Player", "Gamer", "Kot", "Cat",
            "Creeper", "Steve", "Alex", "Herobrine", "Notch",
            "Dragon", "Zombie", "Skeleton", "Enderman",
            "Pivnoy_Gvadelon", "Sosiska_Killer", "Tapok_V_Kedah", "Koshmar_V_Truzah",
            "Gleb_Glebich", "Kashka_S_Maslom", "Kirpich_99", "Oleg_V_Poryadke", "Nosok_Sudby",
            "Pelmen_Bez_Tarelki", "Chainik_V_Ogne", "Ogurets_Molodets", "Baton_Khleba", "Dyrka_V_Zabore",
            "Tsar_Dvorov", "Shkaf_Iz_Ikei", "Lampa_Gennadiy", "Varenik_Nindzya", "Zabytiy_Parol", "Taburetka_Smerti",
            "Kofe_Bez_Sahara", "Vatnaya_Palochka", "Kotletka_S_Pure", "Zheleznaya_Logika", "Sinniy_Traktor",
            "Bublik_V_Kosmose", "Myshka_Sosiska", "Dver_V_Narnia", "Kvashenaya_Kapusta", "Tualetniy_Utenok", "Siniy_Ekran",
            "Error_404_Found", "Zhmot_Vasilich", "Nochnoy_Doed", "Shlep_Shlep", "Golub_Gennadiy", "Vkusniy_Kley",
            "Kaktus_Valera", "Pylniy_Ventilyator", "Siniy_Pelmen", "Morkovniy_Nindzya", "Shokoladniy_Zayats", "Finalniy_Boss",
            "Krot_V_Palto", "Ezhik_V_Tumane", "Seryy_Gusin", "Chelovek_Pauk_007", "Karton_2024", "Gribnoy_Dozhd", "Krasiviy_Kaktus",
            "Stariy_Botinok", "Gromkiy_Shypot", "Bananoviy_Korol", "Zloy_Prizrak", "Mokraya_Voda", "Sukhoy_Led", "Rybnaya_Golova",
            "Velikiy_Pofigist", "Tykva_V_Shlyape", "Kozhura_Ot_Banana", "Zubnaya_Pasta", "Chemodan_Bez_Ruchki", "Tantsuyushiy_Utug",
            "Stariy_Povar", "Chernaya_Dyrka", "Umnaya_Mebel", "Soleniy_Arbuz", "Siniy_Iny", "Pustoy_Koshelek", "Dlinniy_Shnurok", "Zloveshy_Tapok", "Morkovniy_Sok", "Lisiy_Khvost", "Ogromniy_Mikrob", "Smeshniy_Sosed", "Tikhaya_Sova",
            "Yarkiy_Fonarik", "Zolotaya_Ryba", "Kremoviy_Tort", "Zeleniy_Chay", "Belyy_List", "Serebryaniy_Serniy", "Kosmicheskiy_Kot",
            "Kruglyy_Stol", "Sladkiy_Perets", "Bystraya_Cherepaha", "Zheleznaya_Dver", "Grib_Borovik", "Sinii_Kit", "Pustaya_Banka", "Goryachiy_Sneg",
            "Kvadratniy_Krug", "Vash_Menedzher", "Agent_000", "Borsch_V_Kruzhke", "Chay_S_Saharnim", "Domasniy_Tapok", "Zloy_Adminko", "Malenkaya_Loshadka",
            "Pustoy_Bakal"
        )
        val suffix = KtRandom.nextInt(100, 9999)
        return "${prefixes.random()}_$suffix"
    }

    fun generateRandomUUID(): String {
        val random = KtRandom
        return String.format(
            "%08x-%04x-%04x-%04x-%012x",
            random.nextInt(),
            random.nextInt(0, 0xffff),
            random.nextInt(0, 0xffff) or 0x4000,
            random.nextInt(0, 0x3fff) or 0x8000,
            random.nextLong() and 0xffffffffffff
        )
    }
    
    fun generateOfflineUUID(username: String): String {
        val bytes = "OfflinePlayer:$username".toByteArray()
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        
        digest[6] = (digest[6].toInt() and 0x0f or 0x30).toByte()
        digest[8] = (digest[8].toInt() and 0x3f or 0x80).toByte()
        
        return String.format(
            "%08x-%04x-%04x-%04x-%012x",
            (0..3).map { digest[it].toInt() and 0xff }.reduce { acc, i -> (acc shl 8) or i },
            (4..5).map { digest[it].toInt() and 0xff }.reduce { acc, i -> (acc shl 8) or i },
            (6..7).map { digest[it].toInt() and 0xff }.reduce { acc, i -> (acc shl 8) or i },
            (8..9).map { digest[it].toInt() and 0xff }.reduce { acc, i -> (acc shl 8) or i },
            (10..15).map { digest[it].toLong() and 0xff }.reduce { acc, i -> (acc shl 8) or i }
        )
    }
    
    fun generateNumericUUID(username: String): String {
        val normalized = normalizeUsername(username)
        return normalized.map { char ->
            when {
                char in 'a'..'z' -> char - 'a' + 1
                char in 'а'..'я' -> char - 'а' + 1
                char.isDigit() -> char.digitToInt() + 27
                else -> 0
            }
        }.joinToString("")
    }
    
    fun generateCompressedNumericUUID(username: String): String {
        val normalized = normalizeUsername(username)
        return normalized.map { char ->
            val pos = when {
                char in 'a'..'z' -> char - 'a' + 1
                char in 'а'..'я' -> char - 'а' + 1
                char.isDigit() -> char.digitToInt() + 27
                else -> 0
            }
            (pos % 10).toString()
        }.joinToString("")
    }
    
    private fun normalizeUsername(username: String): String {
        return username.lowercase()
            .filter { it.isLetterOrDigit() }
            .let { if (it.isEmpty()) "user" else it }
    }

    fun generateHardwareId(): String {
        val input = System.getProperty("user.name") +
                    System.getProperty("os.name") +
                    System.getenv("PROCESSOR_IDENTIFIER") +
                    Runtime.getRuntime().availableProcessors()
        return generateUUIDFromString(input)
    }
    
    fun generateSystemHardwareId(): String? {
        return try {
            val runtime = Runtime.getRuntime()
            val process = when {
                Platform.get() == Platform.WINDOWS -> runtime.exec("wmic csproduct get uuid")
                Platform.get() == Platform.LINUX -> runtime.exec("cat /etc/machine-id")
                Platform.get() == Platform.MACOSX -> runtime.exec("ioreg -rd1 -c IOPlatformExpertDevice")
                else -> null
            }
            
            process?.let {
                BufferedReader(InputStreamReader(it.inputStream)).use { reader ->
                    reader.lines().skip(1).findFirst().orElse("")?.trim()
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun generateUUIDFromString(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        
        return String.format(
            "%08x-%04x-%04x-%04x-%012x",
            (0..3).map { digest[it].toInt() and 0xff }.reduce { acc, i -> (acc shl 8) or i },
            (4..5).map { digest[it].toInt() and 0xff }.reduce { acc, i -> (acc shl 8) or i },
            (6..7).map { digest[it].toInt() and 0xff }.reduce { acc, i -> (acc shl 8) or i },
            (8..9).map { digest[it].toInt() and 0xff }.reduce { acc, i -> (acc shl 8) or i },
            (10..15).map { digest[it].toLong() and 0xff }.reduce { acc, i -> (acc shl 8) or i }
        )
    }
}

enum class UserRole {
    USER,
    DEVELOPER,
    ADMIN,
    CONTRIBUTOR
}