package com.craftassist.builder;

import com.craftassist.CraftAssistMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

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

        Block block = BuiltInRegistries.BLOCK.getValue(id);
        if (block == Blocks.AIR) {
            return null;
        }

        return block;
    }
}
