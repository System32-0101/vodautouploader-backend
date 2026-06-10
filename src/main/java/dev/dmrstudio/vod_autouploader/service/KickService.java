package dev.dmrstudio.vod_autouploader.service;


import dev.dmrstudio.vod_autouploader.dto.kick.KickVideoEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KickService {

    private final RestClient restClient;

    public KickService() {
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();

        this.restClient = RestClient.builder()
                .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient))
                .defaultHeader("User-Agent", "KickExport/1.0")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public String getVodM3u8Url(String channelName, String vodId) {

        log.info("Kick: " + channelName);
        List<KickVideoEntry> videos = restClient.get()
                .uri("https://kick.com/api/v2/channels/" + channelName + "/videos")
                .retrieve()
                .body(new ParameterizedTypeReference<List<KickVideoEntry>>() {
                });


        KickVideoEntry entry = videos.stream()
                .filter(v -> v.getVideo().getUuid().equals(vodId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VOD not found"));

        return entry.getSource();
    }

    public List<String> getChunkUrls(String masterM3u8Url) {
        String masterContent = restClient.get()
                .uri(masterM3u8Url)
                .retrieve()
                .body(String.class);

        String[] lines = masterContent.split("\n");
        String subPlaylistRelative = null;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().startsWith("#EXT-X-STREAM-INF") && i + 1 < lines.length) {
                subPlaylistRelative = lines[i + 1].trim();
                break;
            }
        }

        if (subPlaylistRelative == null) {
            throw new RuntimeException("No sub-playlist found in master m3u8");
        }

        String subPlaylistUrl = java.net.URI.create(masterM3u8Url).resolve(subPlaylistRelative).toString();
        log.info("Sub-playlist URL: {}", subPlaylistUrl);

        String mediaContent = restClient.get()
                .uri(subPlaylistUrl)
                .retrieve()
                .body(String.class);

        java.net.URI subPlaylistUri = java.net.URI.create(subPlaylistUrl);
        return Arrays.stream(mediaContent.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(line -> line.startsWith("http") ? line : subPlaylistUri.resolve(line).toString())
                .collect(Collectors.toList());
    }
}
