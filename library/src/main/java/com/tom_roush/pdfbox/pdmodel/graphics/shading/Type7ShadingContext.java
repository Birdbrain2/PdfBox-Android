/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tom_roush.pdfbox.pdmodel.graphics.shading;

import android.graphics.Rect;

import java.io.IOException;

import com.tom_roush.harmony.awt.geom.AffineTransform;
import com.tom_roush.pdfbox.util.Matrix;

/**
 * PaintContext for tensor-product patch meshes (type 7) shading. This was
 * done as part of GSoC2014, Tilman Hausherr is the mentor.
 *
 * @author Shaola Ren
 */
class Type7ShadingContext extends PatchMeshesShadingContext
{
    /**
     * Constructor creates an instance to be used for fill operations.
     *
     * @param shading the shading type to be used
     * @param xform transformation for user to device space
     * @param matrix the pattern matrix concatenated with that of the parent content stream
     * @param deviceBounds device bounds
     * @throws IOException if something went wrong
     */
    Type7ShadingContext(PDShadingType7 shading, AffineTransform xform,
        Matrix matrix, Rect deviceBounds) throws IOException
    {
        super(shading, xform, matrix, deviceBounds, 16);
    }
}
