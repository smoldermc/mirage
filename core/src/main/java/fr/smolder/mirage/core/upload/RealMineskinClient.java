package fr.smolder.mirage.core.upload;

import fr.smolder.mirage.core.model.SkinData;
import org.mineskin.JsoupRequestHandler;
import org.mineskin.data.ValueAndSignature;
import org.mineskin.data.Variant;
import org.mineskin.data.Visibility;
import org.mineskin.request.GenerateRequest;
import org.mineskin.response.GenerateResponse;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class RealMineskinClient implements MineskinClient, AutoCloseable {
    private final ExecutorService requestExecutor;
    private final ScheduledExecutorService scheduler;
    private final org.mineskin.MineSkinClient client;

    public RealMineskinClient(String apiKey) {
        this.requestExecutor = Executors.newFixedThreadPool(2, daemonFactory("mirage-mineskin-request"));
        this.scheduler = Executors.newSingleThreadScheduledExecutor(daemonFactory("mirage-mineskin-scheduler"));
        this.client = org.mineskin.MineSkinClient.builder()
                .apiKey(apiKey)
                .getExecutor(requestExecutor)
                .generateExecutor(requestExecutor)
                .generateRequestScheduler(scheduler)
                .getRequestScheduler(scheduler)
                .jobCheckScheduler(scheduler)
                .requestHandler(JsoupRequestHandler::new)
                .build();
    }

    @Override
    public SkinData upload(String tileHash, BufferedImage skinImage) throws IOException, InterruptedException {
        GenerateRequest request = GenerateRequest.upload(skinImage)
                .name("mirage-" + tileHash.substring(0, Math.min(tileHash.length(), 16)))
                .visibility(Visibility.PRIVATE)
                .variant(Variant.AUTO);
        try {
            GenerateResponse response = client.generate().submitAndWait(request).get();
            ValueAndSignature texture = response.getSkin().texture().data();
            return new SkinData(texture.value(), texture.signature());
        } catch (ExecutionException exception) {
            throw new IOException("Failed to upload a tile to Mineskin.", exception.getCause());
        }
    }

    @Override
    public void close() {
        requestExecutor.shutdownNow();
        scheduler.shutdownNow();
    }

    private static ThreadFactory daemonFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
