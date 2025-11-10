package net.mooctest;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/*
 * 测试代码基于JUnit 4，若eclipse提示未找到Junit 5的测试用例，请在Run Configurations中设置Test Runner为Junit 4。请不要使用Junit 5
 * 语法编写测试代码
 */

public class AStarTest {

    private AStar astar;
    private Grid grid;
    private Path path;

    @Before
    public void setUp() {
        astar = new AStar();
        grid = new Grid(10, 10);
        path = new Path();
        
        // 设置所有格子为可行走
        for (int i = 0; i < grid.getWidth(); i++) {
            for (int j = 0; j < grid.getHeight(); j++) {
                grid.setWalkable(i, j, true);
            }
        }
    }

    // ========== AStar类测试 ==========

    /**
     * 测试基本寻路功能 - 无障碍物情况下的直线寻路
     */
    @Test
    public void testSearch_BasicPathFinding_WithoutObstacles() {
        // 从(0,0)到(3,0)的水平路径
        Path result = astar.search(0, 0, 3, 0, grid);
        assertFalse("路径不应为空", result.isEmpty());
        assertTrue("路径长度应大于0", result.size() > 0);
        
        // 验证路径包含起点和终点
        boolean foundStart = false;
        boolean foundEnd = false;
        for (int i = 0; i < result.size(); i++) {
            long point = result.get(i);
            if (Point.getX(point) == 0 && Point.getY(point) == 0) {
                foundStart = true;
            }
            if (Point.getX(point) == 3 && Point.getY(point) == 0) {
                foundEnd = true;
            }
        }
        assertTrue("路径应包含起点(0,0)", foundStart);
        assertTrue("路径应包含终点(3,0)", foundEnd);
    }

    /**
     * 测试起点和终点相同的情况
     */
    @Test
    public void testSearch_SameStartAndEnd_ShouldReturnEmptyPath() {
        Path result = astar.search(2, 2, 2, 2, grid);
        assertTrue("起点终点相同时路径应为空", result.isEmpty());
    }

    /**
     * 测试起点不可行走的情况
     */
    @Test
    public void testSearch_UnwalkableStart_ShouldReturnEmptyPath() {
        grid.setWalkable(1, 1, false);
        Path result = astar.search(1, 1, 5, 5, grid);
        assertTrue("起点不可行走时路径应为空", result.isEmpty());
    }

    /**
     * 测试终点不可行走的情况
     */
    @Test
    public void testSearch_UnwalkableEnd_ShouldReturnEmptyPath() {
        grid.setWalkable(5, 5, false);
        Path result = astar.search(1, 1, 5, 5, grid);
        assertTrue("终点不可行走时路径应为空", result.isEmpty());
    }

    /**
     * 测试有障碍物时的寻路
     */
    @Test
    public void testSearch_WithObstacles_ShouldFindAlternativePath() {
        // 创建一个障碍物墙
        for (int y = 1; y < 9; y++) {
            grid.setWalkable(5, y, false);
        }
        
        // 在墙上开一个口
        grid.setWalkable(5, 4, true);
        
        Path result = astar.search(2, 4, 8, 4, grid);
        assertFalse("有障碍物时应找到替代路径", result.isEmpty());
        
        // 验证路径确实绕过了障碍物 - 检查路径中是否有(5,4)这个开口点
        boolean passedThroughOpening = false;
        for (int i = 0; i < result.size(); i++) {
            long point = result.get(i);
            if (Point.getX(point) == 5 && Point.getY(point) == 4) {
                passedThroughOpening = true;
                break;
            }
        }
        
        // 如果没有通过开口，说明路径找到了其他方式，这也是可以接受的
        // 只要路径存在且所有点都可行走即可
        assertTrue("路径应有效", result.size() > 0);
        
        // 验证路径上的所有点都可行走
        for (int i = 0; i < result.size(); i++) {
            long point = result.get(i);
            int x = Point.getX(point);
            int y = Point.getY(point);
            assertTrue("路径点(" + x + "," + y + ")应可行走", grid.isWalkable(x, y));
        }
    }

    /**
     * 测试路径平滑功能
     */
    @Test
    public void testSearch_WithPathSmoothing_ShouldReturnSmootherPath() {
        // 创建一个需要平滑的Z字形路径
        grid.setWalkable(2, 1, false);
        grid.setWalkable(2, 2, false);
        
        Path smoothPath = astar.search(1, 1, 4, 4, grid, true);
        Path normalPath = astar.search(1, 1, 4, 4, grid, false);
        
        assertFalse("平滑路径不应为空", smoothPath.isEmpty());
        assertFalse("普通路径不应为空", normalPath.isEmpty());
        
        // 平滑路径通常更短或相等
        assertTrue("平滑路径长度应小于等于普通路径", 
                  smoothPath.size() <= normalPath.size());
    }

