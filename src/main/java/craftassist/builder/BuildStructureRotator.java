package craftassist.builder;

import net.minecraft.core.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 偵測建築入口牆面，並旋轉整個 {@link BuildStructure} 使入口面對玩家。
 * <p>
 * 所有旋轉以 Y 軸為中心，順時針 (CW) 90° 為單位。
 * 座標使用 normalize 公式 {@code (newX = maxZ - oldZ, newZ = oldX)} 確保旋轉後座標不會產生負值偏移。
 */
public class BuildStructureRotator {

    private static final Logger LOGGER = LoggerFactory.getLogger("CraftAssist");

    private static final String[] CW_FACING = {"south", "west", "north", "east"};

    // ==================== 入口偵測 ====================

    /**
     * 偵測建築入口（門）所在的牆面。
     * <p>
     * 策略（投票法）：對每扇門分別判斷最近的牆面（對比僅 regions 的 bbox），
     * 統計每面牆的門數，票數最多的牆面為入口。票數相同時用門平均座標作 tiebreaker。
     * <p>
     * 投票法的優勢：大門（雙開門 = 2 個 lower-half = 2 票）自然比小門（1 票）更有話語權，
     * 避免平均座標被不同牆面的門拉到建築中心。
     *
     * @return 入口所在牆面方向，找不到門時回傳 null
     */
    public static Direction detectEntranceWall(BuildStructure structure) {
        List<int[]> doorPositions = collectDoorPositions(structure);
        if (doorPositions.isEmpty()) {
            return null;
        }

        // 用僅 regions 的 bbox 作為結構邊界（排除 blocks 中的裝飾）
        BuildingOffsetCalculator.BoundingBox regionBbox =
                BuildingOffsetCalculator.computeRegionBoundingBox(structure);

        // 對每扇門投票：判斷最近的牆面
        int[] votes = new int[4]; // NORTH=0, SOUTH=1, WEST=2, EAST=3
        for (int[] pos : doorPositions) {
            double distNorth = Math.abs(pos[2] - regionBbox.minZ());
            double distSouth = Math.abs(pos[2] - regionBbox.maxZ());
            double distWest = Math.abs(pos[0] - regionBbox.minX());
            double distEast = Math.abs(pos[0] - regionBbox.maxX());

            double minDist = Math.min(Math.min(distNorth, distSouth), Math.min(distWest, distEast));

            if (minDist == distNorth) votes[0]++;
            else if (minDist == distSouth) votes[1]++;
            else if (minDist == distWest) votes[2]++;
            else votes[3]++;
        }

        // 找到最高票數
        int maxVotes = Math.max(Math.max(votes[0], votes[1]), Math.max(votes[2], votes[3]));

        // 如果有平手，用門平均座標作 tiebreaker
        Direction[] voteOrder = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        List<Direction> candidates = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (votes[i] == maxVotes) {
                candidates.add(voteOrder[i]);
            }
        }

        Direction result;
        if (candidates.size() == 1) {
            result = candidates.get(0);
        } else {
            // Tiebreaker：用門平均座標選最近的牆
            double avgX = 0, avgZ = 0;
            for (int[] pos : doorPositions) {
                avgX += pos[0];
                avgZ += pos[2];
            }
            avgX /= doorPositions.size();
            avgZ /= doorPositions.size();

            result = candidates.get(0);
            double bestDist = distToWall(result, avgX, avgZ, regionBbox);
            for (int i = 1; i < candidates.size(); i++) {
                double d = distToWall(candidates.get(i), avgX, avgZ, regionBbox);
                if (d < bestDist) {
                    bestDist = d;
                    result = candidates.get(i);
                }
            }
        }

        LOGGER.info("[CraftAssist] detectEntranceWall: 門{}扇, 投票=[N={},S={},W={},E={}], regionBbox=[{},{}→{},{}], 入口牆面={}",
                doorPositions.size(), votes[0], votes[1], votes[2], votes[3],
                regionBbox.minX(), regionBbox.minZ(), regionBbox.maxX(), regionBbox.maxZ(),
                result);

