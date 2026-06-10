package dev.dmrstudio.vod_autouploader.repository;

import dev.dmrstudio.vod_autouploader.model.KickChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KickChannelRepository extends JpaRepository<KickChannel, Long> {
    Optional<KickChannel> findByUserId(Long userId);
}
