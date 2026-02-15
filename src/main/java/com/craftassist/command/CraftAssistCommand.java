package com.craftassist.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;

public class CraftAssistCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("ca")
                    .requires(source -> source.permissions().hasPermission(
                            net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.literal("build")
                            .then(Commands.argument("description", StringArgumentType.greedyString())
                                    .executes(BuildCommand::execute)))
                    .then(Commands.literal("undo")
                            .executes(UndoCommand::execute)));
        });
    }
}
