package dev.dmrstudio.vod_autouploader.service;

import dev.dmrstudio.vod_autouploader.model.UploadJob;
import dev.dmrstudio.vod_autouploader.model.UploadStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
public class YoutubeService {

    private static final int ALIGNMENT       = 262_144; // 256 KB — requerido por YouTube
    private static final int DOWNLOAD_WINDOW = 10;      // descargas paralelas
    private static final int MAX_RETRIES     = 6;       // reintentos máximos por chunk
    private static final long INITIAL_DELAY  = 2_000;   // 2 s de espera inicial
    private static final long MAX_DELAY      = 60_000;  // tope de 60 s

    private final RestClient restClient;
    private final KickService kickService;
    private final UploadJobService uploadJobService;
    private final ProgressService progressService;

    public YoutubeService(KickService kickService,
                          UploadJobService uploadJobService,
                          ProgressService progressService) {
        this.restClient = RestClient.create();
        this.kickService = kickService;
        this.uploadJobService = uploadJobService;
        this.progressService = progressService;
    }

    // ── Inicia una sesión de subida resumable ──────────────────────────
    public String initiateUpload(String accessToken, String title, String description,
                                 List<String> tags, String privacyStatus) {
        String tagsJson = tags != null && !tags.isEmpty()
                ? "\"tags\": [" + tags.stream()
                        .map(t -> "\"" + t + "\"")
                        .collect(java.util.stream.Collectors.joining(",")) + "],"
                : "";

        String metadata = """
                {
                    "snippet": {
                        "title": "%s",
                        "description": "%s",
                        %s
                        "categoryId": "20"
                    },
                    "status": {
                        "privacyStatus": "%s"
                    }
                }
                """.formatted(
                title != null ? title : "Kick VOD",
                description != null ? description : "",
                tagsJson,
                privacyStatus != null ? privacyStatus : "private"
        );

        ResponseEntity<Void> response = restClient.post()
                .uri("https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("X-Upload-Content-Type", "video/MP2T")
                .body(metadata)
                .retrieve()
                .toBodilessEntity();

        return response.getHeaders().getFirst("Location");
    }

    // ── Envía un chunk al URI de subida ───────────────────────────────
    // Para el último chunk (isLast=true), valida 200 OK y devuelve el YouTube video ID.
    // Para chunks intermedios devuelve null.
    public String uploadChunk(String uploadUri, byte[] chunk,
                              long startByte, boolean isLast, long totalBytesIfLast) {
        String contentRange = isLast
                ? "bytes %d-%d/%d".formatted(startByte, startByte + chunk.length - 1, totalBytesIfLast)
                : "bytes %d-%d/*".formatted(startByte, startByte + chunk.length - 1);

        if (isLast) {
            // YouTube devuelve 200 OK con el recurso de video en el cuerpo
            ResponseEntity<String> response = restClient.put()
                    .uri(uploadUri)
                    .header("Content-Range", contentRange)
                    .header("Content-Type", "video/MP2T")
                    .body(chunk)
                    .retrieve()
                    .toEntity(String.class);

            int statusCode = response.getStatusCode().value();
            if (statusCode != 200) {
                // 308 = incompleto, cualquier otro ≠ 200 es error
                throw new RuntimeException("Upload incompleto: YouTube respondió " + statusCode
                        + " al último chunk (esperado 200). El video puede estar corrupto o incompleto.");
            }

            // Extraer video ID del JSON de respuesta: {"id":"VIDEO_ID",...}
            String body = response.getBody();
            if (body != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("\"id\"\\s*:\\s*\"([^\"]+)\"")
                        .matcher(body);
                if (m.find()) return m.group(1);
            }
            throw new RuntimeException("Upload completado pero YouTube no devolvió el video ID. Body: " + body);
        }

        // Chunk intermedio: YouTube responde 308 Resume Incomplete
        restClient.put()
                .uri(uploadUri)
                .header("Content-Range", contentRange)
                .header("Content-Type", "video/MP2T")
                .body(chunk)
                .retrieve()
                .toBodilessEntity();
        return null;
    }

    // ── Retry con backoff exponencial para errores transitorios ───────
    // YouTube recomienda reintentar en 500 / 502 / 503 / 504
    private String uploadChunkWithRetry(String uploadUri, byte[] chunk,
                                        long startByte, boolean isLast,
                                        long total, int chunkIndex, int totalChunks) throws Exception {
        long delay = INITIAL_DELAY;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return uploadChunk(uploadUri, chunk, startByte, isLast, total);
            } catch (HttpServerErrorException e) {
                int status = e.getStatusCode().value();
                boolean retryable = status == 500 || status == 502 || status == 503 || status == 504;

                if (retryable && attempt < MAX_RETRIES) {
                    log.warn("Chunk {}/{} — YouTube {} (intento {}/{}), reintentando en {} ms...",
                            chunkIndex, totalChunks, status, attempt, MAX_RETRIES, delay);
                    Thread.sleep(delay);
                    delay = Math.min(delay * 2, MAX_DELAY);
                } else {
                    log.error("Chunk {}/{} — error definitivo {} tras {} intentos",
                            chunkIndex, totalChunks, status, attempt);
                    throw e;
                }
            }
        }
        throw new RuntimeException("No debería llegar aquí");
    }

    // ── Descarga un segmento .ts con retry ───────────────────────────
    private byte[] downloadSegmentWithRetry(String url, int segIndex, int total) throws Exception {
        long delay = 1_000;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                byte[] data = restClient.get().uri(url).retrieve().body(byte[].class);
                if (data == null || data.length == 0) throw new RuntimeException("Respuesta vacía");
                return data;
            } catch (Exception e) {
                if (attempt < MAX_RETRIES) {
                    log.warn("Segmento {}/{} — error de descarga (intento {}/{}): {}, reintentando en {} ms...",
                            segIndex, total, attempt, MAX_RETRIES, e.getMessage(), delay);
                    Thread.sleep(delay);
                    delay = Math.min(delay * 2, 10_000);
                } else {
                    log.error("Segmento {}/{} — descarga fallida tras {} intentos: {}",
                            segIndex, total, attempt, e.getMessage());
                    throw e;
                }
            }
        }
        throw new RuntimeException("No debería llegar aquí");
    }

    // ── Proceso completo: descarga → buffer → subida a YouTube ────────
    @Async("uploadExecutor")
    public void processUpload(UploadJob job, String m3u8Url, String accessToken,
                              String title, String description,
                              List<String> tags, String privacyStatus) {

        ExecutorService downloadPool = Executors.newFixedThreadPool(DOWNLOAD_WINDOW);
        try {
            progressService.update(job.getId(), 0);
            uploadJobService.updateStatus(job.getId(), UploadStatus.DOWNLOADING);

            List<String> tsUrls = kickService.getChunkUrls(m3u8Url);
            log.info("Job {} — segmentos .ts encontrados: {}", job.getId(), tsUrls.size());

            String uploadUri = initiateUpload(accessToken, title, description, tags, privacyStatus);
            uploadJobService.updateStatus(job.getId(), UploadStatus.UPLOADING);

            // Ventana circular: DOWNLOAD_WINDOW descargas en vuelo como máximo
            @SuppressWarnings("unchecked")
            Future<byte[]>[] window = new Future[DOWNLOAD_WINDOW];

            for (int i = 0; i < Math.min(DOWNLOAD_WINDOW, tsUrls.size()); i++) {
                final int idx = i;
                final String url = tsUrls.get(i);
                window[i] = downloadPool.submit(
                        () -> downloadSegmentWithRetry(url, idx + 1, tsUrls.size()));
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            long startByte = 0L;
            int chunksSent = 0;

            for (int i = 0; i < tsUrls.size(); i++) {
                boolean isLastTs = (i == tsUrls.size() - 1);

                // Espera al segmento actual
                byte[] ts = window[i % DOWNLOAD_WINDOW].get();

                // Programa la descarga del siguiente segmento
                if (i + DOWNLOAD_WINDOW < tsUrls.size()) {
                    final int nextIdx = i + DOWNLOAD_WINDOW;
                    final String url = tsUrls.get(nextIdx);
                    window[i % DOWNLOAD_WINDOW] = downloadPool.submit(
                            () -> downloadSegmentWithRetry(url, nextIdx + 1, tsUrls.size()));
                }

                buffer.writeBytes(ts);

                if (!isLastTs) {
                    int alignedSize = (buffer.size() / ALIGNMENT) * ALIGNMENT;
                    if (alignedSize > 0) {
                        byte[] all = buffer.toByteArray();
                        byte[] toSend = Arrays.copyOf(all, alignedSize);
                        byte[] remainder = Arrays.copyOfRange(all, alignedSize, all.length);

                        chunksSent++;
                        int percent = (i + 1) * 100 / tsUrls.size();
                        log.info("Job {} — chunk {} | segmento {}/{} ({}%) | {} bytes",
                                job.getId(), chunksSent, i + 1, tsUrls.size(), percent, toSend.length);

                        progressService.update(job.getId(), percent);
                        uploadChunkWithRetry(uploadUri, toSend, startByte, false, 0,
                                chunksSent, tsUrls.size());
                        startByte += alignedSize;

                        buffer.reset();
                        buffer.writeBytes(remainder);
                    }
                } else {
                    // Último chunk — tamaño real conocido
                    byte[] last = buffer.toByteArray();
                    chunksSent++;
                    log.info("Job {} — chunk final {} | {} bytes", job.getId(), chunksSent, last.length);
                    String youtubeVideoId = uploadChunkWithRetry(uploadUri, last, startByte, true,
                            startByte + last.length, chunksSent, tsUrls.size());
                    uploadJobService.updateYoutubeVideoId(job.getId(), youtubeVideoId);
                    log.info("Job {} — video ID de YouTube: {}", job.getId(), youtubeVideoId);
                }
            }

            progressService.update(job.getId(), 100);
            uploadJobService.updateStatus(job.getId(), UploadStatus.COMPLETED);
            log.info("Job {} completado exitosamente", job.getId());

        } catch (Exception e) {
            log.error("Job {} fallido: {}", job.getId(), e.getMessage());
            uploadJobService.updateStatus(job.getId(), UploadStatus.FAILED);
        } finally {
            downloadPool.shutdownNow();
        }
    }
}
