/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.command.impl;

import so.aporia.utils.user.command.Command;
import so.aporia.utils.user.command.CommandManager;

public class PrefixCommand implements Command {

    @Override
    public String name() { return "prefix"; }

    @Override
    public String description() { return "Changes the command prefix. Usage: prefix <new>"; }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            CommandManager.chat("§eCurrent prefix: §f" + CommandManager.PREFIX);
            return;
        }
        CommandManager.PREFIX = args[1];
        CommandManager.chat("§aPrefix changed to: §f" + CommandManager.PREFIX);
    }
}
