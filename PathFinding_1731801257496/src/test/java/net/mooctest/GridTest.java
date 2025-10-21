package net.mooctest;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 中文注释：
 * 本类测试Grid栅格图的读写、边界判断、开放/关闭节点索引与清理逻辑，确保分支覆盖全面。
 */
public class GridTest {

    @Test
    public void testWalkable_AndBounds() {
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
    public void testInfo_OpenCloseAndParentDirection() {
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
}
