package craftassist.command;

import craftassist.config.ConfigManager;
import craftassist.util.MessageUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public class ReloadCommand {

    public static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ConfigManager.load();

        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            MessageUtil.sendSuccess(player, "配置已重新載入");
        }

        return Command.SINGLE_SUCCESS;
    }
}
