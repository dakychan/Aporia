/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.command;

/**
 * Base interface for all Aporia client-side commands.
 */
public interface Command {

    /**
     * @return primary command name, e.g. {@code "help"}
     */
    String name();

    /**
     * @return short description shown in {@code .help}
     */
    String description();

    /**
     * Executes the command.
     *
     * @param args tokens split by space; {@code args[0]} is the command name
     */
    void execute(String[] args);
}
