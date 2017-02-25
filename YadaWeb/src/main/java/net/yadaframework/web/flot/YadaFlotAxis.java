package net.yadaframework.web.flot;

// https://github.com/dunse/FlotJF

/**
 * Class to setup X or Y axis options.
 * All variables are default set to null (except for {@link #setShow(Boolean)}).
 * When a value is set to null the json printer will skip this value and
 * have no impact on the behaviour of flot.
 * <p>
 * Time series data
 * ================
 * Time series are a bit more difficult than scalar data because
 * calendars don't follow a simple base 10 system. For many cases, Flot
 * abstracts most of this away, but it can still be a bit difficult to
 * get the data into Flot. So we'll first discuss the data format.
 * <p>
 * The time series support in Flot is based on Javascript timestamps,
 * i.e. everywhere a time value is expected or handed over, a Javascript
 * timestamp number is used. This is a number, not a Date object. A
 * Javascript timestamp is the number of milliseconds since January 1,
 * 1970 00:00:00 UTC. This is almost the same as Unix timestamps, except it's
 * in milliseconds, so remember to multiply by 1000!
 * <p>
 * You can see a timestamp like this
 * <pre>
 *   alert((new Date()).getTime())
 * </pre>
 * Normally you want the timestamps to be displayed according to a
 * certain time zone, usually the time zone in which the data has been
 * produced. However, Flot always displays timestamps according to UTC.
 * It has to as the only alternative with core Javascript is to interpret
 * the timestamps according to the time zone that the visitor is in,
 * which means that the ticks will shift unpredictably with the time zone
 * and daylight savings of each visitor.
 * <p>
 * So given that there's no good support for custom time zones in
 * Javascript, you'll have to take care of this server-side.
 * <p>
 * The easiest way to think about it is to pretend that the data
 * production time zone is UTC, even if it isn't. So if you have a
 * datapoint at 2002-02-20 08:00, you can generate a timestamp for eight
 * o'clock UTC even if it really happened eight o'clock UTC+0200.
 * <p>
 * In PHP you can get an appropriate timestamp with
 * 'strtotime("2002-02-20 UTC") * 1000', in Python with
 * 'calendar.timegm(datetime_object.timetuple()) * 1000', in .NET with
 * something like:
 * <pre>
 *   public static int GetJavascriptTimestamp(System.DateTime input)
 *   {
 *     System.TimeSpan span = new System.TimeSpan(System.DateTime.Parse("1/1/1970").Ticks);
 *     System.DateTime time = input.Subtract(span);
 *     return (long)(time.Ticks / 10000);
 *   }
 * </pre>
 * Javascript also has some support for parsing date strings, so it is
 * possible to generate the timestamps manually client-side.
 * <p>
 * If you've already got the real UTC timestamp, it's too late to use the
 * pretend trick described above. But you can fix up the timestamps by
 * adding the time zone offset, e.g. for UTC+0200 you would add 2 hours
 * to the UTC timestamp you got. Then it'll look right on the plot. Most
 * programming environments have some means of getting the timezone
 * offset for a specific date (note that you need to get the offset for
 * each individual timestamp to account for daylight savings).
 * <p>
 * Once you've gotten the timestamps into the data and specified "time"
 * as the axis mode, Flot will automatically generate relevant ticks and
 * format them. As always, you can tweak the ticks via the "ticks" option
 * - just remember that the values should be timestamps (numbers), not
 * Date objects.
 * <p>
 * Tick generation and formatting can also be controlled separately
 * through the following axis options:
 * <pre>
 *   minTickSize: array
 *   timeformat: null or format string
 *   monthNames: null or array of size 12 of strings
 *   twelveHourClock: boolean
 * </pre>
 * Here "timeformat" is a format string to use. You might use it like
 * this:
 * <pre>
 *   xaxis: {
 *     mode: "time"
 *     timeformat: "%y/%m/%d"
 *   }
 * </pre>
 * This will result in tick labels like "2000/12/24". The following
 * specifiers are supported
 * <pre>
 *  %h: hours
 *  %H: hours (left-padded with a zero)
 *  %M: minutes (left-padded with a zero)
 *  %S: seconds (left-padded with a zero)
 *  %d: day of month (1-31), use %0d for zero-padding
 *  %m: month (1-12), use %0m for zero-padding
 *  %y: year (four digits)
 *  %b: month name (customizable)
 *  %p: am/pm, additionally switches %h/%H to 12 hour instead of 24
 *  %P: AM/PM (uppercase version of %p)
 * </pre>
 * Inserting a zero like %0m or %0d means that the specifier will be
 * left-padded with a zero if it's only single-digit. So %y-%0m-%0d
 * results in unambigious ISO timestamps like 2007-05-10 (for May 10th).
 * <p>
 * You can customize the month names with the "monthNames" option. For
 * instance, for Danish you might specify:
 * <pre>
 *   monthNames: ["jan", "feb", "mar", "apr", "maj", "jun", "jul", "aug", "sep", "okt", "nov", "dec"]
 * </pre>
 * If you set "twelveHourClock" to true, the autogenerated timestamps
 * will use 12 hour AM/PM timestamps instead of 24 hour.
 * <p>
 * The format string and month names are used by a very simple built-in
 * format function that takes a date object, a format string (and
 * optionally an array of month names) and returns the formatted string.
 * If needed, you can access it as $.plot.formatDate(date, formatstring,
 * monthNames) or even replace it with another more advanced function
 * from a date library if you're feeling adventurous.
 * <p>
 * If everything else fails, you can control the formatting by specifying
 * a custom tick formatter function as usual. Here's a simple example
 * which will format December 24 as 24/12:
 * <pre>
 *   tickFormatter: function (val, axis) {
 *     var d = new Date(val);
 *     return d.getUTCDate() + "/" + (d.getUTCMonth() + 1);
 *   }
 * </pre>
 * Note that for the time mode "tickSize" and "minTickSize" are a bit
 * special in that they are arrays on the form "[value, unit]" where unit
 * is one of "second", "minute", "hour", "day", "month" and "year". So
 * you can specify
 * <pre>
 *   minTickSize: [1, "month"]
 * </pre>
 * to get a tick interval size of at least 1 month and correspondingly,
 * if axis.tickSize is [2, "day"] in the tick formatter, the ticks have
 * been produced with two days in-between.
 * <b>JSON Data format for XAxis/YAxis:</b>
 * <pre>
 *  xaxis, yaxis: {
 *    show: null or true/false
 *    position: "bottom" or "top" or "left" or "right"
 *    mode: null or "time"
 *
 *    color: null or color spec
 *    tickColor: null or color spec
 *    font: null or font spec object
 *
 *    min: null or number
 *    max: null or number
 *    autoscaleMargin: null or number
 *
 *    transform: null or fn: number -> number
 *    inverseTransform: null or fn: number -> number
 *
 *    ticks: null or number or ticks array or (fn: axis -> ticks array)
 *    tickSize: number or array
 *    minTickSize: number or array
 *    tickFormatter: (fn: number, object -> string) or string
 *    tickDecimals: null or number
 *
 *    labelWidth: null or number
 *    labelHeight: null or number
 *    reserveSpace: null or true
 *
 *    tickLength: null or number
 *
 *    alignTicksWithAxis: null or number
 * }
 * </pre>
 * <p>
 * This class has been constructed as per flot API documentation.
 * @see <A href="http://flot.googlecode.com/svn/trunk/API.txt" target="_blank">Flot API.txt</A>
 */
