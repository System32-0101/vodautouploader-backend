package dev.dmrstudio.vod_autouploader.service;

import dev.dmrstudio.vod_autouploader.dto.CreateUploadJobRequest;
import dev.dmrstudio.vod_autouploader.model.KickChannel;
import dev.dmrstudio.vod_autouploader.model.UploadJob;
import dev.dmrstudio.vod_autouploader.model.UploadStatus;
import dev.dmrstudio.vod_autouploader.repository.KickChannelRepository;
import dev.dmrstudio.vod_autouploader.repository.UploadJobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UploadJobService {
    private final UploadJobRepository uploadJobRepository;

    private final KickChannelRepository kickChannelRepository;

    public UploadJobService(UploadJobRepository uploadJobRepository, KickChannelRepository kickChannelRepository) {
        this.uploadJobRepository = uploadJobRepository;
        this.kickChannelRepository = kickChannelRepository;
    }


    public UploadJob getUploadJobById(Long id) {
        return uploadJobRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload Job not found"));
    }

    public UploadJob createUploadJobService(CreateUploadJobRequest createUploadJobRequest) {

        KickChannel linkedKickChannel = kickChannelRepository.findById(createUploadJobRequest.getKickChannelId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked Channel not found"));

        UploadJob createdUploadJob = new UploadJob();
        createdUploadJob.setKickVodId(createUploadJobRequest.getKickVodId());
        createdUploadJob.setUploadStatus(UploadStatus.PENDING);
        createdUploadJob.setKickChannel(linkedKickChannel);
        return uploadJobRepository.save(createdUploadJob);
    }

    public List<UploadJob> getJobsByChannelId(Long channelId) {
        return uploadJobRepository.findByKickChannelIdOrderByCreatedAtDesc(channelId);
    }

    public UploadJob updateStatus(Long jobId, UploadStatus newStatus){
        UploadJob uploadJob = uploadJobRepository.findById(jobId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        uploadJob.setUploadStatus(newStatus);
        return uploadJobRepository.save(uploadJob);
    }

    public UploadJob updateYoutubeVideoId(Long jobId, String youtubeVideoId) {
        UploadJob uploadJob = uploadJobRepository.findById(jobId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        uploadJob.setYoutubeVideoId(youtubeVideoId);
        return uploadJobRepository.save(uploadJob);
    }
}