        return result;
    }

    private static double distToWall(Direction wall, double x, double z,
                                     BuildingOffsetCalculator.BoundingBox bbox) {
        return switch (wall) {
            case NORTH -> Math.abs(z - bbox.minZ());
            case SOUTH -> Math.abs(z - bbox.maxZ());
            case WEST -> Math.abs(x - bbox.minX());
            case EAST -> Math.abs(x - bbox.maxX());
            default -> Double.MAX_VALUE;
        };
    }

    /**
     * 收集所有門方塊的座標（只取 lower half 避免重複計算）。
     */
    private static List<int[]> collectDoorPositions(BuildStructure structure) {
        List<int[]> positions = new ArrayList<>();
        if (structure.getBlocks() == null) {
            return positions;
        }

        for (BuildStructure.IndividualBlock block : structure.getBlocks()) {
            String id = block.getBlock();
            if (id == null || !id.contains("_door") || id.contains("trapdoor")) {
                continue;
            }
            int[] pos = block.getPos();
            if (pos == null || pos.length != 3) {
                continue;
            }

            // 只取 lower half（或無 half 屬性的門）
            Map<String, String> props = block.getProperties();
            if (props != null) {
                String half = props.get("half");
                if (half != null && half.equalsIgnoreCase("upper")) {
                    continue;
                }
            }

            positions.add(pos);
        }
        return positions;
    }

    // ==================== 旋轉計算 ====================

    /**
     * 計算需要順時針旋轉幾次 90° 才能讓入口牆面對齊目標位置。
     *
     * @param entranceWall 門目前所在的牆面
     * @param playerFacing 玩家面向的方向
     * @return 順時針旋轉次數 (0-3)
     */
    public static int computeRotationCount(Direction entranceWall, Direction playerFacing) {
        Direction targetWall = playerFacing.getOpposite();
        int fromIdx = directionToCWIndex(entranceWall);
        int toIdx = directionToCWIndex(targetWall);
        return (toIdx - fromIdx + 4) % 4;
    }

    // ==================== 旋轉執行 ====================

    /**
     * 原地旋轉建築結構（修改所有座標和方向屬性）。
     * <p>
     * 每次 CW 90° 旋轉使用 normalize 公式：
     * {@code newX = maxZ - oldZ, newZ = oldX}
     * 確保旋轉後座標保持合理的相對位置。
     *
     * @param structure    要旋轉的建築結構
     * @param cwRotations  順時針旋轉次數 (0-3)
     */
    public static void rotateStructure(BuildStructure structure, int cwRotations) {
        cwRotations = ((cwRotations % 4) + 4) % 4;
        if (cwRotations == 0) {
            return;
        }

        for (int i = 0; i < cwRotations; i++) {
            rotateCW90(structure);
        }
    }

    /**
     * 執行一次 CW 90° 旋轉。
     */
    private static void rotateCW90(BuildStructure structure) {
        // 先計算旋轉前的完整 bbox（包含 regions 和 blocks）
        BuildingOffsetCalculator.BoundingBox bbox =
                BuildingOffsetCalculator.computeBoundingBox(structure);
        int maxZ = bbox.maxZ();

        // 旋轉所有 regions
        if (structure.getRegions() != null) {
            for (BuildStructure.BlockRegion region : structure.getRegions()) {
                int[] from = region.getFrom();
                int[] to = region.getTo();
                if (from != null && from.length == 3) {
                    region.setFrom(rotateCW90Coord(from, maxZ));
                }
                if (to != null && to.length == 3) {
                    region.setTo(rotateCW90Coord(to, maxZ));
                }
                // 旋轉 properties（優先）或 facing
                if (region.getProperties() != null && !region.getProperties().isEmpty()) {
                    rotateProperties(region.getProperties(), 1);
                } else if (region.getFacing() != null) {
                    region.setFacing(rotateFacingCW(region.getFacing(), 1));
                }
                // 旋轉 exclude 座標
                List<int[][]> excludeList = region.getExclude();
                if (excludeList != null && !excludeList.isEmpty()) {
                    List<int[][]> rotated = new java.util.ArrayList<>();
                    for (int[][] ex : excludeList) {
                        if (ex != null && ex.length == 2) {
                            int[] p0 = (ex[0] != null && ex[0].length == 3) ? rotateCW90Coord(ex[0], maxZ) : ex[0];
                            int[] p1 = (ex[1] != null && ex[1].length == 3) ? rotateCW90Coord(ex[1], maxZ) : ex[1];
                            rotated.add(new int[][]{p0, p1});
                        } else {
                            rotated.add(ex);
                        }
                    }
                    region.setExclude(rotated);
                }
            }
        }

        // 旋轉所有 blocks
        if (structure.getBlocks() != null) {
            for (BuildStructure.IndividualBlock block : structure.getBlocks()) {
                int[] pos = block.getPos();
                if (pos != null && pos.length == 3) {
                    block.setPos(rotateCW90Coord(pos, maxZ));
                }
                rotateProperties(block.getProperties(), 1);
            }
        }
    }

    /**
     * 單次 CW 90° 座標旋轉：{@code (x, y, z) → (maxZ - z, y, x)}
     */
    private static int[] rotateCW90Coord(int[] coord, int maxZ) {
        return new int[]{maxZ - coord[2], coord[1], coord[0]};
    }

    // ==================== 屬性旋轉 ====================

    /**
     * 旋轉 block properties 中的方向性屬性。
     */
    private static void rotateProperties(Map<String, String> props, int cwTimes) {
        if (props == null) {
            return;
        }

        // 旋轉 facing
        if (props.containsKey("facing")) {
            String rotated = rotateFacingCW(props.get("facing"), cwTimes);
            if (rotated != null) {
                props.put("facing", rotated);
            }
        }

        // 旋轉 axis
        if (props.containsKey("axis")) {
            props.put("axis", rotateAxis(props.get("axis"), cwTimes));
        }
    }

    /**
     * 順時針旋轉水平方向字串。
     * CW 順序：south → west → north → east → south
     *
     * @return 旋轉後的方向字串，非水平方向（up/down）或 null 原樣回傳
     */
    static String rotateFacingCW(String facing, int times) {
        if (facing == null) {
            return null;
        }
        String lower = facing.toLowerCase();
        int idx = facingToIndex(lower);
        if (idx < 0) {
            // up/down 或無效值，不旋轉
            return facing;
        }
        return CW_FACING[(idx + times) % 4];
    }

    /**
     * 旋轉軸屬性。奇數次旋轉時 x↔z，y 不變。
     */
    static String rotateAxis(String axis, int times) {
        if (axis == null) {
            return null;
        }
        if ((times % 2) == 0) {
            return axis;
        }
        return switch (axis.toLowerCase()) {
            case "x" -> "z";
            case "z" -> "x";
            default -> axis; // "y" 不變
        };
    }

    // ==================== 工具方法 ====================

    private static int directionToCWIndex(Direction dir) {
        return switch (dir) {
            case SOUTH -> 0;
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
    }

    private static int facingToIndex(String facing) {
        return switch (facing) {
            case "south" -> 0;
            case "west" -> 1;
            case "north" -> 2;
            case "east" -> 3;
            default -> -1;
        };
    }
}
