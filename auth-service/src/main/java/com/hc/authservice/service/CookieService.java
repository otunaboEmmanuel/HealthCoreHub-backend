package com.hc.authservice.service;

import com.hc.authservice.config.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CookieService {

    private final CookieProperties cookieProperties;

    /**
     * Set access token cookie
     */
    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = createCookie(
                cookieProperties.getAccessTokenName(),
                token,
                cookieProperties.getAccessTokenMaxAge()
        );
        response.addCookie(cookie);
        log.debug(" Access token cookie set");
    }

    /**
     * Set refresh token cookie
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = createCookie(
                cookieProperties.getRefreshTokenName(),
                token,
                cookieProperties.getRefreshTokenMaxAge()
        );
        response.addCookie(cookie);
        log.debug(" Refresh token cookie set");
    }

    /**
     * Get access token from cookie
     */
    public Optional<String> getAccessTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, cookieProperties.getAccessTokenName());
    }

    /**
     * Get refresh token from cookie
     */
    public Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, cookieProperties.getRefreshTokenName());
    }

    /**
     * Clear all auth cookies (logout)
     */
    public void clearAuthCookies(HttpServletResponse response) {
        clearCookie(response, cookieProperties.getAccessTokenName());
        clearCookie(response, cookieProperties.getRefreshTokenName());
        log.info(" Auth cookies cleared");
    }

    /**
     * Clear specific cookie
     */
    private void clearCookie(HttpServletResponse response, String name) {
        Cookie cookie = createCookie(name, "", 0);
        response.addCookie(cookie);
    }

    /**
     * Create a cookie with standard settings
     */
    private Cookie createCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(cookieProperties.isHttpOnly());
        cookie.setSecure(cookieProperties.isSecure());
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);

        // Set domain for cross-subdomain support
        if (cookieProperties.getDomain() != null && !cookieProperties.getDomain().equals("localhost")) {
            cookie.setDomain(cookieProperties.getDomain());
        }

        // SameSite attribute (requires Servlet 6.0+ or manual header setting)
        // For older versions, we'll set it via response header

        return cookie;
    }

    /**
     * Get cookie value by name
     */
    private Optional<String> getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    /**
     * Set SameSite attribute manually (for cross-domain)
     * This is needed because Cookie class doesn't support SameSite directly in older Servlet versions
     */
    public void setSameSiteAttribute(HttpServletResponse response) {
        String sameSite = cookieProperties.getSameSite();
        if (sameSite != null && !sameSite.isEmpty()) {
            // Modify Set-Cookie headers to add SameSite
            response.setHeader("Set-Cookie",
                    String.format("%s; SameSite=%s", response.getHeader("Set-Cookie"), sameSite));
        }
    }
}