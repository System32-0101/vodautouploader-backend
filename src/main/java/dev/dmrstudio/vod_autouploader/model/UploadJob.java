package dev.dmrstudio.vod_autouploader.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "upload_jobs")
@Getter
@Setter
@NoArgsConstructor
public class UploadJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String kickVodId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private UploadStatus uploadStatus;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "kickchannel_id")
    private KickChannel kickChannel;

    private String youtubeVideoId;

    @Transient
    private Integer progress = 0;
}
