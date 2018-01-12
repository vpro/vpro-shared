package nl.vpro.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Michiel Meeuwissen
 * @since 0.53
 */
@Ignore("Doest assert anything, just trying out")
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


    @Test
    @Ignore
    public void background2() throws ExecutionException, InterruptedException {
      Future<?> f1 = ThreadPools.backgroundExecutor.submit(() -> {
          Thread.sleep(10000);
          return null;
          }
        );
        Future<?> f2 = ThreadPools.backgroundExecutor.submit(() -> {
            Thread.sleep(10000);
            return null;
            }
        );

        System.out.println("");
        f1.get();
        f2.get();

        ThreadPools.shutdown();
    }
}
