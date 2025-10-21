package net.mooctest;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 中文注释：
 * 本类测试Point与Path数据结构，覆盖增删、扩容、取值以及判空的逻辑分支。
 */
public class PointPathTest {

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
}