public class YadaFlotAxis {
	private Boolean show = null;
	private String position = null;
	private String mode = null;
	private String timeformat = null;

	private String color = null;
	private String tickColor = null;
	private String font = null;

	private Double min = null;
	private Double max = null;
	private Double autoscaleMargin = null;

	private String transform = null;
	private String inverseTransform = null;

	private String ticks = null;
	private String tickSize = null;
	private String minTickSize = null;
	private String tickFormatter = null;
	private Integer tickDecimals = null;

	private Integer labelWidth = null;
	private Integer labelHeight = null;
	private Integer reserveSpace = null;

	private Integer tickLength = null;

	private Integer alignTicksWithAxis = null;

	/**
	 * Sets the preferred time format for the axis. This setter will automatically
	 * change the mode to "time".
	 * Use the following guide to specify the format as a {@link String}:
	 * <pre>
	 *   %h: hours
	 *   %H: hours (left-padded with a zero)
	 *   %M: minutes (left-padded with a zero)
	 *   %S: seconds (left-padded with a zero)
	 *   %d: day of month (1-31), use %0d for zero-padding
	 *   %m: month (1-12), use %0m for zero-padding
	 *   %y: year (four digits)
	 *   %b: month name (customizable)
	 *   %p: am/pm, additionally switches %h/%H to 12 hour instead of 24
	 *   %P: AM/PM (uppercase version of %p)
	 * </pre>
	 *   For example, specifying timeformat as "%y/%m/%d" will
	 *   result in tick labels like "2000/12/24".
	 * <p>
	 *   @param timeformat What time format given axis should be displayed in.
	 *   @see <A href="http://flot.googlecode.com/svn/trunk/API.txt" target="_blank">Flot API.txt</A>
	 */
	public void setTimeformat(final String timeformat) {
		setMode("time");
		this.timeformat = timeformat;
	}

