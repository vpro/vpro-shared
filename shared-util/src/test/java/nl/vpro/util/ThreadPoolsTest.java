package nl.vpro.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.Test;

/**
 * @author Michiel Meeuwissen
 * @since 0.53
 */
public class ThreadPoolsTest {

    @Test
    public void background() throws ExecutionException, InterruptedException {
        List<Future<Integer>> futures = new ArrayList<>();
        int k = 0;
        for (ExecutorService te : Arrays.asList(ThreadPools.backgroundExecutor, ThreadPools.copyExecutor, ThreadPools.startUpExecutor)) {
            k += 1000;
            for (int i = 1; i < 100; i++) {
                final int j = k + i;
                futures.add(te.submit(() -> {
                    Thread.sleep(100);
                    System.out.println(j);
                    return j;
                }));
            }
        }
        for (Future<Integer> f : futures) {
            f.get();
        }
        ThreadPools.shutdown();
    }

}
