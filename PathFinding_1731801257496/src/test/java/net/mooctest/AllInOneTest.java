package net.mooctest;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * 中文注释：
 * 整合版测试用例（JUnit 4.12）。
 * 目标：
 * 1) 覆盖所有关键分支，提升分支覆盖率与变异杀死率；
 * 2) 保持测试代码可读、可维护；
 * 3) 保证运行效率，不引入不必要的等待与开销；
 *
 * 覆盖对象：AStar、Cost、Grid、Node、Nodes、Path、Point、Reachability、ThreadLocalAStar、Utils。
 */
public class AllInOneTest {

    // ================= Utils =================

    @Test
    public void testUtils_Mask_NormalValues() {
        // 中文：测试正常取掩码的场景
        assertEquals(1, Utils.mask(1));
        assertEquals(3, Utils.mask(2));
        assertEquals(-1, Utils.mask(32)); // 32位全1
    }

    @Test(expected = RuntimeException.class)
    public void testUtils_Mask_TooSmall() {
        // 中文：nbit小于1时应当抛出异常
        Utils.mask(0);
    }

    @Test(expected = RuntimeException.class)
    public void testUtils_Mask_TooLarge() {
        // 中文：nbit大于32时应当抛出异常
        Utils.mask(33);
    }

    @Test
    public void testUtils_Check_WithMessageAndFormat() {
        // 中文：check(true)不应抛异常
        Utils.check(true);

        // 中文：check(false)应抛出异常，并包含自定义信息
        try {
            Utils.check(false, "msg");
            fail("should throw");
        } catch (RuntimeException e) {
            assertEquals("msg", e.getMessage());
        }

        try {
            Utils.check(false, "x=%d,y=%d", 1, 2);
            fail("should throw");
        } catch (RuntimeException e) {
            assertEquals("x=1,y=2", e.getMessage());
        }
    }

    // ================= Point & Path =================

    @Test
    public void testPoint_ToPointAndGetters() {
        // 中文：验证Point转换与反向解析
        long p = Point.toPoint(123, 456);
        assertEquals(123, Point.getX(p));
        assertEquals(456, Point.getY(p));
    }

    @Test
    public void testPath_AddGetRemoveGrowAndIsEmpty() {
        // 中文：验证Path的添加、反向读取、移除、扩容与判空
        Path path = new Path();
        // 初始为空，size<2 即为空路径
        assertTrue(path.isEmpty());

        // 连续加入9个点，触发grow（初始容量为8）
        for (int i = 0; i < 9; i++) {
            path.add(i, i + 1);
        }
        assertEquals(9, path.size());
        // size>=2，表示存在有效路径
        assertFalse(path.isEmpty());

        // get是从尾部反向读取
        long last = path.get(0);
        assertEquals(8, Point.getX(last));
        assertEquals(9, Point.getY(last));

        long secondLast = path.get(1);
        assertEquals(7, Point.getX(secondLast));
        assertEquals(8, Point.getY(secondLast));

        // 移除一个点
        path.remove();
        assertEquals(8, path.size());

        // 清空后再次判空
        path.clear();
        assertTrue(path.isEmpty());
    }

    // ================= Node =================

    @Test
    public void testNode_ToNode_Getters_AndSetGF() {
        // 中文：创建节点并验证所有字段解析
        long n = Node.toNode(3, 5, 7, 11);
        assertEquals(3, Node.getX(n));
        assertEquals(5, Node.getY(n));
        assertEquals(7, Node.getG(n));
        assertEquals(11, Node.getF(n));

        // 中文：更新g与f，确保setGF生效
        long n2 = Node.setGF(n, 2, 9);
        assertEquals(3, Node.getX(n2));
        assertEquals(5, Node.getY(n2));
        assertEquals(2, Node.getG(n2));
        assertEquals(9, Node.getF(n2));
    }

    @Test(expected = TooLongPathException.class)
    public void testNode_ToNode_TooLongPath() {
        // 中文：当f为负数时应抛出TooLongPathException
        Node.toNode(1, 1, 1, -1);
    }

    // ================= Grid =================

