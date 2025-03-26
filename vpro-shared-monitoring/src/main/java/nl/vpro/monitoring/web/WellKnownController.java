package nl.vpro.monitoring.web;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.coyote.BadRequestException;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


/**
 * Support for /.well-known/security.txt. Picks up 'SECURITY_TXT' environment variable, and if set, services it out on /.well-known/security.txt.
 *
 * @since 5.6
 */
@Lazy(false)
@RestController
@RequestMapping(produces = MediaType.TEXT_PLAIN_VALUE)
@Slf4j
public class WellKnownController {


    //if you need to test in macos, use /etc/synthetic.conf (/System/Library/Filesystems/apfs.fs/Contents/Resources/apfs.util -t)

    Path dir = Path.of("/well-known");

    @GetMapping("/{file}")
    public String securityText(@PathVariable(name="file") String fileName) throws IOException {

        Path file = dir.resolve(Path.of(fileName));

        if (!file.toAbsolutePath().normalize().startsWith(dir.toAbsolutePath().normalize())) {
            throw new BadRequestException();
        }
        if (Files.isReadable(file)) {
            return Files.readString(file);
        }

        throw new ResponseStatusException(
            HttpStatusCode.valueOf(404),
            "No %s found".formatted(fileName));

    }
}
