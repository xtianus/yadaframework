package net.yadaframework.web.datatables.options;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaFluentBase;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.web.YadaJsonRawStringSerializer;
import net.yadaframework.web.datatables.YadaDataTable;
import net.yadaframework.web.datatables.config.YadaDataTableColumn;
import net.yadaframework.web.datatables.proxy.YadaDTColumnDefProxy;
import net.yadaframework.web.datatables.proxy.YadaDTColumnsProxy;

/**
 * Class representing options for configuring DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/">DataTables Reference</a>
 */
@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaDTOptions extends YadaFluentBase<YadaDataTable> {
    protected String dataTableExtErrMode;
    // There is no ajax option because it does not make sense in Yada Framework.
    // It can still be custonmized in a yada:preprocessor if needed.
    // protected String ajax;
    // protected YadaDTAjax yadaDTAjax;
    protected Boolean autoWidth;
    protected String caption;
    protected List<YadaDTColumnDef> columnDefs;
    protected List<YadaDTColumns> columns;
    // The YadaJsonRawStringSerializer must be used so that the js function is not quoted
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String createdRow; // function
    protected List<Object> data;
    protected Object deferLoading; // Either integer or array of 2 integers
    protected Boolean deferRender;
    protected Boolean destroy;
    protected Boolean typeDetect;
    protected Integer displayStart;
    // "dom" is deprecated
    // protected String dom;
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String drawCallback; // function
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String footerCallback; // function
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String formatNumber; // function
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String headerCallback; // function
    protected Boolean info;
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String infoCallback; // function
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String initComplete; // function
    // Not used because urls must be processed by thymeleaf
    // protected YadaDTLanguage language;
    // This should cope with all possible values but it's not implemented yet
    protected Object layout; // Fluent api not implemented yet.
    protected Boolean lengthChange;
    protected int[] lengthMenu;
    protected List<YadaDTOrder> order;
    protected Boolean orderClasses;
    protected Boolean orderDescReverse;
    protected Object orderFixed; // Fluent api not implemented yet.
    protected Boolean orderMulti;
    protected Boolean ordering;
    protected Integer pageLength;
    protected Boolean paging;
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String preDrawCallback;
    protected Boolean processing;
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String renderer;
    // can be either a boolean or an object
    protected Boolean responsive;
    protected YadaDTResponsive yadaDTResponsive;
    //
    protected Boolean retrieve;
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String rowCallback;
    protected YadaDTRowGroup rowGroup;
    protected String rowId;
    protected YadaDTRowReorder rowReorder;
    protected Boolean scrollCollapse;
    protected Boolean scrollX;
    protected Boolean scrollY;
    protected YadaDTScroller scroller;
    protected YadaDTSearch search;
    protected YadaDTSearchBuilder searchBuilder;
    protected List<YadaDTSearchCol> searchCols;
    protected Integer searchDelay;
    protected YadaDTSearchPanes searchPanes;
    protected Boolean searching;
    protected YadaDTSelect select;
    protected Boolean serverSide = true; // default to server-side processing
    protected Integer stateDuration;
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String stateLoadCallback;
    protected String stateLoadParams;
    protected String stateLoaded;
    protected Boolean stateSave;
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String stateSaveCallback;
    protected String stateSaveParams;
    protected Integer tabIndex;
    
    public YadaDTOptions(YadaDataTable parent) {
    	super(parent);
    }
    
    //
    // Getters are removed to simplify fluent interface during autocompletion
    //

    //
    // Fluent interface for simple attributes
	//
    
//    /**
//     * The URL where data is fetched from (without language in the path). Can contain thymeleaf expressions.
//     * 
//     * There is no other ajax options available because this is the only one that makes sense in Yada Framework.
//     * This can still be custonmized in a yada:preprocessor
//     * 
//     * @param ajaxUrl the url that returns table data
//     * @return this instance for method chaining
//     * @see <a href="https://datatables.net/reference/option/ajax">ajax</a>
//     */
//    public YadaDTOptions dtAjax(String ajaxUrl) {
//        this.ajax = YadaWebUtil.INSTANCE.ensureThymeleafUrl(ajaxUrl);
//        return this;
//    }
    
	/**
	 * Sets the `data` option.
	 * This is normally not used as data is fetched from the server in Yada Framework.
	 * 
	 * @param data the data to display in the table. Each object of the list must be serializable by Jackson
	 * @return this instance for method chaining
	 * @see <a href="https://datatables.net/reference/option/data">data</a>
	 */
    public YadaDTOptions dtData(List<Object> data) {
	    this.data = data;
	    return this;
    }
    
    /**
     * Sets the `dataTableExtErrMode` option.
     * 
     * @param dataTableExtErrMode the error mode for the DataTable
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/">dataTableExtErrMode</a>
     */
    public YadaDTOptions dtDataTableExtErrMode(String dataTableExtErrMode) {
    	this.dataTableExtErrMode = dataTableExtErrMode;
    	return this;
    }

    /**
     * Disable automatic column width calculation as an optimisation.
     * 
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/autoWidth">autoWidth</a>
     */
    public YadaDTOptions dtAutoWidthOff() {
        this.autoWidth = false;
        return this;
    }

    /**
     * Sets the `caption` option.
     * 
     * @param caption the table caption
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/caption">caption</a>
     */
    public YadaDTOptions dtCaption(String caption) {
        this.caption = caption;
        return this;
    }

    /**
     * Sets the `createdRow` option.
     * 
     * @param createdRow a callback function to manipulate the row after it has been created
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/createdRow">createdRow</a>
     */
    public YadaDTOptions dtCreatedRow(String createdRow) {
        this.createdRow = createdRow;
        return this;
    }

    /**
     * Enables deferred loading, and instructs DataTables as to how many items are in the full data set
     * 
     * @param totItems items in the full data set
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/deferLoading">deferLoading</a>
     * @see dtDeferLoading(int, int)
     */
    public YadaDTOptions dtDeferLoading(int totItems) {
        this.deferLoading = totItems;
        return this;
    }
    
    /**
     * Enables deferred loading, where the first data index tells DataTables how many rows are in 
     * the filtered result set, and the second how many in the full data set without filtering applied.
     * 
     * @param filteredItems items in the filtered data set
     * @param totItems items in the full data set
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/deferLoading">deferLoading</a>
     * @see dtDeferLoading(int)
     */
    public YadaDTOptions dtDeferLoading(int filteredItems, int totItems) {
    	this.deferLoading = totItems;
    	return this;
    }

    /**
     * Do not wait to create HTML elements when they are needed	for display.
     * The only reason to use this option is if you must have all DOM elements available, 
     * even those currently not in the document.
     * 
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/deferRender">deferRender</a>
     */
    public YadaDTOptions dtDeferRenderOff() {
        this.deferRender = false;
        return this;
    }

    /**
     * Initialise a new DataTable as usual, but if there is an existing DataTable 
     * which matches the selector, it will be destroyed and replaced with the new table.
     * 
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/destroy">destroy</a>
     */
    public YadaDTOptions dtDestroy() {
        this.destroy = true;
        return this;
    }

    /**
     * Disable the auto type detection that DataTables performs.
     * This might be useful if you are using server-side processing where data can change between 
     * pages and the client-side is unable to automatically reliably determine a column's data type.
     * 
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/typeDetection">typeDetect</a>
     */
    public YadaDTOptions dtTypeDetectOff() {
        this.typeDetect = false;
        return this;
    }

    /**
     * Sets the `displayStart` option.
     * 
     * @param displayStart the initial page to be displayed
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/displayStart">displayStart</a>
     */
    public YadaDTOptions dtDisplayStart(Integer displayStart) {
        this.displayStart = displayStart;
        return this;
    }

    /**
     * Sets the `drawCallback` option.
     * 
     * @param drawCallback a callback function to execute on each draw event
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/drawCallback">drawCallback</a>
     */
    public YadaDTOptions dtDrawCallback(String drawCallback) {
        this.drawCallback = drawCallback;
        return this;
    }

    /**
     * Sets the `footerCallback` option.
     * 
     * @param footerCallback a callback function to manipulate the table footer
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/footerCallback">footerCallback</a>
     */
    public YadaDTOptions dtFooterCallback(String footerCallback) {
        this.footerCallback = footerCallback;
        return this;
    }

    /**
     * Sets the `formatNumber` option.
     * 
     * @param formatNumber define the number format to use for DataTables
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/formatNumber">formatNumber</a>
     */
    public YadaDTOptions dtFormatNumber(String formatNumber) {
        this.formatNumber = formatNumber;
        return this;
    }

    /**
     * Sets the `headerCallback` option.
     * 
     * @param headerCallback a callback function to manipulate the table header
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/headerCallback">headerCallback</a>
     */
    public YadaDTOptions dtHeaderCallback(String headerCallback) {
        this.headerCallback = headerCallback;
        return this;
    }

    /**
     * Disables showing details about the table including information about 
     * filtered data if that action is being performed.
     * 
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/info">info</a>
     */
    public YadaDTOptions dtInfoOff() {
        this.info = false;
        return this;
    }

    /**
     * Sets the `infoCallback` option.
     * 
     * @param infoCallback a callback function to customize the table information display
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/infoCallback">infoCallback</a>
     */
    public YadaDTOptions dtInfoCallback(String infoCallback) {
        this.infoCallback = infoCallback;
        return this;
    }

    /**
     * Sets the `initComplete` option.
     * 
     * @param initComplete a callback function executed when the table initialization is complete
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/initComplete">initComplete</a>
     */
    public YadaDTOptions dtInitComplete(String initComplete) {
        this.initComplete = initComplete;
        return this;
    }

    /**
     * Sets the `layout` option.
     * Fluent api not implemented yet.
     * 
     * @param layout the layout for the table
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/layout">layout</a>
     */
    public YadaDTOptions dtLayout(String layout) {
        this.layout = layout;
        return this;
    }

    /**
     * Disables user's ability to change the paging display length of the table.
     * 
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/lengthChange">lengthChange</a>
     */
    public YadaDTOptions dtLengthChangeOff() {
        this.lengthChange = false;
        return this;
    }
    
	/**
	 * Sets the `lengthMenu` option.
	 * 
	 * @param lengthChoice the list of page length options to show the user, like 10, 25, 50, 100
	 * @return this instance for method chaining
	 * @see <a href="https://datatables.net/reference/option/lengthMenu">lengthMenu</a>
	 */
	public YadaDTOptions dtLengthMenu(int ... lengthChoice) {
		this.lengthMenu = lengthChoice;
		return this;
	}

    /**
     * Disable the addition of ordering classes to the columns
     * when performance is an issue.
     * 
     * @param orderClasses enable or disable the addition of ordering classes to the columns
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/orderClasses">orderClasses</a>
     */
    public YadaDTOptions dtOrderClassesOff() {
        this.orderClasses = false;
        return this;
    }

    /**
     * Disable the default reverse.
     * 
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/orderDescReverse">orderDescReverse</a>
     */
    public YadaDTOptions dtOrderDescReverseOff() {
        this.orderDescReverse = false;
        return this;
    }
    
	/**
	 * Sets the `orderFixed` option. Fluent api not implemented yet.
	 * 
	 * @param orderFixed define fixed ordering of columns
	 * @return this instance for method chaining
	 * @see <a href="https://datatables.net/reference/option/orderFixed">orderFixed</a>
	 */
	public YadaDTOptions dtOrderFixed(Object orderFixed) {
		this.orderFixed = orderFixed;
		return this;
	}

    /**
     * Disables multiple column ordering ability
     * 
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/orderMulti">orderMulti</a>
     */
    public YadaDTOptions dtOrderMultiOff() {
        this.orderMulti = false;
        return this;
    }

    /**
     * Disables ordering (sorting) abilities
     * 
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/ordering">ordering</a>
     */
    public YadaDTOptions dtOrderingOff() {
        this.ordering = false;
        return this;
    }
    
    /**
     * Sets the `order` option. Can be called multiple times to set multiple orders.
     * There should be no need to use this directly because this functionality is implemented by 
     * the dtColumnObj() fluent interface.
     * @param idx column index
     * @param dir "asc" or "desc"
     * @return this instance for method chaining
     * @see YadaDataTableColumn#dtOrderAsc() 
     * @see <a href="https://datatables.net/reference/type/DataTables.Order">order</a>
     */
    public YadaDTOptions dtOrder(int idx, String dir) {
        if (this.order == null) {
            this.order = new ArrayList<>();
        }
        YadaDTOrder orderObj = new YadaDTOrder(idx, dir);
        this.order.add(orderObj);
        return this;
	}

    /**
     * Sets the `pageLength` option that tells how many rows should be visible.
     * 
     * @param pageLength the number of rows per page
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/pageLength">pageLength</a>
     */
    public YadaDTOptions dtPageLength(Integer pageLength) {
        if (pageLength != null && Boolean.FALSE.equals(this.paging)) {
        	throw new YadaInvalidUsageException("pageLength must be null when paging is false");
        }
        this.pageLength = pageLength;
        return this;
    }

    /**
     * Disable table pagination.
     * 
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/paging">paging</a>
     */
    public YadaDTOptions dtPagingOff() {
        if (this.pageLength != null) {
	        throw new YadaInvalidUsageException("paging can't be disabled when pageLength is not null");
        }
        this.paging = false;
        return this;
    }

    /**
     * Sets the `preDrawCallback` option.
     * 
     * @param preDrawCallback a callback function before the table is redrawn
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/preDrawCallback">preDrawCallback</a>
     */
    public YadaDTOptions dtPreDrawCallback(String preDrawCallback) {
        this.preDrawCallback = preDrawCallback;
        return this;
    }

    /**
     * Enable the display of a 'processing' indicator when the table is being processed (e.g. a sort) 
     * for server-side processing. This is particularly useful for tables with large amounts of data where it 
     * can take a noticeable amount of time to sort the entries.
     * 
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/processing">processing</a>
     */
    public YadaDTOptions dtProcessingOn() {
        this.processing = true;
        return this;
    }

    /**
     * Sets the `renderer` option.
     * 
     * @param renderer define the renderer for table elements
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/renderer">renderer</a>
     */
    public YadaDTOptions dtRenderer(String renderer) {
        this.renderer = renderer;
        return this;
    }

    /**
     * Sets the `retrieve` option.
     * 
     * @param retrieve enable or disable DataTables instance retrieval
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/retrieve">retrieve</a>
     */
    public YadaDTOptions dtRetrieve(Boolean retrieve) {
        this.retrieve = retrieve;
        return this;
    }

    /**
     * Sets the `rowCallback` option.
     * 
     * @param rowCallback a callback function to manipulate a row
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/rowCallback">rowCallback</a>
     */
    public YadaDTOptions dtRowCallback(String rowCallback) {
        this.rowCallback = rowCallback;
        return this;
    }

    /**
     * Sets the `rowId` option.
     * 
     * @param rowId define the row ID field
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/rowId">rowId</a>
     */
    public YadaDTOptions dtRowId(String rowId) {
        this.rowId = rowId;
        return this;
    }

    /**
     * Sets the `scrollCollapse` option.
     * 
     * @param scrollCollapse enable or disable scrolling collapse behavior
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/scrollCollapse">scrollCollapse</a>
     */
    public YadaDTOptions dtScrollCollapse(Boolean scrollCollapse) {
        this.scrollCollapse = scrollCollapse;
        return this;
    }

    /**
     * Sets the `scrollX` option.
     * 
     * @param scrollX enable or disable horizontal scrolling
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/scrollX">scrollX</a>
     */
    public YadaDTOptions dtScrollX(Boolean scrollX) {
        this.scrollX = scrollX;
        return this;
    }

    /**
     * Sets the `scrollY` option.
     * 
     * @param scrollY define the table's vertical scroll height
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/scrollY">scrollY</a>
     */
    public YadaDTOptions dtScrollY(Boolean scrollY) {
        this.scrollY = scrollY;
        return this;
    }

    /**
     * Sets the `searching` option.
     * 
     * @param searching enable or disable DataTables' search feature
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searching">searching</a>
     */
    public YadaDTOptions dtSearching(Boolean searching) {
        this.searching = searching;
        return this;
    }

    /**
     * Sets the `serverSide` option.
     * 
     * @param serverSide enable or disable server-side processing mode
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/serverSide">serverSide</a>
     */
    public YadaDTOptions dtServerSide(Boolean serverSide) {
        this.serverSide = serverSide;
        return this;
    }

    /**
     * Sets the `stateDuration` option.
     * 
     * @param stateDuration the duration for which the saved state is retained
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/stateDuration">stateDuration</a>
     */
    public YadaDTOptions dtStateDuration(Integer stateDuration) {
        this.stateDuration = stateDuration;
        return this;
    }

    /**
     * Sets the `stateLoadCallback` option.
     * 
     * @param stateLoadCallback a callback function to load the state of the table
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/stateLoadCallback">stateLoadCallback</a>
     */
    public YadaDTOptions dtStateLoadCallback(String stateLoadCallback) {
        this.stateLoadCallback = stateLoadCallback;
        return this;
    }

    /**
     * Sets the `stateLoadParams` option.
     * 
     * @param stateLoadParams a callback function to modify the loaded state
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/stateLoadParams">stateLoadParams</a>
     */
    public YadaDTOptions dtStateLoadParams(String stateLoadParams) {
        this.stateLoadParams = stateLoadParams;
        return this;
    }

    /**
     * Sets the `stateLoaded` option.
     * 
     * @param stateLoaded a callback function when the state is loaded
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/stateLoaded">stateLoaded</a>
     */
    public YadaDTOptions dtStateLoaded(String stateLoaded) {
        this.stateLoaded = stateLoaded;
        return this;
    }

    /**
     * Sets the `stateSave` option.
     * 
     * @param stateSave enable or disable table state saving
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/stateSave">stateSave</a>
     */
    public YadaDTOptions dtStateSave(Boolean stateSave) {
        this.stateSave = stateSave;
        return this;
    }

    /**
     * Sets the `stateSaveCallback` option.
     * 
     * @param stateSaveCallback a callback function to save the state of the table
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/stateSaveCallback">stateSaveCallback</a>
     */
    public YadaDTOptions dtStateSaveCallback(String stateSaveCallback) {
        this.stateSaveCallback = stateSaveCallback;
        return this;
    }

    /**
     * Sets the `stateSaveParams` option.
     * 
     * @param stateSaveParams a callback function to modify the saved state
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/stateSaveParams">stateSaveParams</a>
     */
    public YadaDTOptions dtStateSaveParams(String stateSaveParams) {
        this.stateSaveParams = stateSaveParams;
        return this;
    }

    /**
     * Sets the `tabIndex` option.
     * 
     * @param tabIndex define the tab index of the table
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/tabIndex">tabIndex</a>
     */
    public YadaDTOptions dtTabIndex(Integer tabIndex) {
        this.tabIndex = tabIndex;
        return this;
    }    

    //
    // Fluent interface for nested objects
	//

    /**
     * @return The rowGroup configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/rowGroup">DataTables rowGroup option</a>
     */
    public YadaDTRowGroup dtRowGroupObj() {
    	this.rowGroup = YadaUtil.lazyUnsafeInit(this.rowGroup, () -> new YadaDTRowGroup(this));
        return this.rowGroup;
    }

    /**
     * @return The rowReorder configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/rowReorder">DataTables rowReorder option</a>
     */
    public YadaDTRowReorder dtRowReorderObj() {
    	this.rowReorder = YadaUtil.lazyUnsafeInit(this.rowReorder, () -> new YadaDTRowReorder(this));
        return this.rowReorder;
    }

    /**
     * @return The scroller configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/scroller">DataTables scroller option</a>
     */
    public YadaDTScroller dtScrollerObj() {
    	this.scroller = YadaUtil.lazyUnsafeInit(this.scroller, () -> new YadaDTScroller(this));
        return this.scroller;
    }

    /**
     * @return The search configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/search">DataTables search option</a>
     */
    public YadaDTSearch dtSearchObj() {
    	this.search = YadaUtil.lazyUnsafeInit(this.search, () -> new YadaDTSearch(this));
        return this.search;
    }

    /**
     * @return The searchBuilder configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/searchBuilder">DataTables searchBuilder option</a>
     */
    public YadaDTSearchBuilder dtSearchBuilderObj() {
    	this.searchBuilder = YadaUtil.lazyUnsafeInit(this.searchBuilder, () -> new YadaDTSearchBuilder(this));
        return this.searchBuilder;
    }

    /**
     * @return The searchPanes configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/searchPanes">DataTables searchPanes option</a>
     */
    public YadaDTSearchPanes dtSearchPanesObj() {
    	this.searchPanes = YadaUtil.lazyUnsafeInit(this.searchPanes, () -> new YadaDTSearchPanes(this));
        return this.searchPanes;
    }

    /**
     * @return The select configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/select">DataTables select option</a>
     */
    public YadaDTSelect dtSelectObj() {
    	this.select = YadaUtil.lazyUnsafeInit(this.select, () -> new YadaDTSelect(this));
        return this.select;
    }

    /**
     * @return A new column definition for DataTables.
     * @see <a href="https://datatables.net/reference/option/columnDefs">DataTables columnDefs option</a>
     */
    public YadaDTColumnDef dtColumnDefsObj() {
    	this.columnDefs = YadaUtil.lazyUnsafeInit(this.columnDefs);
        YadaDTColumnDef newColumnDef = new YadaDTColumnDefProxy(this);
        this.columnDefs.add(newColumnDef);
        return newColumnDef;
    }

    /**
     * @return A new column configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/columns">DataTables columns option</a>
     */
    public YadaDTColumns dtColumnsObj() {
    	this.columns = YadaUtil.lazyUnsafeInit(this.columns);
        YadaDTColumns newColumn = new YadaDTColumnsProxy(this);
        this.columns.add(newColumn);
        return newColumn;
    }

    /**
     * @return A new search column configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/searchCols">DataTables searchCols option</a>
     */
    public YadaDTSearchCol dtSearchColsObj() {
    	this.searchCols = YadaUtil.lazyUnsafeInit(this.searchCols);
        YadaDTSearchCol newSearchCol = new YadaDTSearchCol(this);
        this.searchCols.add(newSearchCol);
        return newSearchCol;
    }
    
    /**
     * Sets the responsive option so that the table will adapt for different screen sizes.
     * 
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/responsive">DataTables responsive Reference</a>
     */
    public YadaDTOptions dtResponsiveOn() {
        this.responsive = true;
        return this;
    }

    /**
     * Turns on responsive behaviour and provides access to the responsive configuration options for DataTables.
     * 
     * @return the instance of YadaDTResponsive for further configuration
     * @see <a href="https://datatables.net/reference/option/responsive">DataTables responsive Reference</a>
     */
    public YadaDTResponsive dtResponsiveObj() {
    	dtResponsiveOn();
    	this.yadaDTResponsive = YadaUtil.lazyUnsafeInit(this.yadaDTResponsive, () -> new YadaDTResponsive(this));
        return this.yadaDTResponsive;
    }

}
