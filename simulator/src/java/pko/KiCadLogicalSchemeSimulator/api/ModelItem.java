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
package pko.KiCadLogicalSchemeSimulator.api;
import lombok.Getter;
import pko.KiCadLogicalSchemeSimulator.Simulator;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.tools.Log;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

@Getter
public abstract class ModelItem<T> implements IModelItem<T> {
    public String id;
    public SchemaPart parent;
    public String variantId;
    public boolean hiImpedance;
    public boolean triState;
    public boolean processing;
    public boolean hasQueue;
    public long applyMask;
    public byte applyOffset;
    public boolean used;
    public ModelItem<?> source;
    private boolean reportedRecurse;

    protected ModelItem(String id, SchemaPart parent) {
        this.id = id;
        this.parent = parent;
        source = this;
    }

    @Override
    public ModelItem<T> getOptimised(ModelItem<?> source) {
        if (source != null) {
            this.source = source;
        }
        return this;
    }

    public String getName() {
        return parent.id + "_" + id;
    }

    @Override
    public String toString() {
        return hiImpedance + ":" + getName() + (variantId == null ? "" : ":" + variantId) + ":" + super.toString();
    }

    public void setHiImpedance() {
    }

    @Override
    public int compareTo(IModelItem<T> other) {
        return getName().compareTo(other.getName());
    }

    public boolean recurseError() {
        if (Net.stabilizing) {
            hasQueue = false;
            return true;
        } else if (!Simulator.recursive && Utils.notContain(Simulator.recursiveOuts, getName()) && !parent.recursive.contains(getId())) {
            if (!reportedRecurse) {
                Log.error(this.getClass(),
                        "Recursive event loop detected, enable recurse, passing -r parameter to simulator.\n For specific out only pass -ro={} to smulator" +
                                ", or modify Schema parameter file adding `recursive={}` to symPartParam in <part> with id=`{}` in ",
                        getName(),
                        id,
                        parent.id);
                reportedRecurse = true;
            }
            return false;
        } else {
            throw new RuntimeException("Recursive event loop detected, need implement fair queue");
        }
    }

    public boolean useFullOptimiser() {
        return false;
    }
}
