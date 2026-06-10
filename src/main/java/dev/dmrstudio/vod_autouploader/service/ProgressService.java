package dev.dmrstudio.vod_autouploader.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProgressService {

    private final ConcurrentHashMap<Long, Integer> progress = new ConcurrentHashMap<>();

    public void update(Long jobId, int percent) {
        progress.put(jobId, percent);
    }

    public int get(Long jobId) {
        return progress.getOrDefault(jobId, 0);
    }

    public void remove(Long jobId) {
        progress.remove(jobId);
    }
}
