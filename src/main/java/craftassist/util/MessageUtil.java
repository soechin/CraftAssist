package craftassist.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class MessageUtil {

    private static final String PREFIX = "[CraftAssist] ";

    public static void sendProgress(ServerPlayer player, String message) {
        player.sendSystemMessage(
                Component.literal(PREFIX + message).withStyle(ChatFormatting.YELLOW));
    }

    public static void sendSuccess(ServerPlayer player, String message) {
        player.sendSystemMessage(
                Component.literal(PREFIX + message).withStyle(ChatFormatting.GREEN));
    }

    public static void sendError(ServerPlayer player, String message) {
        player.sendSystemMessage(
                Component.literal(PREFIX + message).withStyle(ChatFormatting.RED));
    }
}
