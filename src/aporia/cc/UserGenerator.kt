package aporia.cc

import com.chaos.annotation.Obfuscate
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.MessageDigest
import java.util.Random

@Obfuscate
object UserGenerator {

    private val PREFIXES = listOf(
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
        "Stariy_Povar", "Chernaya_Dyrka", "Umnaya_Mebel", "Soleniy_Arbuz", "Siniy_Iny", "Pustoy_Koshelek", "Dlinniy_Shnurok",
        "Zloveshy_Tapok", "Morkovniy_Sok", "Lisiy_Khvost", "Ogromniy_Mikrob", "Smeshniy_Sosed", "Tikhaya_Sova",
        "Yarkiy_Fonarik", "Zolotaya_Ryba", "Kremoviy_Tort", "Zeleniy_Chay", "Belyy_List", "Serebryaniy_Serniy", "Kosmicheskiy_Kot",
        "Kruglyy_Stol", "Sladkiy_Perets", "Bystraya_Cherepaha", "Zheleznaya_Dver", "Grib_Borovik", "Sinii_Kit", "Pustaya_Banka", "Goryachiy_Sneg",
        "Kvadratniy_Krug", "Vash_Menedzher", "Agent_000", "Borsch_V_Kruzhke", "Chay_S_Saharnim", "Domasniy_Tapok", "Zloy_Adminko", "Malenkaya_Loshadka",
        "Pustoy_Bakal"
    )

    private val RANDOM = Random()

    @JvmStatic
    fun generateRandomUsername(): String {
        val suffix = RANDOM.nextInt(100, 9999)
        return "${PREFIXES[RANDOM.nextInt(PREFIXES.size)]}_$suffix"
    }

    @JvmStatic
    fun generateRandomUUID(): String {
        return String.format(
            "%08x-%04x-%04x-%04x-%012x",
            RANDOM.nextInt(),
            RANDOM.nextInt(0xffff),
            RANDOM.nextInt(0xffff) or 0x4000,
            RANDOM.nextInt(0x3fff) or 0x8000,
            RANDOM.nextLong() and 0xffffffffffffL
        )
    }

    @JvmStatic
    fun generateOfflineUUID(username: String): String {
        return try {
            val bytes = "OfflinePlayer:$username".toByteArray()
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(bytes)

            digest[6] = (digest[6].toInt() and 0x0f or 0x30).toByte()
            digest[8] = (digest[8].toInt() and 0x3f or 0x80).toByte()

            String.format(
                "%08x-%04x-%04x-%04x-%012x",
                ((digest[0].toInt() and 0xff) shl 24) or ((digest[1].toInt() and 0xff) shl 16) or ((digest[2].toInt() and 0xff) shl 8) or (digest[3].toInt() and 0xff),
                ((digest[4].toInt() and 0xff) shl 8) or (digest[5].toInt() and 0xff),
                ((digest[6].toInt() and 0xff) shl 8) or (digest[7].toInt() and 0xff),
                ((digest[8].toInt() and 0xff) shl 8) or (digest[9].toInt() and 0xff),
                ((digest[10].toLong() and 0xffL) shl 40) or ((digest[11].toLong() and 0xffL) shl 32) or ((digest[12].toLong() and 0xffL) shl 24) or
                ((digest[13].toLong() and 0xffL) shl 16) or ((digest[14].toLong() and 0xffL) shl 8) or (digest[15].toLong() and 0xffL)
            )
        } catch (e: Exception) {
            generateRandomUUID()
        }
    }

    @JvmStatic
    fun generateNumericUUID(username: String): String {
        val normalized = normalizeUsername(username)
        val result = StringBuilder()

        for (c in normalized.toCharArray()) {
            val value = when {
                c in 'a'..'z' -> c - 'a' + 1
                c in 'а'..'я' -> c - 'а' + 1
                c.isDigit() -> c.digitToInt() + 27
                else -> 0
            }
            result.append(value)
        }

        return result.toString()
    }

    @JvmStatic
    fun generateCompressedNumericUUID(username: String): String {
        val normalized = normalizeUsername(username)
        val result = StringBuilder()

        for (c in normalized.toCharArray()) {
            val pos = when {
                c in 'a'..'z' -> c - 'a' + 1
                c in 'а'..'я' -> c - 'а' + 1
                c.isDigit() -> c.digitToInt() + 27
                else -> 0
            }
            result.append(pos % 10)
        }

        return result.toString()
    }

    private fun normalizeUsername(username: String): String {
        val normalized = username.lowercase()
            .filter { it.isLetterOrDigit() }
        return normalized.ifEmpty { "user" }
    }

    @JvmStatic
    fun generateHardwareId(): String {
        val input = System.getProperty("user.name") +
                OsManager.osName +
                System.getenv("PROCESSOR_IDENTIFIER") +
                Runtime.getRuntime().availableProcessors()
        return generateUUIDFromString(input)
    }

    @JvmStatic
    fun generateSystemHardwareId(): String? {
        return try {
            val runtime = Runtime.getRuntime()
            val os = OsManager.osName.lowercase()
            val process = when {
                os.contains("win") -> runtime.exec("wmic csproduct get uuid")
                os.contains("linux") -> runtime.exec("cat /etc/machine-id")
                os.contains("mac") -> runtime.exec("ioreg -rd1 -c IOPlatformExpertDevice")
                else -> null
            }

            process?.let {
                BufferedReader(InputStreamReader(it.inputStream)).use { reader ->
                    if (os.contains("win")) {
                        reader.readLine()
                        reader.readLine()?.trim()
                    } else {
                        reader.readLine()?.trim()
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun generateUUIDFromString(input: String): String {
        return try {
            val bytes = input.toByteArray()
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(bytes)

            String.format(
                "%08x-%04x-%04x-%04x-%012x",
                ((digest[0].toInt() and 0xff) shl 24) or ((digest[1].toInt() and 0xff) shl 16) or ((digest[2].toInt() and 0xff) shl 8) or (digest[3].toInt() and 0xff),
                ((digest[4].toInt() and 0xff) shl 8) or (digest[5].toInt() and 0xff),
                ((digest[6].toInt() and 0xff) shl 8) or (digest[7].toInt() and 0xff),
                ((digest[8].toInt() and 0xff) shl 8) or (digest[9].toInt() and 0xff),
                ((digest[10].toLong() and 0xffL) shl 40) or ((digest[11].toLong() and 0xffL) shl 32) or ((digest[12].toLong() and 0xffL) shl 24) or
                ((digest[13].toLong() and 0xffL) shl 16) or ((digest[14].toLong() and 0xffL) shl 8) or (digest[15].toLong() and 0xffL)
            )
        } catch (e: Exception) {
            generateRandomUUID()
        }
    }
}
