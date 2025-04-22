package com.gearshiftgaming.se_mod_manager.backend.domain;

public class ModNotFoundException extends RuntimeException {
    public ModNotFoundException(String message) {
        super(message);
    }
}
