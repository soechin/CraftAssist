package craftassist.builder;

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
    }
}
