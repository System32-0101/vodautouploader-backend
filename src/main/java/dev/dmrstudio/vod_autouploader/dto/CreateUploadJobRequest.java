package dev.dmrstudio.vod_autouploader.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateUploadJobRequest {
    @NotBlank
    private String kickVodId;
    @NotNull
    private Long kickChannelId;
}
