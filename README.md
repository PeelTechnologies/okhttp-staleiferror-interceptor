# okhttp-staleiferror-interceptor
An okhttp3 interceptor that implements `stale-if-error` Cache-Control directive

HTTP protocol (in [RFC 5861](https://tools.ietf.org/html/rfc5861)) defines a Cache-Control extension `stale-if-error` that allows a stale cached response to be used if the server is not reachable. OkHttp library, unfortunately, doesn’t implement this extension, hence, we created this project to implement this as an interceptor.

We use the approach presented in the OkHttp [issue 1083](https://github.com/square/okhttp/issues/1083)) by retrying a failed request with an `only-if-cached` Cache-Control directive with `max-age` of 4 weeks. 
Example:
```
 OkHttpClient.Builder client = new OkHttpClient.Builder();
 client.addInterceptor(new StaleIfErrorInterceptor());
 
```
The default 4 weeks `max-age` duration can be configured with constructor parameters:
```
 client.addInterceptor(new StaleIfErrorInterceptor(1, TimeUnit.DAYS));
```

# Use with Gradle
add to your repositories

```
repositories {
    maven { url "https://jitpack.io" }
}
```

In your app build.gradle, add:  `compile "com.github.PeelTechnologies:okhttp-staleiferror-interceptor:1.0.1"`
