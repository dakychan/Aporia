/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

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
