package net.yadaframework.web.flot;

import java.io.Serializable;

//https://github.com/dunse/FlotJF

/**
 * The options inside "series: {}" are copied to each of the series. So
 * you can specify that all series should have bars by putting it in the
 * global options, or override it for individual series by specifying
 * bars in a particular the series object in the array of data.
 * <p>
 * The most important options are "lines", "points" and "bars" that
 * specify whether and how lines, points and bars should be shown for
 * each data series. In case you don't specify anything at all, Flot will
 * default to showing lines (you can turn this off with
 * lines: { show: false }). You can specify the various types
 * independently of each other, and Flot will happily draw each of them
 * in turn (this is probably only useful for lines and points), e.g.
 * <pre>
 *   var options = {
 *     series: {
 *       lines: { show: true, fill: true, fillColor: "rgba(255, 255, 255, 0.8)" },
 *       points: { show: true, fill: false }
 *     }
 *   };
 * </pre>
 * <b>JSON Data format for Series:</b>
 * <pre>
 *   series: {
 *     lines, points, bars: {
 *       show: boolean
 *       lineWidth: number
 *       fill: boolean or number
 *       fillColor: null or color/gradient
 *     }
 *
 *     points: {
 *       radius: number
 *       symbol: "circle" or function
 *     }
 *
 *     bars: {
 *       barWidth: number
 *       align: "left" or "center"
 *       horizontal: boolean
 *     }
 *
 *     lines: {
 *       steps: boolean
 *     }
 *
 *     shadowSize: number
 * }
 * </pre>
 */
public class YadaFlotSeriesOptions implements Serializable {
	private static final long serialVersionUID = 1L;
	
//	/** */
//	private LinesOptions lines;
//	/** */
//	private PointsOptions points;
//	/** */
//	private BarsOptions bars;
//	/** */
//	private Integer shadowSize;
//
//	/**
//	 * @param lines the lines to set
//	 * @see LinesOptions
//	 */
//	public void setLines(final LinesOptions lines) {
//		this.lines = lines;
//	}
//
//	/**
//	 * @param points the points to set
//	 * @see PointsOptions
//	 */
//	public void setPoints(final PointsOptions points) {
//		this.points = points;
//	}
//
//	/**
//	 * @return the points
//	 */
//	public PointsOptions getPoints() {
//		return points;
//	}
//
//	/**
//	 * @param bars the bars to set
//	 * @see BarsOptions
//	 */
//	public void setBars(final BarsOptions bars) {
//		this.bars = bars;
//	}
//	/**
//	 * "shadowSize" is the default size of shadows in pixels. Set it to 0 to
//	 * remove shadows.
//	 *
//	 * @param shadowSize the shadowSize to set
//	 */
//	public void setShadowSize(final Integer shadowSize) {
//		this.shadowSize = shadowSize;
//	}
//
//
//	/**
//	 * @return the shadowSize
//	 */
//	public Integer getShadowSize() {
//		return shadowSize;
//	}
//
//	/**
//	 * Helper method to set Point options.
//	 *
//	 * @param lineWidth TODO
//	 * @param radius TODO
//	 */
//	public void setPointOptions(final Integer lineWidth, final Integer radius) {
//		if (points == null) {
//			this.points = new PointsOptions();
//		}
//		this.points.setShow(true);
//		this.points.setLineWidth(lineWidth);
//		this.points.setRadius(radius);
//	}
//
//	/**
//	 * Helper method to set Point options.
//	 *
//	 */
//	public void setPointOptions() {
//		setPointOptions(null, null);
//	}
//
//	/**
//	 * Helper method to set Line options.
//	 *
//	 * @param lineWidth TODO
//	 * @param fillColor TODO
//	 */
//	public void setLineOptions(final Integer lineWidth, final String fillColor) {
//		if (lines == null) {
//			this.lines = new LinesOptions();
//		}
//		this.lines.setShow(true);
//		this.lines.setLineWidth(lineWidth);
//		if (fillColor != null) {
//			this.lines.setFillColor(fillColor);
//			this.lines.setFill("true");
//		}
//	}
//
//	/**
//	 * Helper method to set Line options.
//	 *
//	 * @param lineWidth TODO
//	 */
//	public void setLineOptions(final Integer lineWidth) {
//		setLineOptions(lineWidth, null);
//	}
//
//	/**
//	 * @return the lines
//	 */
//	public LinesOptions getLines() {
//		return lines;
//	}
//
//	/**
//	 * Helper method to set Line options.
//	 */
//	public void setLineOptions() {
//		setLineOptions(null, null);
//	}
//
//	/**
//	 * Helper method to set Line options.
//	 *
//	 * @param lineWidth TODO
//	 * @param fillColor TODO
//	 */
//	public void setBarOptions(final Integer lineWidth, final String fillColor) {
//		if (bars == null) {
//			this.bars = new BarsOptions();
//		}
//		this.bars.setShow(true);
//		this.bars.setLineWidth(lineWidth);
//		if (fillColor != null) {
//			this.bars.setFillColor(fillColor);
//			this.bars.setFill("true");
//		}
//	}
//
//	/**
//	 * @return the bars
//	 */
//	public BarsOptions getBars() {
//		return bars;
//	}
//
//	/**
//	 * Helper method to set Bar options.
//	 */
//	public void setBarOptions() {
//		setBarOptions(null, null);
//	}
}
