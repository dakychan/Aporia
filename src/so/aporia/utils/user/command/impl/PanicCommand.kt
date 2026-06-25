package so.aporia.utils.user.command.impl

import aporia.cc.PanicSystem
import com.chaos.annotation.Obfuscate
import so.aporia.utils.user.command.Command

@Obfuscate
class PanicCommand : Command {
    override fun name() = "panic"
    override fun description() = "Hides all client features until you type your username"
    override fun execute(args: Array<String>) {
        PanicSystem.INSTANCE.panic()
    }
}
