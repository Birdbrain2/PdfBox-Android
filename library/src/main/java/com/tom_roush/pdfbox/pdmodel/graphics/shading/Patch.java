package com.tom_roush.pdfbox.pdmodel.graphics.shading;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;

abstract class Patch {

    protected PointF[][] controlPoints;
    protected float[][] cornerColor;

    /*
     level = {levelU, levelV}, levelU defines the patch's u direction edges should be 
     divided into 2^levelU parts, level V defines the patch's v direction edges should
     be divided into 2^levelV parts
     */
    protected int[] level;
    protected List<ShadedTriangle> listOfTriangles;

    /**
     * Constructor of Patch.
     *
     * @param color 4 corner's colors
     */
    Patch(float[][] color)
    {
        cornerColor = color.clone();
    }

    /**
     * Get the implicit edge for flag = 1.
     *
     * @return implicit control points
     */
    protected abstract PointF[] getFlag1Edge();

    /**
     * Get the implicit edge for flag = 2.
     *
     * @return implicit control points
     */
    protected abstract PointF[] getFlag2Edge();

    /**
     * Get the implicit edge for flag = 3.
     *
     * @return implicit control points
     */
    protected abstract PointF[] getFlag3Edge();

    /**
     * Get the implicit color for flag = 1.
     *
     * @return color
     */
    protected float[][] getFlag1Color()
    {
        int numberOfColorComponents = cornerColor[0].length;
        float[][] implicitCornerColor = new float[2][numberOfColorComponents];
        for (int i = 0; i < numberOfColorComponents; i++)
        {
            implicitCornerColor[0][i] = cornerColor[1][i];
            implicitCornerColor[1][i] = cornerColor[2][i];
        }
        return implicitCornerColor;
    }

    /**
     * Get implicit color for flag = 2.
     *
     * @return color
     */
    protected float[][] getFlag2Color()
    {
        int numberOfColorComponents = cornerColor[0].length;
        float[][] implicitCornerColor = new float[2][numberOfColorComponents];
        for (int i = 0; i < numberOfColorComponents; i++)
        {
            implicitCornerColor[0][i] = cornerColor[2][i];
            implicitCornerColor[1][i] = cornerColor[3][i];
        }
        return implicitCornerColor;
    }

    /**
     * Get implicit color for flag = 3.
     *
     * @return color
     */
    protected float[][] getFlag3Color()
    {
        int numberOfColorComponents = cornerColor[0].length;
        float[][] implicitCornerColor = new float[2][numberOfColorComponents];
        for (int i = 0; i < numberOfColorComponents; i++)
        {
            implicitCornerColor[0][i] = cornerColor[3][i];
            implicitCornerColor[1][i] = cornerColor[0][i];
        }
        return implicitCornerColor;
    }

    /**
     * Calculate the distance from point ps to point pe.
     *
     * @param ps one end of a line
     * @param pe the other end of the line
     * @return length of the line
     */
    protected double getLen(PointF ps, PointF pe)
    {
        double x = pe.x - ps.x;
        double y = pe.y - ps.y;
        return Math.sqrt(x * x + y * y);
    }

    /**
     * Whether the for control points are on a line.
     *
     * @param ctl an edge's control points, the size of ctl is 4
     * @return true when 4 control points are on a line, otherwise false
     */
    protected boolean isEdgeALine(PointF[] ctl)
    {
        double ctl1 = Math.abs(edgeEquationValue(ctl[1], ctl[0], ctl[3]));
        double ctl2 = Math.abs(edgeEquationValue(ctl[2], ctl[0], ctl[3]));
        double x = Math.abs(ctl[0].x - ctl[3].x);
        double y = Math.abs(ctl[0].y - ctl[3].y);
        return (ctl1 <= x && ctl2 <= x) || (ctl1 <= y && ctl2 <= y);
    }

    /**
     * A line from point p1 to point p2 defines an equation, adjust the form of
     * the equation to let the rhs equals 0, then calculate the lhs value by
     * plugging the coordinate of p in the lhs expression.
     *
     * @param p target point
     * @param p1 one end of a line
     * @param p2 the other end of a line
     * @return calculated value
     */
    protected double edgeEquationValue(PointF p, PointF p1, PointF p2)
    {
        return (p2.y - p1.y) * (p.x - p1.x) - (p2.x - p1.x) * (p.y - p1.y);
    }

    /**
     * An assistant method to accomplish type 6 and type 7 shading.
     *
     * @param patchCC all the crossing point coordinates and color of a grid
     * @return a ShadedTriangle list which can compose the grid patch
     */
    protected List<ShadedTriangle> getShadedTriangles(CoordinateColorPair[][] patchCC)
    {
        List<ShadedTriangle> list = new ArrayList<ShadedTriangle>();
        int szV = patchCC.length;
        int szU = patchCC[0].length;
        for (int i = 1; i < szV; i++)
        {
            for (int j = 1; j < szU; j++)
            {
                PointF p0 = patchCC[i - 1][j - 1].coordinate;
                PointF p1 = patchCC[i - 1][j].coordinate;
                PointF p2 = patchCC[i][j].coordinate;
                PointF p3 = patchCC[i][j - 1].coordinate;
                boolean ll = true;
                if (overlaps(p0, p1) || overlaps(p0, p3))
                {
                    ll = false;
                }
                else
                {
                    // p0, p1 and p3 are in counter clock wise order, p1 has priority over p0, p3 has priority over p1
                    PointF[] llCorner =
                            {
                                    p0, p1, p3
                            };
                    float[][] llColor =
                            {
                                    patchCC[i - 1][j - 1].color, patchCC[i - 1][j].color, patchCC[i][j - 1].color
                            };
                    ShadedTriangle tmpll = new ShadedTriangle(llCorner, llColor); // lower left triangle
                    list.add(tmpll);
                }
                if (ll && (overlaps(p2, p1) || overlaps(p2, p3)))
                {
                }
                else
                {
                    // p3, p1 and p2 are in counter clock wise order, p1 has priority over p3, p2 has priority over p1
                    PointF[] urCorner =
                            {
                                    p3, p1, p2
                            };
                    float[][] urColor =
                            {
                                    patchCC[i][j - 1].color, patchCC[i - 1][j].color, patchCC[i][j].color
                            };
                    ShadedTriangle tmpur = new ShadedTriangle(urCorner, urColor); // upper right triangle
                    list.add(tmpur);
                }
            }
        }
        return list;
    }

    // whether two points p0 and p1 are degenerated into one point within the coordinates' accuracy 0.001
    private boolean overlaps(PointF p0, PointF p1)
    {
        return Math.abs(p0.x - p1.x) < 0.001 && Math.abs(p0.y - p1.y) < 0.001;
    }
}
