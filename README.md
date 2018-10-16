# okhttp-staleiferror-interceptor
An okhttp3 interceptor that implements stale-if-error Cache-Control directive

Example:
```
 OkHttpClient.Builder client = new OkHttpClient.Builder();
 client.addInterceptor(new StaleIfErrorInterceptor(1, TimeUnit.HOURS));
```

# Use with Gradle
add to your repositories

```
repositories {
    maven { url "https://jitpack.io" }
}
```

In your app build.gradle, add:  `compile "com.github.PeelTechnologies:okhttp-staleiferror-interceptor:1.0.1"`
