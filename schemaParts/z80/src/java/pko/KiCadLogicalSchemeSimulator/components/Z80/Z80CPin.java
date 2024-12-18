package pko.KiCadLogicalSchemeSimulator.components.Z80;
import com.codingrodent.microprocessor.Z80.Z80Core;
import com.codingrodent.microprocessor.io.device.DeviceRequest;
import com.codingrodent.microprocessor.io.memory.MemoryRequest;
import com.codingrodent.microprocessor.io.queue.AsyncIoQueue;
import com.codingrodent.microprocessor.io.queue.ReadRequest;
import com.codingrodent.microprocessor.io.queue.Request;
import com.codingrodent.microprocessor.io.queue.WriteRequest;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class Z80CPin extends InPin {
    final public InBus dIn;
    final public AsyncIoQueue ioQueue;
    final public Z80Core cpu;
    final Z80Cpu parent;
    public Pin refreshPin;
    public Bus dOut;
    public Bus aOut;
    public Pin rdPin;
    public Pin wrPin;
    public Pin mReqPin;
    public Pin m1Pin;
    public Pin ioReqPin;
    int T;
    int M;
    boolean notInWait;
    boolean nmiTriggered;
    boolean extraWait;

    public Z80CPin(String id, Z80Cpu parent) {
        super(id, parent);
        this.parent = parent;
        refreshPin = parent.refreshPin;
        aOut = parent.aOut;
        dOut = parent.dOut;
        rdPin = parent.rdPin;
        wrPin = parent.wrPin;
        mReqPin = parent.mReqPin;
        m1Pin = parent.m1Pin;
        ioReqPin = parent.ioReqPin;
        dIn = parent.dIn;
        ioQueue = parent.ioQueue;
        cpu = parent.cpu;
    }

    @Override
    public void setHi() {
        state = true;
        if ((M != 1 && T == 3) || T == 4) {
            T = 1;
            if (!refreshPin.state) {
                refreshPin.setHi();
            }
            if (!dOut.hiImpedance) {
                dOut.setHiImpedance();
            }
            ioQueue.next();
            if (ioQueue.request == null) {
                if (nmiTriggered) {
                    nmiTriggered = false;
                    cpu.processNMI();
                    M++;
                } else {
                    M = 1;
                }
            } else {
                M++;
            }
        } else if (T == 2 && (extraWait || !notInWait)) {
            extraWait = false;
        } else {
            T++;
        }
        Request ioRequest = ioQueue.request;
        switch (T) {
            case 1 -> {
                if (M == 1) {
                    m1Pin.setLo();
                    cpu.executeOneInstruction();
                    ioRequest = ioQueue.request;
                }
                aOut.setState(ioRequest.address);
                extraWait = ioRequest instanceof DeviceRequest;
            }
            case 2 -> {
                if (ioRequest instanceof DeviceRequest && notInWait) {
                    ioReqPin.setLo();
                    if (ioRequest instanceof WriteRequest) {
                        wrPin.setLo();
                    } else {
                        rdPin.setLo();
                    }
                }
            }
            case 3 -> {
                if (M == 1) {
                    ((ReadRequest) ioRequest).callback.accept((int) dIn.state);
                    rdPin.setHi();
                    mReqPin.setHi();
                    m1Pin.setHi();
                    //FixMe create refresh address counter and set address from it.
                    refreshPin.setLo();
                }
            }
        }
    }

    @Override
    public void setLo() {
        state = false;
        Request ioRequest = ioQueue.request;
        switch (T) {
            case 1 -> {
                if (ioRequest instanceof MemoryRequest) {
                    mReqPin.setLo();
                    if (ioRequest instanceof ReadRequest) {
                        rdPin.setLo();
                    }
                }
                if (ioRequest instanceof WriteRequest writeRequest) {
                    dOut.setState(writeRequest.payload);
                }
            }
            case 2 -> {
                if (ioRequest instanceof WriteRequest && ioRequest instanceof MemoryRequest && notInWait) {
                    wrPin.setLo();
                }
                notInWait = parent.waitPin.state;
            }
            case 3 -> {
                if (M == 1) {
                    mReqPin.setLo();
                } else if (ioRequest instanceof MemoryRequest) {
                    if (ioRequest instanceof ReadRequest readRequest) {
                        readRequest.callback.accept((int) dIn.state);
                        rdPin.setHi();
                    } else {
                        wrPin.setHi();
                    }
                    mReqPin.setHi();
                } else {
                    if (ioRequest instanceof ReadRequest readRequest) {
                        readRequest.callback.accept((int) dIn.state);
                        rdPin.setHi();
                    } else {
                        wrPin.setHi();
                    }
                    ioReqPin.setHi();
                }
            }
            default -> mReqPin.setHi();
        }
    }
}
