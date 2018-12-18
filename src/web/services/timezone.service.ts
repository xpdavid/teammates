import { Injectable } from '@angular/core';
import moment from 'moment-timezone';
import { Observable } from 'rxjs';
import { default as timezone } from '../data/timezone.json';
import { HttpRequestService } from './http-request.service';

/**
 * The date time format used in date time resolution.
 */
export const LOCAL_DATE_TIME_FORMAT: string = 'YYYY-MM-DD HH:mm';

/**
 * Handles timezone information provision.
 */
@Injectable({
  providedIn: 'root',
})
export class TimezoneService {

  tzVersion: string = '';
  tzOffsets: { [key: string]: number } = {};

  // These short timezones are not supported by Java
  private readonly badZones: { [key: string]: boolean } = {
    EST: true, 'GMT+0': true, 'GMT-0': true, HST: true, MST: true, ROC: true,
  };

  constructor(private httpRequestService: HttpRequestService) {
    const d: Date = new Date();
    moment.tz.load(timezone);
    this.tzVersion = moment.tz.dataVersion;
    moment.tz.names()
        .filter((tz: string) => !this.isBadZone(tz))
        .forEach((tz: string) => {
          const offset: number = moment.tz.zone(tz).utcOffset(d) * -1;
          this.tzOffsets[tz] = offset;
        });
  }

  /**
   * Gets the timezone database version.
   */
  getTzVersion(): string {
    return this.tzVersion;
  }

  /**
   * Gets the mapping of time zone ID to offset values.
   */
  getTzOffsets(): { [key: string]: number } {
    return this.tzOffsets;
  }

  /**
   * Returns true if the specified time zone ID is "bad", i.e. not supported by back-end.
   */
  isBadZone(tz: string): boolean {
    return this.badZones[tz];
  }

  /**
   * Get the resolved local data time.
   */
  getResolveLocalDateTime(localDateTime: string, timeZone: string): Observable<LocalDateTimeInfo> {
    const params: { [key: string]: string } = { localdatetime: localDateTime, timezone: timeZone };
    return this.httpRequestService.get('/localdatetime', params);
  }
}

/**
 * Represents the resolution of local data time.
 */
export interface LocalDateTimeInfo {
  resolvedTimestamp: number;
  resolvedStatus: LocalDateTimeAmbiguityStatus;

  earlierInterpretationTimestamp?: number;
  laterInterpretationTimestamp?: number;
}

/**
 * Represents the ambiguity status for a local date time at a given time Zone,
 * brought about by Daylight Saving Time (DST).
 */
export enum LocalDateTimeAmbiguityStatus {
  /**
   * The local date time can be unambiguously resolved to a single timestamp.
   * It has only one valid interpretation.
   */
  UNAMBIGUOUS = 'UNAMBIGUOUS',

  /**
   * The local date time falls within the gap period when clocks spring forward at the start of DST.
   * Strictly speaking, it is non-existent, and needs to be readjusted to be valid.
   */
  GAP = 'GAP',

  /**
   * The local date time falls within the overlap period when clocks fall back at the end of DST.
   * It has more than one valid interpretation.
   */
  OVERLAP = 'OVERLAP',
}
