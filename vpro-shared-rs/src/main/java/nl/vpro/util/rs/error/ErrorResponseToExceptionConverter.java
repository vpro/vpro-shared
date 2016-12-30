package nl.vpro.util.rs.error;

import nl.vpro.util.rs.transfer.ErrorResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Date: 2-5-12
 * Time: 18:24
 *
 * @author Ernst Bunders
 */
public class ErrorResponseToExceptionConverter {
    private RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(ErrorResponseToExceptionConverter.class);

    public ErrorResponseToExceptionConverter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public RuntimeException convert(String errorResponseBody, MediaType mediaType) {
        ErrorResponse response = extract(errorResponseBody, mediaType, restTemplate.getMessageConverters());
        if (response.getDataError() != null) {
            return new DataErrorException(response.getMessage(), response.getDataError());
        } else {
            if (response.getStatus() == 404) {
                return new NotFoundException(response.getMessage());
            } else {
                return new ServerErrorException(response.getMessage());
            }
        }
    }

    public ErrorResponse extract(final String response, MediaType contentType, List<HttpMessageConverter<?>> messageConverters) {
        Class responseType = ErrorResponse.class;
        if (StringUtils.isBlank(response)) {
            return null;
        }
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        HttpInputMessage inputMessage = new HttpInputMessage() {
            @Override
            public InputStream getBody() throws IOException {
                return new ByteArrayInputStream(response.getBytes());
            }

            @Override
            public HttpHeaders getHeaders() {
                return null;
            }
        };

        for (HttpMessageConverter messageConverter : messageConverters) {
            if (messageConverter.canRead(responseType, contentType)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Reading [" + responseType.getName() + "] as \"" + contentType
                        + "\" using [" + messageConverter + "]");
                }
                try {
                    return (ErrorResponse) messageConverter.read(responseType, inputMessage);
                } catch (IOException e) {
                    e.printStackTrace();//this should not happen
                }
            }
        }

        throw new RestClientException(
            "Could not extract response: no suitable HttpMessageConverter found for response type [" +
                responseType.getName() + "] and content type [" + contentType + "]");
    }
}
