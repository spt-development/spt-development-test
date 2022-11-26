package com.spt.development.test.integration;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.auth.AuthCache;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

/**
 * Wrapper around {@link org.apache.hc.client5.http.classic.HttpClient} to simplify making calls to a HTTP interface. This class
 * is stateful storing the results of requests between calls. This makes it useful for use with Cucumber where one
 * step makes a HTTP call and subsequent steps assert the results of that call.
 */
public class HttpTestManager {
    private static final String DEFAULT_SCHEME = "http";
    private static final String DEFAULT_HOST = "localhost";

    private final String scheme;
    private final String host;

    private int port;
    private int statusCode;
    private Header[] responseHeaders;
    private String responseBody;

    /**
     * Creates a vanilla test manager instance using the default http schema and localhost host for HTTP calls.
     */
    public HttpTestManager() {
        this(DEFAULT_SCHEME, DEFAULT_HOST);
    }

    /**
     * Creates a test manager that makes http calls using the scheme and host provided.
     *
     * @param scheme the scheme to use for HTTP requests e.g. https.
     * @param host the host to make HTTP requests to e.g. 192.168.4.1.
     */
    public HttpTestManager(String scheme, String host) {
        this.scheme = scheme;
        this.host = host;
    }

    /**
     * Gets the scheme being used when making HTTP requests.
     *
     * @return the http scheme e.g. https.
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * Gets the host being used when making HTTP requests.
     *
     * @return the host e.g. localhost.
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets the port being use when making HTTP requests. See init.
     *
     * @return the port e.g. 8080.
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the HTTP status code returned by the last HTTP request.
     *
     * @return the HTTP status code returned by the last HTTP request e.g. 200.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Gets all of the response headers returned by the last HTTP request.
     *
     * @return the response headers returned by the last HTTP request.
     */
    public Header[] getResponseHeaders() {
        return Arrays.copyOf(responseHeaders, responseHeaders.length);
    }

    /**
     * Gets the value of the response header specified.
     *
     * @param name the name of the header to retrieve the value for.
     *
     * @return an optional header value.
     */
    public Optional<String> getResponseHeaderValue(String name) {
        return Arrays.stream(responseHeaders)
                .filter(h -> name.equals(h.getName()))
                .map(Header::getValue)
                .findFirst();
    }

    /**
     * Gets the response body (as a String) returned by the last HTTP request.
     *
     * @return the response body returned by the last HTTP request.
     */
    public String getResponseBody() {
        return responseBody;
    }

    /**
     * Sets the port used to make HTTP requests and resets the status code, response headers and response body returned
     * by the last HTTP request.
     *
     * @param port the port to use to make HTTP requests.
     */
    public void init(int port) {
        this.port = port;

        this.statusCode = 0;
        this.responseHeaders = null;
        this.responseBody = null;
    }

