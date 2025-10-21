package net.mooctest;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 中文注释：
 * 本类测试Node位编码相关的所有分支，包括正常取值与异常抛出。
 */
public class NodeTest {

    @Test
    public void testToNode_Getters_AndSetGF() {
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
    public void testToNode_TooLongPath() {
        // 中文：当f为负数时应抛出TooLongPathException
        Node.toNode(1, 1, 1, -1);
    }
}
