package com.mineagent.engine.pathing;
import com.mineagent.engine.pathing.goals.GoalPlaceBlock;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class PlacementApproachTest {
    @Test void rejectedClickPositionRequiresAnotherGeometricallyValidApproach(){
        var goal=new GoalPlaceBlock(3,64,3,List.of(new GoalPlaceBlock.Stand(2,64,2)));
        assertFalse(goal.isInGoal(2,64,2));assertTrue(goal.isInGoal(4,64,3));assertTrue(goal.heuristic(2,64,2)>0);
        assertFalse(goal.isInGoal(3,64,3));assertFalse(goal.isInGoal(3,63,3));
        assertTrue(new GoalPlaceBlock(3,64,3).isInGoal(2,64,2));
    }
    @Test void reachableCeilingsDoNotRequireStandingOnTheWall(){
        var goal=new GoalPlaceBlock(3,66,3);
        assertTrue(goal.isInGoal(3,64,3));assertEquals(0,goal.heuristic(3,64,3));
        assertTrue(goal.isInGoal(2,63,3));assertFalse(goal.isInGoal(3,65,3));
        assertFalse(goal.isInGoal(3,62,3));assertFalse(goal.isInGoal(3,66,3));
    }
}
