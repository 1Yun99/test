package net.mooctest;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 中文注释：
 * 本类针对A*核心流程进行全面测试，覆盖：
 * - 搜索路径的基本与异常场景
 * - open方法的关键分支（不可行走判断、已关闭节点、已有开放节点的g值更新与不更新）
 * - fillPath平滑逻辑（smooth=true/false）
 * - 资源清理isCLean
 */
public class AStarAdvancedTest {

    @Test
    public void testSearch_NoObstacles_ShouldReturnPath_AndClean() {
        // 中文：无障碍时应当成功寻路，且完成后数据结构清理干净
        Grid grid = new Grid(6, 6);
        AStar astar = new AStar();
        Path path = astar.search(0, 0, 5, 5, grid);
        assertFalse(path.isEmpty());
        assertTrue(astar.isCLean(grid));
    }

    @Test
    public void testSearch_StartOrEndBlocked_OrSamePoint() {
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
    public void testFillPath_Smooth_False_And_True() {
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
    public void testOpen_Branches_RightDown_LeftUp_Checks() {
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
    public void testOpen_UpdateExistingOpenNode_GBetterOrWorse() {
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
}
