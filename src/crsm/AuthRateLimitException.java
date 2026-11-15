package crsm;

import java.net.http.HttpResponse;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.OptionalLong;

/** Keeps provider Retry-After metadata separate from the app's conservative retry pause. */
public final class AuthRateLimitException extends IllegalStateException {
    private final Long retryAfterSeconds;
    public AuthRateLimitException(HttpResponse<?> response) {
        super("Supabase has temporarily blocked signup or confirmation-email requests for this project. Check your inbox before retrying. If the limit persists, the project administrator needs to review email delivery settings.");
        retryAfterSeconds=parseRetryAfter(response.headers().firstValue("Retry-After").orElse(""));
    }
    public OptionalLong retryAfterSeconds(){return retryAfterSeconds==null?OptionalLong.empty():OptionalLong.of(retryAfterSeconds);}
    public long appPauseSeconds(){return retryAfterSeconds==null?60:Math.max(1,retryAfterSeconds);}
    private static Long parseRetryAfter(String value) {
        try {long seconds=Long.parseLong(value.trim());return seconds>=0&&seconds<=604800?seconds:null;}
        catch(NumberFormatException ignored){}
        try {long seconds=Duration.between(Instant.now(),ZonedDateTime.parse(value,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).getSeconds();return seconds>=0&&seconds<=604800?seconds:null;}
        catch(java.time.DateTimeException ignored){return null;}
    }
}
