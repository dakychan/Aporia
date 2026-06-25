package so.aporia.utils.user.command

import com.chaos.annotation.Obfuscate

@Obfuscate
interface Command {
    fun name(): String
    fun description(): String
    fun execute(args: Array<String>)
}
