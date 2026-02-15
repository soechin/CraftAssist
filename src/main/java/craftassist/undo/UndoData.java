package craftassist.undo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UndoData {

    private final List<BlockSnapshot> snapshots;

    public UndoData() {
        this.snapshots = new ArrayList<>();
    }

    public UndoData(List<BlockSnapshot> snapshots) {
        this.snapshots = snapshots;
    }

    public void addSnapshot(BlockPos pos, BlockState originalState) {
        snapshots.add(new BlockSnapshot(pos.immutable(), originalState));
    }

    public List<BlockSnapshot> getSnapshots() {
        return Collections.unmodifiableList(snapshots);
    }

    public int size() {
        return snapshots.size();
    }

    public record BlockSnapshot(BlockPos pos, BlockState originalState) {
    }
}
