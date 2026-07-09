package so.aporia.utils.user.input

import com.chaos.annotation.Obfuscate
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
object KeyboardLayout {
    private const val EN = "qwertyuiop[]asdfghjkl;'zxcvbnm,." +
                           "QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>"
    private const val RU = "йцукенгшщзхъфывапролджэячсмитьбю" +
                           "ЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ"

    @JvmStatic
    fun convert(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            val idx = RU.indexOf(c)
            if (idx >= 0) { sb.append(EN[idx]); continue }
            val idx2 = EN.indexOf(c)
            if (idx2 >= 0) { sb.append(RU[idx2]); continue }
            sb.append(c)
        }
        return sb.toString()
    }

    @JvmStatic
    fun both(query: String): Array<String> = arrayOf(query, convert(query))
}