    @Test
    public void testGrid_Walkable_AndBounds() {
        Grid g = new Grid(5, 4);

        // 边界外不可行走
        assertFalse(g.isWalkable(-1, 0));
        assertFalse(g.isWalkable(5, 0));
        assertFalse(g.isWalkable(0, -1));
        assertFalse(g.isWalkable(0, 4));

        // 默认可行走
        assertTrue(g.isWalkable(0, 0));
        // 设置不可行走
        g.setWalkable(0, 0, false);
        assertFalse(g.isWalkable(0, 0));
        // 再次设置可行走
        g.setWalkable(0, 0, true);
        assertTrue(g.isWalkable(0, 0));
    }

    @Test
    public void testGrid_Info_OpenCloseAndParentDirection() {
        Grid g = new Grid(3, 3);

        // 初始干净
        assertTrue(g.isClean());

        // 更新父方向并检查
        g.nodeParentDirectionUpdate(1, 1, 3);
        assertEquals(3, g.nodeParentDirection(1, 1));

        // 更新开放节点索引
        g.openNodeIdxUpdate(1, 1, 5);
        int info = g.info(1, 1);
        // 取出索引（5存储的是idx+1，因此openNodeIdx需要-1恢复）
        assertEquals(5, (info & Grid.NODE_MASK));
        assertEquals(4, Grid.openNodeIdx(info));

        // 关闭节点
        g.nodeClosed(1, 1);
        assertTrue(Grid.isClosedNode(g.info(1, 1)));

        // 清理（仅保留可行走标记）
        g.clear();
        assertTrue(g.isClean());
    }

    // ================= Nodes =================

    @Test
    public void testNodes_Open_Close_MinHeapOrder_AndGrow() {
        Grid grid = new Grid(10, 10);
        Nodes nodes = new Nodes();
        nodes.map = grid;

        // 打开多个节点，f = g + h，设计不同f以测试堆序
        nodes.open(1, 1, 5, 5, Grid.DIRECTION_UP);    // f=10
        nodes.open(2, 2, 1, 1, Grid.DIRECTION_UP);    // f=2  最小
        nodes.open(3, 3, 4, 7, Grid.DIRECTION_UP);    // f=11
        nodes.open(4, 4, 2, 3, Grid.DIRECTION_UP);    // f=5

        // 触发扩容：默认容量16，这里额外添加一些点，确保grow逻辑覆盖
        for (int i = 0; i < 20; i++) {
            nodes.open(5 + i, 5, 10 + i, 20 + i, Grid.DIRECTION_UP);
        }
        assertTrue(nodes.nodes.length >= 24); // 已扩容

        // 关闭应返回最小f的节点(2,2)
        long n = nodes.close();
        assertEquals(2, Node.getX(n));
        assertEquals(2, Node.getY(n));
        // 被关闭的节点应在grid中标记为closed
        assertTrue(Grid.isClosedNode(grid.info(2, 2)));
    }

    @Test
    public void testNodes_Close_EmptyReturnsZero() {
        Nodes nodes = new Nodes();
        // 未设置map也应当安全（size==0时直接返回0）
        assertEquals(0, nodes.close());
    }

    @Test(expected = TooLongPathException.class)
    public void testNodes_Open_TooManyOpenNodes_Throws() {
        Grid grid = new Grid(3, 3);
        Nodes nodes = new Nodes();
        nodes.map = grid;
        // 人工设置size达到上限，触发异常分支
        nodes.size = Grid.MAX_OPEN_NODE_SIZE;
        nodes.open(0, 0, 0, 0, Grid.DIRECTION_UP);
    }

    // ================= Reachability =================

    @Test(expected = IllegalArgumentException.class)
    public void testReachability_Scale_Illegal() {
        // 中文：比例小于1应抛异常
        Reachability.getClosestWalkablePointToTarget(0, 0, 1, 1, 0, new Grid(5, 5));
    }

    @Test
    public void testReachability_StartCellUnwalkable() {
        // 中文：起始格不可行走，则最近点为起点
        Grid g = new Grid(5, 5);
        g.setWalkable(0, 0, false);
        long p = Reachability.getClosestWalkablePointToTarget(0, 0, 3, 0, g);
        assertEquals(Point.toPoint(0, 0), p);
        assertFalse(Reachability.isReachable(0, 0, 3, 0, g));
    }

