package com.craftassist.builder;

import com.craftassist.CraftAssistMod;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

import java.util.Map;

/**
 * 動態設定 BlockState 屬性，支援 LLM 生成的 properties Map。
 */
public class PropertyApplier {

    private static final Map<String, String> ALIASES = Map.of(
            "direction", "facing",
            "orientation", "facing",
            "side", "half",
            "slab_type", "type",
            "door_hinge", "hinge"
    );

    public static BlockState applyProperties(BlockState state, Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return state;
        }

        BlockState result = state;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = ALIASES.getOrDefault(entry.getKey().toLowerCase(), entry.getKey().toLowerCase());
            result = applySingleProperty(result, key, entry.getValue());
        }
        return result;
    }

    public static BlockState applyFacing(BlockState state, String facingStr) {
        if (facingStr == null || facingStr.isEmpty()) {
            return state;
        }
        return applySingleProperty(state, "facing", facingStr);
    }

    private static BlockState applySingleProperty(BlockState state, String key, String value) {
        try {
            return switch (key) {
                case "facing" -> applyFacingProperty(state, value);
                case "half" -> applyHalfProperty(state, value);
                case "type" -> applyTypeProperty(state, value);
                case "open" -> applyBooleanProperty(state, BlockStateProperties.OPEN, value);
                case "powered" -> applyBooleanProperty(state, BlockStateProperties.POWERED, value);
                case "waterlogged" -> applyBooleanProperty(state, BlockStateProperties.WATERLOGGED, value);
                case "lit" -> applyBooleanProperty(state, BlockStateProperties.LIT, value);
                case "hinge" -> applyHingeProperty(state, value);
                case "axis" -> applyAxisProperty(state, value);
                case "shape" -> applyStairShapeProperty(state, value);
                default -> state;
            };
        } catch (Exception e) {
            CraftAssistMod.LOGGER.warn("[CraftAssist] 無法套用屬性 {}={}: {}", key, value, e.getMessage());
            return state;
        }
    }

    private static BlockState applyFacingProperty(BlockState state, String value) {
        Direction dir = parseDirection(value);
        if (dir == null) return state;

        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && dir.getAxis().isHorizontal()) {
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, dir);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.setValue(BlockStateProperties.FACING, dir);
        }
        return state;
    }

    private static BlockState applyHalfProperty(BlockState state, String value) {
        String v = value.toLowerCase();

        // 門的 upper/lower
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = v.equals("upper") ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER;
            return state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, half);
        }
        // 階梯/活板門的 top/bottom
        if (state.hasProperty(BlockStateProperties.HALF)) {
            Half half = v.equals("top") ? Half.TOP : Half.BOTTOM;
            return state.setValue(BlockStateProperties.HALF, half);
        }
        return state;
    }

    private static BlockState applyTypeProperty(BlockState state, String value) {
        if (state.hasProperty(BlockStateProperties.SLAB_TYPE)) {
            SlabType type = switch (value.toLowerCase()) {
                case "top" -> SlabType.TOP;
                case "double" -> SlabType.DOUBLE;
                default -> SlabType.BOTTOM;
            };
            return state.setValue(BlockStateProperties.SLAB_TYPE, type);
        }
        return state;
    }

    private static BlockState applyBooleanProperty(BlockState state, BooleanProperty property, String value) {
        if (state.hasProperty(property)) {
            return state.setValue(property, Boolean.parseBoolean(value));
        }
        return state;
    }

    private static BlockState applyHingeProperty(BlockState state, String value) {
        if (state.hasProperty(BlockStateProperties.DOOR_HINGE)) {
            DoorHingeSide hinge = value.equalsIgnoreCase("right")
                    ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
            return state.setValue(BlockStateProperties.DOOR_HINGE, hinge);
        }
        return state;
    }

    private static BlockState applyAxisProperty(BlockState state, String value) {
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            Direction.Axis axis = switch (value.toLowerCase()) {
                case "x" -> Direction.Axis.X;
                case "z" -> Direction.Axis.Z;
                default -> Direction.Axis.Y;
            };
            return state.setValue(BlockStateProperties.AXIS, axis);
        }
        return state;
    }

    private static BlockState applyStairShapeProperty(BlockState state, String value) {
        if (state.hasProperty(BlockStateProperties.STAIRS_SHAPE)) {
            StairsShape shape = switch (value.toLowerCase()) {
                case "inner_left" -> StairsShape.INNER_LEFT;
                case "inner_right" -> StairsShape.INNER_RIGHT;
                case "outer_left" -> StairsShape.OUTER_LEFT;
                case "outer_right" -> StairsShape.OUTER_RIGHT;
                default -> StairsShape.STRAIGHT;
            };
            return state.setValue(BlockStateProperties.STAIRS_SHAPE, shape);
        }
        return state;
    }

    private static Direction parseDirection(String value) {
        return switch (value.toLowerCase()) {
            case "north" -> Direction.NORTH;
            case "south" -> Direction.SOUTH;
            case "east" -> Direction.EAST;
            case "west" -> Direction.WEST;
            case "up" -> Direction.UP;
            case "down" -> Direction.DOWN;
            default -> null;
        };
    }
}