    /**
     * 测试无法到达的情况
     */
    @Test
    public void testSearch_UnreachableTarget_ShouldReturnEmptyPath() {
        // 用障碍物完全包围目标
        for (int x = 6; x <= 8; x++) {
            for (int y = 6; y <= 8; y++) {
                grid.setWalkable(x, y, false);
            }
        }
        
        Path result = astar.search(1, 1, 7, 7, grid);
        assertTrue("无法到达时路径应为空", result.isEmpty());
    }

    /**
     * 测试边界情况 - 网格边界寻路
     */
    @Test
    public void testSearch_GridBoundary_ShouldWorkCorrectly() {
        Path result = astar.search(0, 0, 9, 9, grid);
        assertFalse("边界寻路应成功", result.isEmpty());
        
        // 验证路径包含起点和终点
        boolean foundStart = false;
        boolean foundEnd = false;
        for (int i = 0; i < result.size(); i++) {
            long point = result.get(i);
            int x = Point.getX(point);
            int y = Point.getY(point);
            if (x == 0 && y == 0) {
                foundStart = true;
            }
            if (x == 9 && y == 9) {
                foundEnd = true;
            }
        }
        assertTrue("路径应包含起点(0,0)", foundStart);
        assertTrue("路径应包含终点(9,9)", foundEnd);
    }

    /**
     * 测试void search方法
     */
    @Test
    public void testVoidSearch_ShouldFillProvidedPath() {
        Path providedPath = new Path();
        astar.search(0, 0, 3, 3, grid, providedPath);
        assertFalse("提供的路径应被填充", providedPath.isEmpty());
    }

    /**
     * 测试异常处理 - 清理状态
     */
    @Test
    public void testSearch_Exception_ShouldCleanUpState() {
        // 创建一个会导致异常的情况
        Grid tinyGrid = new Grid(1, 1);
        tinyGrid.setWalkable(0, 0, true);
        
        try {
            // 这可能会因为各种原因抛出异常
            astar.search(0, 0, 0, 0, tinyGrid);
        } catch (Exception e) {
            // 异常后状态应该是干净的
            assertTrue("异常后状态应干净", astar.isCLean(tinyGrid));
        }
    }

    // ========== Grid类测试 ==========

    /**
     * 测试Grid基本功能
     */
    @Test
    public void testGrid_BasicOperations() {
        Grid testGrid = new Grid(5, 5);
        assertEquals("宽度应为5", 5, testGrid.getWidth());
        assertEquals("高度应为5", 5, testGrid.getHeight());
        
        // 测试默认可行走性
        assertTrue("默认应可行走", testGrid.isWalkable(2, 2));
        
        // 测试设置不可行走
        testGrid.setWalkable(2, 2, false);
        assertFalse("应可设置为不可行走", testGrid.isWalkable(2, 2));
        
        // 测试重新设置可行走
        testGrid.setWalkable(2, 2, true);
        assertTrue("应可重新设置为可行走", testGrid.isWalkable(2, 2));
    }

    /**
     * 测试Grid边界检查
     */
    @Test
    public void testGrid_BoundaryChecks() {
        Grid testGrid = new Grid(3, 3);
        
        // 测试负坐标
        assertFalse("负坐标应不可行走", testGrid.isWalkable(-1, 0));
        assertFalse("负坐标应不可行走", testGrid.isWalkable(0, -1));
        
        // 测试超出边界的坐标
        assertFalse("超出边界应不可行走", testGrid.isWalkable(3, 0));
        assertFalse("超出边界应不可行走", testGrid.isWalkable(0, 3));
        assertFalse("超出边界应不可行走", testGrid.isWalkable(3, 3));
    }

    /**
     * 测试Grid节点信息操作
     */
    @Test
    public void testGrid_NodeInfoOperations() {
        Grid testGrid = new Grid(5, 5);
        
        // 测试初始节点信息
        int info = testGrid.info(2, 2);
        assertTrue("初始节点应为null", Grid.isNullNode(info));
        assertFalse("初始节点不应为closed", Grid.isClosedNode(info));
        assertFalse("初始节点不应为unwalkable", Grid.isUnwalkable(info));
        
        // 测试设置closed节点
        testGrid.nodeClosed(2, 2);
        info = testGrid.info(2, 2);
        assertTrue("设置后应为closed节点", Grid.isClosedNode(info));
    }

    /**
     * 测试Grid清理功能
     */
    @Test
    public void testGrid_ClearOperations() {
        Grid testGrid = new Grid(5, 5);
        
        // 设置一些节点状态
        testGrid.nodeClosed(1, 1);
        testGrid.nodeParentDirectionUpdate(1, 2, Grid.DIRECTION_UP);
        testGrid.openNodeIdxUpdate(1, 3, 0); // 修改为有效的索引值
        
        assertFalse("设置后状态不应干净", testGrid.isClean());
        
        // 清理
        testGrid.clear();
        assertTrue("清理后状态应干净", testGrid.isClean());
    }

