package nl.vpro.monitoring.web;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.text.StringSubstitutor;
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


    @Inject
    HttpServletRequest request;
    //if you need to test in macos, use /etc/synthetic.conf (/System/Library/Filesystems/apfs.fs/Contents/Resources/apfs.util -t)

    Path dir = Path.of("/well-known");

    @GetMapping("/{file}")
    public String securityText(@PathVariable(name="file") String fileName) throws IOException {

        Path file = dir.resolve(Path.of(fileName));

        if (!file.toAbsolutePath().normalize().startsWith(dir.toAbsolutePath().normalize())) {
            throw new BadRequestException();
        }
        if (Files.isReadable(file)) {
            var placeHolders = Map.of(
                "REQUEST", getOriginalRequestURL(request)
            );
            StringSubstitutor substitutor = new StringSubstitutor(placeHolders);
            return substitutor.replace(Files.readString(file));
        }

        throw new ResponseStatusException(
            HttpStatusCode.valueOf(404),
            "No %s found".formatted(fileName));

    }


    private static  String getOriginalRequestURL(HttpServletRequest request) {

        // Get scheme - check forwarded headers first
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null) {
            scheme = request.getScheme();
        }

        // Get host
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null) {
            host = request.getServerName();
        }

        // Get port
        int port = request.getServerPort();
        String portHeader = request.getHeader("X-Forwarded-Port");
        if (portHeader != null) {
            try {
                port = Integer.parseInt(portHeader);
            } catch (NumberFormatException e) {
                // Use default port if header is invalid
            }
        }

        // Build URL with port only if non-standard
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(host);
        if (!((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443))) {
            url.append(":").append(port);
        }

        // Add path and query string
        url.append(request.getRequestURI());
        if (request.getQueryString() != null) {
            url.append("?").append(request.getQueryString());
    }

        return url.toString();
    }
}
