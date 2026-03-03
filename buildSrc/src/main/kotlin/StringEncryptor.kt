import org.objectweb.asm.tree.*
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import javax.crypto.spec.IvParameterSpec
import kotlin.random.Random

/**
 * String Encryption - DES + XOR шифрование строк
 * 
 * Превращает все строковые константы в зашифрованные байт-массивы.
 * Расшифровка происходит в runtime через MethodHandle.
 */
class StringEncryptor(private val seed: Long) {
    
    /**
     * Генерация ключей на основе seed
     */
    fun generateKeys(): Pair<Long, Long> {
        val key1 = seed xor 0xDEADBEEF
        val key2 = seed xor 0xCAFEBABE
        return key1 to key2
    }
    
    /**
     * Шифрование строки в байт-массив через DES
     */
    fun encryptString(text: String, key: Long): ByteArray {
        try {
            val keyBytes = longToBytes(key).copyOf(8)
            val cipher = Cipher.getInstance("DES/CBC/PKCS5Padding")
            val desKey = SecretKeyFactory.getInstance("DES")
                .generateSecret(DESKeySpec(keyBytes))
            val iv = IvParameterSpec(ByteArray(8))
            cipher.init(Cipher.ENCRYPT_MODE, desKey, iv)
            return cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            return xorEncrypt(text, key)
        }
    }
    
    /**
     * Fallback XOR шифрование если DES не работает
     */
    private fun xorEncrypt(text: String, key: Long): ByteArray {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val keyBytes = longToBytes(key)
        return bytes.mapIndexed { i, b ->
            (b.toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }.toByteArray()
    }
    
    /**
     * Генерация мусорных строк (как в j0)
     */
    fun generateGarbageStrings(count: Int): List<ByteArray> {
        val garbage = mutableListOf<ByteArray>()
        val random = Random(seed)
        repeat(count) {
            val length = random.nextInt(100, 200)
            val bytes = ByteArray(length) { random.nextInt(-128, 128).toByte() }
            garbage.add(bytes)
        }
        return garbage
    }
    
    /**
     * Генерация long-массива с зашифрованными строками
     */
    fun generateEncryptedArray(strings: List<String>): List<ByteArray> {
        val (key1, key2) = generateKeys()
        return strings.mapIndexed { index, str ->
            val key = if (index % 2 == 0) key1 else key2
            encryptString(str, key)
        }
    }
    
    /**
     * Поиск всех строк в классе
     */
    fun findAllStrings(classNode: ClassNode): List<Pair<MethodNode, LdcInsnNode>> {
        val strings = mutableListOf<Pair<MethodNode, LdcInsnNode>>()
        classNode.methods?.forEach { method ->
            method.instructions?.forEach { insn ->
                if (insn is LdcInsnNode && insn.cst is String) {
                    strings.add(method to insn)
                }
            }
        }
        return strings
    }
    
    private fun longToBytes(value: Long): ByteArray {
        return ByteArray(8) { i ->
            (value shr (56 - i * 8)).toByte()
        }
    }
}