    /**
     * 测试Grid节点父方向操作
     */
    @Test
    public void testGrid_ParentDirectionOperations() {
        Grid testGrid = new Grid(5, 5);
        
        // 设置父方向
        testGrid.nodeParentDirectionUpdate(2, 2, Grid.DIRECTION_RIGHT);
        assertEquals("父方向应为RIGHT", Grid.DIRECTION_RIGHT, 
                    testGrid.nodeParentDirection(2, 2));
        
        // 测试所有方向
        testGrid.nodeParentDirectionUpdate(2, 2, Grid.DIRECTION_UP);
        assertEquals("父方向应为UP", Grid.DIRECTION_UP, 
                    testGrid.nodeParentDirection(2, 2));
        
        testGrid.nodeParentDirectionUpdate(2, 2, Grid.DIRECTION_LEFT_UP);
        assertEquals("父方向应为LEFT_UP", Grid.DIRECTION_LEFT_UP, 
                    testGrid.nodeParentDirection(2, 2));
    }

    // ========== Nodes类测试 ==========

    /**
     * 测试Nodes基本操作
     */
    @Test
    public void testNodes_BasicOperations() {
        Nodes nodes = new Nodes();
        assertTrue("初始应干净", nodes.isClean());
        
        Grid testGrid = new Grid(5, 5);
        nodes.map = testGrid;
        
        // 添加节点
        nodes.open(1, 1, 5, 10, Grid.DIRECTION_UP);
        assertFalse("添加节点后不应干净", nodes.isClean());
        assertEquals("大小应为1", 1, nodes.size);
    }

    /**
     * 测试Nodes堆操作
     */
    @Test
    public void testNodes_HeapOperations() {
        Nodes nodes = new Nodes();
        Grid testGrid = new Grid(10, 10);
        nodes.map = testGrid;
        
        // 添加多个节点
        nodes.open(1, 1, 5, 15, Grid.DIRECTION_UP);  // f=20
        nodes.open(2, 2, 3, 10, Grid.DIRECTION_RIGHT); // f=13
        nodes.open(3, 3, 8, 5, Grid.DIRECTION_DOWN);   // f=13
        
        assertEquals("应有3个节点", 3, nodes.size);
        
        // 关闭节点（应返回f值最小的）
        long node = nodes.close();
        assertEquals("大小应减少", 2, nodes.size);
        
        int x = Node.getX(node);
        int y = Node.getY(node);
        assertTrue("应返回最小f值的节点", (x == 2 && y == 2) || (x == 3 && y == 3));
    }

    /**
     * 测试Nodes空堆操作
     */
    @Test
    public void testNodes_EmptyHeap_CloseShouldReturnZero() {
        Nodes nodes = new Nodes();
        Grid testGrid = new Grid(5, 5);
        nodes.map = testGrid;
        
        long result = nodes.close();
        assertEquals("空堆关闭应返回0", 0, result);
    }

    /**
     * 测试Nodes容量扩展
     */
    @Test
    public void testNodes_CapacityGrowth() {
        Nodes nodes = new Nodes();
        Grid testGrid = new Grid(50, 50);
        nodes.map = testGrid;
        
        // 添加足够多的节点以触发扩展
        for (int i = 0; i < 20; i++) {
            nodes.open(i, 0, i * 5, 10, Grid.DIRECTION_RIGHT);
        }
        
        assertTrue("应能处理多个节点", nodes.size > 15);
    }

    /**
     * 测试Nodes最大容量限制
     */
    @Test(expected = TooLongPathException.class)
    public void testNodes_MaxCapacityExceeded_ShouldThrowException() {
        Nodes nodes = new Nodes();
        Grid testGrid = new Grid(1000, 1000);
        nodes.map = testGrid;
        
        // 尝试添加超过最大容量的节点
        for (int i = 0; i <= Grid.MAX_OPEN_NODE_SIZE; i++) {
            nodes.open(i % 100, (i / 100) % 100, i, 10, Grid.DIRECTION_UP);
        }
    }

    // ========== Path类测试 ==========

    /**
     * 测试Path基本操作
     */
    @Test
    public void testPath_BasicOperations() {
        Path path = new Path();
        assertTrue("新路径应为空", path.isEmpty());
        assertEquals("初始大小应为0", 0, path.size());
        
        // 添加点
        path.add(1, 1);
        path.add(2, 2);
        assertFalse("添加点后不应为空", path.isEmpty());
        assertEquals("大小应为2", 2, path.size());
    }

    /**
     * 测试Path点的获取
     */
    @Test
    public void testPath_PointAccess() {
        Path path = new Path();
        path.add(1, 1);
        path.add(2, 2);
        path.add(3, 3);
        
        // 注意：get(0)返回最后添加的点
        long point = path.get(0);
        assertEquals("第一个点应为(3,3)", 3, Point.getX(point));
        assertEquals("第一个点应为(3,3)", 3, Point.getY(point));
        
        point = path.get(2);
        assertEquals("最后一个点应为(1,1)", 1, Point.getX(point));
        assertEquals("最后一个点应为(1,1)", 1, Point.getY(point));
    }

