package com.infopulse.dto;

import com.infopulse.validation.ReceiveMessageGroup;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Setter
@Getter
@NoArgsConstructor
public class ReceiveMessage {
    @NotNull(groups = ReceiveMessageGroup.class, message = "Name is required")
    private String type;

    private String message;

    private String receiver;
}
