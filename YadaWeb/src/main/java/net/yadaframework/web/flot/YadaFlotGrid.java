package net.yadaframework.web.flot;

//https://github.com/dunse/FlotJF

/**
 * The grid is the thing with the axes and a number of ticks. Many of the
 * things in the grid are configured under the individual axes, but not
 * all.
 * <p>
 * Using {@link #setHoverable(Boolean)} or {@link #setClickable(Boolean)},
 * You can use "plotclick" and "plothover" events like this:
 * <pre>
 *   $.plot($("#placeholder"), [ d ], { grid: { clickable: true } });
 *
 *   $("#placeholder").bind("plotclick", function (event, pos, item) {
 *     alert("You clicked at " + pos.x + ", " + pos.y);
 *     // axis coordinates for other axes, if present, are in pos.x2, pos.x3, ...
 *     // if you need global screen coordinates, they are pos.pageX, pos.pageY
 *
 *     if (item) {
 *       highlight(item.series, item.datapoint);
 *       alert("You clicked a point!");
 *     }
 * });
 * </pre>
 * The item object in this example is either null or a nearby object on the form:
 * <pre>
 *   item: {
 *     datapoint: the point, e.g. [0, 2]
 *     dataIndex: the index of the point in the data array
 *     series: the series object
 *     seriesIndex: the index of the series
 *     pageX, pageY: the global screen coordinates of the point
 *   }
 * </pre>
 * For instance, if you have specified the data like this
 * <pre>
 *   $.plot($("#placeholder"), [ { label: "Foo", data: [[0, 10], [7, 3]] } ], ...);
 * </pre>
 * and the mouse is near the point (7, 3), "datapoint" is [7, 3],
 * "dataIndex" will be 1, "series" is a normalized series object with
 * among other things the "Foo" label in series.label and the color in
 * series.color, and "seriesIndex" is 0. Note that plugins and options
 * that transform the data can shift the indexes from what you specified
 * in the original data array.
 * <p>
 * If you use the above events to update some other information and want
 * to clear out that info in case the mouse goes away, you'll probably
 * also need to listen to "mouseout" events on the placeholder div.
 * <p>
 * <b>JSON Data format for Grid:</b>
 * <pre>
 * grid: {
 *   show: boolean
 *   aboveData: boolean
 *   color: color
 *   backgroundColor: color/gradient or null
 *   labelMargin: number
 *   axisMargin: number
 *   markings: array of markings or (fn: axes -> array of markings)
 *   borderWidth: number
 *   borderColor: color or null
 *   minBorderMargin: number or null
 *   clickable: boolean
 *   hoverable: boolean
 *   autoHighlight: boolean
 *   mouseActiveRadius: number
 * }
 * </pre>
 * This class has been constructed as per flot API documentation.
 * @see <A href="http://flot.googlecode.com/svn/trunk/API.txt" target="_blank">Flot API.txt</A>
 */
public class YadaFlotGrid {
	private Boolean show;
	private Boolean aboveData;
	private String color;
	private String backgroundColor;
	private Integer labelMargin;
	private Integer axisMargin;
	private String markings;
	private Integer borderWidth;
	private String borderColor;
	private Integer minBorderMargin;
	private Boolean clickable;
	private Boolean hoverable;
	private Boolean autoHighlight;
	private Integer mouseActiveRadius;