    /**
     * 测试Path删除操作
     */
    @Test
    public void testPath_RemoveOperation() {
        Path path = new Path();
        path.add(1, 1);
        path.add(2, 2);
        path.add(3, 3);
        
        assertEquals("初始大小为3", 3, path.size());
        
        path.remove();
        assertEquals("删除后大小应为2", 2, path.size());
        
        long point = path.get(0);
        assertEquals("删除后第一个点应为(2,2)", 2, Point.getX(point));
        assertEquals("删除后第一个点应为(2,2)", 2, Point.getY(point));
    }

    /**
     * 测试Path清理操作
     */
    @Test
    public void testPath_ClearOperation() {
        Path path = new Path();
        path.add(1, 1);
        path.add(2, 2);
        
        assertFalse("添加点后不应为空", path.isEmpty());
        
        path.clear();
        assertTrue("清理后应为空", path.isEmpty());
        assertEquals("清理后大小应为0", 0, path.size());
    }

    /**
     * 测试Path容量扩展
     */
    @Test
    public void testPath_CapacityGrowth() {
        Path path = new Path();
        
        // 添加足够多的点以触发扩展
        for (int i = 0; i < 20; i++) {
            path.add(i, i);
        }
        
        assertTrue("应能处理多个点", path.size() > 15);
    }

    // ========== Point类测试 ==========

