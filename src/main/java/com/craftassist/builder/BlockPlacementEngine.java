package com.craftassist.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BlockPlacementEngine {

    /**
     * 預計算建築結構的所有方塊放置清單（不實際放置）。
     * 處理順序：先 regions（主結構），再 blocks（裝飾細節）。
     */
    public static List<BatchPlacementManager.BlockPlacement> preparePlacements(
            BlockPos origin, BuildStructure structure) {

        List<BatchPlacementManager.BlockPlacement> placements = new ArrayList<>();

        // 1. 處理 regions（主結構）
        if (structure.getRegions() != null) {
            for (BuildStructure.BlockRegion region : structure.getRegions()) {
                Block block = BlockValidator.validate(region.getBlock());
                if (block == null) {
                    continue;
                }

                BlockState state = block.defaultBlockState();
                state = PropertyApplier.applyFacing(state, region.getFacing());

                collectRegionPlacements(origin, region, state, placements);
            }
        }

        // 2. 處理 individual blocks（裝飾細節）
        if (structure.getBlocks() != null) {
            for (BuildStructure.IndividualBlock individual : structure.getBlocks()) {
                Block block = BlockValidator.validate(individual.getBlock());
                if (block == null) {
                    continue;
                }

                BlockState state = block.defaultBlockState();
                state = PropertyApplier.applyProperties(state, individual.getProperties());

                int[] pos = individual.getPos();
                if (pos != null && pos.length == 3) {
                    BlockPos worldPos = origin.offset(pos[0], pos[1], pos[2]);
                    placements.add(new BatchPlacementManager.BlockPlacement(worldPos, state));
                }
            }
        }

        return dedup(placements);
    }

    /**
     * 按 BlockPos 去重，後出現的覆蓋先出現的（blocks 覆蓋 regions）。
     */
    private static List<BatchPlacementManager.BlockPlacement> dedup(
            List<BatchPlacementManager.BlockPlacement> placements) {
        Map<BlockPos, BatchPlacementManager.BlockPlacement> map = new LinkedHashMap<>();
        for (BatchPlacementManager.BlockPlacement bp : placements) {
            map.put(bp.pos(), bp);
        }
        return new ArrayList<>(map.values());
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
}
