package dev.dmrstudio.vod_autouploader.repository;

import dev.dmrstudio.vod_autouploader.model.UploadJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UploadJobRepository extends JpaRepository<UploadJob, Long> {
    List<UploadJob> findByKickChannelIdOrderByCreatedAtDesc(Long channelId);
}
