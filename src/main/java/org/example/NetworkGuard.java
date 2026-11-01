package org.example;

import java.net.URI;
import java.util.logging.Logger;

/** Guard ensuring network calls adhere to offline security rules (loopback by default, local network if enabled). */
public class NetworkGuard {
    private static final Logger LOGGER = Logger.getLogger(NetworkGuard.class.getName());

    public static void validateUrl(String urlString, boolean allowLocalNetwork) {
        if (urlString == null || urlString.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or blank");
        }
        try {
            URI uri = URI.create(urlString);
            if (uri.isOpaque() || uri.getScheme() == null || uri.getHost() == null) {
                throw new SecurityException("Only absolute network URLs with a host are allowed");
            }
            String scheme = uri.getScheme().toLowerCase();
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new SecurityException("Unsupported URL scheme blocked by NetworkGuard: " + scheme);
            }
            validateHost(uri.getHost(), allowLocalNetwork);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("Invalid URL blocked by NetworkGuard: " + urlString, e);
        }
    }

    public static void validateHost(String host, boolean allowLocalNetwork) {
        if (host == null) {
            throw new IllegalArgumentException("Host cannot be null");
        }
        String lower = host.toLowerCase().trim();

        if (lower.equals("localhost") || lower.equals("127.0.0.1") || lower.equals("::1") || lower.equals("0:0:0:0:0:0:0:1") || lower.startsWith("127.")) {
            return;
        }

        if (allowLocalNetwork) {
            if (lower.endsWith(".local") || lower.endsWith(".lan") || lower.endsWith(".internal") || lower.equals("host.docker.internal")) {
                return;
            }
            if (isPrivateIp(lower)) {
                return;
            }
        }

        throw new SecurityException("Network call to host '" + host + "' blocked by NetworkGuard (offline security mode active). Enable 'Allow model on local network' in settings if running on another local machine.");
    }

    private static boolean isPrivateIp(String host) {
        if (host.startsWith("10.") || host.startsWith("192.168.")) {
            return true;
        }
        if (host.startsWith("172.")) {
            try {
                String[] parts = host.split("\\.");
                if (parts.length >= 2) {
                    int second = Integer.parseInt(parts[1]);
                    if (second >= 16 && second <= 31) {
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }
        return false;
    }
}
