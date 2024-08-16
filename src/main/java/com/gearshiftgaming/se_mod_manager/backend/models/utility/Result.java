package com.gearshiftgaming.se_mod_manager.backend.models.utility;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

/**
 * Stores both a result of an operation within SEMM and to carry the result of that operation
 * @param <T>
 * @author Gear Shift
 * @version 1.0
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
        this.messages.add(result.getMessages().getLast());
        this.type = result.getType();
    }

    public boolean isSuccess() {
        return type == ResultType.SUCCESS;
    }
}