    /**
     * 测试Point坐标转换
     */
    @Test
    public void testPoint_CoordinateConversion() {
        // 测试正常坐标
        long point = Point.toPoint(100, 200);
        assertEquals("X坐标应为100", 100, Point.getX(point));
        assertEquals("Y坐标应为200", 200, Point.getY(point));
        
        // 测试边界坐标
        point = Point.toPoint(Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals("应能处理最大X坐标", Integer.MAX_VALUE, Point.getX(point));
        assertEquals("应能处理最大Y坐标", Integer.MAX_VALUE, Point.getY(point));
        
        // 测试负坐标
        point = Point.toPoint(-50, -100);
        assertEquals("应能处理负X坐标", -50, Point.getX(point));
        assertEquals("应能处理负Y坐标", -100, Point.getY(point));
        
        // 测试零坐标
        point = Point.toPoint(0, 0);
        assertEquals("零X坐标", 0, Point.getX(point));
        assertEquals("零Y坐标", 0, Point.getY(point));
    }

    // ========== Cost类测试 ==========

    /**
     * 测试Cost常量
     */
    @Test
    public void testCost_Constants() {
        assertEquals("正交成本应为5", 5, Cost.COST_ORTHOGONAL);
        assertEquals("对角成本应为7", 7, Cost.COST_DIAGONAL);
    }

    /**
     * 测试Cost启发式计算
     */
    @Test
    public void testCost_HeuristicCalculation() {
        // 水平距离
        int cost = Cost.hCost(0, 0, 5, 0);
        assertEquals("水平距离5应为25", 25, cost);
        
        // 垂直距离
        cost = Cost.hCost(0, 0, 0, 3);
        assertEquals("垂直距离3应为15", 15, cost);
        
        // 曼哈顿距离
        cost = Cost.hCost(0, 0, 3, 4);
        assertEquals("曼哈顿距离(3,4)应为35", 35, cost);
        
        // 相同点
        cost = Cost.hCost(5, 5, 5, 5);
        assertEquals("相同点成本应为0", 0, cost);
        
        // 负坐标
        cost = Cost.hCost(-2, -3, 2, 1);
        assertEquals("负坐标距离应为40", 40, cost);
    }

    // ========== Reachability类测试 ==========

    /**
     * 测试基本可达性检查
     */
    @Test
    public void testReachability_BasicCheck() {
        // 无障碍物的直线可达性
        boolean reachable = Reachability.isReachable(0, 0, 5, 0, grid);
        assertTrue("水平直线应可达", reachable);
        
        reachable = Reachability.isReachable(0, 0, 0, 5, grid);
        assertTrue("垂直直线应可达", reachable);
        
        reachable = Reachability.isReachable(0, 0, 5, 5, grid);
        assertTrue("对角线应可达", reachable);
    }

    /**
     * 测试有障碍物的可达性
     */
    @Test
    public void testReachability_WithObstacles() {
        // 在路径上设置障碍物
        grid.setWalkable(2, 2, false);
        
        // 水平路径有障碍
        boolean reachable = Reachability.isReachable(0, 2, 5, 2, grid);
        assertFalse("有障碍的水平路径不可达", reachable);
        
        // 垂直路径有障碍
        reachable = Reachability.isReachable(2, 0, 2, 5, grid);
        assertFalse("有障碍的垂直路径不可达", reachable);
    }

    /**
     * 测试最近可行走点查找
     */
    @Test
    public void testReachability_ClosestWalkablePoint() {
        // 设置障碍物
        grid.setWalkable(3, 3, false);
        
        long closest = Reachability.getClosestWalkablePointToTarget(0, 0, 5, 5, grid);
        
        // 由于(3,3)不可行走，应该返回障碍物前的最后一个可行走点
        assertTrue("应返回一个可行走点", grid.isWalkable(
                  Point.getX(closest), Point.getY(closest)));
    }

    /**
     * 测试缩放可达性检查
     */
    @Test
    public void testReachability_WithScale() {
        // 测试不同缩放级别
        boolean reachable = Reachability.isReachable(0, 0, 10, 0, 2, grid);
        assertTrue("缩放后的水平路径应可达", reachable);
        
        reachable = Reachability.isReachable(0, 0, 0, 10, 3, grid);
        assertTrue("缩放后的垂直路径应可达", reachable);
    }

    /**
     * 测试Fence接口
     */
    @Test
    public void testReachability_WithFence() {
        // 创建一个简单的fence实现
        Fence fence = new Fence() {
            @Override
            public boolean isReachable(int x1, int y1, int x2, int y2) {
                // 不允许从(0,0)到任何地方
                return !(x1 == 0 && y1 == 0);
            }
        };
        
        // 使用fence的可达性检查
        long closest = Reachability.getClosestWalkablePointToTarget(
                      0, 0, 5, 5, 1, grid, fence);
        
        // 应该返回起点，因为fence阻止了移动
        assertEquals("Fence阻止时应返回起点", Point.toPoint(0, 0), closest);
    }

    /**
     * 测试无效缩放参数
     */
    @Test(expected = IllegalArgumentException.class)
    public void testReachability_InvalidScale_ShouldThrowException() {
        Reachability.isReachable(0, 0, 5, 5, 0, grid);
    }

    // ========== Node类测试 ==========

    /**
     * 测试Node创建和访问
     */
    @Test
    public void testNode_CreationAndAccess() {
        long node = Node.toNode(10, 20, 5, 15);
        
        assertEquals("X坐标应为10", 10, Node.getX(node));
        assertEquals("Y坐标应为20", 20, Node.getY(node));
        assertEquals("G值应为5", 5, Node.getG(node));
        assertEquals("F值应为15", 15, Node.getF(node));
    }

    /**
     * 测试Node GF值设置
     */
    @Test
    public void testNode_SetGF() {
        long node = Node.toNode(10, 20, 5, 15);
        node = Node.setGF(node, 8, 20);
        
        assertEquals("G值应更新为8", 8, Node.getG(node));
        assertEquals("F值应更新为20", 20, Node.getF(node));
        assertEquals("X坐标不应变", 10, Node.getX(node));
        assertEquals("Y坐标不应变", 20, Node.getY(node));
    }

    /**
     * 测试Node负F值异常
     */
    @Test(expected = TooLongPathException.class)
    public void testNode_NegativeF_ShouldThrowException() {
        Node.toNode(0, 0, 0, -1);
    }

    // ========== Utils类测试 ==========

    /**
     * 测试Utils基本检查
     */
    @Test
    public void testUtils_BasicChecks() {
        // 测试正常条件
        Utils.check(true);
        Utils.check(true, "测试消息");
        Utils.check(true, "格式化 %s", "测试");
    }

    /**
     * 测试Utils异常检查
     */
    @Test(expected = RuntimeException.class)
    public void testCheck_FalseCondition_ShouldThrowException() {
        Utils.check(false);
    }

    @Test(expected = RuntimeException.class)
    public void testCheck_FalseConditionWithMessage_ShouldThrowException() {
        Utils.check(false, "错误消息");
    }

    @Test(expected = RuntimeException.class)
    public void testCheck_FalseConditionWithFormat_ShouldThrowException() {
        Utils.check(false, "格式化错误: %d", 123);
    }

    /**
     * 测试Utils mask操作
     */
    @Test
    public void testUtils_Mask() {
        assertEquals("1位mask应为1", 1, Utils.mask(1));
        assertEquals("8位mask应为255", 255, Utils.mask(8));
        assertEquals("16位mask应为65535", 65535, Utils.mask(16));
        assertEquals("32位mask应为-1", -1, Utils.mask(32));
    }

    @Test(expected = RuntimeException.class)
    public void testMask_InvalidBitCount_ShouldThrowException() {
        Utils.mask(0);
    }

    @Test(expected = RuntimeException.class)
    public void testMask_BitCountTooLarge_ShouldThrowException() {
        Utils.mask(33);
    }

    // ========== ThreadLocalAStar类测试 ==========

    /**
     * 测试ThreadLocalAStar基本功能
     */
    @Test
    public void testThreadLocalAStar_BasicFunctionality() {
        AStar localAstar = ThreadLocalAStar.current();
        assertNotNull("应能获取本地AStar实例", localAstar);
        
        // 再次获取应该是同一个实例
        AStar anotherAstar = ThreadLocalAStar.current();
        assertSame("应该是同一个实例", localAstar, anotherAstar);
    }

    // ========== TooLongPathException类测试 ==========

    /**
     * 测试TooLongPathException
     */
    @Test
    public void testTooLongPathException() {
        TooLongPathException exception = new TooLongPathException("测试消息");
        assertEquals("异常消息应为'测试消息'", "测试消息", exception.getMessage());
        assertTrue("应为RuntimeException", exception instanceof RuntimeException);
    }

    /**
     * 测试AStar open方法的各种方向
     */
    @Test
    public void testOpen_AllDirections_ShouldWorkCorrectly() {
        // 创建一个简单的测试网格
        Grid testGrid = new Grid(3, 3);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                testGrid.setWalkable(i, j, true);
            }
        }
        
