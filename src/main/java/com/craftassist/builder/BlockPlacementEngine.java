package com.craftassist.builder;

import com.craftassist.CraftAssistMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class BlockPlacementEngine {

    /**
     * 放置建築結構。
     *
     * @return 放置的方塊數，若超過限制則回傳 -1
     */
    public static int place(ServerLevel world, BlockPos origin, BuildStructure structure, int maxBlocks) {
        if (structure.getRegions() == null || structure.getRegions().isEmpty()) {
            return 0;
        }

        int totalBlocks = estimateBlocks(structure);
        if (totalBlocks > maxBlocks) {
            return -totalBlocks;
        }

        int placed = 0;
        for (BuildStructure.BlockRegion region : structure.getRegions()) {
            Block block = BlockValidator.validate(region.getBlock());
            if (block == null) {
                continue;
            }

            BlockState state = block.defaultBlockState();
            state = applyFacing(state, region.getFacing());

            placed += placeRegion(world, origin, region, state);
        }

        CraftAssistMod.LOGGER.info("[CraftAssist] 放置了 {} 個方塊", placed);
        return placed;
    }

    private static int placeRegion(ServerLevel world, BlockPos origin,
                                   BuildStructure.BlockRegion region, BlockState state) {
        int[] from = region.getFrom();
        int[] to = region.getTo();
        if (from == null || to == null || from.length != 3 || to.length != 3) {
            return 0;
        }

        int minX = Math.min(from[0], to[0]);
        int minY = Math.min(from[1], to[1]);
        int minZ = Math.min(from[2], to[2]);
        int maxX = Math.max(from[0], to[0]);
        int maxY = Math.max(from[1], to[1]);
        int maxZ = Math.max(from[2], to[2]);

        int count = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (region.isHollow() && !isOuter(x, y, z, minX, minY, minZ, maxX, maxY, maxZ)) {
                        continue;
                    }
                    BlockPos pos = origin.offset(x, y, z);
                    world.setBlockAndUpdate(pos, state);
                    count++;
                }
            }
        }
        return count;
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

    private static int estimateBlocks(BuildStructure structure) {
        int total = 0;
        for (BuildStructure.BlockRegion region : structure.getRegions()) {
            int[] from = region.getFrom();
            int[] to = region.getTo();
            if (from == null || to == null || from.length != 3 || to.length != 3) {
                continue;
            }

            int dx = Math.abs(to[0] - from[0]) + 1;
            int dy = Math.abs(to[1] - from[1]) + 1;
            int dz = Math.abs(to[2] - from[2]) + 1;

            if (region.isHollow()) {
                // 粗略估算外殼方塊數
                total += dx * dy * dz - Math.max(0, (dx - 2) * (dy - 2) * (dz - 2));
            } else {
                total += dx * dy * dz;
            }
        }
        return total;
    }
}
