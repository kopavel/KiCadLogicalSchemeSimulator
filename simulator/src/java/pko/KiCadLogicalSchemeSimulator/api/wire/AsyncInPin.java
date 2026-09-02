/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api.wire;
import pko.KiCadLogicalSchemeSimulator.tools.asyncConsumer.BatchedAsyncConsumer;

public class AsyncInPin extends Pin {
    private final Pin inPin;
    private final BatchedAsyncConsumer consumer = new BatchedAsyncConsumer(4,515) {
        @Override
        public void consume(int payload) {
            if (payload > 0) {
                inPin.setHi();
            } else {
                inPin.setLo();
            }
        }
    };

    public AsyncInPin(Pin oldPin) {
        super(oldPin, "async");
        inPin = oldPin;
    }

    @Override
    public void setHi() {
        consumer.accept(1);
    }

    @Override
    public void setLo() {
        consumer.accept(0);
    }

    @Override
    public boolean hasTriStateIn() {
        return inPin.hasTriStateIn();
    }
}
