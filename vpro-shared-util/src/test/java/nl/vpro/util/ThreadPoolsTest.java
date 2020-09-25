package nl.vpro.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Michiel Meeuwissen
 * @since 0.53
 */
@Disabled("Doest assert anything, just trying out")
@Slf4j
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
                    log.info("{}{}", te, j);
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
    @Disabled
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

    @Test
    @Disabled
    public void test() {
        ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(10);
        ThreadPoolExecutor e = new ThreadPoolExecutor(2,  4, 600, TimeUnit.SECONDS,
            queue,
            ThreadPools.createThreadFactory(
                "nl.vpro.nep.upload",
                false,
                Thread.NORM_PRIORITY));

        for (int i = 0; i < 1000; i++) {
            int fi = i;
            log.info("starting " + fi + " " + queue.size() + "/" + e.getPoolSize());
            e.execute(new Runnable() {
                @Override
                @SneakyThrows
                public void run() {
                    log.info("running " + fi + " " + queue.size() + "/" + e.getPoolSize());
                    Thread.sleep(100);
                }
            });
        }
    }

}