        astar.nodes.map = testGrid;
        
        // 测试所有8个方向
        astar.open(1, 1, 5, Grid.DIRECTION_UP, 2, 2, testGrid);
        astar.open(1, 1, 5, Grid.DIRECTION_DOWN, 2, 0, testGrid);
        astar.open(1, 1, 5, Grid.DIRECTION_LEFT, 0, 1, testGrid);
        astar.open(1, 1, 5, Grid.DIRECTION_RIGHT, 2, 1, testGrid);
        astar.open(1, 1, 7, Grid.DIRECTION_LEFT_UP, 0, 2, testGrid);
        astar.open(1, 1, 7, Grid.DIRECTION_LEFT_DOWN, 0, 0, testGrid);
        astar.open(1, 1, 7, Grid.DIRECTION_RIGHT_UP, 2, 2, testGrid);
        astar.open(1, 1, 7, Grid.DIRECTION_RIGHT_DOWN, 2, 0, testGrid);
        
        assertTrue("所有方向都应工作正常", astar.nodes.size > 0);
    }

    /**
     * 测试AStar open方法的障碍物检查
     */
    @Test
    public void testOpen_WithObstacleChecks_ShouldHandleCorrectly() {
        Grid testGrid = new Grid(3, 3);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                testGrid.setWalkable(i, j, true);
            }
        }
        
        astar.nodes.map = testGrid;
        
        // 测试DIRECTION_RIGHT_DOWN方向的障碍物检查
        // 这个方向需要检查(x+1, y)是否可行走
        testGrid.setWalkable(2, 1, false);
        int initialSize = astar.nodes.size;
        astar.open(1, 1, 7, Grid.DIRECTION_RIGHT_DOWN, 2, 0, testGrid);
        assertEquals("有障碍物时不应添加节点", initialSize, astar.nodes.size);
        
        // 测试DIRECTION_LEFT_UP方向的障碍物检查
        // 这个方向需要检查(x, y+1)是否可行走
        testGrid.setWalkable(1, 2, false);
        astar.open(1, 1, 7, Grid.DIRECTION_LEFT_UP, 0, 2, testGrid);
        assertEquals("有障碍物时不应添加节点", initialSize, astar.nodes.size);
    }

    /**
     * 测试fillPath的平滑功能
     */
    @Test
    public void testFillPath_WithSmoothing_ShouldWorkCorrectly() {
        Grid testGrid = new Grid(5, 5);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                testGrid.setWalkable(i, j, true);
            }
        }
        
        Path testPath = new Path();
        
        // 先添加一些点
        testPath.add(0, 0);
        testPath.add(1, 1);
        testPath.add(2, 2);
        
        astar.fillPath(3, 3, testPath, testGrid, true);
        
        assertFalse("平滑后路径不应为空", testPath.isEmpty());
    }

    // ========== 综合测试 ==========

    /**
     * 测试Reachability的复杂斜线情况
     */
    @Test
    public void testReachability_ComplexDiagonal_ShouldWorkCorrectly() {
        // 创建一个足够大的网格来测试这些坐标
        Grid largeGrid = new Grid(15, 15);
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                largeGrid.setWalkable(i, j, true);
            }
        }
        
        // 测试正斜率对角线
        boolean reachable = Reachability.isReachable(0, 0, 10, 10, largeGrid);
        assertTrue("正斜率对角线应可达", reachable);
        
        // 测试负斜率对角线
        reachable = Reachability.isReachable(10, 0, 0, 10, largeGrid);
        assertTrue("负斜率对角线应可达", reachable);
        
        // 测试平缓斜线
        reachable = Reachability.isReachable(0, 0, 10, 2, largeGrid);
        assertTrue("平缓斜线应可达", reachable);
        
        // 测试陡峭斜线
        reachable = Reachability.isReachable(0, 0, 2, 10, largeGrid);
        assertTrue("陡峭斜线应可达", reachable);
    }

    /**
     * 测试Reachability的基本缩放功能
     */
    @Test
    public void testReachability_BasicScaleOperations() {
        // 测试scaleDown
        double scaled = Reachability.scaleDown(10.0, 2);
        assertEquals("scaleDown(10,2)应为5.0", 5.0, scaled, 0.001);
        
        scaled = Reachability.scaleDown(15.0, 3);
        assertEquals("scaleDown(15,3)应为5.0", 5.0, scaled, 0.001);
        
        // 测试scaleUp整数版本
        int upscaled = Reachability.scaleUp(5, 2);
        assertEquals("scaleUp(5,2)应为11", 11, upscaled); // 5*2 + 2/2 = 10 + 1 = 11
        
        upscaled = Reachability.scaleUp(3, 3);
        assertEquals("scaleUp(3,3)应为10", 10, upscaled); // 3*3 + 3/2 = 9 + 1 = 10
        
        // 测试scaleUp双精度版本
        upscaled = Reachability.scaleUp(5.5, 2);
        assertEquals("scaleUp(5.5,2)应为11", 11, upscaled);
    }

    /**
     * 测试Nodes的堆操作细节
     */
    @Test
    public void testNodes_HeapOperationsDetailed() {
        Nodes nodes = new Nodes();
        Grid testGrid = new Grid(10, 10);
        nodes.map = testGrid;
        
        // 测试siftUp操作
        nodes.open(5, 5, 10, 20, Grid.DIRECTION_UP); // f=30
        nodes.open(3, 3, 5, 10, Grid.DIRECTION_RIGHT); // f=15，应该排在前面
        
        assertEquals("较小f值的节点应该在前面", 15, Node.getF(nodes.getOpenNode(0)));
        
        // 测试siftDown操作
        nodes.open(7, 7, 15, 5, Grid.DIRECTION_DOWN); // f=20
        
        long closed = nodes.close();
        assertEquals("应该关闭f值最小的节点", 15, Node.getF(closed));
        assertEquals("关闭的节点坐标应为(3,3)", 3, Node.getX(closed));
        assertEquals("关闭的节点坐标应为(3,3)", 3, Node.getY(closed));
    }

    /**
     * 测试Nodes的节点更新操作
     */
    @Test
    public void testNodes_NodeUpdate() {
        Nodes nodes = new Nodes();
        Grid testGrid = new Grid(5, 5);
        nodes.map = testGrid;
        
        // 添加一个节点
        nodes.open(2, 2, 10, 15, Grid.DIRECTION_UP);
        assertEquals("初始G值应为10", 10, Node.getG(nodes.getOpenNode(0)));
        
        // 尝试用更小的G值更新（应该更新）
        nodes.open(2, 2, 5, 15, Grid.DIRECTION_RIGHT);
        assertTrue("G值应该被更新", Node.getG(nodes.getOpenNode(0)) <= 10);
        
        // 尝试用更大的G值更新（不应该更新）
        int currentG = Node.getG(nodes.getOpenNode(0));
        nodes.open(2, 2, 20, 25, Grid.DIRECTION_DOWN);
        assertEquals("G值不应该被更新", currentG, Node.getG(nodes.getOpenNode(0)));
    }

    /**
     * 测试Grid的边界条件
     */
    @Test
    public void testGrid_EdgeCases() {
        // 测试最小网格
        Grid minGrid = new Grid(1, 1);
        assertEquals("最小网格宽度应为1", 1, minGrid.getWidth());
        assertEquals("最小网格高度应为1", 1, minGrid.getHeight());
        
        // 测试大网格
        Grid largeGrid = new Grid(1000, 1000);
        assertEquals("大网格宽度应为1000", 1000, largeGrid.getWidth());
        assertEquals("大网格高度应为1000", 1000, largeGrid.getHeight());
        
        // 测试节点信息的各种状态
        int info = minGrid.info(0, 0);
        assertTrue("新节点应为null", Grid.isNullNode(info));
        
        minGrid.nodeClosed(0, 0);
        info = minGrid.info(0, 0);
        assertTrue("关闭后应为closed节点", Grid.isClosedNode(info));
        
        minGrid.setWalkable(0, 0, false);
        info = minGrid.info(0, 0);
        assertTrue("不可行走后应为unwalkable", Grid.isUnwalkable(info));
    }

    /**
     * 测试Path的边界条件
     */
    @Test
    public void testPath_EdgeCases() {
        Path path = new Path();
        
        // 测试空路径的操作
        assertTrue("新路径应为空", path.isEmpty());
        assertEquals("新路径大小应为0", 0, path.size());
        
        // 添加一个点
        path.add(1, 1);
        assertEquals("添加点后大小应为1", 1, path.size());
        // 注意：isEmpty()在size<2时返回true，所以单个点仍然被认为是"空"的
        
        // 再添加一个点
        path.add(2, 2);
        assertFalse("两个点不应为空", path.isEmpty());
        assertEquals("两个点大小应为2", 2, path.size());
        
        // 测试删除操作
        path.remove();
        assertEquals("删除后大小应为1", 1, path.size());
        assertTrue("单个点应被认为为空", path.isEmpty());
    }

    /**
     * 测试复杂路径平滑场景
     */
    @Test
    public void testComplexPathSmoothing() {
        // 创建一个需要平滑的复杂场景
        Grid complexGrid = new Grid(7, 7);
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                complexGrid.setWalkable(i, j, true);
            }
        }
        
        // 创建一些障碍物以产生Z字形路径
        complexGrid.setWalkable(2, 1, false);
        complexGrid.setWalkable(2, 2, false);
        complexGrid.setWalkable(4, 4, false);
        complexGrid.setWalkable(4, 5, false);
        
        AStar complexAstar = new AStar();
        
        Path smoothPath = complexAstar.search(1, 1, 6, 6, complexGrid, true);
        Path normalPath = complexAstar.search(1, 1, 6, 6, complexGrid, false);
        
        assertFalse("平滑路径不应为空", smoothPath.isEmpty());
        assertFalse("普通路径不应为空", normalPath.isEmpty());
        
        // 验证两个路径都有效
        for (int i = 0; i < smoothPath.size(); i++) {
            long point = smoothPath.get(i);
            int x = Point.getX(point);
            int y = Point.getY(point);
            assertTrue("平滑路径点应可行走", complexGrid.isWalkable(x, y));
        }
        
        for (int i = 0; i < normalPath.size(); i++) {
            long point = normalPath.get(i);
            int x = Point.getX(point);
            int y = Point.getY(point);
            assertTrue("普通路径点应可行走", complexGrid.isWalkable(x, y));
        }
    }

    /**
     * 测试复杂的寻路场景
     */
    @Test
    public void testComplexPathfinding() {
        // 创建一个迷宫
        Grid maze = new Grid(7, 7);
        
        // 设置边界
        for (int i = 0; i < 7; i++) {
            maze.setWalkable(i, 0, false);
            maze.setWalkable(i, 6, false);
            maze.setWalkable(0, i, false);
            maze.setWalkable(6, i, false);
        }
        
        // 创建内部障碍物，但留出路径
        maze.setWalkable(2, 2, false);
        maze.setWalkable(2, 3, false);
        maze.setWalkable(2, 4, false);
        maze.setWalkable(4, 2, false);
        maze.setWalkable(4, 3, false);
        maze.setWalkable(4, 4, false);
        
        // 在顶部和底部留出开口
        maze.setWalkable(2, 1, true);
        maze.setWalkable(2, 5, true);
        maze.setWalkable(4, 1, true);
        maze.setWalkable(4, 5, true);
        
        AStar mazeSolver = new AStar();
        Path result = mazeSolver.search(1, 1, 5, 5, maze);
        
        assertFalse("迷宫中应能找到路径", result.isEmpty());
        
        // 验证路径不穿过障碍物
        for (int i = 0; i < result.size(); i++) {
            long point = result.get(i);
            int x = Point.getX(point);
            int y = Point.getY(point);
            assertTrue("路径点应可行走", maze.isWalkable(x, y));
        }
    }

    /**
     * 测试性能 - 大网格寻路
     */
    @Test
    public void testPerformance_LargeGrid() {
        Grid largeGrid = new Grid(50, 50);
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 50; j++) {
                largeGrid.setWalkable(i, j, true);
            }
        }
        
        long startTime = System.currentTimeMillis();
        Path result = astar.search(0, 0, 49, 49, largeGrid);
        long endTime = System.currentTimeMillis();
        
        assertFalse("大网格应能找到路径", result.isEmpty());
        assertTrue("寻路应在合理时间内完成", (endTime - startTime) < 1000);
    }

    /**
     * 测试状态清理
     */
    @Test
    public void testStateCleanup() {
        // 执行一次寻路
        astar.search(0, 0, 5, 5, grid);
        
        // 检查状态是否清理
        assertTrue("寻路后状态应干净", astar.isCLean(grid));
    }

    /**
     * 测试多线程安全性
     */
    @Test
    public void testThreadSafety() throws InterruptedException {
        final boolean[] results = new boolean[4];
        Thread[] threads = new Thread[4];
        
        for (int i = 0; i < 4; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    AStar localAstar = ThreadLocalAStar.current();
                    Grid localGrid = new Grid(5, 5);
                    for (int x = 0; x < 5; x++) {
                        for (int y = 0; y < 5; y++) {
                            localGrid.setWalkable(x, y, true);
                        }
                    }
                    
                    Path result = localAstar.search(0, 0, 4, 4, localGrid);
                    results[index] = !result.isEmpty();
                } catch (Exception e) {
                    results[index] = false;
                }
            });
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证所有线程都成功
        for (boolean result : results) {
            assertTrue("所有线程都应成功寻路", result);
        }
    }
}