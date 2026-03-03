package aporia.cc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class UserGenerator {
    
    private static final List<String> PREFIXES = Arrays.asList(
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
    );

    private static final Random RANDOM = new Random();

    public static String generateRandomUsername() {
        int suffix = RANDOM.nextInt(100, 9999);
        return PREFIXES.get(RANDOM.nextInt(PREFIXES.size())) + "_" + suffix;
    }

    public static String generateRandomUUID() {
        return String.format(
            "%08x-%04x-%04x-%04x-%012x",
            RANDOM.nextInt(),
            RANDOM.nextInt(0xffff),
            RANDOM.nextInt(0xffff) | 0x4000,
            RANDOM.nextInt(0x3fff) | 0x8000,
            RANDOM.nextLong() & 0xffffffffffffL
        );
    }
    
    public static String generateOfflineUUID(String username) {
        try {
            byte[] bytes = ("OfflinePlayer:" + username).getBytes();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            
            digest[6] = (byte) (digest[6] & 0x0f | 0x30);
            digest[8] = (byte) (digest[8] & 0x3f | 0x80);
            
            return String.format(
                "%08x-%04x-%04x-%04x-%012x",
                ((digest[0] & 0xff) << 24) | ((digest[1] & 0xff) << 16) | ((digest[2] & 0xff) << 8) | (digest[3] & 0xff),
                ((digest[4] & 0xff) << 8) | (digest[5] & 0xff),
                ((digest[6] & 0xff) << 8) | (digest[7] & 0xff),
                ((digest[8] & 0xff) << 8) | (digest[9] & 0xff),
                ((digest[10] & 0xffL) << 40) | ((digest[11] & 0xffL) << 32) | ((digest[12] & 0xffL) << 24) | 
                ((digest[13] & 0xffL) << 16) | ((digest[14] & 0xffL) << 8) | (digest[15] & 0xffL)
            );
        } catch (Exception e) {
            return generateRandomUUID();
        }
    }
    
    public static String generateNumericUUID(String username) {
        String normalized = normalizeUsername(username);
        StringBuilder result = new StringBuilder();
        
        for (char c : normalized.toCharArray()) {
            int value;
            if (c >= 'a' && c <= 'z') {
                value = c - 'a' + 1;
            } else if (c >= 'а' && c <= 'я') {
                value = c - 'а' + 1;
            } else if (Character.isDigit(c)) {
                value = Character.getNumericValue(c) + 27;
            } else {
                value = 0;
            }
            result.append(value);
        }
        
        return result.toString();
    }
    
    public static String generateCompressedNumericUUID(String username) {
        String normalized = normalizeUsername(username);
        StringBuilder result = new StringBuilder();
        
        for (char c : normalized.toCharArray()) {
            int pos;
            if (c >= 'a' && c <= 'z') {
                pos = c - 'a' + 1;
            } else if (c >= 'а' && c <= 'я') {
                pos = c - 'а' + 1;
            } else if (Character.isDigit(c)) {
                pos = Character.getNumericValue(c) + 27;
            } else {
                pos = 0;
            }
            result.append(pos % 10);
        }
        
        return result.toString();
    }
    
    private static String normalizeUsername(String username) {
        String normalized = username.toLowerCase()
            .chars()
            .filter(Character::isLetterOrDigit)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
        
        return normalized.isEmpty() ? "user" : normalized;
    }

    public static String generateHardwareId() {
        String input = System.getProperty("user.name") +
                       System.getProperty("os.name") +
                       System.getenv("PROCESSOR_IDENTIFIER") +
                       Runtime.getRuntime().availableProcessors();
        return generateUUIDFromString(input);
    }
    
    public static String generateSystemHardwareId() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = null;
            
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                process = runtime.exec("wmic csproduct get uuid");
            } else if (os.contains("linux")) {
                process = runtime.exec("cat /etc/machine-id");
            } else if (os.contains("mac")) {
                process = runtime.exec("ioreg -rd1 -c IOPlatformExpertDevice");
            }
            
            if (process != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    if (os.contains("win")) {
                        // Skip header line for Windows
                        reader.readLine();
                        String line = reader.readLine();
                        return line != null ? line.trim() : null;
                    } else {
                        String line = reader.readLine();
                        return line != null ? line.trim() : null;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    private static String generateUUIDFromString(String input) {
        try {
            byte[] bytes = input.getBytes();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            
            return String.format(
                "%08x-%04x-%04x-%04x-%012x",
                ((digest[0] & 0xff) << 24) | ((digest[1] & 0xff) << 16) | ((digest[2] & 0xff) << 8) | (digest[3] & 0xff),
                ((digest[4] & 0xff) << 8) | (digest[5] & 0xff),
                ((digest[6] & 0xff) << 8) | (digest[7] & 0xff),
                ((digest[8] & 0xff) << 8) | (digest[9] & 0xff),
                ((digest[10] & 0xffL) << 40) | ((digest[11] & 0xffL) << 32) | ((digest[12] & 0xffL) << 24) | 
                ((digest[13] & 0xffL) << 16) | ((digest[14] & 0xffL) << 8) | (digest[15] & 0xffL)
            );
        } catch (Exception e) {
            return generateRandomUUID();
        }
    }
}