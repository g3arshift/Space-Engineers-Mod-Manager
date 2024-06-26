package com.gearshiftgaming.se_mod_manager.models.utility;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
public class Result<T> {

    private ResultType type;

    @Setter
    private T payload;

    private final ArrayList<String> messages = new ArrayList<>();

    public Result(ResultType type) {
        this.type = type;
    }

    public Result() {
    }

    public void addMessage(String message, ResultType type) {
        messages.add(message);
        this.type = type;
    }

    public boolean isSuccess() {
        return type == ResultType.SUCCESS;
    }
}
