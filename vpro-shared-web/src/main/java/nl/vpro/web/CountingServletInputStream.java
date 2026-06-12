package nl.vpro.web;

import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

public class CountingServletInputStream extends ServletInputStream  {

    private final ServletInputStream sourceStream;

    @Getter
    private final AtomicLong count = new AtomicLong(0);

    private final LongConsumer consumer;

    public CountingServletInputStream(ServletInputStream sourceStream, LongConsumer listener) {
        this.sourceStream = sourceStream;
        this.consumer = listener;
    }


    @Override
    public int readLine(byte[] b, int off, int len) throws IOException {
        int result =  sourceStream.readLine(b, off, len);
        count.addAndGet(result);
        return result;
    }
    //@Override
    public int read(ByteBuffer buffer) throws IOException {
        int result =  sourceStream.read(buffer);
        count.addAndGet(result);
        return result;
    }


    public boolean isFinished() {
        return sourceStream.isFinished();
    }

    public  boolean isReady() {
        return sourceStream.isReady();
    }


    public  void setReadListener(ReadListener readListener) {
        sourceStream.setReadListener(readListener);
    }


    @Override
    public int read() throws IOException {
        int r = sourceStream.read();
        if (r != -1) {
            count.incrementAndGet();
            consume();
        }
        return r;
    }


    public void consume() {
        consumer.accept(count.longValue());
    }
}
