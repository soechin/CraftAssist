package craftassist.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuildStructure {

    private List<BlockRegion> regions;
    private List<IndividualBlock> blocks;

    public List<BlockRegion> getRegions() {
        return regions;
    }

    public List<IndividualBlock> getBlocks() {
        return blocks;
    }

    public static class BlockRegion {
        private String block;
        private int[] from;
        private int[] to;
        private boolean hollow;
        private String facing;
        private Map<String, String> properties;
        private JsonArray exclude;

        public String getBlock() {
            return block;
        }

        public int[] getFrom() {
            return from;
        }

        public int[] getTo() {
            return to;
        }

        public boolean isHollow() {
            return hollow;
        }

        public String getFacing() {
            return facing;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        /**
         * 解析 exclude 為標準格式 List<int[][]>，容錯兩種 LLM 輸出格式：
         * - 三層巢狀（正確）：[[[from],[to]], [[from2],[to2]]]
         * - 兩層巢狀（常見錯誤）：[[from],[to]]（只有一組排除，自動包裝）
         */
        public List<int[][]> getExclude() {
            if (exclude == null || exclude.isEmpty()) {
                return null;
            }
            return parseExclude(exclude);
        }

        public JsonArray getExcludeRaw() {
            return exclude;
        }

        private static List<int[][]> parseExclude(JsonArray arr) {
            List<int[][]> result = new ArrayList<>();
            if (arr.isEmpty()) return result;

            // 判斷格式：看第一個元素的第一個元素是否為數字
            // 三層格式：[[[1,2,3],[4,5,6]]] → arr[0] = [[1,2,3],[4,5,6]] → arr[0][0] = [1,2,3] → arr[0][0][0] = 1 (array)
            // 兩層格式：[[1,2,3],[4,5,6]] → arr[0] = [1,2,3] → arr[0][0] = 1 (number)
            JsonElement first = arr.get(0);
            if (!first.isJsonArray()) return result;

            JsonArray firstArr = first.getAsJsonArray();
            if (firstArr.isEmpty()) return result;

            JsonElement firstInner = firstArr.get(0);

            if (firstInner.isJsonPrimitive()) {
                // 兩層格式：[[from],[to]] — 自動包裝為一組
                if (arr.size() >= 2) {
                    int[][] pair = new int[2][];
                    pair[0] = jsonArrayToIntArray(arr.get(0).getAsJsonArray());
                    pair[1] = jsonArrayToIntArray(arr.get(1).getAsJsonArray());
                    if (pair[0] != null && pair[1] != null) {
                        result.add(pair);
                    }
                }
            } else if (firstInner.isJsonArray()) {
                // 三層格式：[[[from],[to]], ...] — 正常解析
                for (JsonElement group : arr) {
                    if (!group.isJsonArray()) continue;
                    JsonArray groupArr = group.getAsJsonArray();
                    if (groupArr.size() >= 2) {
                        int[][] pair = new int[2][];
                        pair[0] = jsonArrayToIntArray(groupArr.get(0).getAsJsonArray());
                        pair[1] = jsonArrayToIntArray(groupArr.get(1).getAsJsonArray());
                        if (pair[0] != null && pair[1] != null) {
                            result.add(pair);
                        }
                    }
                }
            }
            return result;
        }

        private static int[] jsonArrayToIntArray(JsonArray arr) {
            if (arr == null || arr.size() != 3) return null;
            try {
                return new int[]{arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt()};
            } catch (Exception e) {
                return null;
            }
        }

        public void setFrom(int[] from) {
            this.from = from;
        }

        public void setTo(int[] to) {
            this.to = to;
        }

        public void setFacing(String facing) {
            this.facing = facing;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }

        public void setExclude(List<int[][]> exclude) {
            // 將 List<int[][]> 轉為 JsonArray 儲存
            JsonArray arr = new JsonArray();
            if (exclude != null) {
                for (int[][] pair : exclude) {
                    JsonArray group = new JsonArray();
                    JsonArray from = new JsonArray();
                    for (int v : pair[0]) from.add(v);
                    JsonArray to = new JsonArray();
                    for (int v : pair[1]) to.add(v);
                    group.add(from);
                    group.add(to);
                    arr.add(group);
                }
            }
            this.exclude = arr;
        }
    }

    public static class IndividualBlock {
        private String block;
        private int[] pos;
        private Map<String, String> properties;

        public String getBlock() {
            return block;
        }

        public int[] getPos() {
            return pos;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setPos(int[] pos) {
            this.pos = pos;
        }
    }
}
