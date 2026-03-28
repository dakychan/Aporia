package com.ferra13671.discordipc;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Consumer;

@AllArgsConstructor
public class OnChangeHolder<T> {
    @Getter
    private T value;
    private final Consumer<T> onChangeConsumer;

    public OnChangeHolder(Consumer<T> onChangeConsumer) {
        this(null, onChangeConsumer);
    }

    public void setValue(T value) {
        if (this.value != value && !this.value.equals(value)) {
            this.value = value;
            this.onChangeConsumer.accept(value);
        }
    }
}
