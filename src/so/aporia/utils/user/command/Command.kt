package so.aporia.utils.user.command
import com.chaos.annotation.Obfuscate
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
interface Command {
    fun name(): String
    fun description(): String
    fun execute(args: Array<String>)
}