package net.mooctest;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 中文注释：
 * 本类对Nodes最小堆结构的打开、关闭、上滤/下滤、扩容以及越界异常进行测试。
 */
public class NodesTest {

    @Test
    public void testOpen_Close_MinHeapOrder_AndGrow() {
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
    public void testClose_EmptyReturnsZero() {
        Nodes nodes = new Nodes();
        // 未设置map也应当安全（size==0时直接返回0）
        assertEquals(0, nodes.close());
    }

    @Test(expected = TooLongPathException.class)
    public void testOpen_TooManyOpenNodes_Throws() {
        Grid grid = new Grid(3, 3);
        Nodes nodes = new Nodes();
        nodes.map = grid;
        // 人工设置size达到上限，触发异常分支
        nodes.size = Grid.MAX_OPEN_NODE_SIZE;
        nodes.open(0, 0, 0, 0, Grid.DIRECTION_UP);
    }
}
