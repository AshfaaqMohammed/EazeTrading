package com.eaze.idempotency;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdempotencyRecord implements Serializable {
    public enum State {
        IN_PROGRESS,
        DONE
    }
    private State state;
    private int httpStatus;
    private String body;
}