    @Test
    public void testReachability_SameGrid_WithFenceBlockedAndPass() {
        // 中文：同一格子内，有Fence阻断则返回起点；无阻断返回终点
        Grid g = new Grid(5, 5);
        Fence fence = (x1, y1, x2, y2) -> false;
        long p = Reachability.getClosestWalkablePointToTarget(1, 1, 1, 1, 1, g, fence);
        assertEquals(Point.toPoint(1, 1), p);

        Fence pass = (x1, y1, x2, y2) -> true;
        long p2 = Reachability.getClosestWalkablePointToTarget(1, 1, 1, 1, 1, g, pass);
        assertEquals(Point.toPoint(1, 1), p2);
    }

    @Test
    public void testReachability_Horizontal_Line_BlockedImmediatelyAndLater() {
        Grid g = new Grid(10, 3);
        int y = 1;
        // 立即阻断：起点右侧第一格不可走 -> 返回起点
        g.setWalkable(1, y, false);
        long p = Reachability.getClosestWalkablePointToTarget(0, y, 5, y, g);
        assertEquals(Point.toPoint(0, y), p);

        // 取消第一格阻断，改为更远处阻断 -> 返回阻断前的格子
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
    public void testReachability_Vertical_Line_BlockedImmediatelyAndLater() {
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
    public void testReachability_Diagonal_NoObstacle_PositiveSlope_ExactCorner() {
        // 中文：斜线（k>0）穿过网格交点时，不需要额外检查
        Grid g = new Grid(10, 10);
        assertTrue(Reachability.isReachable(0, 0, 2, 2, g));
        assertEquals(Point.toPoint(2, 2), Reachability.getClosestWalkablePointToTarget(0, 0, 2, 2, g));
    }

    @Test
    public void testReachability_Diagonal_WithFenceInterrupt() {
        // 中文：斜线路径中途被Fence打断，应回退到上一个检查点
        Grid g = new Grid(10, 10);
        Fence blockAllButEnd = (x1, y1, x2, y2) -> x2 == 5 && y2 == 5; // 仅允许终点坐标
        long p = Reachability.getClosestWalkablePointToTarget(0, 0, 8, 7, 1, g, blockAllButEnd);
        // 由于Fence几乎全阻断，返回点必非终点
        assertNotEquals(Point.toPoint(8, 7), p);
    }

    @Test
    public void testReachability_Diagonal_StepYBranch_NoObstacle() {
        // 中文：|dy|>|dx| 走y步进分支
        Grid g = new Grid(10, 10);
        long p = Reachability.getClosestWalkablePointToTarget(1, 1, 2, 6, g);
        assertEquals(Point.toPoint(2, 6), p);
    }

    @Test
    public void testReachability_ScaleHelpers() {
        // 中文：辅助方法的正确性
        assertEquals(2.5, Reachability.scaleDown(5, 2), 1e-9);
        assertEquals(5, Reachability.scaleUp(2, 2)); // 中心点：2*2+1=5
        assertEquals(7, Reachability.scaleUp(3.5, 2));
        assertEquals(Point.toPoint(5, 7), Reachability.scaleUpPoint(2.0, 3.5, 2));
    }

    // ================= ThreadLocalAStar =================

    @Test
    public void testThreadLocal_Current_SameThreadSameInstance() {
        // 中文：同一线程多次获取应为同一实例
        AStar a1 = ThreadLocalAStar.current();
        AStar a2 = ThreadLocalAStar.current();
        assertSame(a1, a2);
    }

    @Test
    public void testThreadLocal_Current_DifferentThreadDifferentInstance() throws Exception {
        final AStar mainThreadInstance = ThreadLocalAStar.current();
        final AtomicReference<AStar> ref = new AtomicReference<>();
        Thread t = new Thread(() -> ref.set(ThreadLocalAStar.current()));
        t.start();
        t.join();
        assertNotSame(mainThreadInstance, ref.get());
    }

    // ================= Cost =================

    @Test
    public void testCost_HCost() {
        // 中文：同一位置，启发式成本为0
        assertEquals(0, Cost.hCost(0, 0, 0, 0));
        // 中文：水平移动两格：|dx|=2，成本为2*5
        assertEquals(10, Cost.hCost(0, 0, 2, 0));
        // 中文：竖直移动三格：|dy|=3，成本为3*5
        assertEquals(15, Cost.hCost(0, 0, 0, 3));
        // 中文：L型移动：|dx|+|dy|=5，成本为25
        assertEquals(25, Cost.hCost(1, 2, 4, 3));
    }

    // ================= A* =================

    @Test
    public void testAStar_Search_NoObstacles_ShouldReturnPath_AndClean() {
        // 中文：无障碍时应当成功寻路，且完成后数据结构清理干净
        Grid grid = new Grid(6, 6);
        AStar astar = new AStar();
        Path path = astar.search(0, 0, 5, 5, grid);
        assertFalse(path.isEmpty());
        assertTrue(astar.isCLean(grid));
    }

    @Test
    public void testAStar_Search_StartOrEndBlocked_OrSamePoint() {
        Grid grid = new Grid(4, 4);
        AStar astar = new AStar();

        // 起点被封锁
        grid.setWalkable(0, 0, false);
        Path p1 = astar.search(0, 0, 3, 3, grid);
        assertTrue(p1.isEmpty());

        // 终点被封锁
        grid.setWalkable(0, 0, true);
        grid.setWalkable(3, 3, false);
        Path p2 = astar.search(0, 0, 3, 3, grid);
        assertTrue(p2.isEmpty());

        // 起终点相同
        grid.setWalkable(3, 3, true);
        Path p3 = astar.search(1, 1, 1, 1, grid);
        assertTrue(p3.isEmpty());
    }

    @Test
    public void testAStar_FillPath_Smooth_False_And_True() {
        // 中文：直接测试fillPath的平滑分支
        Grid grid = new Grid(10, 10);
        AStar astar = new AStar();
        Path path = new Path();

        // 先加入两个点，形成一段路径
        path.add(0, 0);
        path.add(1, 1);

        // 不平滑：应直接追加新点
        astar.fillPath(2, 2, path, grid, false);
        assertEquals(3, path.size());
        assertEquals(2, Point.getX(path.get(0)));
        assertEquals(2, Point.getY(path.get(0)));

        // 清空后测试平滑：中间点(1,1)会被消去，仅保留(0,0)->(2,2)
        path.clear();
        path.add(0, 0);
        path.add(1, 1);
        astar.fillPath(2, 2, path, grid, true);
        assertEquals(2, path.size());
        assertEquals(2, Point.getX(path.get(0)));
        assertEquals(2, Point.getY(path.get(0)));
        assertEquals(0, Point.getX(path.get(1)));
        assertEquals(0, Point.getY(path.get(1)));

        // 再构造一个不可直达的案例：阻断(1,1)，则应当无法平滑，直接添加新点
        path.clear();
        path.add(0, 0);
        path.add(1, 1);
        grid.setWalkable(1, 1, false);
        astar.fillPath(2, 2, path, grid, true);
        assertEquals(3, path.size());
        grid.setWalkable(1, 1, true);
    }

    @Test
    public void testAStar_Open_Branches_RightDown_LeftUp_Checks() {
        Grid grid = new Grid(5, 5);
        AStar astar = new AStar();
        // 准备内部Nodes对map的引用
        astar.nodes.map = grid;

        // DIRECTION_RIGHT_DOWN 分支：检测(x+1,y)不可行走则直接返回，不应打开节点
        grid.setWalkable(2 + 1, 2, false);
        astar.open(2, 2, 10, Grid.DIRECTION_RIGHT_DOWN, 4, 4, grid);
        assertEquals(0, grid.info(2, 2)); // 未被标记为开放
        grid.setWalkable(3, 2, true);

        // DIRECTION_LEFT_UP 分支：检测(x,y+1)不可行走则直接返回
        grid.setWalkable(2, 2 + 1, false);
        astar.open(2, 2, 10, Grid.DIRECTION_LEFT_UP, 4, 4, grid);
        assertEquals(0, grid.info(2, 2));
        grid.setWalkable(2, 3, true);

        // 其他方向：应成功打开空节点
        astar.open(2, 2, 8, Grid.DIRECTION_UP, 4, 4, grid);
        int info = grid.info(2, 2);
        assertTrue(info != 0); // 已打开
        assertFalse(Grid.isClosedNode(info));

        // 将节点标记为已关闭，再次open应无效果
        grid.nodeClosed(2, 2);
        int before = grid.info(2, 2);
        astar.open(2, 2, 5, Grid.DIRECTION_UP, 4, 4, grid);
        assertEquals(before, grid.info(2, 2));
    }

    @Test
    public void testAStar_Open_UpdateExistingOpenNode_GBetterOrWorse() {
        Grid grid = new Grid(6, 6);
        AStar astar = new AStar();
        astar.nodes.map = grid;

        // 先打开一个节点(1,1)，g=10
        astar.open(1, 1, 10, Grid.DIRECTION_UP, 5, 5, grid);
        int idxInfo = grid.info(1, 1);
        assertTrue(idxInfo != 0);
        int idx = Grid.openNodeIdx(idxInfo);
        long n0 = astar.nodes.getOpenNode(idx);
        assertEquals(10, Node.getG(n0));

        // g更差（>=ng）: 不更新
        astar.open(1, 1, 12, Grid.DIRECTION_LEFT, 5, 5, grid);
        long n1 = astar.nodes.getOpenNode(idx);
        assertEquals(10, Node.getG(n1));
        // 父方向不变（仍为第一次设置的UP）
        assertEquals(Grid.DIRECTION_UP, grid.nodeParentDirection(1, 1));

        // g更优（<ng）: 应更新g与f，并更新父方向
        astar.open(1, 1, 3, Grid.DIRECTION_RIGHT, 5, 5, grid);
        long n2 = astar.nodes.getOpenNode(Grid.openNodeIdx(grid.info(1, 1)));
        assertEquals(3, Node.getG(n2));
        assertEquals(Grid.DIRECTION_RIGHT, grid.nodeParentDirection(1, 1));
    }

    // ================= 额外强化用例 =================

    @Test(expected = RuntimeException.class)
    public void testGrid_Constructor_WidthZero_ShouldThrow() {
        new Grid(0, 5);
    }

    @Test(expected = RuntimeException.class)
    public void testGrid_Constructor_HeightZero_ShouldThrow() {
        new Grid(5, 0);
    }

    @Test
    public void testPoint_NegativeCoordinates_RoundTrip() {
        long p = Point.toPoint(-7, -9);
        assertEquals(-7, Point.getX(p));
        assertEquals(-9, Point.getY(p));
    }

    @Test
    public void testAStar_Open_Unwalkable_ShouldNoOp() {
        Grid grid = new Grid(4, 4);
        AStar astar = new AStar();
        astar.nodes.map = grid;
        grid.setWalkable(1, 1, false);
        astar.open(1, 1, 5, Grid.DIRECTION_UP, 3, 3, grid);
        assertEquals(0, grid.info(1, 1));
    }

    @Test
    public void testAStar_Open_EqualG_NoUpdate() {
        Grid grid = new Grid(6, 6);
        AStar astar = new AStar();
        astar.nodes.map = grid;

        astar.open(2, 2, 10, Grid.DIRECTION_UP, 5, 5, grid);
        int idx = Grid.openNodeIdx(grid.info(2, 2));
        long n0 = astar.nodes.getOpenNode(idx);
        assertEquals(10, Node.getG(n0));
        assertEquals(Grid.DIRECTION_UP, grid.nodeParentDirection(2, 2));

        astar.open(2, 2, 10, Grid.DIRECTION_LEFT, 5, 5, grid);
        long n1 = astar.nodes.getOpenNode(idx);
        assertEquals(10, Node.getG(n1));
        assertEquals(Grid.DIRECTION_UP, grid.nodeParentDirection(2, 2));
    }

    @Test
    public void testAStar_Search_SmoothVsRaw_PathLength() {
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
        astar.nodes.size = Grid.MAX_OPEN_NODE_SIZE;
        try {
            astar.search(0, 0, 4, 4, grid, path, false);
            fail("should throw TooLongPathException");
        } catch (TooLongPathException e) {
            assertTrue(path.isEmpty());
            assertTrue(astar.isCLean(grid));
        }
    }

    @Test
    public void testAStar_FillPath_WithParentDirections_NoSmooth() {
        Grid grid = new Grid(5, 5);
        AStar astar = new AStar();
        Path path = new Path();
        grid.nodeParentDirectionUpdate(2, 1, Grid.DIRECTION_LEFT);
        grid.nodeParentDirectionUpdate(1, 1, Grid.DIRECTION_LEFT);
        grid.nodeParentDirectionUpdate(0, 1, Grid.DIRECTION_DOWN);

        astar.fillPath(2, 1, 0, 0, path, grid, false);
        assertEquals(3, path.size());
        assertEquals(0, Point.getX(path.get(2)));
        assertEquals(0, Point.getY(path.get(2)));
        assertEquals(0, Point.getX(path.get(1)));
        assertEquals(1, Point.getY(path.get(1)));
        assertEquals(2, Point.getX(path.get(0)));
        assertEquals(1, Point.getY(path.get(0)));
    }

    @Test
    public void testAStar_FillPath_WithParentDirections_Smooth() {
        Grid grid = new Grid(5, 5);
        AStar astar = new AStar();
        Path path = new Path();
        grid.nodeParentDirectionUpdate(2, 1, Grid.DIRECTION_LEFT);
        grid.nodeParentDirectionUpdate(1, 1, Grid.DIRECTION_LEFT);
        grid.nodeParentDirectionUpdate(0, 1, Grid.DIRECTION_DOWN);

        astar.fillPath(2, 1, 0, 0, path, grid, true);
        assertEquals(2, path.size());
        assertEquals(0, Point.getX(path.get(1)));
        assertEquals(0, Point.getY(path.get(1)));
        assertEquals(2, Point.getX(path.get(0)));
        assertEquals(1, Point.getY(path.get(0)));
    }

    @Test
    public void testReachability_Diagonal_NegativeSlope_NoObstacle() {
        Grid g = new Grid(10, 10);
        assertTrue(Reachability.isReachable(0, 2, 2, 0, g));
        assertEquals(Point.toPoint(2, 0), Reachability.getClosestWalkablePointToTarget(0, 2, 2, 0, g));
    }

    @Test
    public void testReachability_WithScaleGreaterThanOne() {
        Grid g = new Grid(20, 20);
        long p = Reachability.getClosestWalkablePointToTarget(1, 1, 7, 9, 2, g);
        assertEquals(Point.toPoint(7, 9), p);
        assertTrue(Reachability.isReachable(1, 1, 7, 9, 2, g));
    }

    @Test
    public void testNodes_CloseMultiple_NonDecreasingF() {
        Grid grid = new Grid(10, 10);
        Nodes nodes = new Nodes();
        nodes.map = grid;

        nodes.open(0, 0, 5, 5, Grid.DIRECTION_UP);
        nodes.open(1, 0, 3, 8, Grid.DIRECTION_UP);
        nodes.open(2, 0, 1, 2, Grid.DIRECTION_UP);
        nodes.open(3, 0, 6, 1, Grid.DIRECTION_UP);
        nodes.open(4, 0, 4, 4, Grid.DIRECTION_UP);
        nodes.open(5, 0, 7, 7, Grid.DIRECTION_UP);
        nodes.open(6, 0, 2, 2, Grid.DIRECTION_UP);

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

    @Test
    public void testAStar_Search_Unreachable_ShouldReturnEmptyAndClean() {
        Grid grid = new Grid(3, 3);
        AStar astar = new AStar();
        grid.setWalkable(0, 1, false);
        grid.setWalkable(1, 0, false);
        grid.setWalkable(1, 1, false);
        Path p = astar.search(0, 0, 2, 2, grid);
        assertTrue(p.isEmpty());
        assertTrue(astar.isCLean(grid));
    }

    @Test
    public void testReachability_Horizontal_NoObstacle_ReturnEnd() {
        Grid g = new Grid(10, 3);
        long p = Reachability.getClosestWalkablePointToTarget(0, 1, 5, 1, g);
        assertEquals(Point.toPoint(5, 1), p);
    }

    @Test
    public void testReachability_Vertical_NoObstacle_ReturnEnd() {
        Grid g = new Grid(3, 10);
        long p = Reachability.getClosestWalkablePointToTarget(1, 0, 1, 5, g);
        assertEquals(Point.toPoint(1, 5), p);
    }

    @Test
    public void testUtils_CheckFalse_NoMsg() {
        try {
            Utils.check(false);
            fail("should throw");
        } catch (RuntimeException e) {
            assertNull(e.getMessage());
        }
    }
}
