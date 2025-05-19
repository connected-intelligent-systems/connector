package org.eclipse.edc.extension.policy;

import java.util.Objects;

/**
 * Bounding box class representing a rectangular area in 2D space.
 */
public class BoundingBox {
    public double xmin;
    public double ymin;
    public double xmax;
    public double ymax;

    public BoundingBox() {
        this.xmin = 0;
        this.ymin = 0;
        this.xmax = 0;
        this.ymax = 0;
    }



    public BoundingBox(double xmin, double ymin, double xmax, double ymax) {
        this.xmin = xmin;
        this.ymin = ymin;
        this.xmax = xmax;
        this.ymax = ymax;
    }

    public boolean isInside(BoundingBox other) {
        return xmin >= other.xmin && ymin >= other.ymin &&
                xmax <= other.xmax && ymax <= other.ymax;
    }

    public boolean intersects(BoundingBox other) {
        return xmax >= other.xmin && xmin <= other.xmax &&
                ymax >= other.ymin && ymin <= other.ymax;
    }

    public double getXmin() {
        return xmin;
    }

    public void setXmin(double xmin) {
        this.xmin = xmin;
    }

    public double getYmin() {
        return ymin;
    }

    public void setYmin(double ymin) {
        this.ymin = ymin;
    }

    public double getXmax() {
        return xmax;
    }

    public void setXmax(double xmax) {
        this.xmax = xmax;
    }

    public double getYmax() {
        return ymax;
    }

    public void setYmax(double ymax) {
        this.ymax = ymax;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoundingBox that = (BoundingBox) o;
        return Double.compare(xmin, that.xmin) == 0 && Double.compare(ymin, that.ymin) == 0 && Double.compare(xmax, that.xmax) == 0 && Double.compare(ymax, that.ymax) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(xmin, ymin, xmax, ymax);
    }

    @Override
    public String toString() {
        return "BoundingBox[xmin=" + xmin + ", ymin=" + ymin + ", xmax=" + xmax + ", ymax=" + ymax + "]";
    }
}
