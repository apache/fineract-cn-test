/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.core.test.domain;

import io.mifos.core.lang.DateConverter;
import org.junit.Assert;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Support class for testing that the correct time stamp is returned
 *
 * @author Myrle Krantz
 */
public class TimeStampChecker {
  private final LocalDateTime expectedTimeStamp;
  private final Duration maximumDelta;

  public static TimeStampChecker roughlyNow()
  {
    return new TimeStampChecker(LocalDateTime.now(ZoneId.of("UTC")), Duration.ofSeconds(3));
  }

  public static TimeStampChecker allowSomeWiggleRoom(final Duration maximumDelta)
  {
    return new TimeStampChecker(LocalDateTime.now(ZoneId.of("UTC")), maximumDelta);
  }

  private TimeStampChecker(final LocalDateTime expectedTimeStamp, final Duration maximumDelta) {
    this.expectedTimeStamp = expectedTimeStamp;
    this.maximumDelta = maximumDelta;
  }

  public void assertCorrect(final String timeStamp)
  {
    final LocalDateTime parsedTimeStamp = DateConverter.fromIsoString(timeStamp);

    final Duration deltaFromExpected = Duration.ofNanos(Math.abs(parsedTimeStamp.until(expectedTimeStamp, ChronoUnit.NANOS)));

    Assert.assertTrue("Delta from expected should have been less than " + maximumDelta + ", but was " + deltaFromExpected +
                    ". Timestamp string was " + timeStamp + ".",
            deltaFromExpected.compareTo(maximumDelta) < 0);
  }
}