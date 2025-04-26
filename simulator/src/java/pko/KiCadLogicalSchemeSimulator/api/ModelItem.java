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
import pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.warn;

@Getter
public abstract class ModelItem<T> implements IModelItem<T> {
    public String id;
    public SchemaPart parent;
    public int priority;
    public String variantId;
    public boolean hiImpedance;
    public boolean triStateIn;
    public boolean triStateOut;
    public int processing;
    public int applyMask;
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
    public boolean isHiImpedance() {
        return (source == null || source == this) ? hiImpedance : source.isHiImpedance();
    }

    public boolean isTriState(ModelItem<?> source) {
        return triStateIn & ((source == null || source == this) ? triStateOut : source.isTriState(source.source));
    }

    @Override
    public int compareTo(IModelItem<T> other) {
        return getName().compareTo(other.getName());
    }

    public void recurseError() {
        if (parent.net.stabilizing) {
            processing--;
            return;
        } else if (getRecursionMode() == warn) {
            if (!reportedRecurse) {
                String partId = parent.id;
                String unitId = null;
                if (partId.contains("#")) {
                    unitId = partId.substring(partId.indexOf("#") + 1);
                    partId = partId.substring(0, partId.indexOf("#"));
                }
                String message = """
                        Recursive event loop detected!
                            Enable recursive events for all outs (slower) using one of:
                                - Pass -r=all to simulator
                                - Modify Schema parameter file adding `recursion="all"` attribute to <params> tag (slower)
                            Specify specific out as "recursive" using one of:
                                - pass -ro={} to simulator
                                - Modify Schema parameter file adding `recursive={}` to param in <part id="{}">""";
                if (unitId != null) {
                    message += "<unit name=\"{}\">";
                }
                message += """
                        
                        read documentation at https://github.com/kopavel/KiCadLogicalSchemeSimulator/blob/main/stuff/parameters.md#schema-parameter-file" for more information
                        """;
                Log.error(this.getClass(), message, getName(), id, partId, unitId);
                reportedRecurse = true;
            }
            return;
        } else {
            throw new RuntimeException("Recursive event loop detected on " + getName() + ", need implement fair queue");
        }
    }

    public RecursionMode getRecursionMode() {
        return parent.net.parameterResolver.getRecursionMode(parent.id, id);
    }

    public boolean useFullOptimiser() {
        return false;
    }
}
