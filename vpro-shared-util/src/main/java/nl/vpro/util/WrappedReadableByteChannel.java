package nl.vpro.util;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.function.LongConsumer;

import org.meeuw.functional.Unwrappable;

/**
 * Wraps a {@link ReadableByteChannel}. Records the number of channelled bytes. Also, it can have a consumer, which is called every batchsize bytes.
 * @since 4.2
 */
@Log4j2
public class WrappedReadableByteChannel implements ReadableByteChannel, Unwrappable<ReadableByteChannel> {

    @Getter
    volatile long total = 0;
    long prevBatch = 0;
    final long batchSize;
    final ReadableByteChannel delegate;
    final LongConsumer consumer;
    final boolean hasConsumer;

    @lombok.Builder
    private WrappedReadableByteChannel(
        InputStream inputStream,
        ReadableByteChannel delegate,
        Long batchSize,
        LongConsumer consumer) {
        this.delegate = inputStream == null ?  delegate: Channels.newChannel(inputStream) ;
        if (inputStream != null && delegate != null) {
            throw new IllegalArgumentException("Only one of inputStream or delegate should be set");
        }
        if (inputStream == null && delegate == null) {
            throw new IllegalArgumentException("One of inputStream or delegate should be set");
        }
        this.batchSize = batchSize == null ? 1_000_000L : batchSize;
        if (this.batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        this.consumer = consumer;
        this.hasConsumer = consumer != null;
    }


    @Override
    public int read(ByteBuffer dst) throws IOException {
        int result =  delegate.read(dst);
        if (result > 0) {
            total += result;
            if (hasConsumer) {
                consumeCompletedBatches(result);
            }
        }
        return result;
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public void close() throws IOException {
        IOException closeException = null;
        try {
            delegate.close();
        } catch (IOException e) {
            closeException = e;
        }
        if (prevBatch > 0) {
            try {
                consume(total);
                prevBatch = 0;
            } catch (RuntimeException e) {
                if (closeException == null) {
                    throw e;
                }
                closeException.addSuppressed(e);
            }
        }
        if (closeException != null) {
            throw closeException;
        }
    }

    @Override
    public ReadableByteChannel unwrap() {
        return delegate;
    }

    private void consumeCompletedBatches(int result) {
        int remaining = result;
        while (remaining > 0) {
            long untilNextBatch = batchSize - prevBatch;
            if (remaining < untilNextBatch) {
                prevBatch += remaining;
                return;
            }
            long consumed = total - remaining + untilNextBatch;
            remaining -= (int) untilNextBatch;
            prevBatch = 0;
            consume(consumed);
        }
    }

    private void consume(long consumed) {
        consumer.accept(consumed);
    }
}
