package com.infopulse.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@NoArgsConstructor
public class RedisMessage implements Serializable {
    private String sender;
    private String message;
}
