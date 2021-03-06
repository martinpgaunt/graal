/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.graal.pointsto.flow;

import org.graalvm.compiler.nodes.ValueNode;
import com.oracle.graal.pointsto.BigBang;
import com.oracle.graal.pointsto.meta.AnalysisType;
import com.oracle.graal.pointsto.typestate.TypeState;

public class NullCheckTypeFlow extends TypeFlow<ValueNode> {

    /** If true, lets anything but null pass through. If false only null passes through. */
    private final boolean filterNull;

    public NullCheckTypeFlow(ValueNode node, AnalysisType inputType, boolean filterNull) {
        /*
         * OffsetLoadTypeFlow reflects the state of the receiver array or unsafe read object. Null
         * check type flow filters based on the values that can be written to the object.
         */
        super(node, inputType);
        this.filterNull = filterNull;
    }

    public NullCheckTypeFlow(MethodFlowsGraph methodFlows, NullCheckTypeFlow original) {
        super(original, methodFlows);
        this.filterNull = original.filterNull;
    }

    @Override
    public TypeFlow<ValueNode> copy(BigBang bb, MethodFlowsGraph methodFlows) {
        return new NullCheckTypeFlow(methodFlows, this);
    }

    /** If true, lets anything but null pass through. If false only null passes through. */
    public boolean isFilterNull() {
        return filterNull;
    }

    @Override
    public TypeState filter(BigBang bb, TypeState newState) {
        if (filterNull) {
            return newState.forNonNull(bb);
        } else if (newState.canBeNull()) {
            return TypeState.forNull();
        } else {
            return TypeState.forEmpty();
        }
    }

    @Override
    public boolean addState(BigBang bb, TypeState add) {
        assert this.isClone();
        return super.addState(bb, add);
    }

    @Override
    public String toString() {
        return "NullCheckTypeFlow<" + (getDeclaredType() != null ? getDeclaredType().toJavaName(false) : "null") + " : " + getState() + ">";
    }

}
