package dev.dmrstudio.vod_autouploader.config;

import dev.dmrstudio.vod_autouploader.model.User;
import dev.dmrstudio.vod_autouploader.service.JwtService;
import dev.dmrstudio.vod_autouploader.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;


@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.frontend-url:}")
    private String frontendUrl;

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UserService userService;
    private final JwtService jwtService;

    public OAuth2SuccessHandler(UserService userService, JwtService jwtService, OAuth2AuthorizedClientService authorizedClientService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        OAuth2AuthorizedClient authorizedClient = authorizedClientService
                .loadAuthorizedClient("google", authentication.getName());

        String youtubeAccessToken = authorizedClient.getAccessToken().getTokenValue();
        Instant expiry = authorizedClient.getAccessToken().getExpiresAt();

        LocalDateTime youtubeTokenExpiry = expiry != null
                ? LocalDateTime.ofInstant(expiry, ZoneId.systemDefault())
                : LocalDateTime.now().plusHours(1);

        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        User user = userService.findOrCreateUser(googleId, email, name, youtubeAccessToken, youtubeTokenExpiry);
        String token = jwtService.generateToken(user);

        String userJson = String.format(
                "{\"id\":%d,\"email\":\"%s\",\"name\":\"%s\"}",
                user.getId(),
                escapeJson(user.getEmail()),
                escapeJson(user.getName())
        );

        if (frontendUrl != null && !frontendUrl.isBlank()) {
            String redirectUrl = frontendUrl + "/oauth2/callback"
                    + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                    + "&user=" + URLEncoder.encode(userJson, StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
        } else {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    String.format("{\"token\":\"%s\",\"user\":%s}", token, userJson)
            );
            response.getWriter().flush();
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
