package net.mooctest;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 中文注释：
 * 本类覆盖到达性判定的所有关键分支：比例缩放、同格子、水平/垂直/斜线、围栏Fence、以及障碍导致的最近可达点回退。
 */
public class ReachabilityTest {

    @Test(expected = IllegalArgumentException.class)
    public void testScale_Illegal() {
        // 中文：比例小于1应抛异常
        Reachability.getClosestWalkablePointToTarget(0, 0, 1, 1, 0, new Grid(5, 5));
    }

    @Test
    public void testStartCellUnwalkable() {
        // 中文：起始格不可行走，则最近点为起点
        Grid g = new Grid(5, 5);
        g.setWalkable(0, 0, false);
        long p = Reachability.getClosestWalkablePointToTarget(0, 0, 3, 0, g);
        assertEquals(Point.toPoint(0, 0), p);
        assertFalse(Reachability.isReachable(0, 0, 3, 0, g));
    }

    @Test
    public void testSameGrid_WithFenceBlocked() {
        // 中文：同一格子内，有Fence阻断则返回起点
        Grid g = new Grid(5, 5);
        Fence fence = (x1, y1, x2, y2) -> false;
        long p = Reachability.getClosestWalkablePointToTarget(1, 1, 1, 1, 1, g, fence);
        assertEquals(Point.toPoint(1, 1), p);

        // 无阻断则返回终点
        Fence pass = (x1, y1, x2, y2) -> true;
        long p2 = Reachability.getClosestWalkablePointToTarget(1, 1, 1, 1, 1, g, pass);
        assertEquals(Point.toPoint(1, 1), p2);
    }

    @Test
    public void testHorizontal_Line_BlockedImmediatelyAndLater() {
        Grid g = new Grid(10, 3);
        // y=1水平线
        int y = 1;
        // 立即阻断：起点右侧第一格不可走 -> 返回起点
        g.setWalkable(1, y, false);
        long p = Reachability.getClosestWalkablePointToTarget(0, y, 5, y, g);
        assertEquals(Point.toPoint(0, y), p);

        // 取消第一格阻断，改为更远处阻断 -> 返回阻断前的格子中心（scale=1下即格子索引）
        g.setWalkable(1, y, true);
        g.setWalkable(2, y, false);
        long p2 = Reachability.getClosestWalkablePointToTarget(0, y, 5, y, g);
        assertEquals(Point.toPoint(1, y), p2);

        // 反向行走，左移
        g.setWalkable(2, y, true);
        g.setWalkable(3, y, false);
        long p3 = Reachability.getClosestWalkablePointToTarget(5, y, 0, y, g);
        assertEquals(Point.toPoint(4, y), p3);
    }

    @Test
    public void testVertical_Line_BlockedImmediatelyAndLater() {
        Grid g = new Grid(3, 10);
        int x = 1;
        // 立即阻断：起点上方第一格不可走 -> 返回起点
        g.setWalkable(x, 1, false);
        long p = Reachability.getClosestWalkablePointToTarget(x, 0, x, 5, g);
        assertEquals(Point.toPoint(x, 0), p);

        // 更远处阻断 -> 返回阻断前的格子
        g.setWalkable(x, 1, true);
        g.setWalkable(x, 2, false);
        long p2 = Reachability.getClosestWalkablePointToTarget(x, 0, x, 5, g);
        assertEquals(Point.toPoint(x, 1), p2);

        // 反向行走，向下
        g.setWalkable(x, 2, true);
        g.setWalkable(x, 3, false);
        long p3 = Reachability.getClosestWalkablePointToTarget(x, 5, x, 0, g);
        assertEquals(Point.toPoint(x, 4), p3);
    }

    @Test
    public void testDiagonal_NoObstacle_PositiveSlope_ExactCorner() {
        // 中文：斜线（k>0）穿过网格交点时，不需要额外检查
        Grid g = new Grid(10, 10);
        assertTrue(Reachability.isReachable(0, 0, 2, 2, g));
        assertEquals(Point.toPoint(2, 2), Reachability.getClosestWalkablePointToTarget(0, 0, 2, 2, g));
    }

    @Test
    public void testDiagonal_WithFenceInterrupt() {
        // 中文：斜线路径中途被Fence打断，应回退到上一个检查点
        Grid g = new Grid(10, 10);
        Fence blockAllButEnd = (x1, y1, x2, y2) -> x2 == 5 && y2 == 5; // 仅允许终点坐标
        long p = Reachability.getClosestWalkablePointToTarget(0, 0, 8, 7, 1, g, blockAllButEnd);
        // 由于Fence几乎全阻断，返回点必非终点
        assertNotEquals(Point.toPoint(8, 7), p);
    }

    @Test
    public void testDiagonal_StepYBranch_NoObstacle() {
        // 中文：|dy|>|dx| 走y步进分支
        Grid g = new Grid(10, 10);
        long p = Reachability.getClosestWalkablePointToTarget(1, 1, 2, 6, g);
        assertEquals(Point.toPoint(2, 6), p);
    }

    @Test
    public void testScaleHelpers() {
        // 中文：辅助方法的正确性
        assertEquals(2.5, Reachability.scaleDown(5, 2), 1e-9);
        assertEquals(5, Reachability.scaleUp(2, 2)); // 中心点：2*2+1=5
        assertEquals(7, Reachability.scaleUp(3.5, 2));
        assertEquals(Point.toPoint(5, 7), Reachability.scaleUpPoint(2.0, 3.5, 2));
    }
}
