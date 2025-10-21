package net.mooctest;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 中文注释：
 * 强化用例，补充更多边界场景以提升分支覆盖率与变异杀死率。
 */
public class AllInOneExtraTest {

    // ============== Grid 构造与边界 ==============

    @Test(expected = RuntimeException.class)
    public void testGrid_Constructor_WidthZero_ShouldThrow() {
        // 中文：宽度为0非法
        new Grid(0, 5);
    }

    @Test(expected = RuntimeException.class)
    public void testGrid_Constructor_HeightZero_ShouldThrow() {
        // 中文：高度为0非法
        new Grid(5, 0);
    }

    // ============== Point 负坐标编码 ==============

    @Test
    public void testPoint_NegativeCoordinates_RoundTrip() {
        // 中文：验证负坐标的编码/解码
        long p = Point.toPoint(-7, -9);
        assertEquals(-7, Point.getX(p));
        assertEquals(-9, Point.getY(p));
    }

    // ============== AStar.open 分支补充 ==============

    @Test
    public void testAStar_Open_Unwalkable_ShouldNoOp() {
        // 中文：目标格不可行走时，open应直接返回不修改grid
        Grid grid = new Grid(4, 4);
        AStar astar = new AStar();
        astar.nodes.map = grid;
        grid.setWalkable(1, 1, false);
        astar.open(1, 1, 5, Grid.DIRECTION_UP, 3, 3, grid);
        assertEquals(0, grid.info(1, 1)); // 没有被打开
    }

    @Test
    public void testAStar_Open_EqualG_NoUpdate() {
        Grid grid = new Grid(6, 6);
        AStar astar = new AStar();
        astar.nodes.map = grid;

        // 打开(2,2) g=10
        astar.open(2, 2, 10, Grid.DIRECTION_UP, 5, 5, grid);
        int idx = Grid.openNodeIdx(grid.info(2, 2));
        long n0 = astar.nodes.getOpenNode(idx);
        assertEquals(10, Node.getG(n0));
        assertEquals(Grid.DIRECTION_UP, grid.nodeParentDirection(2, 2));

        // 使用相同g再次打开，不应更新父方向
        astar.open(2, 2, 10, Grid.DIRECTION_LEFT, 5, 5, grid);
        long n1 = astar.nodes.getOpenNode(idx);
        assertEquals(10, Node.getG(n1));
        assertEquals(Grid.DIRECTION_UP, grid.nodeParentDirection(2, 2));
    }

    // ============== AStar.search 平滑对比与异常路径 ==============

    @Test
    public void testAStar_Search_SmoothVsRaw_PathLength() {
        // 中文：平滑路径应不长于非平滑路径
        Grid grid = new Grid(8, 8);
        AStar astar = new AStar();
        Path raw = astar.search(0, 0, 7, 6, grid, false);
        Path smooth = astar.search(0, 0, 7, 6, grid, true);
        assertFalse(raw.isEmpty());
        assertFalse(smooth.isEmpty());
        assertTrue(smooth.size() <= raw.size());
    }

    @Test
    public void testAStar_Search_TooManyOpenNodes_ShouldClearPathAndThrow() {
        Grid grid = new Grid(5, 5);
        AStar astar = new AStar();
        Path path = new Path();
        // 中文：让内部open第一步就抛TooLongPathException
        astar.nodes.size = Grid.MAX_OPEN_NODE_SIZE; // 使第一次open直接抛异常
        try {
            astar.search(0, 0, 4, 4, grid, path, false);
            fail("should throw TooLongPathException");
        } catch (TooLongPathException e) {
            // 中文：发生异常时路径被清空，并且结构被清理
            assertTrue(path.isEmpty());
            assertTrue(astar.isCLean(grid));
        }
    }

    // ============== AStar.fillPath(ex,ey,sx,sy) 方向回溯 ==============