    /**
     * Performs a HTTP POST request, posting the contents of the resource in the request body.
     *
     * @param resource the resource path containing the request body.
     * @param contentType the content type of the resource body.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred reading the resource or making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPostRequest(String resource, ContentType contentType, String path, NameValuePair... parameters)
            throws IOException, URISyntaxException {

        try (final InputStream is = this.getClass().getResourceAsStream(resource)) {
            doPostRequest(is, contentType, path, parameters);
        }
    }

    /**
     * Performs a HTTP POST request, posting the contents of the stream in the request body.
     *
     * @param is a stream to read the request body from.
     * @param contentType the content type of the resource body.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPostRequest(InputStream is, ContentType contentType, String path, NameValuePair... parameters)
            throws IOException, URISyntaxException {

        doPostRequest(null, new InputStreamEntity(is, contentType), path, parameters);
    }

    /**
     * Performs a HTTP POST request using basic authentication, posting the contents of the resource in the request body.
     *
     * @param username the username used for authentication.
     * @param password the password used for authentication.
     * @param resource the resource path containing the request body.
     * @param contentType the content type of the resource body.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred reading the resource or making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPostRequest(String username, String password, String resource, ContentType contentType, String path, NameValuePair... parameters)
            throws IOException, URISyntaxException {

        try (final InputStream is = this.getClass().getResourceAsStream(resource)) {
            doPostRequest(basicCredentialsProvider(username, password), is, contentType, path, parameters);
        }
    }

    /**
     * Performs a HTTP POST request using basic authentication, posting the contents of the stream in the request body.
     *
     * @param username the username used for authentication.
     * @param password the password used for authentication.
     * @param is a stream to read the request body from.
     * @param contentType the content type of the resource body.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPostRequest(String username, String password, InputStream is, ContentType contentType, String path, NameValuePair... parameters)
            throws IOException, URISyntaxException {

        doPostRequest(basicCredentialsProvider(username, password), is, contentType, path, parameters);
    }

    /**
     * Performs a HTTP POST request with authentication, posting the contents of the stream in the request body.
     *
     * @param credentialsProvider used for authentication. This parameter is nullable if authentication is not required
     *                            or is/can be done through some other means such as with headers added to the request.
     * @param is a stream to read the request body from.
     * @param contentType the content type of the resource body.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPostRequest(CredentialsProvider credentialsProvider, InputStream is, ContentType contentType, String path,
                              NameValuePair... parameters) throws IOException, URISyntaxException {

        doPostRequest(credentialsProvider, new InputStreamEntity(is, contentType), path, parameters);
    }

    /**
     * Performs a HTTP POST request, posting the request body defined by the {@link HttpEntity}.
     *
     * @param httpEntity the request body definition.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPostRequest(HttpEntity httpEntity, String path, NameValuePair... parameters) throws IOException, URISyntaxException {
        doPostRequest(null, httpEntity, path, parameters);
    }

    /**
     * Performs a HTTP POST request using basic authentication, posting the request body defined by the {@link HttpEntity}.
     *
     * @param username the username used for authentication.
     * @param password the password used for authentication.
     * @param httpEntity the request body definition.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPostRequest(String username, String password, HttpEntity httpEntity, String path, NameValuePair... parameters)
            throws IOException, URISyntaxException {

        doPostRequest(basicCredentialsProvider(username, password), httpEntity, path, parameters);
    }

    /**
     * Performs a HTTP POST request with authentication, posting the request body defined by the {@link HttpEntity}.
     *
     * @param credentialsProvider used for authentication. This parameter is nullable if authentication is not required
     *                            or is/can be done through some other means such as with headers added to the request.
     * @param httpEntity the request body definition.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPostRequest(CredentialsProvider credentialsProvider, HttpEntity httpEntity, String path, NameValuePair... parameters)
            throws IOException, URISyntaxException {

        final Function<URI, HttpUriRequestBase> requestFactory = (uri) -> {
            final HttpPost request = new HttpPost(uri);
            request.setEntity(httpEntity);

            return request;
        };

        doRequest(credentialsProvider, requestFactory, path, parameters);
    }

    /**
     * Performs a HTTP PUT request, putting the contents of the resource in the request body.
     *
     * @param resource the resource path containing the request body.
     * @param contentType the content type of the resource body.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred reading the resource or making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPutRequest(String resource, ContentType contentType, String path, NameValuePair... parameters)
            throws IOException, URISyntaxException {

        try (final InputStream is = this.getClass().getResourceAsStream(resource)) {
            doPutRequest(is, contentType, path, parameters);
        }
    }

    /**
     * Performs a HTTP PUT request, putting the contents of the stream in the request body.
     *
     * @param is a stream to read the request body from.
     * @param contentType the content type of the resource body.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPutRequest(InputStream is, ContentType contentType, String path, NameValuePair... parameters)
            throws IOException, URISyntaxException {

        doPutRequest(null, new InputStreamEntity(is, contentType), path, parameters);
    }

    /**
     * Performs a HTTP PUT request using basic authentication, putting the contents of the resource in the request body.
     *
     * @param username the username used for authentication.
     * @param password the password used for authentication.
     * @param resource the resource path containing the request body.
     * @param contentType the content type of the resource body.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred reading the resource or making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPutRequest(String username, String password, String resource, ContentType contentType, String path, NameValuePair... parameters)
            throws IOException, URISyntaxException {

        try (final InputStream is = this.getClass().getResourceAsStream(resource)) {
            doPutRequest(basicCredentialsProvider(username, password), is, contentType, path, parameters);
        }
    }

    /**
     * Performs a HTTP PUT request using basic authentication, putting the contents of the stream in the request body.
     *
     * @param username the username used for authentication.
     * @param password the password used for authentication.
     * @param is a stream to read the request body from.
     * @param contentType the content type of the resource body.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPutRequest(String username, String password, InputStream is, ContentType contentType, String path, NameValuePair... parameters)
            throws IOException, URISyntaxException {

        doPutRequest(basicCredentialsProvider(username, password), is, contentType, path, parameters);
    }

    /**
     * Performs a HTTP PUT request with authentication, putting the contents of the stream in the request body.
     *
     * @param credentialsProvider used for authentication. This parameter is nullable if authentication is not required
     *                            or is/can be done through some other means such as with headers added to the request.
     * @param is a stream to read the request body from.
     * @param contentType the content type of the resource body.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPutRequest(CredentialsProvider credentialsProvider, InputStream is, ContentType contentType, String path,
                              NameValuePair... parameters) throws IOException, URISyntaxException {

        doPutRequest(credentialsProvider, new InputStreamEntity(is, contentType), path, parameters);
    }

    /**
     * Performs a HTTP PUT request, putting the request body defined by the {@link HttpEntity}.
     *
     * @param httpEntity the request body definition.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPutRequest(HttpEntity httpEntity, String path, NameValuePair... parameters) throws IOException, URISyntaxException {
        doPutRequest(null, httpEntity, path, parameters);
    }

    /**
     * Performs a HTTP PUT request using basic authentication, putting the request body defined by the {@link HttpEntity}.
     *
     * @param username the username used for authentication.
     * @param password the password used for authentication.
     * @param httpEntity the request body definition.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPutRequest(String username, String password, HttpEntity httpEntity, String path, NameValuePair... parameters)
            throws IOException, URISyntaxException {

        doPutRequest(basicCredentialsProvider(username, password), httpEntity, path, parameters);
    }

    /**
     * Performs a HTTP PUT request with authentication, putting the request body defined by the {@link HttpEntity}.
     *
     * @param credentialsProvider used for authentication. This parameter is nullable if authentication is not required
     *                            or is/can be done through some other means such as with headers added to the request.
     * @param httpEntity the request body definition.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPutRequest(CredentialsProvider credentialsProvider, HttpEntity httpEntity, String path, NameValuePair... parameters)
            throws IOException, URISyntaxException {

        final Function<URI, HttpUriRequestBase> requestFactory = (uri) -> {
            final HttpPut request = new HttpPut(uri);
            request.setEntity(httpEntity);

            return request;
        };

        doRequest(credentialsProvider, requestFactory, path, parameters);
    }

    /**
     * Performs a HTTP PATCH request using basic authentication, with no request body.
     *
     * @param username the username used for authentication.
     * @param password the password used for authentication.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred reading the resource or making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPatchRequest(String username, String password, String path, NameValuePair... parameters) throws IOException, URISyntaxException {
        doPatchRequest(basicCredentialsProvider(username, password), new StringEntity(StringUtils.EMPTY), path, parameters);
    }

    /**
     * Performs a HTTP PATCH request using basic authentication, posting the contents of the stream in the request body.
     *
     * @param username the username used for authentication.
     * @param password the password used for authentication.
     * @param is a stream to read the request body from.
     * @param contentType the content type of the resource body.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPatchRequest(String username, String password, InputStream is, ContentType contentType, String path, NameValuePair... parameters)
            throws IOException, URISyntaxException {

        doPatchRequest(basicCredentialsProvider(username, password), is, contentType, path, parameters);
    }

    /**
     * Performs a HTTP PATCH request with authentication, posting the contents of the stream in the request body.
     *
     * @param credentialsProvider used for authentication. This parameter is nullable if authentication is not required
     *                            or is/can be done through some other means such as with headers added to the request.
     * @param is a stream to read the request body from.
     * @param contentType the content type of the resource body.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPatchRequest(CredentialsProvider credentialsProvider, InputStream is, ContentType contentType, String path,
                               NameValuePair... parameters) throws IOException, URISyntaxException {

        doPatchRequest(credentialsProvider, new InputStreamEntity(is, contentType), path, parameters);
    }

    /**
     * Performs a HTTP PATCH request, posting the request body defined by the {@link HttpEntity}.
     *
     * @param httpEntity the request body definition.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPatchRequest(HttpEntity httpEntity, String path, NameValuePair... parameters) throws IOException, URISyntaxException {
        doPatchRequest(null, httpEntity, path, parameters);
    }

    /**
     * Performs a HTTP PATCH request using basic authentication, posting the request body defined by the {@link HttpEntity}.
     *
     * @param username the username used for authentication.
     * @param password the password used for authentication.
     * @param httpEntity the request body definition.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPatchRequest(String username, String password, HttpEntity httpEntity, String path, NameValuePair... parameters)
            throws IOException, URISyntaxException {

        doPatchRequest(basicCredentialsProvider(username, password), httpEntity, path, parameters);
    }

    /**
     * Performs a HTTP PATCH request with authentication, posting the request body defined by the {@link HttpEntity}.
     *
     * @param credentialsProvider used for authentication. This parameter is nullable if authentication is not required
     *                            or is/can be done through some other means such as with headers added to the request.
     * @param httpEntity the request body definition.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doPatchRequest(CredentialsProvider credentialsProvider, HttpEntity httpEntity, String path, NameValuePair... parameters)
            throws IOException, URISyntaxException {

        final Function<URI, HttpUriRequestBase> requestFactory = (uri) -> {
            final HttpPatch request = new HttpPatch(uri);
            request.setEntity(httpEntity);

            return request;
        };

        doRequest(credentialsProvider, requestFactory, path, parameters);
    }

    /**
     * Performs a HTTP GET request.
     *
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doGetRequest(String path, NameValuePair... parameters) throws IOException, URISyntaxException {
        doRequest(null, HttpGet::new, path, parameters);
    }

    /**
     * Performs a HTTP GET request using basic authentication.
     *
     * @param username the username used for authentication.
     * @param password the password used for authentication.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doGetRequest(String username, String password, String path, NameValuePair... parameters) throws IOException, URISyntaxException {
        doGetRequest(basicCredentialsProvider(username, password), path, parameters);
    }

    /**
     * Performs a HTTP GET request with authentication.
     *
     * @param credentialsProvider used for authentication. This parameter is nullable if authentication is not required
     *                            or is/can be done through some other means such as with headers added to the request.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doGetRequest(CredentialsProvider credentialsProvider, String path, NameValuePair... parameters)
            throws IOException, URISyntaxException {

        doRequest(credentialsProvider, HttpGet::new, path, parameters);
    }

    /**
     * Performs a HTTP DELETE request.
     *
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doDeleteRequest(String path, NameValuePair... parameters) throws IOException, URISyntaxException {
        doRequest(null, HttpDelete::new, path, parameters);
    }

    /**
     * Performs a HTTP DELETE request using basic authentication.
     *
     * @param username the username used for authentication.
     * @param password the password used for authentication.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doDeleteRequest(String username, String password, String path, NameValuePair... parameters) throws IOException, URISyntaxException {
        doDeleteRequest(basicCredentialsProvider(username, password), path, parameters);
    }

    /**
     * Creates a new {@link BasicCredentialsProvider} with the credentials set to the username and password provided.
     *
     * @param username the username credential.
     * @param password the password credential.
     *
     * @return a new {@link BasicCredentialsProvider}.
     */
    public static CredentialsProvider basicCredentialsProvider(String username, String password) {
        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(null, -1), new UsernamePasswordCredentials(username, password.toCharArray()));

