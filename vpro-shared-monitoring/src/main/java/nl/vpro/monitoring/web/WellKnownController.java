package nl.vpro.monitoring.web;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import nl.vpro.web.HttpServletRequestUtils;

import static org.springframework.http.HttpStatusCode.valueOf;


/**
 * Support for files under /.well-known/ Noticeably <a href="https://securitytxt.org/">security.txt<a>
 * <p>
 * These files are just served from the directory '/well-known'. The idea is to mount a config map there (assuming kubernetes)
 * <p>
 * e.g. a file like 'security.txt' can be mounted
 * <pre>
 * Contact: mailto:poms@omreop.nl
 * Expires: 2025-12-31T23:59:59+00:00
 * Preferred-Languages: en,nl
 * Canonical: ${REQUEST}
 * </pre>
 *
 * @since 5.6
 * @author Michiel Meeuwissen
 */
@Lazy(false)
@RestController
@RequestMapping(produces = MediaType.TEXT_PLAIN_VALUE)
@Slf4j
public class WellKnownController {


    @Autowired
    HttpServletRequest request;

    @Inject
    HttpServletResponse response;

    /**
     *  This supposes that the directory '/well-known' is available in the filesystem. The idea is that can just make it if you need to test this on
     *  localhost for some reason
     *  <p>
     *  if you need to test in macos, use /etc/synthetic.conf (/System/Library/Filesystems/apfs.fs/Contents/Resources/apfs.util -t)
     * e.g.:
     * <pre>
     * cat /etc/synthetic.conf
     * data	Users/michiel/data
     * share	Users/michiel/share
     * well-known	Users/michiel/well-known
     * </pre>
     */

    final static Path DIR = Path.of("/well-known");

    @GetMapping("/{file}")
    public String wellKnownFile(@PathVariable(name="file") String fileName) throws IOException {

        Path file = DIR.resolve(Path.of(fileName));

        if (!file.toAbsolutePath().normalize().startsWith(DIR)) {
            throw new ResponseStatusException(valueOf(400));
        }
        if (Files.isReadable(file)) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=3600");
            var placeHolders = Map.of(
                "REQUEST", HttpServletRequestUtils.getOriginalRequestURL(request)
            );
            StringSubstitutor substitutor = new StringSubstitutor(placeHolders);
            return substitutor.replace(Files.readString(file));
        }

        throw new ResponseStatusException(valueOf(404), "No %s found".formatted(fileName));

    }



}
