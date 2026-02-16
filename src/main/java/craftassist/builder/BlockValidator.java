package craftassist.builder;

import craftassist.CraftAssistMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public class BlockValidator {

    public static Block validate(String blockId) {
        if (blockId == null || blockId.isEmpty()) {
            return null;
        }

        Identifier id = Identifier.tryParse(blockId);
        if (id == null) {
            CraftAssistMod.LOGGER.warn("[CraftAssist] 無效的方塊 ID: {}", blockId);
            return null;
        }

        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            CraftAssistMod.LOGGER.warn("[CraftAssist] 未知的方塊: {}", blockId);
            return null;
        }

        return BuiltInRegistries.BLOCK.getValue(id);
    }
}
