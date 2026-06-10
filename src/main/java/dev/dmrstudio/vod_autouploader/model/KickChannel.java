package dev.dmrstudio.vod_autouploader.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "kick_channels")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KickChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String channelName;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
