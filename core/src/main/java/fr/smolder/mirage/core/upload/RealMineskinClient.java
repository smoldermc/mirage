package fr.smolder.mirage.core.upload;

import fr.smolder.mirage.core.model.SkinData;
import org.mineskin.JsoupRequestHandler;
import org.mineskin.data.ValueAndSignature;
import org.mineskin.data.Variant;
import org.mineskin.data.Visibility;
import org.mineskin.request.GenerateRequest;
import org.mineskin.response.GenerateResponse;
import org.slf4j.Logger;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class RealMineskinClient implements MineskinClient, AutoCloseable {
	private static final String USER_AGENT = "Mirage/0.1.0-SNAPSHOT";

	private final Logger logger;
	private final Visibility visibility;
	private final ExecutorService requestExecutor;
	private final ScheduledExecutorService scheduler;
	private final org.mineskin.MineSkinClient client;

	public RealMineskinClient(String apiKey, String visibility, Logger logger) {
		this.logger = logger;
		this.visibility = parseVisibility(visibility, logger);
		this.requestExecutor = Executors.newFixedThreadPool(2, daemonFactory("mirage-mineskin-request"));
		this.scheduler = Executors.newSingleThreadScheduledExecutor(daemonFactory("mirage-mineskin-scheduler"));
		this.client = org.mineskin.MineSkinClient.builder()
				.apiKey(apiKey)
				.userAgent(USER_AGENT)
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
				.visibility(visibility)
				.variant(Variant.AUTO);
		try {
			logger.info("Uploading tile {} to Mineskin with visibility {}.", tileHash, visibility.getName());
			GenerateResponse response = client.generate().submitAndWait(request).get();
			ValueAndSignature texture = response.getSkin().texture().data();
			logger.info(
					"Uploaded tile {} to Mineskin (duplicate={}, nextRequest={}).",
					tileHash,
					response.getSkin().duplicate(),
					response.getRateLimit() != null && response.getRateLimit().next() != null
							? response.getRateLimit().next().absolute()
							: "unknown"
			);
			return new SkinData(texture.value(), "");
		} catch (ExecutionException exception) {
			logger.error("Failed to upload tile {} to Mineskin.", tileHash, exception.getCause());
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

	private static Visibility parseVisibility(String configuredVisibility, Logger logger) {
		String normalized = configuredVisibility == null ? "" : configuredVisibility.trim().toUpperCase();
		try {
			return Visibility.valueOf(normalized);
		} catch (IllegalArgumentException exception) {
			logger.warn(
					"Unknown Mineskin visibility '{}', defaulting to '{}'.",
					configuredVisibility,
					Visibility.UNLISTED.getName()
			);
			return Visibility.UNLISTED;
		}
	}
}
