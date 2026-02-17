package craftassist.builder;

import craftassist.CraftAssistMod;
import craftassist.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                if (region.getProperties() != null && !region.getProperties().isEmpty()) {
                    state = PropertyApplier.applyProperties(state, region.getProperties());
                } else {
                    state = PropertyApplier.applyFacing(state, region.getFacing());
                }

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

        List<BatchPlacementManager.BlockPlacement> result = dedup(placements);
        sortByPlacementOrder(result);
        return result;
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

    // 需要先放置支撐方塊才能正確放置的附著方塊
    private static final Set<String> ATTACHED_BLOCKS = Set.of(
            "torch", "wall_torch", "soul_torch", "soul_wall_torch",
            "lantern", "soul_lantern", "lever", "ladder",
            "vine", "glow_lichen", "hanging_roots",
            "wall_sign", "oak_wall_sign", "spruce_wall_sign", "birch_wall_sign",
            "dark_oak_wall_sign", "jungle_wall_sign", "acacia_wall_sign",
            "mangrove_wall_sign", "cherry_wall_sign", "bamboo_wall_sign",
            "crimson_wall_sign", "warped_wall_sign"
    );

    // 多格方塊（門、床），需要在結構放好後才放
    private static final Set<String> MULTI_PART_BLOCKS = Set.of(
            "oak_door", "spruce_door", "birch_door", "dark_oak_door",
            "jungle_door", "acacia_door", "mangrove_door", "cherry_door",
            "bamboo_door", "crimson_door", "warped_door", "iron_door",
            "red_bed", "blue_bed", "white_bed", "black_bed",
            "brown_bed", "cyan_bed", "gray_bed", "green_bed",
            "light_blue_bed", "light_gray_bed", "lime_bed", "magenta_bed",
            "orange_bed", "pink_bed", "purple_bed", "yellow_bed"
    );

    /**
     * 按放置順序排序：結構 → air → 多格方塊（門/床） → 附著方塊 → 裝飾。
     * 確保方塊放置時其支撐方塊已存在。
     */
    private static void sortByPlacementOrder(List<BatchPlacementManager.BlockPlacement> placements) {
        placements.sort(Comparator.comparingInt(bp -> placementPriority(bp.state())));
    }

    private static int placementPriority(BlockState state) {
        if (state.isAir()) {
            return 1; // air 在結構之後（用來清空空間）
        }
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (MULTI_PART_BLOCKS.contains(blockId)) {
            return 2; // 門和床需要牆壁先就位
        }
        if (ATTACHED_BLOCKS.contains(blockId)) {
            return 3; // 附著方塊需要支撐面
        }
        if (blockId.contains("flower_pot") || blockId.contains("carpet")
                || blockId.contains("pressure_plate") || blockId.contains("button")) {
            return 3; // 地面/牆面裝飾也需要支撐
        }
        return 0; // 結構方塊優先
    }

    private static boolean isRegionValid(BuildStructure.BlockRegion region) {
        int[] from = region.getFrom();
        int[] to = region.getTo();
        if (from == null || to == null || from.length != 3 || to.length != 3) {
            return false;
        }

        int maxCoord = ConfigManager.getConfig().getMaxCoordinate();
        for (int i = 0; i < 3; i++) {
            if (Math.abs(from[i]) > maxCoord || Math.abs(to[i]) > maxCoord) {
                CraftAssistMod.LOGGER.warn("[CraftAssist] 區域座標超出限制 (±{}): from={}, to={}",
                        maxCoord, Arrays.toString(from), Arrays.toString(to));
                return false;
            }
        }

        long dx = Math.abs((long) to[0] - from[0]) + 1;
        long dy = Math.abs((long) to[1] - from[1]) + 1;
        long dz = Math.abs((long) to[2] - from[2]) + 1;
        long volume = dx * dy * dz;

        int maxVolume = ConfigManager.getConfig().getMaxRegionVolume();
        if (volume > maxVolume) {
            CraftAssistMod.LOGGER.warn("[CraftAssist] 區域體積 {} 超過限制 {}: from={}, to={}",
                    volume, maxVolume, Arrays.toString(from), Arrays.toString(to));
            return false;
        }

        return true;
    }

    private static void collectRegionPlacements(BlockPos origin, BuildStructure.BlockRegion region,
                                                 BlockState state,
                                                 List<BatchPlacementManager.BlockPlacement> output) {
        if (!isRegionValid(region)) {
            return;
        }

        int[] from = region.getFrom();
        int[] to = region.getTo();

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
                    if (isExcluded(x, y, z, region.getExclude())) {
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

    private static boolean isExcluded(int x, int y, int z, java.util.List<int[][]> excludes) {
        if (excludes == null) {
            return false;
        }
        for (int[][] ex : excludes) {
            if (ex == null || ex.length != 2 || ex[0].length != 3 || ex[1].length != 3) {
                continue;
            }
            int exMinX = Math.min(ex[0][0], ex[1][0]);
            int exMaxX = Math.max(ex[0][0], ex[1][0]);
            int exMinY = Math.min(ex[0][1], ex[1][1]);
            int exMaxY = Math.max(ex[0][1], ex[1][1]);
            int exMinZ = Math.min(ex[0][2], ex[1][2]);
            int exMaxZ = Math.max(ex[0][2], ex[1][2]);
            if (x >= exMinX && x <= exMaxX && y >= exMinY && y <= exMaxY && z >= exMinZ && z <= exMaxZ) {
                return true;
            }
        }
        return false;
    }
}
