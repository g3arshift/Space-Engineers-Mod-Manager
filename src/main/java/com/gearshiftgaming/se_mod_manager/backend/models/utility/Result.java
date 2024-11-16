package com.gearshiftgaming.se_mod_manager.backend.models.utility;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

/**
 * Stores both a result of an operation within SEMM and a message attached to that operation.
 * <p>
 * Copyright (C) 2024 Gear Shift Gaming - All Rights Reserved
 * You may use, distribute and modify this code under the terms of the GPL3 license.
 * <p>
 * You should have received a copy of the GPL3 license with
 * this file. If not, please write to: gearshift@gearshiftgaming.com.

 */

@Getter
public class Result<T> {

    private ResultType type;

    @Setter
    private T payload;

    private final ArrayList<String> MESSAGES = new ArrayList<>();

    public Result() {
        this.type = ResultType.NOT_INITIALIZED;
    }

    public void addMessage(String message, ResultType type) {
        MESSAGES.add(message);
        this.type = type;
    }

    public void addMessage(Result<?> result) {
        this.MESSAGES.add(result.getCurrentMessage());
        this.type = result.getType();
    }

    public boolean isSuccess() {
        return type == ResultType.SUCCESS;
    }

    public String getCurrentMessage() {
        return MESSAGES.getLast();
    }
}
