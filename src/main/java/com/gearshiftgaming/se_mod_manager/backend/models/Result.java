package com.gearshiftgaming.se_mod_manager.backend.models;

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

    private final ArrayList<String> messages = new ArrayList<>();

    public Result() {
        this.type = ResultType.NOT_INITIALIZED;
    }

    public void addMessage(String message, ResultType type) {
        messages.add(message);
        this.type = type;
    }

    public void addMessage(Result<?> result) {
        this.messages.add(result.getCurrentMessage());
        this.type = result.getType();
    }

    //TODO: We really ought to rework this and associate a type with EACH message, not globally for the class.
    public void addAllMessages(Result<?> result) {
        this.type = result.getType();
        for(String message : result.getMessages()) {
            this.addMessage(message, result.getType());
        }
    }

    public boolean isSuccess() {
        return type == ResultType.SUCCESS || type == ResultType.WARN;
    }

    public boolean isFailure() {
        return !isSuccess();
    }

    public String getCurrentMessage() {
        return messages.getLast();
    }
}
