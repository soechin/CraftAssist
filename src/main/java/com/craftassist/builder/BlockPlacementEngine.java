package com.craftassist.builder;

import com.craftassist.CraftAssistMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

public class BlockPlacementEngine {

    /**
     * 預計算建築結構的所有方塊放置清單（不實際放置）。
     * 供 BatchPlacementManager 分批放置使用。
     */
    public static List<BatchPlacementManager.BlockPlacement> preparePlacements(
            BlockPos origin, BuildStructure structure) {

        List<BatchPlacementManager.BlockPlacement> placements = new ArrayList<>();

        if (structure.getRegions() == null || structure.getRegions().isEmpty()) {
            return placements;
        }

        for (BuildStructure.BlockRegion region : structure.getRegions()) {
            Block block = BlockValidator.validate(region.getBlock());
            if (block == null) {
                continue;
            }

            BlockState state = block.defaultBlockState();
            state = applyFacing(state, region.getFacing());

            collectRegionPlacements(origin, region, state, placements);
        }

        return placements;
    }

    private static void collectRegionPlacements(BlockPos origin, BuildStructure.BlockRegion region,
                                                 BlockState state,
                                                 List<BatchPlacementManager.BlockPlacement> output) {
        int[] from = region.getFrom();
        int[] to = region.getTo();
        if (from == null || to == null || from.length != 3 || to.length != 3) {
            return;
        }

        int minX = Math.min(from[0], to[0]);
        int minY = Math.min(from[1], to[1]);
        int minZ = Math.min(from[2], to[2]);
        int maxX = Math.max(from[0], to[0]);
        int maxY = Math.max(from[1], to[1]);
        int maxZ = Math.max(from[2], to[2]);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (region.isHollow() && !isOuter(x, y, z, minX, minY, minZ, maxX, maxY, maxZ)) {
                        continue;
                    }
                    BlockPos pos = origin.offset(x, y, z);
                    output.add(new BatchPlacementManager.BlockPlacement(pos, state));
                }
            }
        }
    }

    private static boolean isOuter(int x, int y, int z,
                                   int minX, int minY, int minZ,
                                   int maxX, int maxY, int maxZ) {
        return x == minX || x == maxX ||
               y == minY || y == maxY ||
               z == minZ || z == maxZ;
    }

    private static BlockState applyFacing(BlockState state, String facingStr) {
        if (facingStr == null || facingStr.isEmpty()) {
            return state;
        }

        try {
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                Direction dir = switch (facingStr.toLowerCase()) {
                    case "north" -> Direction.NORTH;
                    case "south" -> Direction.SOUTH;
                    case "east" -> Direction.EAST;
                    case "west" -> Direction.WEST;
                    default -> Direction.NORTH;
                };
                return state.setValue(BlockStateProperties.HORIZONTAL_FACING, dir);
            }
        } catch (Exception e) {
            CraftAssistMod.LOGGER.warn("[CraftAssist] 無法設定方塊朝向: {}", facingStr);
        }

        return state;
    }
}
