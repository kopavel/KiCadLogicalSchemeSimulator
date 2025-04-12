package pko.KiCadLogicalSchemeSimulator.components.mos6502.queue;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.F0Pin;
import pko.KiCadLogicalSchemeSimulator.components.mos6502.core.Cpu;

public class IoQueue {
    private final Cpu core;
    private final F0Pin f0Pin;
    private final Callback loReadWordCallback = lowByte -> this.lowByte = lowByte;
    public Request head;
    int lowByte;
    public Request tail;
    int[] resultArray;
    int arrayPos;
    int arrayLength;
    Callback finalCallback;
    private final Callback hiWordReadCallback = hiByte -> finalCallback.accept((hiByte << 8) | lowByte);
    ArrayCallback arrayCallback;
    private final Callback arrayReadCallback = read -> {
        resultArray[arrayPos++] = read;
        if (arrayPos == arrayLength) {
            arrayCallback.accept();
        }
    };

    public IoQueue(Cpu core, F0Pin f0Pin) {
        this.core = core;
        this.f0Pin = f0Pin;
        tail = new Request();
        head = tail;
        tail.next = tail;
    }

    public void write(int address, int value) {
        shiftWrite();
        Request request;
        (request = tail).address = address;
        request.read = false;
        request.payload = value;
    }

    public void read(int address, Callback callback) {
        shiftWrite();
        Request request;
        (request = tail).address = address;
        request.callback = callback;
        request.read = true;
    }

    public void readArray(int[] addresses, int[] destination, int length, ArrayCallback callback) {
        resultArray = destination;
        arrayPos = 0;
        arrayLength = length;
        arrayCallback = callback;
        for (int address : addresses) {
            shiftWrite();
            Request request;
            (request = tail).address = address;
            request.callback = arrayReadCallback;
            request.read = true;
        }
    }

    public void writeWord(int lo, int hi, int value) {
        shiftWrite();
        Request request;
        (request = tail).address = lo;
        request.read = false;
        request.payload = value & 0xff;
        shiftWrite();
        (request = tail).address = hi;
        request.read = false;
        request.payload = value >> 8;
    }

    public void readWord(int lo, int hi, Callback callback) {
        shiftWrite();
        finalCallback = callback;
        Request request;
        (request = tail).address = lo;
        request.callback = loReadWordCallback;
        request.read = true;
        shiftWrite();
        (request = tail).address = hi;
        request.callback = hiWordReadCallback;
        request.read = true;
    }

    public void clear() {
        head = tail;
        tail.address = -1;
        tail.next = tail;
    }

    public Request pop() {
        Request currentRequest;
        if ((currentRequest = head).address < 0) {
            if (currentRequest == tail) {
                core.step();
                f0Pin.opCode = true;
            } else {
                currentRequest = (head = currentRequest.next);
                if (currentRequest.address < 0) {
                    core.step();
                    f0Pin.opCode = true;
                }
            }
        }
        return currentRequest;
    }

    private void shiftWrite() {
        if (tail.address >= 0) {
            Request next = tail.next;
            if (next.address != -1) {
                Request newRequest = new Request();
                newRequest.next = next;
                tail = (tail.next = newRequest);
            } else {
                tail = next;
            }
        }
    }
}
