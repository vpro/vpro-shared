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
class MediaInfoCallerTest {



    @Test
    public void getMediaInfo() {
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

        MediaInfoCaller mediaInfoCaller = new MediaInfoCaller();
        MediaInfoCaller.Result info = mediaInfoCaller.apply(Path.of("/Users/michiel/samples/portrait.mp4"));

        log.info("MediaInfo: {}", info.displayAspectRatio());
        assertThat(info.displayAspectRatio()).contains("9:16");
    }

}
