package com.gearshiftgaming.se_mod_manager.operatingsystem;

/**
 * Copyright (C) 2025 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute, and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.
 */
public class UnsupportedOperatingSystemException extends RuntimeException {
  public UnsupportedOperatingSystemException(String message) {
    super(message);
  }
}