	/**
	 * The options "min"/"max" are the precise minimum/maximum value on the
	 * scale. If you don't specify either of them, a value will automatically
	 * be chosen based on the minimum/maximum data values. Note that Flot
	 * always examines all the data values you feed to it, even if a
	 * restriction on another axis may make some of them invisible (this
	 * makes interactive use more stable).
	 * <p>
	 * @param min null or number
	 * @param max null or number
	 * @param tickSize number
	 */
	public void setRange(final Double min, final Double max, final Double tickSize) {
		setMin(min);
		setMax(max);
		if (tickSize != null) {
			setTickSize(tickSize.toString());
		}
	}

	/**
	 * @see #setRange(Long, Long, Long)
	 * @param min null or number
	 * @param max null or number
	 */
	public void setRange(final Double min, final Double max) {
		setRange(min, max, null);
	}

	/**
	 * If you don't set the "show" option (i.e. it is null), visibility is
	 * auto-detected, i.e. the axis will show up if there's data associated
	 * with it. You can override this by setting the "show" option to true or
	 * false.
	 * <p>
	 * @param show null or true/false
	 */
	public void setShow(final Boolean show) {
		this.show = show;
	}

	/**
	 * The "position" option specifies where the axis is placed, bottom or
	 * top for x axes, left or right for y axes. The "mode" option determines
	 * how the data is interpreted, the default of null means as decimal
	 * numbers. Use "time" for time series data, see the time series data
	 * section.
	 * <p>
	 * @param position "bottom" or "top" or "left" or "right"
	 */
	public void setPosition(final String position) {
		this.position = position;
	}

	/**
	 * @param mode TODO
	 */
	public void setMode(final String mode) {
		this.mode = mode;
	}

	/**
	 * The "color" option determines the color of the labels and ticks for
	 * the axis (default is the grid color). For more fine-grained control
	 * you can also set the color of the ticks separately with "tickColor"
	 * (otherwise it's autogenerated as the base color with some transparency).
	 * <p>
	 * @param color null or color spec
	 */
	public void setColor(final String color) {
		this.color = color;
	}

	/**
	 * @param tickColor the tickColor to set
	 */
	public void setTickColor(final String tickColor) {
		this.tickColor = tickColor;
	}

