package dev.dmrstudio.vod_autouploader.controller;

import dev.dmrstudio.vod_autouploader.dto.CreateUploadJobRequest;
import dev.dmrstudio.vod_autouploader.dto.StartUploadJobRequest;
import dev.dmrstudio.vod_autouploader.model.UploadJob;
import dev.dmrstudio.vod_autouploader.model.User;
import dev.dmrstudio.vod_autouploader.service.ProgressService;
import dev.dmrstudio.vod_autouploader.service.UploadJobService;
import dev.dmrstudio.vod_autouploader.service.YoutubeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import java.util.List;

@RestController
@RequestMapping("/upload-jobs")
public class UploadJobController {

    private final UploadJobService uploadJobService;
    private final YoutubeService youtubeService;
    private final ProgressService progressService;

    public UploadJobController(UploadJobService uploadJobService,
                               YoutubeService youtubeService,
                               ProgressService progressService) {
        this.uploadJobService = uploadJobService;
        this.youtubeService = youtubeService;
        this.progressService = progressService;
    }

    @GetMapping("/channel/{channelId}")
    public List<UploadJob> getJobsByChannel(@PathVariable Long channelId) {
        List<UploadJob> jobs = uploadJobService.getJobsByChannelId(channelId);
        jobs.forEach(job -> job.setProgress(progressService.get(job.getId())));
        return jobs;
    }

    @GetMapping("/{id}")
    public UploadJob getUploadJob(@PathVariable Long id) {
        UploadJob job = uploadJobService.getUploadJobById(id);
        job.setProgress(progressService.get(id));
        return job;
    }

    @PostMapping
    public ResponseEntity<UploadJob> createUploadJob(@Valid @RequestBody CreateUploadJobRequest request) {
        UploadJob uploadJob = uploadJobService.createUploadJobService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadJob);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<UploadJob> startUploadJob(@PathVariable Long id,
                                                    @Valid @RequestBody StartUploadJobRequest request,
                                                    Authentication authentication) {
        UploadJob uploadJob = uploadJobService.getUploadJobById(id);
        User user = (User) authentication.getPrincipal();

        // Verificar que el access token de YouTube no esté expirado
        if (user.getYoutubeAccessToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }
        if (user.getYoutubeTokenExpiry() != null &&
                user.getYoutubeTokenExpiry().isBefore(LocalDateTime.now().plusMinutes(2))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }

        try {
            youtubeService.processUpload(
                    uploadJob,
                    request.getM3u8Url(),
                    user.getYoutubeAccessToken(),
                    request.getTitle(),
                    request.getDescription(),
                    request.getTags(),
                    request.getPrivacyStatus()
            );
        } catch (java.util.concurrent.RejectedExecutionException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "El servidor está procesando demasiadas subidas. Inténtalo más tarde."
            );
        }

        return ResponseEntity.accepted().body(uploadJob);
    }
}