        return credentialsProvider;
    }

    /**
     * Performs a HTTP DELETE request with authentication.
     *
     * @param credentialsProvider used for authentication. This parameter is nullable if authentication is not required
     *                            or is/can be done through some other means such as with headers added to the request.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doDeleteRequest(CredentialsProvider credentialsProvider, String path, NameValuePair... parameters)
            throws IOException, URISyntaxException {

        doRequest(credentialsProvider, HttpDelete::new, path, parameters);
    }

    /**
     * Performs a generic HTTP request with authentication, defined by the {@link HttpUriRequestBase}.
     *
     * @param credentialsProvider used for authentication. This parameter is nullable if authentication is not required
     *                            or is/can be done through some other means such as with headers added to the request.
     * @param requestFactory a factory for creating a request for a {@link URI}.
     * @param path the path to make the HTTP request to.
     * @param parameters optional query parameters to include in the request.
     *
     * @throws IOException a problem occurred making the HTTP request.
     * @throws URISyntaxException a problem occurred constructing the URI to make the HTTP request to.
     */
    public void doRequest(CredentialsProvider credentialsProvider,
                          Function<URI, HttpUriRequestBase> requestFactory,
                          String path, NameValuePair... parameters)
            throws URISyntaxException, IOException {

        final HttpUriRequestBase request = requestFactory.apply(
                new URIBuilder()
                        .setScheme(scheme)
                        .setHost(host)
                        .setPort(port)
                        .setPath(path)
                        .setParameters(parameters)
                        .build()
        );

        doRequest(credentialsProvider, request);
    }

    /**
     * Performs a generic HTTP request with authentication, defined by the {@link HttpUriRequestBase}.
     *
     * @param credentialsProvider used for authentication. This parameter is nullable if authentication is not required
     *                            or is/can be done through some other means such as with headers added to the request.
     * @param request the defintion of the request to make.
     *
     * @throws IOException a problem occurred making the HTTP request.
     */
    public void doRequest(CredentialsProvider credentialsProvider, HttpUriRequestBase request) throws IOException {

        try (final CloseableHttpClient client = createHttpClient();
             final CloseableHttpResponse response = client.execute(request, createHttpContext(credentialsProvider))) {

            statusCode = response.getCode();
            responseHeaders = response.getHeaders();
            responseBody = null;

            if (response.getEntity() != null) {
                responseBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            }
        }
    }

    private CloseableHttpClient createHttpClient() {
        return HttpClients.custom()
                .disableAutomaticRetries()
                .build();
    }

    // Required to perform preemptive authentication. Without preemptive authentication posts will fail because the
    // port request will be performed initially without sending credentials and then performed again with the
    // credentials, however you can't retry with the same HttpPost entity.
    private HttpContext createHttpContext(CredentialsProvider credentialsProvider) {
        if (credentialsProvider != null) {
            final HttpHost targetHost = new HttpHost(scheme, host, port);
            final BasicScheme authScheme = new BasicScheme();

            authScheme.initPreemptive(credentialsProvider.getCredentials(
                    new AuthScope(host, port), null
            ));

            final AuthCache authCache = new BasicAuthCache();
            authCache.put(targetHost, authScheme);

            final HttpClientContext context = HttpClientContext.create();
            context.setCredentialsProvider(credentialsProvider);
            context.setAuthCache(authCache);

            return context;
        }
        return null;
    }
}
