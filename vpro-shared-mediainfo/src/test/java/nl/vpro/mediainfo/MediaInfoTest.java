package nl.vpro.mediainfo;

import lombok.extern.log4j.Log4j2;

import java.io.OutputStream;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import nl.vpro.util.CommandExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Log4j2
class MediaInfoTest {

    @Test
    public void testMediaInfo() {
        CommandExecutor mock = mock(CommandExecutor.class);
        when(mock.execute(any(OutputStream.class), any(OutputStream.class), any(String.class)))
            .thenAnswer(new Answer<Integer>() {
                @Override
                public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                    IOUtils.copy(getClass().getResourceAsStream("/sampleout.xml"), (OutputStream) invocationOnMock.getArguments()[0]);
                    return 0;
                }
            });
        when(mock.execute(any(), any(), any(), any())).thenReturn(0);

        MediaInfo mediaInfoCaller = new MediaInfo(mock);

        testMediaInfo(mediaInfoCaller);
    }



     void testMediaInfo(MediaInfo mediaInfoCaller) {

        MediaInfo.Result info = mediaInfoCaller.apply(Path.of("/Users/michiel/samples/portrait.mp4"));

        log.info("MediaInfo: {}", info);
        assertThat(info.containingRectangle().get().aspectRatio()).isEqualTo("9:16");

         assertThat(info.toString()).isEqualTo("/Users/michiel/samples/portrait.mp4 (video 9:16), bitrate: 19558.138671875 kbps, duration: PT50.072S");


    }

}
