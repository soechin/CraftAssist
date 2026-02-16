package craftassist.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * 計算建築物的 bounding box 並根據玩家面向偏移 origin，
 * 使建築物出現在玩家前方而非覆蓋玩家位置。
 */
public class BuildingOffsetCalculator {

    private static final int GAP = 2;

    public record BoundingBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public int width() { return maxX - minX + 1; }
        public int depth() { return maxZ - minZ + 1; }
    }

    /**
     * 掃描 BuildStructure 的所有座標，計算相對座標的 bounding box。
     */
    public static BoundingBox computeBoundingBox(BuildStructure structure) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        boolean hasAny = false;

        if (structure.getRegions() != null) {
            for (BuildStructure.BlockRegion region : structure.getRegions()) {
                int[] from = region.getFrom();
                int[] to = region.getTo();
                if (from == null || to == null || from.length != 3 || to.length != 3) {
                    continue;
                }
                hasAny = true;
                minX = Math.min(minX, Math.min(from[0], to[0]));
                minY = Math.min(minY, Math.min(from[1], to[1]));
                minZ = Math.min(minZ, Math.min(from[2], to[2]));
                maxX = Math.max(maxX, Math.max(from[0], to[0]));
                maxY = Math.max(maxY, Math.max(from[1], to[1]));
                maxZ = Math.max(maxZ, Math.max(from[2], to[2]));
            }
        }

        if (structure.getBlocks() != null) {
            for (BuildStructure.IndividualBlock block : structure.getBlocks()) {
                int[] pos = block.getPos();
                if (pos == null || pos.length != 3) {
                    continue;
                }
                hasAny = true;
                minX = Math.min(minX, pos[0]);
                minY = Math.min(minY, pos[1]);
                minZ = Math.min(minZ, pos[2]);
                maxX = Math.max(maxX, pos[0]);
                maxY = Math.max(maxY, pos[1]);
                maxZ = Math.max(maxZ, pos[2]);
            }
        }

        if (!hasAny) {
            return new BoundingBox(0, 0, 0, 0, 0, 0);
        }
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * 根據玩家面向和建築 bounding box，計算偏移後的 origin，
     * 使玩家站在建築物入口前方約 {@value GAP} 格的位置，且建築置中於玩家視線。
     */
    public static BlockPos computeOrigin(BlockPos playerPos, Direction facing, BoundingBox bbox) {
        return switch (facing) {
            // 玩家面向北(Z-)，入口在南牆(max-Z)，建築在玩家北方
            case NORTH -> playerPos.offset(
                    -bbox.minX - bbox.width() / 2,
                    0,
                    -bbox.maxZ - GAP
            );
            // 玩家面向南(Z+)，入口在北牆(min-Z)，建築在玩家南方
            case SOUTH -> playerPos.offset(
                    -bbox.minX - bbox.width() / 2,
                    0,
                    -bbox.minZ + GAP
            );
            // 玩家面向東(X+)，入口在西牆(min-X)，建築在玩家東方
            case EAST -> playerPos.offset(
                    -bbox.minX + GAP,
                    0,
                    -bbox.minZ - bbox.depth() / 2
            );
            // 玩家面向西(X-)，入口在東牆(max-X)，建築在玩家西方
            case WEST -> playerPos.offset(
                    -bbox.maxX - GAP,
                    0,
                    -bbox.minZ - bbox.depth() / 2
            );
            default -> playerPos;
        };
    }
}
