package dev.dmrstudio.vod_autouploader.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;

@Getter
public class StartUploadJobRequest {

    @NotBlank
    private String m3u8Url;

    private String title;
    private String description;
    private List<String> tags;
    private String privacyStatus = "private"; // public, private, unlisted
}
