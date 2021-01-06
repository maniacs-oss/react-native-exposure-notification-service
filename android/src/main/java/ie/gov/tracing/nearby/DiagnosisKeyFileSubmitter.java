package ie.gov.tracing.nearby;

import android.content.Context;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.threeten.bp.Duration;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import ie.gov.tracing.Tracing;
import ie.gov.tracing.common.AppExecutors;
import ie.gov.tracing.common.Events;
import ie.gov.tracing.common.ExposureConfig;
import ie.gov.tracing.common.TaskToFutureAdapter;
import ie.gov.tracing.storage.SharedPrefs;

import ie.gov.tracing.common.ApiAvailabilityCheckUtils;
import ie.gov.tracing.hms.ContactShieldWrapper;

class DiagnosisKeyFileSubmitter {
  private static final Duration API_TIMEOUT = Duration.ofSeconds(10);
  private final ExposureNotificationClientWrapper gmsClient;
  private final ContactShieldWrapper hmsClient;
  private final boolean isGMS;
  private final boolean isHMS;

  DiagnosisKeyFileSubmitter(Context context) {
    isGMS = ApiAvailabilityCheckUtils.isGMS(context);
    isHMS = ApiAvailabilityCheckUtils.isHMS(context);
    gmsClient = isGMS ? ExposureNotificationClientWrapper.get(context) : null;
    hmsClient = isHMS ? ContactShieldWrapper.getInstance(context) : null;
  }

  ListenableFuture<?> parseFiles(List<File> files, String token, ExposureConfig config) {
    if (files == null || files.size() == 0) {
      SharedPrefs.setString("lastError", "No files available to process", Tracing.currentContext);
      Events.raiseEvent(Events.INFO, "parseFiles - No export files to process.");
      return Futures.immediateFuture(null);
    }

    Events.raiseEvent(Events.INFO, "Processing " + files.size() + " export files...");

    if (config.getV2Mode()) {
      client.setDiagnosisKeysDataMapping(config);
      return TaskToFutureAdapter.getFutureWithTimeout(
        isGMS ? gmsClient.provideDiagnosisKeys(files) : null,
        isHMS ? hmsClient.provideDiagnosisKeys(files) : null,
        API_TIMEOUT.toMillis(),
        TimeUnit.MILLISECONDS,
        AppExecutors.getScheduledExecutor());      
    } else {
      return TaskToFutureAdapter.getFutureWithTimeout(
        isGMS ? gmsClient.provideDiagnosisKeys(files, token, config) : null,
        isHMS ? hmsClient.provideDiagnosisKeys(files, token, config) : null,
        API_TIMEOUT.toMillis(),
        TimeUnit.MILLISECONDS,
        AppExecutors.getScheduledExecutor());        
    }
  }
}
