package com.mineagent.engine.pathing.goals;

/**
 * Goal: stand in a nearby cell from which a block can be placed at the target.
 *
 * <p>The generic interaction goal also accepts the cell directly below a
 * target. That is valid for mining, but a player's two-block-tall body then
 * overlaps a solid block placed at the target. Placement needs a distinct
 * goal which stays horizontally offset, allows reachable overhead placement,
 * or stands directly above the target for a legitimate place-below action.
 * Actual clicks still enforce vanilla reach, support faces and ray visibility.
 */
public final class GoalPlaceBlock implements Goal {

    private final int x;
    private final int y;
    private final int z;
    public record Stand(int x,int y,int z) {}
    private final java.util.List<Stand> rejected;

    public GoalPlaceBlock(int x, int y, int z) {
        this(x,y,z,java.util.List.of());
    }
    public GoalPlaceBlock(int x,int y,int z,java.util.List<Stand> rejected){
        this.x=x;this.y=y;this.z=z;this.rejected=java.util.List.copyOf(rejected);
    }

    @Override
    public boolean isInGoal(int px, int py, int pz) {
        if(rejected(px,py,pz))return false;
        long dx = Math.abs((long) px - x);
        long dz = Math.abs((long) pz - z);
        long dy = (long) py - y;

        // A roof up to three cells above feet is reachable without climbing
        // its wall. Do not force a ground-level builder onto the roof first.
        if (Math.max(dx, dz) == 1 && dy >= -3 && dy <= 1) return true;

        // Standing directly above an empty target is also collision-safe. The
        // Adjacent cells below are excluded because the body would overlap;
        // a two-cell clearance is enough for legitimate overhead placement.
        return dx == 0 && dz == 0 && (dy == 1 || dy == -2 || dy == -3);
    }

    @Override
    public double heuristic(int px, int py, int pz) {
        double best = Double.POSITIVE_INFINITY;
        for (int yOffset = -3; yOffset <= 1; yOffset++) {
            for (int xOffset = -1; xOffset <= 1; xOffset++) {
                for (int zOffset = -1; zOffset <= 1; zOffset++) {
                    boolean horizontalNeighbor = Math.max(
                            Math.abs(xOffset), Math.abs(zOffset)) == 1;
                    boolean verticalClearance = xOffset == 0 && zOffset == 0
                            && (yOffset == 1 || yOffset <= -2);
                    if (!horizontalNeighbor && !verticalClearance) continue;
                    if(rejected(x+xOffset,y+yOffset,z+zOffset))continue;

                    double dx = (double) px - (x + xOffset);
                    double dy = (double) py - (y + yOffset);
                    double dz = (double) pz - (z + zOffset);
                    best = Math.min(best, Math.sqrt(dx * dx + dy * dy + dz * dz));
                }
            }
        }
        return best;
    }

    private boolean rejected(int px,int py,int pz){
        for(var stand:rejected)if(stand.x()==px&&stand.y()==py&&stand.z()==pz)return true;
        return false;
    }

    @Override
    public String toString() {
        return "GoalPlaceBlock[" + x + ", " + y + ", " + z + "]";
    }
}