	/**
	 * @param font the font to set
	 */
	public void setFont(final String font) {
		this.font = font;
	}

	/**
	 * @see #setRange
	 * @param min null or number
	 */
	public void setMin(final Double min) {
		this.min = min;
	}

	/**
	 * @see #setRange
	 * @param max null or number
	 */
	public void setMax(final Double max) {
		this.max = max;
	}

	/**
	 * The "autoscaleMargin" is a bit esoteric: it's the fraction of margin
	 * that the scaling algorithm will add to avoid that the outermost points
	 * ends up on the grid border. Note that this margin is only applied when
	 * a min or max value is not explicitly set. If a margin is specified,
	 * the plot will furthermore extend the axis end-point to the nearest
	 * whole tick. The default value is "null" for the x axes and 0.02 for y
	 * axes which seems appropriate for most cases.
	 * <p>
	 * @param autoscaleMargin null or number
	 */
	public void setAutoscaleMargin(final Double autoscaleMargin) {
		this.autoscaleMargin = autoscaleMargin;
	}

	/**
	 * "transform" and "inverseTransform" are callbacks you can put in to
	 * change the way the data is drawn. You can design a function to
	 * compress or expand certain parts of the axis non-linearly, e.g.
	 * suppress weekends or compress far away points with a logarithm or some
	 * other means. When Flot draws the plot, each value is first put through
	 * the transform function. Here's an example, the x axis can be turned
	 * into a natural logarithm axis with the following code:
	 * <pre>
	 *   xaxis: {
	 *     transform: function (v) { return Math.log(v); },
	 *     inverseTransform: function (v) { return Math.exp(v); }
	 *   }
	 * </pre>
	 * Similarly, for reversing the y axis so the values appear in inverse
	 * order:
	 * <pre>
	 *   yaxis: {
	 *     transform: function (v) { return -v; },
	 *     inverseTransform: function (v) { return -v; }
	 *   }
	 * </pre>
	 * Note that for finding extrema, Flot assumes that the transform
	 * function does not reorder values (it should be monotone).
	 * <p>
	 * The inverseTransform is simply the inverse of the transform function
	 * (so v == inverseTransform(transform(v)) for all relevant v). It is
	 * required for converting from canvas coordinates to data coordinates,
	 * e.g. for a mouse interaction where a certain pixel is clicked. If you
	 * don't use any interactive features of Flot, you may not need it.
	 *
	 * @param transform null or fn: number -> number
	 */
	public void setTransform(final String transform) {
		this.transform = transform;
	}

	/**
	 * @see #setTransform
	 * @param inverseTransform null or fn: number -> number
	 */
	public void setInverseTransform(final String inverseTransform) {
		this.inverseTransform = inverseTransform;
	}

	/**
	 * If you don't specify any ticks, a tick generator algorithm will make
	 * some for you. The algorithm has two passes. It first estimates how
	 * many ticks would be reasonable and uses this number to compute a nice
	 * round tick interval size. Then it generates the ticks.
	 * <p>
	 * You can specify how many ticks the algorithm aims for by setting
	 * "ticks" to a number. The algorithm always tries to generate reasonably
	 * round tick values so even if you ask for three ticks, you might get
	 * five if that fits better with the rounding. If you don't want any
	 * ticks at all, set "ticks" to 0 or an empty array.
	 * <p>
	 * Another option is to skip the rounding part and directly set the tick
	 * interval size with "{@link #setTickSize(String) tickSize}". If you set
	 * it to 2, you'll get ticks at 2, 4, 6, etc.
	 * <p>
	 * Alternatively, you can specify that you just don't want
	 * ticks at a size less than a specific tick size with
	 * "{@link #setMinTickSize(String) minTickSize}".
	 * <p>
	 * Note that for time series, the format is an array like [2, "month"],
	 * see the next section.
	 * <p>
	 * If you want to completely override the tick algorithm, you can specify
	 * an array for "ticks", either like this:
	 * <pre>  ticks: [0, 1.2, 2.4]</pre>
	 * Or like this where the labels are also customized:
	 * <pre>  ticks: [[0, "zero"], [1.2, "one mark"], [2.4, "two marks"]]</pre>
	 * You can mix the two if you like.
	 * <p>
	 * For extra flexibility you can specify a function as the "ticks"
	 * parameter. The function will be called with an object with the axis
	 * min and max and should return a ticks array. Here's a simplistic tick
	 * generator that spits out intervals of pi, suitable for use on the x
	 * axis for trigonometric functions:
	 * <pre>
	 *   function piTickGenerator(axis) {
	 *     var res = [], i = Math.floor(axis.min / Math.PI);
	 *     do {
	 *       var v = i * Math.PI;
	 *       res.push([v, i + "\u03c0"]);
	 *       ++i;
	 *     } while (v < axis.max);
	 *
	 *     return res;
	 *   }
	 * </pre>
	 * @param ticks the ticks to set
	 */
	public void setTicks(final String ticks) {
		this.ticks = ticks;
	}

