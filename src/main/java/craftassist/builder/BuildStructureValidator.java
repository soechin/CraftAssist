package craftassist.builder;

import craftassist.config.ModConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * 驗證 BuildStructure 的合法性，包含方塊 ID、座標範圍與區域體積。
 * 支援注入自定義 blockValidator 以便單元測試。
 */
public class BuildStructureValidator {

    public static ValidationResult validate(BuildStructure structure, ModConfig config) {
        return validate(structure, config, blockId -> BlockValidator.validate(blockId) != null);
    }

    public static ValidationResult validate(BuildStructure structure, ModConfig config,
                                            Predicate<String> blockValidator) {
        List<String> issues = new ArrayList<>();
        int maxCoord = config.getMaxCoordinate();
        int maxVolume = config.getMaxRegionVolume();

        if (structure.getRegions() != null) {
            for (int i = 0; i < structure.getRegions().size(); i++) {
                BuildStructure.BlockRegion region = structure.getRegions().get(i);
                String prefix = "Region[" + i + "] (block=" + region.getBlock() + ")";

                if (region.getBlock() == null || !blockValidator.test(region.getBlock())) {
                    issues.add(prefix + ": 無效的方塊 ID");
                }

                int[] from = region.getFrom();
                int[] to = region.getTo();

                if (from == null || from.length != 3) {
                    issues.add(prefix + ": 缺少或格式錯誤的 'from' 座標");
                } else if (exceedsMaxCoord(from, maxCoord)) {
                    issues.add(prefix + ": 'from' 座標超出範圍 " + maxCoord + ": " + Arrays.toString(from));
                }

                if (to == null || to.length != 3) {
                    issues.add(prefix + ": 缺少或格式錯誤的 'to' 座標");
                } else if (exceedsMaxCoord(to, maxCoord)) {
                    issues.add(prefix + ": 'to' 座標超出範圍 " + maxCoord + ": " + Arrays.toString(to));
                }

                if (from != null && from.length == 3 && to != null && to.length == 3) {
                    long volume = computeVolume(from, to);
                    if (volume > maxVolume) {
                        issues.add(prefix + ": 區域體積 " + volume + " 超出最大值 " + maxVolume);
                    }
                }
            }
        }

        if (structure.getBlocks() != null) {
            for (int i = 0; i < structure.getBlocks().size(); i++) {
                BuildStructure.IndividualBlock block = structure.getBlocks().get(i);
                String prefix = "Block[" + i + "] (block=" + block.getBlock() + ")";

                if (block.getBlock() == null || !blockValidator.test(block.getBlock())) {
                    issues.add(prefix + ": 無效的方塊 ID");
                }

                int[] pos = block.getPos();
                if (pos == null || pos.length != 3) {
                    issues.add(prefix + ": 缺少或格式錯誤的 'pos' 座標");
                } else if (exceedsMaxCoord(pos, maxCoord)) {
                    issues.add(prefix + ": 'pos' 座標超出範圍 " + maxCoord + ": " + Arrays.toString(pos));
                }
            }
        }

        return new ValidationResult(issues);
    }

    private static boolean exceedsMaxCoord(int[] coords, int max) {
        for (int c : coords) {
            if (Math.abs(c) > max) return true;
        }
        return false;
    }

    private static long computeVolume(int[] from, int[] to) {
        long dx = Math.abs(to[0] - from[0]) + 1;
        long dy = Math.abs(to[1] - from[1]) + 1;
        long dz = Math.abs(to[2] - from[2]) + 1;
        return dx * dy * dz;
    }

    public static class ValidationResult {
        private final List<String> issues;

        ValidationResult(List<String> issues) {
            this.issues = issues;
        }

        public boolean hasIssues() {
            return !issues.isEmpty();
        }

        public String getReport() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < issues.size(); i++) {
                sb.append(i + 1).append(". ").append(issues.get(i)).append("\n");
            }
            return sb.toString();
        }

        public List<String> getIssues() {
            return Collections.unmodifiableList(issues);
        }
    }
}
