package dev.dmrstudio.vod_autouploader.controller;

import dev.dmrstudio.vod_autouploader.dto.CreateKickChannelRequest;
import dev.dmrstudio.vod_autouploader.model.KickChannel;
import dev.dmrstudio.vod_autouploader.service.KickChannelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/kickchannels")
public class KickChannelController {

    private final KickChannelService kickChannelService;

    public KickChannelController(KickChannelService kickChannelService) {
        this.kickChannelService = kickChannelService;
    }

    @GetMapping("/{id}")
    public KickChannel getKickChannelById(@PathVariable Long id) {
        return kickChannelService.getKickChannelById(id);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<KickChannel> getKickChannelByUserId(@PathVariable Long userId) {
        return kickChannelService.getKickChannelByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<KickChannel> createKickChannel(
            @Valid
            @RequestBody CreateKickChannelRequest kickChannelRequest
    ) {
        KickChannel savedKickChannel = kickChannelService.createKickChannel(kickChannelRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedKickChannel);
    }
}
