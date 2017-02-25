package net.yadaframework.web.flot;

import java.io.Serializable;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

//https://github.com/dunse/FlotJF

/**
 * A Flot Series in Object format.
 * 
 * 
 * <b>JSON Data format for Data:</b>
 * <pre>
{
    color: color or number
    data: rawdata
    label: string
    lines: specific lines options
    bars: specific bars options
    points: specific points options
    xaxis: number
    yaxis: number
    clickable: boolean
    hoverable: boolean
    shadowSize: number
    highlightColor: color or number
}
 * </pre>
 * @see <A href="https://github.com/flot/flot/blob/master/API.md" target="_blank">Flot Documentation</A>
 */

@JsonInclude(Include.NON_DEFAULT)
public class YadaFlotSeriesObject extends YadaFlotSeriesOptions implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String color = null;
	private ArrayList<Object[]> data = new ArrayList<Object[]>();
	private String label = null;
	private Integer xaxis = null;
	private Integer yaxis = null;
	private Boolean clickable = null;
	private Boolean hoverable = null;

	public YadaFlotSeriesObject() {
	}

	/**
	 * @param label Plot Label
	 */
	public YadaFlotSeriesObject(final String label) {
		this.label = label;
	}

	/**
	 * @param label Plot Label
	 * @param color Plot Colour
	 */
	public YadaFlotSeriesObject(final String label, final String color) {
		this.label = label;
		this.color = color;
	}
	
	/**
	 * @param x X coordinate
	 * @param y Y coordinate
	 */
	public void addPoint(final Object x, final Object y) {
		Object[] point = new Object[2];
		point[0] = x;
		point[1] = y;
		this.data.add(point);
	}

	/**
	 *
	 */
	public void setRightYAxis() {
		this.yaxis = new Integer(2);
	}

	/**
	 * @param color the color to set
	 */
	public void setColor(final String color) {
		this.color = color;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(final ArrayList<Object[]> data) {
		this.data = data;
	}

	/**
	 * @param label the label to set
	 */
	public void setLabel(final String label) {
		this.label = label;
	}

	/**
	 * The "xaxis" and "yaxis" options specify which axis to use. The axes
	 * are numbered from 1 (default), so { yaxis: 2} means that the series
	 * should be plotted against the second y axis.
	 *
	 * @param xaxis the xaxis to set
	 */
	public void setXaxis(final Integer xaxis) {
		this.xaxis = xaxis;
	}

	/**
	 * The "xaxis" and "yaxis" options specify which axis to use. The axes
	 * are numbered from 1 (default), so { yaxis: 2} means that the series
	 * should be plotted against the second y axis.
	 *
	 * @param yaxis the yaxis to set
	 */
	public void setYaxis(final Integer yaxis) {
		this.yaxis = yaxis;
	}

	/**
	 * "clickable" and "hoverable" can be set to false to disable
	 * interactivity for specific series if interactivity is turned on in
	 * the plot, see TODO.
	 *
	 * @param clickable the clickable to set
	 */
	public void setClickable(final Boolean clickable) {
		this.clickable = clickable;
	}

	/**
	 * "clickable" and "hoverable" can be set to false to disable
	 * interactivity for specific series if interactivity is turned on in
	 * the plot, see TODO.
	 *
	 * @param hoverable the hoverable to set
	 */
	public void setHoverable(final Boolean hoverable) {
		this.hoverable = hoverable;
	}

	/**
	 * @return the color
	 */
	public String getColor() {
		return color;
	}

	/**
	 * @return the data
	 */
	public ArrayList<Object[]> getData() {
		return data;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return the xaxis
	 */
	public Integer getXaxis() {
		return xaxis;
	}

	/**
	 * @return the yaxis
	 */
	public Integer getYaxis() {
		return yaxis;
	}

	/**
	 * @return the clickable
	 */
	public Boolean getClickable() {
		return clickable;
	}

	/**
	 * @return the hoverable
	 */
	public Boolean getHoverable() {
		return hoverable;
	}
}
