package com.craftassist.builder;

import java.util.List;

public class BuildStructure {

    private List<BlockRegion> regions;

    public List<BlockRegion> getRegions() {
        return regions;
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
}
