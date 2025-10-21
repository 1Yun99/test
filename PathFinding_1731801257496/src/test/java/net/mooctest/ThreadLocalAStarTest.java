package net.mooctest;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * 中文注释：
 * 本类测试线程本地AStar实例，确保同线程复用、不同线程隔离。
 */
public class ThreadLocalAStarTest {

    @Test
    public void testCurrent_SameThreadSameInstance() {
        // 中文：同一线程多次获取应为同一实例
        AStar a1 = ThreadLocalAStar.current();
        AStar a2 = ThreadLocalAStar.current();
        assertSame(a1, a2);
    }

    @Test
    public void testCurrent_DifferentThreadDifferentInstance() throws Exception {
        final AStar mainThreadInstance = ThreadLocalAStar.current();
        final AtomicReference<AStar> ref = new AtomicReference<>();
        Thread t = new Thread(() -> ref.set(ThreadLocalAStar.current()));
        t.start();
        t.join();
        assertNotSame(mainThreadInstance, ref.get());
    }
}