    @Test
    public void testAStar_FillPath_WithParentDirections_NoSmooth() {
        Grid grid = new Grid(5, 5);
        AStar astar = new AStar();
        Path path = new Path();
        // 构造父方向：终点(2,1) <- (1,1) <- (0,1) <- (0,0)
        grid.nodeParentDirectionUpdate(2, 1, Grid.DIRECTION_LEFT);
        grid.nodeParentDirectionUpdate(1, 1, Grid.DIRECTION_LEFT);
        grid.nodeParentDirectionUpdate(0, 1, Grid.DIRECTION_DOWN);

        astar.fillPath(2, 1, 0, 0, path, grid, false);
        // 路径应包含起点、折点与终点
        assertEquals(3, path.size());
        assertEquals(0, Point.getX(path.get(2))); // 起点
        assertEquals(0, Point.getY(path.get(2)));
        assertEquals(0, Point.getX(path.get(1))); // 折点(0,1)
        assertEquals(1, Point.getY(path.get(1)));
        assertEquals(2, Point.getX(path.get(0))); // 终点
        assertEquals(1, Point.getY(path.get(0)));
    }

    @Test
    public void testAStar_FillPath_WithParentDirections_Smooth() {
        Grid grid = new Grid(5, 5);
        AStar astar = new AStar();
        Path path = new Path();
        // 同一路径，但采用平滑应尽量消除中间点（如果可直达）
        grid.nodeParentDirectionUpdate(2, 1, Grid.DIRECTION_LEFT);
        grid.nodeParentDirectionUpdate(1, 1, Grid.DIRECTION_LEFT);
        grid.nodeParentDirectionUpdate(0, 1, Grid.DIRECTION_DOWN);

        astar.fillPath(2, 1, 0, 0, path, grid, true);
        // 平滑后应尽量去掉冗余点，此例(0,0)->(2,1)可直达，故仅保留起点与终点
        assertEquals(2, path.size());
        assertEquals(0, Point.getX(path.get(1))); // 起点
        assertEquals(0, Point.getY(path.get(1)));
        assertEquals(2, Point.getX(path.get(0))); // 终点
        assertEquals(1, Point.getY(path.get(0)));
    }

    // ============== Reachability 负斜率与缩放 ==============

    @Test
    public void testReachability_Diagonal_NegativeSlope_NoObstacle() {
        Grid g = new Grid(10, 10);
        assertTrue(Reachability.isReachable(0, 2, 2, 0, g));
        assertEquals(Point.toPoint(2, 0), Reachability.getClosestWalkablePointToTarget(0, 2, 2, 0, g));
    }

    @Test
    public void testReachability_WithScaleGreaterThanOne() {
        Grid g = new Grid(20, 20);
        // 中文：scale=2 时仍可正确到达
        long p = Reachability.getClosestWalkablePointToTarget(1, 1, 7, 9, 2, g);
        assertEquals(Point.toPoint(7, 9), p);
        assertTrue(Reachability.isReachable(1, 1, 7, 9, 2, g));
    }

    // ============== Nodes 多次close顺序检验 ==============

    @Test
    public void testNodes_CloseMultiple_NonDecreasingF() {
        Grid grid = new Grid(10, 10);
        Nodes nodes = new Nodes();
        nodes.map = grid;

        // 插入一批随机分布的f值
        nodes.open(0, 0, 5, 5, Grid.DIRECTION_UP);   // f=10
        nodes.open(1, 0, 3, 8, Grid.DIRECTION_UP);   // f=11
        nodes.open(2, 0, 1, 2, Grid.DIRECTION_UP);   // f=3
        nodes.open(3, 0, 6, 1, Grid.DIRECTION_UP);   // f=7
        nodes.open(4, 0, 4, 4, Grid.DIRECTION_UP);   // f=8
        nodes.open(5, 0, 7, 7, Grid.DIRECTION_UP);   // f=14
        nodes.open(6, 0, 2, 2, Grid.DIRECTION_UP);   // f=4

        int lastF = Integer.MIN_VALUE;
        while (true) {
            long n = nodes.close();
            if (n == 0) break;
            int f = Node.getF(n);
            assertTrue(f >= lastF);
            lastF = f;
        }
        assertTrue(nodes.isClean());
    }
}
