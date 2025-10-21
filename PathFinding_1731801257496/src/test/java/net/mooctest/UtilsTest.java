package net.mooctest;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 中文注释：
 * 本类针对Utils工具类的边界条件进行测试，确保mask与check方法的分支覆盖齐全。
 */
public class UtilsTest {

    @Test
    public void testMask_NormalValues() {
        // 中文：测试正常取掩码的场景
        assertEquals(1, Utils.mask(1));
        assertEquals(3, Utils.mask(2));
        assertEquals(-1, Utils.mask(32)); // 32位全1
    }

    @Test(expected = RuntimeException.class)
    public void testMask_TooSmall() {
        // 中文：nbit小于1时应当抛出异常
        Utils.mask(0);
    }

    @Test(expected = RuntimeException.class)
    public void testMask_TooLarge() {
        // 中文：nbit大于32时应当抛出异常
        Utils.mask(33);
    }

    @Test
    public void testCheck_WithMessageAndFormat() {
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
}
