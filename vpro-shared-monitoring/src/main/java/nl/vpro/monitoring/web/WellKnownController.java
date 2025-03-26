package nl.vpro.monitoring.web;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
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

    Path file = Path.of("/well-known/security.txt");

    @GetMapping("/security.txt")
    public String securityText() throws IOException {

        if (Files.isReadable(file)) {
            return Files.readString(file);
        }
        String securityTxt = System.getenv("SECURITY_TXT");
        if (StringUtils.isNotBlank(securityTxt)) {
            return securityTxt;
        } else {
            throw new ResponseStatusException(
                HttpStatusCode.valueOf(404),
                "No security.txt found");
        }
    }
}
