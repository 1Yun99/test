package net.mooctest;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 中文注释：
 * 本类测试启发式代价函数hCost，确保不同坐标差下返回值正确。
 */
public class CostTest {

    @Test
    public void testHCost() {
        // 中文：同一位置，启发式成本为0
        assertEquals(0, invokeH(0, 0, 0, 0));
        // 中文：水平移动两格：|dx|=2，成本为2*5
        assertEquals(10, invokeH(0, 0, 2, 0));
        // 中文：竖直移动三格：|dy|=3，成本为3*5
        assertEquals(15, invokeH(0, 0, 0, 3));
        // 中文：L型移动：|dx|+|dy|=5，成本为25
        assertEquals(25, invokeH(1, 2, 4, 3));
    }

    // 中文：访问包内可见方法
    private int invokeH(int x1, int y1, int x2, int y2) {
        return Cost.hCost(x1, y1, x2, y2);
    }
}
