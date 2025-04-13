package pko.KiCadLogicalSchemeSimulator.components.Z80.core.queue;
public class IoQueue {
    public Request request;
    public Request write;
    int lowByte;
    private final Callback loReadWordCallback = lowByte -> {
        this.lowByte = lowByte;
    };
    Callback wordCallback;
    private final Callback hiWordReadCallback = (hiByte) -> wordCallback.accept((hiByte << 8) | lowByte);

    public IoQueue() {
        write = new Request();
        request = write;
        write.next = write;
    }

    public void writeWord(int address, int value) {
        shiftWrite();
        write.address = address;
        write.read = false;
        write.memory = true;
        write.payload = value & 0xff;
        shiftWrite();
        write.address = address + 1;
        write.read = false;
        write.memory = true;
        write.payload = value >> 8;
    }

    public void writeByte(int address, int value) {
        shiftWrite();
        write.address = address;
        write.read = false;
        write.memory = true;
        write.payload = value;
    }

    public void readByte(int address, Callback callback) {
        shiftWrite();
        write.address = address;
        write.read = true;
        write.memory = true;
        write.callback = callback;
    }

    public void readWord(int address, Callback callback) {
        shiftWrite();
        wordCallback = callback;
        write.address = address;
        write.read = true;
        write.memory = true;
        write.callback = loReadWordCallback;
        shiftWrite();
        write.address = address + 1;
        write.read = true;
        write.memory = true;
        write.callback = hiWordReadCallback;
    }

    public void ioRead(int address, Callback callback) {
        shiftWrite();
        write.address = address;
        write.read = true;
        write.memory = false;
        write.callback = callback;
    }

    public void ioWrite(int address, int value) {
        shiftWrite();
        write.address = address;
        write.read = false;
        write.memory = false;
        write.payload = value;
    }

    public void clear() {
        request = write;
        write.address = -1;
        write.next = write;
    }

    public void next() {
        request.address = -1;
        request = request.next;
    }

    private void shiftWrite() {
        if (write.next.address != -1) {
            Request next = write.next;
            Request newRequest = new Request();
            newRequest.next = next;
            write.next = newRequest;
            write = newRequest;
        } else {
            write = write.next;
        }
    }
}
