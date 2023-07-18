package com.gopay.transcript;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

@Getter
@Setter
@Slf4j
public class TranscriptBaritoBatchAppender extends AppenderBase<ILoggingEvent> {

    private static final String CONTENT_TYPE_HEADER_KEY = "Content-Type";
    private static final String CACHE_CONTROL_HEADER_KEY = "Cache-Control";
    private static final String BARITO_APP_GROUP_SECRET_HEADER_KEY = "X-App-Group-Secret";
    private static final String BARITO_APP_NAME_HEADER_KEY = "X-App-Name";
    private static final String BARITO_BATCH_PRODUCE_URL = "https://barito-router.golabs.io/produce_batch";
    private static final Long DEFAULT_BATCH_SIZE = 10L;

    private String appName;
    private String appGroupSecret;
    private String httpClientLogLevel;
    private OkHttpClient httpClient;
    private Long batchSize;
    private boolean isDisabled;

    private Queue<String> logBuffer;
    private ExecutorService executor;

    public TranscriptBaritoBatchAppender() {
        super();
    }

    public TranscriptBaritoBatchAppender(@NonNull String appName, @NonNull String appGroupSecret,
                                         Long batchSize, String httpClientLogLevel, boolean isDisabled) {
        this.appName = appName;
        this.appGroupSecret = appGroupSecret;
        this.httpClientLogLevel = httpClientLogLevel;
        this.isDisabled = isDisabled;

        if (Objects.nonNull(batchSize)) {
            this.batchSize = batchSize;
        } else {
            this.batchSize = DEFAULT_BATCH_SIZE;
        }
    }

    @Override
    public void start() {
        super.start();

        final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();

        if (Objects.isNull(httpClientLogLevel)) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        } else {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.valueOf(httpClientLogLevel));
        }

        final OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        okHttpClientBuilder.connectionPool(new ConnectionPool());
        okHttpClientBuilder.retryOnConnectionFailure(false);
        okHttpClientBuilder.interceptors().add(loggingInterceptor);

        httpClient = okHttpClientBuilder.build();
        logBuffer = new ConcurrentLinkedQueue<>();
        executor = Executors.newCachedThreadPool();
    }

    @Override
    public void stop() {
        super.stop();
        try {
            if (httpClient != null) {
                httpClient.dispatcher().executorService().shutdown();
                httpClient.connectionPool().evictAll();
            }
            if (executor != null) {
                executor.shutdown();
            }
        } catch (Exception e) {
            log.error("Error when stopping transcript appender due to : ", e);
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        final String logMessage = event.getFormattedMessage();
        if (isDisabled) {
            log.warn("TranscriptBaritoBatchAppender was disabled, skipping submitting logs to barito and log to console instead");
            log.info(event.getFormattedMessage());
            return;
        }
        logBuffer.add(logMessage);

        if (logBuffer.size() >= batchSize) {
            sendBufferedLogsAsync();
        }
    }

    protected void sendBufferedLogsAsync() {
        executor.submit(() -> {
            try {
                final StringBuilder bodyBuilder = new StringBuilder();
                bodyBuilder.append("{\"items\":[");
                while (!logBuffer.isEmpty()) {
                    final var logMessage = logBuffer.poll();
                    bodyBuilder.append(logMessage).append(",");
                }
                bodyBuilder.deleteCharAt(bodyBuilder.length() - 1);
                bodyBuilder.append("]}");

                final RequestBody requestBody = RequestBody.create(MediaType.get("application/json"), compressData(bodyBuilder.toString()));
                final Request request = new Request.Builder()
                        .url(BARITO_BATCH_PRODUCE_URL)
                        .addHeader(CONTENT_TYPE_HEADER_KEY, "application/json")
                        .addHeader(CACHE_CONTROL_HEADER_KEY, "no-cache")
                        .addHeader(BARITO_APP_GROUP_SECRET_HEADER_KEY, appGroupSecret)
                        .addHeader(BARITO_APP_NAME_HEADER_KEY, appName)
                        .post(requestBody)
                        .build();

                log.info("Successfully Submitting transcript to barito");
                httpClient.newCall(request).execute();
            } catch (Exception e) {
                log.error("Error when submitting transcript log to barito due to : ", e);
            }
        });
    }

    protected byte[] compressData(String data) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(data.getBytes());
        }
        return byteArrayOutputStream.toByteArray();
    }
}
