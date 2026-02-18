package craftassist.builder;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuildStructureParseExcludeTest {

    private static final Gson GSON = new Gson();

    private BuildStructure.BlockRegion regionWithExclude(String excludeJson) {
        String regionJson = """
                {"block":"minecraft:stone","from":[0,0,0],"to":[6,5,8],"hollow":false,"exclude":%s}
                """.formatted(excludeJson);
        return GSON.fromJson(regionJson, BuildStructure.BlockRegion.class);
    }

    @Test
    void threeLayerNested_singlePair_parsedCorrectly() {
        BuildStructure.BlockRegion region = regionWithExclude("[[[1,2,3],[4,5,6]]]");
        List<int[][]> exclude = region.getExclude();

        assertNotNull(exclude);
        assertEquals(1, exclude.size());
        assertArrayEquals(new int[]{1, 2, 3}, exclude.get(0)[0]);
        assertArrayEquals(new int[]{4, 5, 6}, exclude.get(0)[1]);
    }

    @Test
    void threeLayerNested_multiplePairs_allParsed() {
        BuildStructure.BlockRegion region =
                regionWithExclude("[[[1,2,3],[4,5,6]],[[7,8,9],[10,11,12]]]");
        List<int[][]> exclude = region.getExclude();

        assertNotNull(exclude);
        assertEquals(2, exclude.size());
        assertArrayEquals(new int[]{1, 2, 3}, exclude.get(0)[0]);
        assertArrayEquals(new int[]{4, 5, 6}, exclude.get(0)[1]);
        assertArrayEquals(new int[]{7, 8, 9}, exclude.get(1)[0]);
        assertArrayEquals(new int[]{10, 11, 12}, exclude.get(1)[1]);
    }

    @Test
    void twoLayerNested_autoWrappedAsSinglePair() {
        // [[1,2,3],[4,5,6]] — LLM 常見錯誤格式，自動包裝為一組
        BuildStructure.BlockRegion region = regionWithExclude("[[1,2,3],[4,5,6]]");
        List<int[][]> exclude = region.getExclude();

        assertNotNull(exclude);
        assertEquals(1, exclude.size());
        assertArrayEquals(new int[]{1, 2, 3}, exclude.get(0)[0]);
        assertArrayEquals(new int[]{4, 5, 6}, exclude.get(0)[1]);
    }

    @Test
    void excludeNull_returnsNull() {
        String regionJson = """
                {"block":"minecraft:stone","from":[0,0,0],"to":[4,4,4],"hollow":false}
                """;
        BuildStructure.BlockRegion region = GSON.fromJson(regionJson, BuildStructure.BlockRegion.class);
        assertNull(region.getExclude());
    }

    @Test
    void excludeEmptyArray_returnsNull() {
        BuildStructure.BlockRegion region = regionWithExclude("[]");
        assertNull(region.getExclude());
    }

    @Test
    void invalidFormat_nonArrayElement_returnsEmptyList() {
        // 第一個元素不是陣列
        BuildStructure.BlockRegion region = regionWithExclude("[1,2,3]");
        List<int[][]> exclude = region.getExclude();

        assertNotNull(exclude);
        assertTrue(exclude.isEmpty());
    }

    @Test
    void coordinateWithWrongLength_returnedPairIsNull() {
        // 座標元素不足 3 個
        BuildStructure.BlockRegion region = regionWithExclude("[[[1,2],[4,5,6]]]");
        List<int[][]> exclude = region.getExclude();

        // pair[0] 為 null 時，該組不加入結果
        assertNotNull(exclude);
        assertTrue(exclude.isEmpty());
    }
}
