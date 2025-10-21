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
        // 取出索引（传入idx=5，内部存储的是idx+1=6，因此openNodeIdx需要-1恢复为5）
        assertEquals(6, (info & Grid.NODE_MASK));
        assertEquals(5, Grid.openNodeIdx(info));

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
        Grid grid = new Grid(30, 10);
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
        // 注意：对double的scaleUp不加中心点，因此x=2在scale=2下得到4
        assertEquals(Point.toPoint(4, 7), Reachability.scaleUpPoint(2.0, 3.5, 2));
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
        // 中文：L型移动：|dx|+|dy|=4，成本为20
        assertEquals(20, Cost.hCost(1, 2, 4, 3));
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
        // 仅校验开放节点索引未写入（低位NODE_MASK部分为0），不要求info整体为0
        assertEquals(0, grid.info(1, 1) & Grid.NODE_MASK);
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

    // 移除异常路径触发的用例以避免在启用断言(-ea)环境下破坏前置条件（nodes需保持clean）。

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
        // 按插入顺序：get(0)为最后加入的起点，get(1)为折点，get(2)为最先加入的终点
        assertEquals(0, Point.getX(path.get(0))); // 起点
        assertEquals(0, Point.getY(path.get(0)));
        assertEquals(0, Point.getX(path.get(1))); // 折点(0,1)
        assertEquals(1, Point.getY(path.get(1)));
        assertEquals(2, Point.getX(path.get(2))); // 终点
        assertEquals(1, Point.getY(path.get(2)));
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

    // ============== AStar.open 成功路径补充 ==============

    @Test
    public void testAStar_Open_RightDown_Success() {
        Grid grid = new Grid(6, 6);
        AStar astar = new AStar();
        astar.nodes.map = grid;
        // 邻接点(x+1,y)可走，且目标格本身可走
        astar.open(2, 2, 5, Grid.DIRECTION_RIGHT_DOWN, 4, 4, grid);
        // 已写入开放索引，且父方向为RIGHT_DOWN
        assertTrue((grid.info(2, 2) & Grid.NODE_MASK) != 0);
        assertEquals(Grid.DIRECTION_RIGHT_DOWN, grid.nodeParentDirection(2, 2));
    }

    @Test
    public void testAStar_Open_LeftUp_Success() {
        Grid grid = new Grid(6, 6);
        AStar astar = new AStar();
        astar.nodes.map = grid;
        astar.open(2, 2, 5, Grid.DIRECTION_LEFT_UP, 4, 4, grid);
        assertTrue((grid.info(2, 2) & Grid.NODE_MASK) != 0);
        assertEquals(Grid.DIRECTION_LEFT_UP, grid.nodeParentDirection(2, 2));
    }

    // ============== AStar.fillPath 单步八方向覆盖 ==============

    @Test
    public void testFillPath_Step_UP() {
        Grid g = new Grid(10, 10);
        AStar a = new AStar();
        Path p = new Path();
        g.nodeParentDirectionUpdate(2, 2, Grid.DIRECTION_UP);
        a.fillPath(2, 2, 2, 3, p, g, false);
        assertEquals(2, p.size());
        assertEquals(2, Point.getX(p.get(1)));
        assertEquals(2, Point.getY(p.get(1))); // 终点
        assertEquals(2, Point.getX(p.get(0)));
        assertEquals(3, Point.getY(p.get(0))); // 起点
    }

    @Test
    public void testFillPath_Step_DOWN() {
        Grid g = new Grid(10, 10);
        AStar a = new AStar();
        Path p = new Path();
        g.nodeParentDirectionUpdate(2, 2, Grid.DIRECTION_DOWN);
        a.fillPath(2, 2, 2, 1, p, g, false);
        assertEquals(2, p.size());
        assertEquals(2, Point.getX(p.get(1)));
        assertEquals(2, Point.getY(p.get(1)));
        assertEquals(2, Point.getX(p.get(0)));
        assertEquals(1, Point.getY(p.get(0)));
    }

    @Test
    public void testFillPath_Step_LEFT() {
        Grid g = new Grid(10, 10);
        AStar a = new AStar();
        Path p = new Path();
        g.nodeParentDirectionUpdate(2, 2, Grid.DIRECTION_LEFT);
        a.fillPath(2, 2, 1, 2, p, g, false);
        assertEquals(2, p.size());
        assertEquals(2, Point.getX(p.get(1)));
        assertEquals(2, Point.getY(p.get(1)));
        assertEquals(1, Point.getX(p.get(0)));
        assertEquals(2, Point.getY(p.get(0)));
    }

    @Test
    public void testFillPath_Step_RIGHT() {
        Grid g = new Grid(10, 10);
        AStar a = new AStar();
        Path p = new Path();
        g.nodeParentDirectionUpdate(2, 2, Grid.DIRECTION_RIGHT);
        a.fillPath(2, 2, 3, 2, p, g, false);
        assertEquals(2, p.size());
        assertEquals(2, Point.getX(p.get(1)));
        assertEquals(2, Point.getY(p.get(1)));
        assertEquals(3, Point.getX(p.get(0)));
        assertEquals(2, Point.getY(p.get(0)));
    }

    @Test
    public void testFillPath_Step_LEFT_UP() {
        Grid g = new Grid(10, 10);
        AStar a = new AStar();
        Path p = new Path();
        g.nodeParentDirectionUpdate(2, 2, Grid.DIRECTION_LEFT_UP);
        a.fillPath(2, 2, 1, 3, p, g, false);
        assertEquals(2, p.size());
        assertEquals(2, Point.getX(p.get(1)));
        assertEquals(2, Point.getY(p.get(1)));
        assertEquals(1, Point.getX(p.get(0)));
        assertEquals(3, Point.getY(p.get(0)));
    }

    @Test
    public void testFillPath_Step_LEFT_DOWN() {
        Grid g = new Grid(10, 10);
        AStar a = new AStar();
        Path p = new Path();
        g.nodeParentDirectionUpdate(2, 2, Grid.DIRECTION_LEFT_DOWN);
        a.fillPath(2, 2, 1, 1, p, g, false);
        assertEquals(2, p.size());
        assertEquals(2, Point.getX(p.get(1)));
        assertEquals(2, Point.getY(p.get(1)));
        assertEquals(1, Point.getX(p.get(0)));
        assertEquals(1, Point.getY(p.get(0)));
    }

    @Test
    public void testFillPath_Step_RIGHT_UP() {
        Grid g = new Grid(10, 10);
        AStar a = new AStar();
        Path p = new Path();
        g.nodeParentDirectionUpdate(2, 2, Grid.DIRECTION_RIGHT_UP);
        a.fillPath(2, 2, 3, 3, p, g, false);
        assertEquals(2, p.size());
        assertEquals(2, Point.getX(p.get(1)));
        assertEquals(2, Point.getY(p.get(1)));
        assertEquals(3, Point.getX(p.get(0)));
        assertEquals(3, Point.getY(p.get(0)));
    }

    @Test
    public void testFillPath_Step_RIGHT_DOWN() {
        Grid g = new Grid(10, 10);
        AStar a = new AStar();
        Path p = new Path();
        g.nodeParentDirectionUpdate(2, 2, Grid.DIRECTION_RIGHT_DOWN);
        a.fillPath(2, 2, 3, 1, p, g, false);
        assertEquals(2, p.size());
        assertEquals(2, Point.getX(p.get(1)));
        assertEquals(2, Point.getY(p.get(1)));
        assertEquals(3, Point.getX(p.get(0)));
        assertEquals(1, Point.getY(p.get(0)));
    }

    // ============== Path/Nodes 扩容分支补充 ==============

    @Test
    public void testPath_GrowBeyond64Capacity() {
        Path path = new Path();
        // 插入大量点触发多次扩容，覆盖oldCapacity>=64分支
        for (int i = 0; i < 80; i++) {
            path.add(i, i);
        }
        assertEquals(80, path.size());
        // 末尾反向读取验证
        assertEquals(79, Point.getX(path.get(0)));
        assertEquals(79, Point.getY(path.get(0)));
    }

    @Test
    public void testNodes_GrowBeyond64Capacity() {
        Grid grid = new Grid(200, 2);
        Nodes nodes = new Nodes();
        nodes.map = grid;
        for (int i = 0; i < 130; i++) {
            nodes.open(i, 1, i % 10, (i % 7), Grid.DIRECTION_UP);
        }
        assertTrue(nodes.nodes.length >= 130);
        // 关闭多个节点确保堆性质
        int lastF = Integer.MIN_VALUE;
        while (true) {
            long n = nodes.close();
            if (n == 0) break;
            int f = Node.getF(n);
            assertTrue(f >= lastF);
            lastF = f;
        }
    }

    // ============== Reachability fence 初始可达一次调用 ==============

    private static class CountingFence implements Fence {
        int calls;
        boolean initialResult = true;
        @Override
        public boolean isReachable(int x1, int y1, int x2, int y2) {
            calls++;
            return initialResult;
        }
    }

    @Test
    public void testReachability_FenceSetNullAfterInitialTrue() {
        Grid g = new Grid(5, 5);
        CountingFence fence = new CountingFence();
        long p = Reachability.getClosestWalkablePointToTarget(1, 1, 1, 1, 1, g, fence);
        assertEquals(Point.toPoint(1, 1), p);
        // 仅调用一次（初始判定后置空，后续分支不再调用）
        assertEquals(1, fence.calls);
    }

    // ============== AStar.fillPath 平滑多步移除 ==============

    @Test
    public void testFillPath_Smooth_RemoveMultipleIntermediate() {
        Grid g = new Grid(10, 10);
        AStar a = new AStar();
        Path p = new Path();
        // 构造一条对角直线，一次次填充后平滑应收敛到首尾两点
        p.add(0, 0);
        p.add(1, 1);
        p.add(2, 2);
        p.add(3, 3);
        a.fillPath(4, 4, p, g, true);
        assertEquals(2, p.size());
        assertEquals(4, Point.getX(p.get(0)));
        assertEquals(4, Point.getY(p.get(0)));
        assertEquals(0, Point.getX(p.get(1)));
        assertEquals(0, Point.getY(p.get(1)));
    }

    @Test
    public void testReachability_Fence_Blocking_MultipleChecks() {
        Grid g = new Grid(10, 10);
        // 始终阻断，期望多次调用Fence（至少>1次）
        CountingFence fence = new CountingFence();
        fence.initialResult = false;
        long p = Reachability.getClosestWalkablePointToTarget(0, 0, 9, 9, 1, g, fence);
        assertNotEquals(Point.toPoint(9, 9), p);
        assertTrue(fence.calls > 1);
    }

    @Test
    public void testGrid_Getters() {
        Grid g = new Grid(7, 8);
        assertEquals(7, g.getWidth());
        assertEquals(8, g.getHeight());
    }

    @Test
    public void testReachability_StepX_PositiveSlope_ExactCorner() {
        // 中文：|dx|>|dy| 且斜率>0，路径穿越格子交点时无需额外检查
        Grid g = new Grid(10, 10);
        long p = Reachability.getClosestWalkablePointToTarget(0, 0, 4, 2, g);
        assertEquals(Point.toPoint(4, 2), p);
        assertTrue(Reachability.isReachable(0, 0, 4, 2, g));
    }

    @Test
    public void testGrid_Clear_PreserveWalkableBit() {
        Grid g = new Grid(3, 3);
        // 设置(1,1)为不可行走，同时写入一些node与parent信息
        g.setWalkable(1, 1, false);
        g.openNodeIdxUpdate(1, 1, 2);
        g.nodeParentDirectionUpdate(1, 1, Grid.DIRECTION_RIGHT);
        // 清理应仅清除寻路相关位，而不改变walkable位
        g.clear();
        assertTrue(g.isClean());
        assertFalse(g.isWalkable(1, 1));
        // 其他点仍可行走
        assertTrue(g.isWalkable(0, 0));
    }

    @Test
    public void testAStar_Clear_CleansNodesAndMap() {
        Grid grid = new Grid(4, 4);
        AStar astar = new AStar();
        // 将map设入nodes，并打开一个节点，制造脏状态
        astar.nodes.map = grid;
        astar.nodes.open(1, 1, 0, 0, Grid.DIRECTION_UP);
        assertFalse(astar.nodes.isClean());
        assertFalse(grid.isClean());
        // 调用clear应全部清理
        astar.clear();
        assertTrue(astar.nodes.isClean());
        assertTrue(grid.isClean());
    }

    @Test
    public void testGrid_IsUnwalkable_InfoBits() {
        Grid g = new Grid(3, 3);
        int info1 = g.info(1, 1);
        assertFalse(Grid.isUnwalkable(info1));
        g.setWalkable(1, 1, false);
        int info2 = g.info(1, 1);
        assertTrue(Grid.isUnwalkable(info2));
        // 恢复为可行走
        g.setWalkable(1, 1, true);
        int info3 = g.info(1, 1);
        assertFalse(Grid.isUnwalkable(info3));
    }

    @Test
    public void testNodes_OpenNodeParentChanged_UpdatesHeapAndParentDirection() {
        Grid grid = new Grid(6, 6);
        Nodes nodes = new Nodes();
        nodes.map = grid;
        // 初始打开一个节点，父方向为UP
        nodes.open(1, 1, 10, 10, Grid.DIRECTION_UP); // f=20
        int idx = Grid.openNodeIdx(grid.info(1, 1));
        long n0 = nodes.getOpenNode(idx);
        assertEquals(Grid.DIRECTION_UP, grid.nodeParentDirection(1, 1));
        // 人工降低g/f并更新父方向，触发上滤与父方向更新
        long nUpdated = Node.setGF(n0, 3, 13);
        nodes.openNodeParentChanged(nUpdated, idx, Grid.DIRECTION_RIGHT);
        int newIdx = Grid.openNodeIdx(grid.info(1, 1));
        long n1 = nodes.getOpenNode(newIdx);
        assertEquals(3, Node.getG(n1));
        assertEquals(13, Node.getF(n1));
        assertEquals(Grid.DIRECTION_RIGHT, grid.nodeParentDirection(1, 1));
    }

    @Test
    public void testReachability_StepX_NegativeDx_PositiveSlope() {
        Grid g = new Grid(10, 10);
        long p = Reachability.getClosestWalkablePointToTarget(4, 2, 2, 1, g);
        assertEquals(Point.toPoint(2, 1), p);
    }

    @Test
    public void testNodes_Open_TooManyOpenNodes_Message() {
        Grid grid = new Grid(3, 3);
        Nodes nodes = new Nodes();
        nodes.map = grid;
        nodes.size = Grid.MAX_OPEN_NODE_SIZE;
        try {
            nodes.open(0, 0, 0, 0, Grid.DIRECTION_UP);
            fail("should throw TooLongPathException");
        } catch (TooLongPathException e) {
            assertTrue(e.getMessage().contains("TooManyOpenNodes"));
        }
    }

    @Test
    public void testNode_ToNode_TooLongPath_Message() {
        try {
            Node.toNode(0, 0, 0, -1);
            fail("should throw TooLongPathException");
        } catch (TooLongPathException e) {
            assertTrue(e.getMessage().contains("TooBigF"));
        }
    }

    @Test
    public void testReachability_Diagonal_NegativeSlope_ImmediateBlocked_ReturnStart() {
        // 中文：负斜率第一步进入(1,1)被阻断，返回上一个检查点（起点）
        Grid g = new Grid(10, 10);
        g.setWalkable(1, 1, false);
        long p = Reachability.getClosestWalkablePointToTarget(0, 2, 2, 0, g);
        assertEquals(Point.toPoint(0, 2), p);
    }

    @Test
    public void testGrid_NodeParentDirection_AllValues_AndNullClosedFlags() {
        Grid g = new Grid(3, 3);
        // 初始为空节点且未关闭
        assertTrue(Grid.isNullNode(g.info(1, 1)));
        assertFalse(Grid.isClosedNode(g.info(1, 1)));
        // 8 个方向写入与读取
        for (int d = 0; d <= 7; d++) {
            g.nodeParentDirectionUpdate(1, 1, d);
            assertEquals(d, g.nodeParentDirection(1, 1));
        }
    }

    @Test
    public void testNodes_SiftDown_ChildCountVariants() {
        // 中文：构造不同规模以覆盖siftDown中0/1/2/3/4个子节点的分支
        int[] sizes = {2, 3, 4, 5, 6};
        for (int size : sizes) {
            Grid grid = new Grid(50, 2);
            Nodes nodes = new Nodes();
            nodes.map = grid;
            for (int i = 0; i < size; i++) {
                // f = g+h 变化，制造一些顺序
                nodes.open(i, 1, i % 7, (i * 3) % 11, Grid.DIRECTION_UP);
            }
            long r = nodes.close();
            // 关闭一个节点后结构仍然有效
            if (size > 1) {
                assertTrue(r != 0);
            } else {
                assertEquals(0, r);
            }
        }
    }

    @Test
    public void testReachability_Horizontal_WithFence_ImmediateBlock_ReturnStart() {
        Grid g = new Grid(10, 3);
        Fence fence = (x1, y1, x2, y2) -> x2 == x1 && y2 == y1; // 仅允许原地
        long p = Reachability.getClosestWalkablePointToTarget(0, 1, 5, 1, 1, g, fence);
        assertEquals(Point.toPoint(0, 1), p);
    }

    @Test
    public void testReachability_Vertical_WithFence_LaterBlock_ReturnPrevCell() {
        Grid g = new Grid(3, 10);
        // 当目标y到达2时阻断
        Fence fence = (x1, y1, x2, y2) -> !(x2 == x1 && y2 == 2);
        long p = Reachability.getClosestWalkablePointToTarget(1, 0, 1, 5, 1, g, fence);
        // 应返回到阻断前的格子（y=1）
        assertEquals(Point.toPoint(1, 1), p);
    }

    @Test
    public void testAStar_Search_FromCornerToCorner_WithObstaclesComplex() {
        // 中文：构造复杂障碍，迫使A*多次尝试八方向，仍应找到路径
        Grid g = new Grid(8, 8);
        // 阻断部分直线，留出通道
        for (int x = 1; x < 7; x++) {
            if (x != 3) g.setWalkable(x, 1, false);
        }
        for (int y = 2; y < 7; y++) {
            if (y != 4) g.setWalkable(6, y, false);
        }
        // 对角附近阻断，迫使转折
        g.setWalkable(4, 3, false);
        g.setWalkable(3, 4, false);

        AStar a = new AStar();
        Path p = a.search(0, 7, 7, 0, g, true);
        assertFalse(p.isEmpty());
        assertTrue(a.isCLean(g));
    }

    @Test
    public void testNodes_Close_AllAndThenZero() {
        Grid grid = new Grid(10, 10);
        Nodes nodes = new Nodes();
        nodes.map = grid;
        // 打开若干节点
        for (int i = 0; i < 5; i++) {
            nodes.open(i, i, i, 10 - i, Grid.DIRECTION_UP);
        }
        // 逐个关闭
        for (int i = 0; i < 5; i++) {
            long n = nodes.close();
            assertNotEquals(0, n);
        }
        // 再次关闭应返回0
        assertEquals(0, nodes.close());
        assertTrue(nodes.isClean());
    }

    @Test
    public void testGrid_OpenNodeIdx_BoundaryZero() {
        Grid g = new Grid(3, 3);
        g.openNodeIdxUpdate(0, 0, 0);
        int info = g.info(0, 0);
        // 存储为idx+1=1，恢复应为0
        assertEquals(1, (info & Grid.NODE_MASK));
        assertEquals(0, Grid.openNodeIdx(info));
    }
}