	/**
	 * You can turn off the whole grid including tick labels by setting
	 * "show" to false.
	 * <p>
	 * @param show boolean
	 */
	public void setShow(final Boolean show) {
		this.show = show;
	}
	/**
	 * "aboveData" determines whether the grid is drawn
	 * above the data or below (below is default).
	 * <p>
	 * @param aboveData boolean
	 */
	public void setAboveData(final Boolean aboveData) {
		this.aboveData = aboveData;
	}
	/**
	 * "color" is the colour of the grid itself.
	 * <p>
	 * @param color colour
	 */
	public void setColor(final String color) {
		this.color = color;
	}
	/**
	 * "backgroundColor" specifies the background colour inside the grid area,
	 * here null means that the background is transparent.
	 * <p>
	 * @param backgroundColor colour/gradient or null
	 */
	public void setBackgroundColor(final String backgroundColor) {
		this.backgroundColor = backgroundColor;
	}
	/**
	 * "labelMargin" is the space in pixels between tick labels and axis line.
	 * <p>
	 * @param labelMargin number
	 */
	public void setLabelMargin(final Integer labelMargin) {
		this.labelMargin = labelMargin;
	}
	/**
	 * "axisMargin" is the space in pixels between axes when there are two next
	 * to each other.
	 * <p>
	 * @param axisMargin number
	 */
	public void setAxisMargin(final Integer axisMargin) {
		this.axisMargin = axisMargin;
	}
	/**
	 * "markings" is used to draw simple lines and rectangular areas in the
	 * background of the plot. You can either specify an array of ranges on
	 * the form { xaxis: { from, to }, yaxis: { from, to } } (with multiple
	 * axes, you can specify coordinates for other axes instead, e.g. as
	 * x2axis/x3axis/...) or with a function that returns such an array given
	 * the axes for the plot in an object as the first parameter.
	 * <p>
	 * You can set the color of markings by specifying "color" in the ranges
	 * object. Here's an example array:
	 * <pre>
	 *   markings: [ { xaxis: { from: 0, to: 2 }, yaxis: { from: 10, to: 10 }, color: "#bb0000" }, ... ]
	 * </pre>
	 * If you leave out one of the values, that value is assumed to go to the
	 * border of the plot. So for example if you only specify { xaxis: {
	 * from: 0, to: 2 } } it means an area that extends from the top to the
	 * bottom of the plot in the x range 0-2.
	 * <p>
	 * A line is drawn if from and to are the same, e.g.
	 * <pre>
	 *   markings: [ { yaxis: { from: 1, to: 1 } }, ... ]
	 * </pre>
	 * would draw a line parallel to the x axis at y = 1. You can control the
	 * line width with "lineWidth" in the range object.
	 * @param markings array of markings or (fn: axes -> array of markings)
	 */
	public void setMarkings(final String markings) {
		this.markings = markings;
	}
	/**
	 * "borderWidth" is the width of the border around the plot. Set it to 0
	 * to disable the border.
	 * <p>
	 * @param borderWidth number
	 */
	public void setBorderWidth(final Integer borderWidth) {
		this.borderWidth = borderWidth;
	}
	/**
	 * You can also set "borderColor" if you want the
	 * border to have a different color than the grid lines.
	 * <p>
	 * @param borderColor color or null
	 */
	public void setBorderColor(final String borderColor) {
		this.borderColor = borderColor;
	}
	/**
	 * "minBorderMargin" controls the default minimum margin around the
	 * border - it's used to make sure that points aren't accidentally
	 * clipped by the canvas edge so by default the value is computed from
	 * the point radius.
	 * <p>
	 * @param minBorderMargin number or null
	 */
	public void setMinBorderMargin(final Integer minBorderMargin) {
		this.minBorderMargin = minBorderMargin;
	}
	/**
	 * If you set "clickable" to true, the plot will listen for click events
	 * on the plot area and fire a "plotclick" event on the placeholder with
	 * a position and a nearby data item object as parameters. The coordinates
	 * are available both in the unit of the axes (not in pixels) and in
	 * global screen coordinates.
	 * <p>
	 * @param clickable boolean
	 */
	public void setClickable(final Boolean clickable) {
		this.clickable = clickable;
	}
	/**
	 * Likewise, if you set "hoverable" to true, the plot will listen for
	 * mouse move events on the plot area and fire a "plothover" event with
	 * the same parameters as the "plotclick" event. If "autoHighlight" is
	 * true (the default), nearby data items are highlighted automatically.
	 * If needed, you can disable highlighting and control it yourself with
	 * the highlight/unhighlight plot methods described elsewhere.
	 * <p>
	 * @param hoverable boolean
	 */
	public void setHoverable(final Boolean hoverable) {
		this.hoverable = hoverable;
	}
	/**
	 * If "autoHighlight" is true (the default), nearby data items are
	 * highlighted automatically. If needed, you can disable highlighting
	 * and control it yourself with the highlight/unhighlight plot methods
	 * described elsewhere.
	 * <p>
	 * @param autoHighlight boolean
	 */
	public void setAutoHighlight(final Boolean autoHighlight) {
		this.autoHighlight = autoHighlight;
	}
	/**
	 * "mouseActiveRadius" specifies how far the mouse can be from an item
	 * and still activate it. If there are two or more points within this
	 * radius, Flot chooses the closest item. For bars, the top-most bar
	 * (from the latest specified data series) is chosen.
	 * <p>
	 * @param mouseActiveRadius number
	 */
	public void setMouseActiveRadius(final Integer mouseActiveRadius) {
		this.mouseActiveRadius = mouseActiveRadius;
	}
}
