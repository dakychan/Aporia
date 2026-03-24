package so.aporia.utils.user.command.impl;

import aporia.cc.PanicSystem;
import so.aporia.utils.user.command.Command;

public class PanicCommand implements Command {

    @Override
    public String name() { return "panic"; }

    @Override
    public String description() { return "Hides all client features until you type your username"; }

    @Override
    public void execute(String[] args) {
        PanicSystem.INSTANCE.panic();
    }
}
