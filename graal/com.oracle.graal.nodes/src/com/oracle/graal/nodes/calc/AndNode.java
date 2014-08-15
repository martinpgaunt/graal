/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.nodes.calc;

import com.oracle.graal.api.meta.*;
import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.spi.*;
import com.oracle.graal.lir.gen.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.type.*;
import com.oracle.graal.nodes.util.*;

@NodeInfo(shortName = "&")
public class AndNode extends BitLogicNode implements NarrowableArithmeticNode {

    public static AndNode create(ValueNode x, ValueNode y) {
        return new AndNodeGen(x, y);
    }

    AndNode(ValueNode x, ValueNode y) {
        super(StampTool.and(x.stamp(), y.stamp()), x, y);
        assert x.stamp().isCompatible(y.stamp());
    }

    @Override
    public boolean inferStamp() {
        return updateStamp(StampTool.and(getX().stamp(), getY().stamp()));
    }

    @Override
    public Constant evalConst(Constant... inputs) {
        assert inputs.length == 2;
        return Constant.forPrimitiveInt(PrimitiveStamp.getBits(stamp()), inputs[0].asLong() & inputs[1].asLong());
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY) {
        if (GraphUtil.unproxify(forX) == GraphUtil.unproxify(forY)) {
            return forX;
        }
        if (forX.isConstant() && !forY.isConstant()) {
            return new AndNode(forY, forX);
        }
        if (forX.isConstant()) {
            return ConstantNode.forPrimitive(stamp(), evalConst(forX.asConstant(), forY.asConstant()));
        } else if (forY.isConstant()) {
            long rawY = forY.asConstant().asLong();
            long mask = IntegerStamp.defaultMask(PrimitiveStamp.getBits(stamp()));
            if ((rawY & mask) == mask) {
                return forX;
            }
            if ((rawY & mask) == 0) {
                return ConstantNode.forIntegerStamp(stamp(), 0);
            }
            if (forX instanceof SignExtendNode) {
                SignExtendNode ext = (SignExtendNode) forX;
                if (rawY == ((1L << ext.getInputBits()) - 1)) {
                    return new ZeroExtendNode(ext.getValue(), ext.getResultBits());
                }
            }
            if (forX.stamp() instanceof IntegerStamp) {
                IntegerStamp xStamp = (IntegerStamp) forX.stamp();
                if (((xStamp.upMask() | xStamp.downMask()) & ~rawY) == 0) {
                    // No bits are set which are outside the mask, so the mask will have no effect.
                    return forX;
                }
            }

            return BinaryNode.reassociate(this, ValueNode.isConstantPredicate(), forX, forY);
        }
        return this;
    }

    @Override
    public void generate(NodeMappableLIRBuilder builder, ArithmeticLIRGenerator gen) {
        builder.setResult(this, gen.emitAnd(builder.operand(getX()), builder.operand(getY())));
    }
}