	/**
	 * @see #setTicks(String)
	 * @param tickSize the tickSize to set
	 */
	public void setTickSize(final String tickSize) {
		this.tickSize = tickSize;
	}

	/**
	 * @see #setTicks(String)
	 * @param minTickSize the minTickSize to set
	 */
	public void setMinTickSize(final String minTickSize) {
		this.minTickSize = minTickSize;
	}

	/**
	 * Alternatively, for ultimate control over how ticks are formatted you can
	 * provide a function to "tickFormatter". The function is passed two
	 * parameters, the tick value and an axis object with information, and
	 * should return a string. The default formatter looks like this:
	 * <pre>
	 *   function formatter(val, axis) {
	 *     return val.toFixed(axis.tickDecimals);
	 *   }
	 * </pre>
	 * The axis object has "min" and "max" with the range of the axis,
	 * "tickDecimals" with the number of decimals to round the value to and
	 * "tickSize" with the size of the interval between ticks as calculated
	 * by the automatic axis scaling algorithm (or specified by you). Here's
	 * an example of a custom formatter:
	 * <pre>
	 *   function suffixFormatter(val, axis) {
	 *     if (val > 1000000)
	 *       return (val / 1000000).toFixed(axis.tickDecimals) + " MB";
	 *     else if (val > 1000)
	 *       return (val / 1000).toFixed(axis.tickDecimals) + " kB";
	 *     else
	 *       return val.toFixed(axis.tickDecimals) + " B";
	 *   }
	 * </pre>
	 * @param tickFormatter the tickFormatter to set
	 */
	public void setTickFormatter(final String tickFormatter) {
		this.tickFormatter = tickFormatter;
	}

	/**
	 * You can control how the ticks look like with "tickDecimals", the
	 * number of decimals to display (default is auto-detected).
	 * <p>
	 * @param tickDecimals the tickDecimals to set
	 */
	public void setTickDecimals(final Integer tickDecimals) {
		this.tickDecimals = tickDecimals;
	}

	/**
	 * "labelWidth" and "labelHeight" specifies a fixed size of the tick
	 * labels in pixels. They're useful in case you need to align several
	 * plots. "reserveSpace" means that even if an axis isn't shown, Flot
	 * should reserve space for it - it is useful in combination with
	 * labelWidth and labelHeight for aligning multi-axis charts.
	 * <p>
	 * @param labelWidth TODO
	 * @param labelHeight TODO
	 */
	public void setLabelSize(final Integer labelWidth, final Integer labelHeight) {
		setLabelWidth(labelWidth);
		setLabelHeight(labelHeight);
	}

	/**
	 * @see #setLabelSize(Integer, Integer)
	 * @param labelWidth the labelWidth to set
	 */
	public void setLabelWidth(final Integer labelWidth) {
		this.labelWidth = labelWidth;
	}

