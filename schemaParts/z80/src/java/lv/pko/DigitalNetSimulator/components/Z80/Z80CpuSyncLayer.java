/*
 * Copyright (c) 2024 Pavel Korzh
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package lv.pko.DigitalNetSimulator.components.Z80;
import com.codingrodent.microprocessor.IBaseDevice;
import com.codingrodent.microprocessor.IMemory;
import lv.pko.DigitalNetSimulator.tools.BusyWaitMutex;
import lv.pko.DigitalNetSimulator.tools.BusyWaitSemaphore;

public class Z80CpuSyncLayer extends Z80CpuIo {
    protected final BusyWaitSemaphore ioSemaphore = new BusyWaitSemaphore();
    protected final BusyWaitSemaphore ioDoneSemaphore = new BusyWaitSemaphore();
    protected final BusyWaitSemaphore addressSemaphore = new BusyWaitSemaphore();
    protected final BusyWaitSemaphore addressDoneSemaphore = new BusyWaitSemaphore();
    protected final BusyWaitSemaphore calcSemaphore = new BusyWaitSemaphore();
    protected final BusyWaitMutex coreBusyMutex = new BusyWaitMutex();
    protected volatile boolean readRequest;
    protected volatile boolean ioRequest;
    protected volatile boolean cpuDone = true;

    public Z80CpuSyncLayer(String id, String sParam) {
        super(id, sParam);
    }

    protected IBaseDevice getPortHandler() {
        return new IBaseDevice() {
            @Override
            public int IORead(int address) {
                if (M == 0) {
                    return 0;
                } else {
//                    Log.trace(Z80Cpu.class, "ioRead at {},{}", M, T);
                    coreFree();
                    ioRequest = true;
                    readRequest = true;
                    acquireSetAddressPermission();
                    addOut.setState(address);
                    setAddressDone();
                    acquireIoPermission();
                    int data = (int) dIn.getState();
                    coreBusy();
                    ioDone();
                    return data;
                }
            }

            @Override
            public void IOWrite(int address, int data) {
                if (M != 0) {
//                    Log.trace(Z80Cpu.class, "ioWrite at {},{}", M, T);
                    coreFree();
                    ioRequest = true;
                    readRequest = false;
                    acquireSetAddressPermission();
                    addOut.setState(address);
                    setAddressDone();
                    acquireIoPermission();
                    dOut.setState(data);
                    coreBusy();
                    ioDone();
                }
            }
        };
    }

    protected IMemory getMemoryHandler() {
        return new IMemory() {
            @Override
            public int readByte(int address) {
                if (M == 0) {
                    return 0;
                } else {
//                    Log.trace(Z80Cpu.class, "byteRead at {},{}", M, T);
                    coreFree();
                    ioRequest = false;
                    readRequest = true;
                    acquireSetAddressPermission();
                    addOut.setState(address);
                    setAddressDone();
                    acquireIoPermission();
                    int data = (int) dIn.getState();
                    coreBusy();
                    ioDone();
                    return data;
                }
            }

            @Override
            public int readWord(int address) {
                if (M == 0) {
                    return 0;
                } else {
//                    Log.trace(Z80Cpu.class, "wordRead at {},{}", M, T);
                    coreFree();
                    ioRequest = false;
                    readRequest = true;
                    int lowByte;
                    acquireSetAddressPermission();
                    addOut.setState(address);
                    setAddressDone();
                    acquireIoPermission();
                    lowByte = (int) dIn.getState();
                    ioDone();
                    acquireSetAddressPermission();
                    addOut.setState(address + 1);
                    setAddressDone();
                    acquireIoPermission();
                    int data = (int) ((dIn.getState() << 8) + lowByte);
                    coreBusy();
                    ioDone();
                    return data;
                }
            }

            @Override
            public void writeByte(int address, int data) {
                if (M != 0) {
//                    Log.trace(Z80Cpu.class, "byteWrite at {},{}", M, T);
                    coreFree();
                    ioRequest = false;
                    readRequest = false;
                    acquireSetAddressPermission();
                    addOut.setState(address);
                    setAddressDone();
                    acquireIoPermission();
                    dOut.setState(data);
                    coreBusy();
                    ioDone();
                }
            }

            @Override
            public void writeWord(int address, int data) {
                if (M != 0) {
//                    Log.trace(Z80Cpu.class, "wordWrite at {},{}", M, T);
                    coreFree();
                    ioRequest = false;
                    readRequest = false;
                    int lowByte = data & 0xff;
                    int highByte = (data >> 8) & 0xff;
                    acquireSetAddressPermission();
                    addOut.setState(address);
                    setAddressDone();
                    acquireIoPermission();
                    dOut.setState(lowByte);
                    ioDone();
                    acquireSetAddressPermission();
                    addOut.setState(address + 1);
                    setAddressDone();
                    acquireIoPermission();
                    dOut.setState(highByte);
                    coreBusy();
                    ioDone();
                }
            }
        };
    }

    protected void permitIO() {
//        Log.trace(Z80Cpu.class, "permit IO         ,   free is {}", ioSemaphore.getAvailableAmount());
        ioSemaphore.release();
//        Log.trace(Z80Cpu.class, "ioPermitted       ,   free is {}", ioSemaphore.getAvailableAmount());
//        Log.trace(Z80Cpu.class, "ack ioDone        ,   free is {}", ioDoneSemaphore.getAvailableAmount());
        ioDoneSemaphore.acquire();
//        Log.trace(Z80Cpu.class, "got ioDone        ,   free is {}", ioDoneSemaphore.getAvailableAmount());
    }

    protected void permitAddress() {
//        Log.trace(Z80Cpu.class, "permit setAddress,    free is {}", addressSemaphore.getAvailableAmount());
        addressSemaphore.release();
//        Log.trace(Z80Cpu.class, "permitted setAddress, free is {}", addressSemaphore.getAvailableAmount());
//        Log.trace(Z80Cpu.class, "ack addressDone,      free is {}", addressDoneSemaphore.getAvailableAmount());
        addressDoneSemaphore.acquire();
//        Log.trace(Z80Cpu.class, "got addressDone     , free is {}", addressDoneSemaphore.getAvailableAmount());
    }

    protected void coreBusy() {
//        Log.trace(Z80Cpu.class, "ack coreBusy,   acquired {}", coreBusyMutex.isAcquired());
        coreBusyMutex.acquire();
//        Log.trace(Z80Cpu.class, "got coreBusy,   acquired {}", coreBusyMutex.isAcquired());
    }

    protected void coreFree() {
//        Log.trace(Z80Cpu.class, "free coreBusy,  acquired {}", coreBusyMutex.isAcquired());
        coreBusyMutex.release();
//        Log.trace(Z80Cpu.class, "freed coreBusy, acquired {}", coreBusyMutex.isAcquired());
    }

    protected void doCalc() {
//        Log.trace(Z80Cpu.class, "set cpuDone=f");
        cpuDone = false;
//        Log.trace(Z80Cpu.class, "free calcMutex, amount {}", calcSemaphore.getAvailableAmount());
        calcSemaphore.release();
//        Log.trace(Z80Cpu.class, "freed calcMutex,amount {}", calcSemaphore.getAvailableAmount());
    }

    private void acquireIoPermission() {
//        Log.trace(Z80Cpu.class, "ack ioSemaphore   ,   free is {}", ioSemaphore.getAvailableAmount());
        ioSemaphore.acquire();
//        Log.trace(Z80Cpu.class, "got ioSemaphore   ,   free is {}", ioSemaphore.getAvailableAmount());
    }

    private void ioDone() {
//        Log.trace(Z80Cpu.class, "permit ioDone     ,   free is {}", ioDoneSemaphore.getAvailableAmount());
        ioDoneSemaphore.release();
//        Log.trace(Z80Cpu.class, "permited ioDone   ,   free is {}", ioDoneSemaphore.getAvailableAmount());
    }

    private void acquireSetAddressPermission() {
//        Log.trace(Z80Cpu.class, "ack SetAddress    ,   free is {}", addressSemaphore.getAvailableAmount());
        addressSemaphore.acquire();
//        Log.trace(Z80Cpu.class, "got SetAddress    ,   free is {}", addressSemaphore.getAvailableAmount());
    }

    private void setAddressDone() {
//        Log.trace(Z80Cpu.class, "setAddress done,      free is {}", addressDoneSemaphore.getAvailableAmount());
        addressDoneSemaphore.release();
//        Log.trace(Z80Cpu.class, "setAddress done end   free is {}", addressDoneSemaphore.getAvailableAmount());
    }
}
