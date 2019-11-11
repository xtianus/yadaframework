package net.yadaframework.raw;

import javax.persistence.Embeddable;

/**
 * Like java.awt.Dimension but with int values.
 *
 */
@Embeddable
public class YadaIntDimension implements java.io.Serializable {
	private static final long serialVersionUID = 526069244927838614L;

	/**
     * The width dimension; negative values can be used.
     */
    public int width;

    /**
     * The height dimension; negative values can be used.
     */
    public int height;

    /**
     * Creates an instance of <code>YadaIntDimension</code> with a width
     * of zero and a height of zero.
     */
    public YadaIntDimension() {
        this(0, 0);
    }

    /**
     * Creates an instance of <code>YadaIntDimension</code> whose width
     * and height are the same as for the specified dimension.
     *
     * @param    d   the specified dimension for the
     *               <code>width</code> and
     *               <code>height</code> values
     */
    public YadaIntDimension(YadaIntDimension d) {
        this(d.width, d.height);
    }

    /**
     * Constructs a <code>YadaIntDimension</code> and initializes
     * it to the specified width and specified height.
     *
     * @param width the width, null becomes 0
     * @param height the height, null becomes 0
     */
    public YadaIntDimension(Integer width, Integer height) {
        this.width = width==null?0:width;
        this.height = height==null?0:height;
    }

    /**
     * Returns the biggest dimension, or the first one if there are many of the same size
     * @param dimensions can be null
     * @return
     */
    public static YadaIntDimension biggest(YadaIntDimension...dimensions) {
    	if (dimensions==null) {
    		return null;
    	}
    	YadaIntDimension biggest = null;
    	for (YadaIntDimension dimension : dimensions) {
			if (dimension.isBiggerThan(biggest)) {
				biggest = dimension;
			}
		}
    	return biggest;
    }

    /**
     * Returns the smallest dimension, or the first one if there are many of the same size
     * @param dimensions can be null
     * @return
     */
    public static YadaIntDimension smallest(YadaIntDimension...dimensions) {
    	if (dimensions==null) {
    		return null;
    	}
    	YadaIntDimension smallest = null;
    	for (YadaIntDimension dimension : dimensions) {
    		if (dimension.isSmallerThan(smallest)) {
    			smallest = dimension;
    		}
    	}
    	return smallest;
    }

    /**
     * Returns true if at least one dimension is bigger than the corresponding argument and the other one is not smaller.
     * @param yadaIntDimension can be null
     * @return
     */
    public boolean isBiggerThan(YadaIntDimension yadaIntDimension) {
    	if (yadaIntDimension==null) {
    		return true;
    	}
    	return !this.isEqualTo(yadaIntDimension) &&
    		this.width >= yadaIntDimension.width && this.height >= yadaIntDimension.height;
    }

    /**
     * Returns true if at least one dimension is smaller than the corresponding argument and the other one is not bigger.
     * @param yadaIntDimension can be null
     * @return
     */
    public boolean isSmallerThan(YadaIntDimension yadaIntDimension) {
       	if (yadaIntDimension==null) {
    		return true;
    	}
    	return !this.isEqualTo(yadaIntDimension) &&
			this.width <= yadaIntDimension.width && this.height <= yadaIntDimension.height;
    }

    /**
      * Returns true if this instance is equal to the argument
     * @param yadaIntDimension can be null
     * @return
     */
    public boolean isEqualTo(YadaIntDimension yadaIntDimension) {
       	if (yadaIntDimension==null) {
    		return false;
    	}
    	return this.width == yadaIntDimension.width && this.height == yadaIntDimension.height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Sets the size of this <code>YadaIntDimension</code> object to
     * the specified width and height.
     *
     * @param width  the new width for the <code>YadaIntDimension</code> object
     * @param height the new height for the <code>YadaIntDimension</code> object
     * @since 1.2
     */
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Sets the size of this <code>YadaIntDimension</code> object to the specified size.
     */
    public void setSize(YadaIntDimension d) {
        setSize(d.width, d.height);
    }

    /**
     * Checks whether two dimension objects have equal values.
     */
    public boolean equals(Object obj) {
        if (obj instanceof YadaIntDimension) {
        	YadaIntDimension d = (YadaIntDimension)obj;
            return (width == d.width) && (height == d.height);
        }
        return false;
    }

    /**
     * Returns the hash code for this <code>YadaIntDimension</code>.
     *
     */
    public int hashCode() {
        int sum = width + height;
        return sum * (sum + 1)/2 + width;
    }

    /**
     * Returns a string representation in the form WxH
     */
    public String toString() {
        return width + "x" + height;
    }
}
