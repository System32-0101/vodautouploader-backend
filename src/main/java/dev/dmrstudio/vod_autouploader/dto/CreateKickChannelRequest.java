package dev.dmrstudio.vod_autouploader.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateKickChannelRequest {
    @NotBlank
    private String channelName;
    @NotNull
    private Long userId;
}
