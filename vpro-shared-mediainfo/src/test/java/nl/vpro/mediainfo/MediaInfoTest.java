package nl.vpro.mediainfo;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import nl.vpro.util.CommandExecutor;

import static java.util.Objects.requireNonNull;
import static nl.vpro.test.util.jackson2.Jackson2TestUtil.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Log4j2
class MediaInfoTest {

    @Test
    public void testMediaInfo() throws IOException {
        CommandExecutor mock = mock(CommandExecutor.class);
        when(mock.execute(any(OutputStream.class), any(OutputStream.class), any(String.class)))
            .thenAnswer(new Answer<Integer>() {
                @Override
                public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                    IOUtils.copy(requireNonNull(getClass().getResourceAsStream("/sampleout.xml")), (OutputStream) invocationOnMock.getArguments()[0]);
                    return 0;
                }
            });
        when(mock.execute(any(), any(), any(), any())).thenReturn(0);

        MediaInfoService mediaInfoCaller = new MediaInfoService(mock);

        testMediaInfo(mediaInfoCaller);

    }



    void testMediaInfo(MediaInfoService mediaInfoCaller) throws IOException {
        Path test = Files.createTempFile("test", ".mp4");
        //MediaInfo.Result info = mediaInfoCaller.apply(Path.of("/Users/michiel/samples/portrait.mp4"));
        MediaInfo info = mediaInfoCaller.apply(test);

        log.info("MediaInfo: {}", info);
        assertThat(info.circumscribedRectangle().get().aspectRatio()).isEqualTo("9:16");

        assertThat(info.toString()).isEqualTo("video 9:16, bitrate: 19558.139 kbps, duration: PT50.072S");

        assertThatJson(info.basic()).isSimilarTo("""
            {
              "name" : "portrait.mp4",
              "duration" : 50072,
              "isVideo" : true,
              "vertical" : true,
              "aspectRatio" : "9:16"
            }
            """);


        assertThatJson(info)

            .ignore("/path")
            .isSimilarTo("""
            {
              "mediaInfo" : {
                "creatingLibrary" : {
                  "value" : "MediaInfoLib",
                  "version" : "25.04",
                  "url" : "https://mediaarea.net/MediaInfo"
                },
                "media" : [ { }, {
                  "track" : [ {
                    "AudioCount" : 1,
                    "CodecID_Compatible" : "isom/iso2/mp41",
                    "CodecID" : "isom",
                    "DataSize" : 125310855,
                    "Duration" : 50.072,
                    "Encoded_Date" : "2025-05-04 18:29:03 UTC",
                    "Encoded_Hardware_CompanyName" : "Google",
                    "Encoded_Hardware_Name" : "Pixel 5",
                    "FileExtension" : "mp4",
                    "File_Modified_Date_Local" : "2025-05-28 10:03:30",
                    "File_Modified_Date" : "2025-05-28 08:03:30 UTC",
                    "FileSize" : "125352337",
                    "FooterSize" : 41454,
                    "Format" : "MPEG-4",
                    "Format_Profile" : "Base Media",
                    "FrameCount" : 1505,
                    "FrameRate" : 30.056,
                    "HeaderSize" : 28,
                    "IsStreamable" : "No",
                    "OtherCount" : 1,
                    "OverallBitRate" : 2.0027534E7,
                    "Recorded_Location" : "+53.3029+5.0523/",
                    "StreamSize" : 247096,
                    "Tagged_Date" : "2025-05-04 18:29:03 UTC",
                    "VideoCount" : 1,
                    "type" : "General"
                  }, {
                    "extra" : {
                      "anies" : [ "<CodecConfigurationBox xmlns=\\"https://mediaarea.net/mediainfo\\" xmlns:xsi=\\"http://www.w3.org/2001/XMLSchema-instance\\">hvcC</CodecConfigurationBox>" ]
                    },
                    "BitDepth" : 8,
                    "BitRate" : 1.9795696E7,
                    "ChromaSubsampling" : "4:2:0",
                    "CodecID" : "hvc1",
                    "ColorSpace" : "YUV",
                    "colour_description_present" : "Yes",
                    "colour_description_present_Source" : "Container / Stream",
                    "colour_primaries" : "BT.601 PAL",
                    "colour_primaries_Source" : "Container / Stream",
                    "colour_range" : "Full",
                    "colour_range_Source" : "Container / Stream",
                    "DisplayAspectRatio" : 1.778,
                    "Duration" : 50.072,
                    "Encoded_Date" : "2025-05-04 18:29:03 UTC",
                    "Format_Level" : "5.1",
                    "Format" : "HEVC",
                    "Format_Profile" : "Main",
                    "Format_Tier" : "Main",
                    "FrameCount" : 1505,
                    "FrameRate_Maximum" : 30.1,
                    "FrameRate_Minimum" : 29.99,
                    "FrameRate" : 30.056,
                    "FrameRate_Mode" : "VFR",
                    "FrameRate_Real" : 30.0,
                    "Height" : 1080,
                    "ID" : "3",
                    "Language" : "en",
                    "matrix_coefficients" : "BT.601",
                    "matrix_coefficients_Original" : "BT.470 System B/G",
                    "matrix_coefficients_Original_Source" : "Stream",
                    "matrix_coefficients_Source" : "Container",
                    "PixelAspectRatio" : 1.0,
                    "Rotation" : "90.000",
                    "Sampled_Height" : 1080,
                    "Sampled_Width" : 1920,
                    "Standard" : "NTSC",
                    "Stored_Height" : 1088,
                    "StreamOrder" : "2",
                    "StreamSize" : 123904231,
                    "Tagged_Date" : "2025-05-04 18:29:03 UTC",
                    "Title" : "VideoHandle",
                    "transfer_characteristics" : "BT.709",
                    "transfer_characteristics_Original" : "BT.601",
                    "transfer_characteristics_Original_Source" : "Stream",
                    "transfer_characteristics_Source" : "Container",
                    "Width" : 1920,
                    "type" : "Video"
                  }, {
                    "BitRate" : 192000.0,
                    "BitRate_Mode" : "CBR",
                    "ChannelLayout" : "L R",
                    "ChannelPositions" : "Front: L R",
                    "Channels" : 2,
                    "CodecID" : "mp4a-40-2",
                    "Compression_Mode" : "Lossy",
                    "Duration" : 50.054,
                    "Encoded_Date" : "2025-05-04 18:29:03 UTC",
                    "Format_AdditionalFeatures" : "LC",
                    "Format" : "AAC",
                    "FrameCount" : 2346,
                    "FrameRate" : 46.875,
                    "ID" : "2",
                    "Language" : "en",
                    "SamplesPerFrame" : 1024.0,
                    "SamplingCount" : 2402592,
                    "SamplingRate" : 48000.0,
                    "StreamOrder" : "1",
                    "StreamSize" : 1201010,
                    "Tagged_Date" : "2025-05-04 18:29:03 UTC",
                    "Title" : "SoundHandle",
                    "type" : "Audio"
                  }, {
                    "extra" : {
                      "anies" : [ "<Encoded_Date xmlns=\\"https://mediaarea.net/mediainfo\\" xmlns:xsi=\\"http://www.w3.org/2001/XMLSchema-instance\\">2025-05-04 18:29:03 UTC</Encoded_Date>", "<Tagged_Date xmlns=\\"https://mediaarea.net/mediainfo\\" xmlns:xsi=\\"http://www.w3.org/2001/XMLSchema-instance\\">2025-05-04 18:29:03 UTC</Tagged_Date>" ]
                    },
                    "BitRate_Mode" : "VBR",
                    "CodecID" : "mett-application/meta",
                    "Duration" : 50.072,
                    "Format" : "mett-application/meta",
                    "FrameCount" : 1505,
                    "ID" : "1",
                    "Language" : "en",
                    "StreamOrder" : "0",
                    "Title" : "MetaHandle",
                    "Type" : "meta",
                    "type" : "Other"
                  } ],
                  "ref" : "/Users/michiel/samples/portrait.mp4"
                } ],
                "version" : "2.0"
              },
              "status" : 0,
              "name" : "portrait.mp4"
            }
            """);



    }

}
