package nl.vpro.elasticsearchclient;

import lombok.Getter;
import vc.inreach.aws.request.AWSSigner;
import vc.inreach.aws.request.AWSSigningRequestInterceptor;

import java.time.LocalDateTime;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClientBuilder;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

/**
 * @author Michiel Meeuwissen
 * @since 2.18
 */
public class AWSConfigCallback implements RestClientBuilder.HttpClientConfigCallback {

    private final AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();

    @Getter
    private final String region;

    @Getter
    private final String serviceName;

    public AWSConfigCallback(String region, String serviceName) {
        this.region = region;
        this.serviceName = serviceName;
    }

    @Override
    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
        AWSSigner awsSigner = new AWSSigner(credentialsProvider, region, serviceName, LocalDateTime::now);
        HttpRequestInterceptor interceptor = new AWSSigningRequestInterceptor(awsSigner);
        httpClientBuilder.addInterceptorLast(interceptor);
        return httpClientBuilder;
    }
}
