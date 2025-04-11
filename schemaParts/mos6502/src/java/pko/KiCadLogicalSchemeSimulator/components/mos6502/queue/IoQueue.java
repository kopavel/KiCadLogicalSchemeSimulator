package pko.KiCadLogicalSchemeSimulator.components.mos6502.queue;
public class IoQueue {
    public Request request;
    public Request write;
    int hiAddress;
    int lowByte;
    int[] resultArray;
    int arrayPos;
    Callback finalCallback;
    private final Callback hiWordReadCallback = hiByte -> finalCallback.accept((hiByte << 8) | lowByte);
    private final Callback loReadWordCallback = lowByte -> {
        shiftWrite();
        this.lowByte = lowByte;
        write.address = hiAddress;
        write.read = true;
        write.callback = hiWordReadCallback;
    };
    ArrayCallback arrayCallback;
    private final Callback arrayReadCallback = read -> {
        resultArray[arrayPos++] = read;
        if (arrayPos == resultArray.length) {
            arrayCallback.accept(resultArray);
        }
    };

    public IoQueue() {
        write = new Request();
        request = write;
        write.next = write;
    }

    public void write(int address, int value) {
        shiftWrite();
        write.address = address;
        write.read = false;
        write.payload = value;
    }

    public int endAddress() {
        return 0xffff;
    }

    public void read(int address, Callback callback) {
        shiftWrite();
        write.address = address;
        write.read = true;
        write.callback = callback;
    }

    public void read(int[] addresses, ArrayCallback callback) {
        resultArray = new int[addresses.length];
        arrayPos = 0;
        arrayCallback = callback;
        for (int address : addresses) {
            shiftWrite();
            write.address = address;
            write.read = true;
            write.callback = arrayReadCallback;
        }
    }

    public void writeWord(int lo, int hi, int value) {
        shiftWrite();
        write.address = lo;
        write.read = false;
        write.payload = value & 0xff;
        shiftWrite();
        write.address = hi;
        write.read = false;
        write.payload = value >> 8;
    }

    public void readWord(int lo, int hi, Callback callback) {
        shiftWrite();
        hiAddress = hi;
        finalCallback = callback;
        write.address = lo;
        write.read = true;
        write.callback = loReadWordCallback;
    }

    public void clear() {
        request = write;
        write.address = -1;
        write.next = write;
    }

    public Request next() {
        request.address = -1;
        return (request = request.next);
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
