/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import android.graphics.PointF;
import android.util.Log;

import com.tom_roush.harmony.awt.geom.AffineTransform;
import com.tom_roush.harmony.javax.imageio.stream.ImageInputStream;
import com.tom_roush.harmony.javax.imageio.stream.MemoryCacheImageInputStream;
import com.tom_roush.pdfbox.cos.COSDictionary;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.cos.COSStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRange;
import com.tom_roush.pdfbox.rendering.WrapPaint;
import com.tom_roush.pdfbox.util.Matrix;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resources for a shading type 4 (Free-Form Gouraud-Shaded Triangle Mesh).
 */
public class PDShadingType4 extends PDTriangleBasedShadingType
{
    /**
     * Constructor using the given shading dictionary.
     *
     * @param shadingDictionary the dictionary for this shading
     */
    public PDShadingType4(COSDictionary shadingDictionary)
    {
        super(shadingDictionary);
    }

    @Override
    public int getShadingType()
    {
        return PDShading.SHADING_TYPE4;
    }

    /**
     * The bits per flag of this shading. This will return -1 if one has not
     * been set.
     *
     * @return The number of bits per flag.
     */
    public int getBitsPerFlag()
    {
        return getCOSObject().getInt(COSName.BITS_PER_FLAG, -1);
    }

    /**
     * Set the number of bits per flag.
     *
     * @param bitsPerFlag the number of bits per flag
     */
    public void setBitsPerFlag(int bitsPerFlag)
    {
        getCOSObject().setInt(COSName.BITS_PER_FLAG, bitsPerFlag);
    }

    @Override
    public WrapPaint toPaint(Matrix matrix)
    {
        return new Type4ShadingPaint(this, matrix);
    }

    @Override
    List<ShadedTriangle> collectTriangles(AffineTransform xform, Matrix matrix)
            throws IOException
    {
        int bitsPerFlag = getBitsPerFlag();
        COSDictionary dict = getCOSObject();
        if (!(dict instanceof COSStream))
        {
            return Collections.emptyList();
        }
        PDRange rangeX = getDecodeForParameter(0);
        PDRange rangeY = getDecodeForParameter(1);
        if (rangeX == null || rangeY == null ||
                Float.compare(rangeX.getMin(), rangeX.getMax()) == 0 ||
                Float.compare(rangeY.getMin(), rangeY.getMax()) == 0)
        {
            return Collections.emptyList();
        }
        PDRange[] colRange = new PDRange[getNumberOfColorComponents()];
        for (int i = 0; i < colRange.length; ++i)
        {
            colRange[i] = getDecodeForParameter(2 + i);
            if (colRange[i] == null)
            {
                throw new IOException("Range missing in shading /Decode entry");
            }
        }
        List<ShadedTriangle> list = new ArrayList<ShadedTriangle>();
        long maxSrcCoord = (long) Math.pow(2, getBitsPerCoordinate()) - 1;
        long maxSrcColor = (long) Math.pow(2, getBitsPerComponent()) - 1;
        COSStream stream = (COSStream) dict;

        ImageInputStream mciis = new MemoryCacheImageInputStream(stream.createInputStream());
        try
        {
            byte flag = (byte) 0;
            try
            {
                flag = (byte) (mciis.readBits(bitsPerFlag) & 3);
            }
            catch (EOFException ex)
            {
                Log.e("Pdfbox-Android", ex.getMessage());
            }

            boolean eof = false;
            while (!eof)
            {
                Vertex p0;
                Vertex p1;
                Vertex p2;
                PointF[] ps;
                float[][] cs;
                int lastIndex;
                try
                {
                    switch (flag)
                    {
                        case 0:
                            p0 = readVertex(mciis, maxSrcCoord, maxSrcColor, rangeX, rangeY, colRange,
                                    matrix, xform);
                            flag = (byte) (mciis.readBits(bitsPerFlag) & 3);
                            if (flag != 0)
                            {
                                Log.e("Pdfbox-Android", "bad triangle: " + flag);
                            }
                            p1 = readVertex(mciis, maxSrcCoord, maxSrcColor, rangeX, rangeY, colRange,
                                    matrix, xform);
                            mciis.readBits(bitsPerFlag);
                            if (flag != 0)
                            {
                                Log.e("Pdfbox-Android", "bad triangle: " + flag);
                            }
                            p2 = readVertex(mciis, maxSrcCoord, maxSrcColor, rangeX, rangeY, colRange,
                                    matrix, xform);
                            if(isAllVertexsZero(p0, p1, p2)){
                                throw new EOFException("end");
                            }
                            ps = new PointF[] { p0.point, p1.point, p2.point };
                            cs = new float[][] { p0.color, p1.color, p2.color };
                            list.add(new ShadedTriangle(ps, cs));
                            // here should raise EOFException in pdfbox, but it not raised in android, so we check isAllVertexsZero and raise it manually
                            flag = (byte) (mciis.readBits(bitsPerFlag) & 3);
                            break;
                        case 1:
                        case 2:
                            lastIndex = list.size() - 1;
                            if (lastIndex < 0)
                            {
                                Log.e("Pdfbox-Android", "broken data stream: " + list.size());
                            }
                            else
                            {
                                ShadedTriangle preTri = list.get(lastIndex);
                                p2 = readVertex(mciis, maxSrcCoord, maxSrcColor, rangeX, rangeY,
                                        colRange, matrix, xform);
                                ps = new PointF[] { flag == 1 ? preTri.corner[1] : preTri.corner[0],
                                        preTri.corner[2],
                                        p2.point };
                                cs = new float[][] { flag == 1 ? preTri.color[1] : preTri.color[0],
                                        preTri.color[2],
                                        p2.color };
                                list.add(new ShadedTriangle(ps, cs));
                                flag = (byte) (mciis.readBits(bitsPerFlag) & 3);
                            }
                            break;
                        default:
                            Log.w("Pdfbox-Android", "bad flag: " + flag);
                            break;
                    }
                }
                catch (EOFException ex)
                {
                    eof = true;
                }
            }
        }
        finally
        {
            mciis.close();
        }
        return list;
    }

    /**
     * check whether the read vertex is all zero
     * @param p0
     * @param p1
     * @param p2
     * @return
     */
    private boolean isAllVertexsZero(Vertex p0, Vertex p1, Vertex p2) {
        return p0.point.x + p0.point.y == 0 && p1.point.x + p1.point.y == 0 && p2.point.x + p2.point.y == 0;
    }

}