	/**
	 * @see #setLabelSize(Integer, Integer)
	 * @param labelHeight the labelHeight to set
	 */
	public void setLabelHeight(final Integer labelHeight) {
		this.labelHeight = labelHeight;
	}

	/**
	 * @param reserveSpace the reserveSpace to set
	 */
	public void setReserveSpace(final Integer reserveSpace) {
		this.reserveSpace = reserveSpace;
	}

	/**
	 * "tickLength" is the length of the tick lines in pixels. By default, the
	 * innermost axes will have ticks that extend all across the plot, while
	 * any extra axes use small ticks. A value of null means use the default,
	 * while a number means small ticks of that length - set it to 0 to hide
	 * the lines completely.
	 * <p>
	 * @param tickLength the tickLength to set
	 */
	public void setTickLength(final Integer tickLength) {
		this.tickLength = tickLength;
	}

	/**
	 * If you set "alignTicksWithAxis" to the number of another axis, e.g.
	 * alignTicksWithAxis: 1, Flot will ensure that the autogenerated ticks
	 * of this axis are aligned with the ticks of the other axis. This may
	 * improve the looks, e.g. if you have one y axis to the left and one to
	 * the right, because the grid lines will then match the ticks in both
	 * ends. The trade-off is that the forced ticks won't necessarily be at
	 * natural places.
	 * <p>
	 * @param alignTicksWithAxis the alignTicksWithAxis to set
	 */
	public void setAlignTicksWithAxis(final Integer alignTicksWithAxis) {
		this.alignTicksWithAxis = alignTicksWithAxis;
	}

	/**
	 * @return the show
	 */
	public Boolean getShow() {
		return show;
	}

	/**
	 * @return the position
	 */
	public String getPosition() {
		return position;
	}

	/**
	 * @return the mode
	 */
	public String getMode() {
		return mode;
	}

	/**
	 * @return the timeformat
	 */
	public String getTimeformat() {
		return timeformat;
	}

	/**
	 * @return the color
	 */
	public String getColor() {
		return color;
	}

	/**
	 * @return the tickColor
	 */
	public String getTickColor() {
		return tickColor;
	}

	/**
	 * @return the font
	 */
	public String getFont() {
		return font;
	}

	/**
	 * @return the min
	 */
	public Double getMin() {
		return min;
	}

	/**
	 * @return the max
	 */
	public Double getMax() {
		return max;
	}

	/**
	 * @return the autoscaleMargin
	 */
	public Double getAutoscaleMargin() {
		return autoscaleMargin;
	}

	/**
	 * @return the transform
	 */
	public String getTransform() {
		return transform;
	}

	/**
	 * @return the inverseTransform
	 */
	public String getInverseTransform() {
		return inverseTransform;
	}

	/**
	 * @return the ticks
	 */
	public String getTicks() {
		return ticks;
	}

	/**
	 * @return the tickSize
	 */
	public String getTickSize() {
		return tickSize;
	}

	/**
	 * @return the minTickSize
	 */
	public String getMinTickSize() {
		return minTickSize;
	}

	/**
	 * @return the tickFormatter
	 */
	public String getTickFormatter() {
		return tickFormatter;
	}

	/**
	 * @return the tickDecimals
	 */
	public Integer getTickDecimals() {
		return tickDecimals;
	}

	/**
	 * @return the labelWidth
	 */
	public Integer getLabelWidth() {
		return labelWidth;
	}

	/**
	 * @return the labelHeight
	 */
	public Integer getLabelHeight() {
		return labelHeight;
	}

	/**
	 * @return the reserveSpace
	 */
	public Integer getReserveSpace() {
		return reserveSpace;
	}

	/**
	 * @return the tickLength
	 */
	public Integer getTickLength() {
		return tickLength;
	}

	/**
	 * @return the alignTicksWithAxis
	 */
	public Integer getAlignTicksWithAxis() {
		return alignTicksWithAxis;
	}
}
