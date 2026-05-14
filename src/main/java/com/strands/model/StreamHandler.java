package com.strands.model;

import com.strands.types.Message;
import com.strands.types.StopReason;

public interface StreamHandler {

    default void onTextDelta(String delta) {
    }

    default void onToolUseStart(String toolName, String toolUseId) {
    }

    default void onToolUseDelta(String inputDelta) {
    }

    default void onComplete(Message message, StopReason stopReason) {
    }
}
