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
    long total = 0;
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
        this.consumer = consumer;
        this.hasConsumer = consumer != null;
    }


    @Override
    public int read(ByteBuffer dst) throws IOException {
        int result =  delegate.read(dst);
        if (result > 0) {
            total += result;
            if (hasConsumer) {
                prevBatch += result;
                if (prevBatch >= batchSize) {
                    consume();
                }
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
        delegate.close();
        if (prevBatch > 0) {
            consume();
        }
    }

    @Override
    public ReadableByteChannel unwrap() {
        return delegate;
    }

    private void consume() {
        consumer.accept(total);
        prevBatch = 0;
    }
}
