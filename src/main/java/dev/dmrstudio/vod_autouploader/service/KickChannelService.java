package dev.dmrstudio.vod_autouploader.service;

import dev.dmrstudio.vod_autouploader.dto.CreateKickChannelRequest;
import dev.dmrstudio.vod_autouploader.model.KickChannel;
import dev.dmrstudio.vod_autouploader.model.User;
import dev.dmrstudio.vod_autouploader.repository.KickChannelRepository;
import dev.dmrstudio.vod_autouploader.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class KickChannelService {
    private final KickChannelRepository kickChannelRepository;
    private final UserRepository userRepository;

    public KickChannelService(KickChannelRepository kickChannelRepository, UserRepository userRepository) {
        this.kickChannelRepository = kickChannelRepository;
        this.userRepository = userRepository;
    }

    public KickChannel getKickChannelById(Long id) {
        return kickChannelRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KickChannel not found"));
    }

    public Optional<KickChannel> getKickChannelByUserId(Long userId) {
        return kickChannelRepository.findByUserId(userId);
    }

    public KickChannel createKickChannel(CreateKickChannelRequest kickChannelRequest) {
        User kickChannelLinkedUser = userRepository.findById(kickChannelRequest.getUserId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User does not exists"));
        KickChannel kickChannel = new KickChannel(null, kickChannelRequest.getChannelName(), kickChannelLinkedUser);
        return kickChannelRepository.save(kickChannel);
    }

}
