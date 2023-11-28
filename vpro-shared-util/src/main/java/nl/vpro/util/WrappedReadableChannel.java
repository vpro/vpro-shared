package nl.vpro.util;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.function.LongConsumer;

/**
 * @since 4.2
 */
@Log4j2
public class WrappedReadableChannel implements ReadableByteChannel {

    long total = 0;
    long prevBatch = 0;
    final long batchSize;
    final ReadableByteChannel delegate;
    final LongConsumer consumer;

    @lombok.Builder
    private WrappedReadableChannel(
        InputStream inputStream,
        ReadableByteChannel delegate,
        Long batchSize,
        LongConsumer consumer) {
        this.delegate = inputStream == null ?  delegate: Channels.newChannel(inputStream) ;
        this.batchSize = batchSize == null ? 1_000_000L : batchSize;
        this.consumer = consumer;
    }


    @Override
    public int read(ByteBuffer dst) throws IOException {
        int result =  delegate.read(dst);
        total += result;
        if (consumer != null) {
            prevBatch += result;
            if (prevBatch > batchSize) {
                consumer.accept(total);
                prevBatch = 0;
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
    }
}
