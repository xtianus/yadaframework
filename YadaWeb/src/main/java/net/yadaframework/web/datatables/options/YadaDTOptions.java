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
    protected YadaDTAutoFill autoFill;
    protected Boolean autoWidth;
    protected String buttonSearchBuilder;
    protected String buttonSearchPanes;
    protected YadaDTButtons buttons;
    protected String caption;
    protected YadaDTColReorder colReorder;
    protected List<YadaDTColumnDef> columnDefs;
    protected List<YadaDTColumns> columns;
    protected String createdRow;
    protected Object data;
    protected Boolean deferLoading;
    protected Boolean deferRender;
    protected Boolean destroy;
    protected String detectType;
    protected Integer displayStart;
    protected String dom;
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String drawCallback;
    protected YadaDTFixedColumns fixedColumns;
    protected YadaDTFixedHeader fixedHeader;
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String footerCallback;
    protected String formatNumber;
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String headerCallback;
    protected Boolean info;
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String infoCallback;
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String initComplete;
    protected YadaDTKeys keys;
    // Not used because urls must be processed by thymeleaf
    // protected YadaDTLanguage language;
    protected String layout;
    protected Boolean lengthChange;
    protected List<Object> lengthMenu;
    protected List<YadaDTOrder> order;
    protected Boolean orderCellsTop;
    protected Boolean orderClasses;
    protected Boolean orderDescReverse;
    protected Object orderFixed;
    protected Boolean orderMulti;
    protected Boolean ordering;
    protected Integer pageLength;
    protected Boolean paging;
    protected String pagingType;
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
     * Sets the `autoWidth` option.
     * 
     * @param autoWidth enable or disable automatic column width calculation
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/autoWidth">autoWidth</a>
     */
    public YadaDTOptions dtAutoWidth(Boolean autoWidth) {
        this.autoWidth = autoWidth;
        return this;
    }

    /**
     * Sets the `buttonSearchBuilder` option.
     * 
     * @param buttonSearchBuilder the text for the SearchBuilder button
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchBuilder.button">buttonSearchBuilder</a>
     */
    public YadaDTOptions dtButtonSearchBuilder(String buttonSearchBuilder) {
        this.buttonSearchBuilder = buttonSearchBuilder;
        return this;
    }

    /**
     * Sets the `buttonSearchPanes` option.
     * 
     * @param buttonSearchPanes the text for the SearchPanes button
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.button">buttonSearchPanes</a>
     */
    public YadaDTOptions dtButtonSearchPanes(String buttonSearchPanes) {
        this.buttonSearchPanes = buttonSearchPanes;
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
     * Sets the `deferLoading` option.
     * 
     * @param deferLoading enable or disable deferred loading of data
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/deferLoading">deferLoading</a>
     */
    public YadaDTOptions dtDeferLoading(Boolean deferLoading) {
        this.deferLoading = deferLoading;
        return this;
    }

    /**
     * Sets the `deferRender` option.
     * 
     * @param deferRender enable or disable deferred rendering of rows
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/deferRender">deferRender</a>
     */
    public YadaDTOptions dtDeferRender(Boolean deferRender) {
        this.deferRender = deferRender;
        return this;
    }

    /**
     * Sets the `destroy` option.
     * 
     * @param destroy enable or disable DataTables destruction before reinitialization
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/destroy">destroy</a>
     */
    public YadaDTOptions dtDestroy(Boolean destroy) {
        this.destroy = destroy;
        return this;
    }

    /**
     * Sets the `detectType` option.
     * 
     * @param detectType the detection type for DataTable columns
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/typeDetection">detectType</a>
     */
    public YadaDTOptions dtDetectType(String detectType) {
        this.detectType = detectType;
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
     * Sets the `dom` option.
     * 
     * @param dom define the table control elements to display
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/dom">dom</a>
     */
    public YadaDTOptions dtDom(String dom) {
        this.dom = dom;
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
     * Sets the `info` option.
     * 
     * @param info enable or disable the table information display
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/info">info</a>
     */
    public YadaDTOptions dtInfo(Boolean info) {
        this.info = info;
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
     * Sets the `lengthChange` option.
     * 
     * @param lengthChange enable or disable the ability for the end user to change the page length
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/lengthChange">lengthChange</a>
     */
    public YadaDTOptions dtLengthChange(Boolean lengthChange) {
        this.lengthChange = lengthChange;
        return this;
    }

    /**
     * Sets the `orderCellsTop` option.
     * 
     * @param orderCellsTop control if ordering cells appear at the top of the table
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/orderCellsTop">orderCellsTop</a>
     */
    public YadaDTOptions dtOrderCellsTop(Boolean orderCellsTop) {
        this.orderCellsTop = orderCellsTop;
        return this;
    }

    /**
     * Sets the `orderClasses` option.
     * 
     * @param orderClasses enable or disable the addition of ordering classes to the columns
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/orderClasses">orderClasses</a>
     */
    public YadaDTOptions dtOrderClasses(Boolean orderClasses) {
        this.orderClasses = orderClasses;
        return this;
    }

    /**
     * Sets the `orderDescReverse` option.
     * 
     * @param orderDescReverse enable or disable the reverse of default descending order
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/orderDescReverse">orderDescReverse</a>
     */
    public YadaDTOptions dtOrderDescReverse(Boolean orderDescReverse) {
        this.orderDescReverse = orderDescReverse;
        return this;
    }

    /**
     * Sets the `orderMulti` option.
     * 
     * @param orderMulti enable or disable multiple column ordering
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/orderMulti">orderMulti</a>
     */
    public YadaDTOptions dtOrderMulti(Boolean orderMulti) {
        this.orderMulti = orderMulti;
        return this;
    }

    /**
     * Sets the `ordering` option.
     * 
     * @param ordering false to disable the initial sorting of the table
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/ordering">ordering</a>
     */
    public YadaDTOptions dtOrdering(boolean ordering) {
        this.ordering = ordering;
        return this;
    }
    
    /**
     * Sets the `order` option. Can be called multiple times to set multiple orders.
     * @param idx column index
     * @param dir "asc" or "desc"
     * @return this instance for method chaining
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
        this.pageLength = pageLength;
        if (pageLength != null && Boolean.FALSE.equals(this.paging)) {
        	throw new YadaInvalidUsageException("pageLength must be null when paging is false");
        }
        dtPaging(Boolean.TRUE); // Force paging, just to be safe
        return this;
    }

    /**
     * Sets the `paging` option.
     * 
     * @param paging enable or disable pagination
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/paging">paging</a>
     */
    public YadaDTOptions dtPaging(Boolean paging) {
        this.paging = paging;
        if (Boolean.FALSE.equals(paging) && this.pageLength != null) {
	        throw new YadaInvalidUsageException("paging must be true when pageLength is not null");
        }
        return this;
    }

    /**
     * Sets the `pagingType` option.
     * 
     * @param pagingType define the pagination button style
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/pagingType">pagingType</a>
     */
    public YadaDTOptions dtPagingType(String pagingType) {
        this.pagingType = pagingType;
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
     * Sets the `processing` option.
     * 
     * @param processing enable or disable the display of a 'processing' indicator
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/processing">processing</a>
     */
    public YadaDTOptions dtProcessing(Boolean processing) {
        this.processing = processing;
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
     * @return The autoFill configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/autoFill">DataTables autoFill option</a>
     */
    public YadaDTAutoFill dtAutoFillObj() {
        if (this.autoFill == null) {
            this.autoFill = new YadaDTAutoFill(this);
        }
        return this.autoFill;
    }

    /**
     * @return The buttons configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/buttons">DataTables buttons option</a>
     */
    public YadaDTButtons dtButtonsObj() {
        if (this.buttons == null) {
            this.buttons = new YadaDTButtons(this);
        }
        return this.buttons;
    }

    /**
     * @return The colReorder configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/colReorder">DataTables colReorder option</a>
     */
    public YadaDTColReorder dtColReorderObj() {
        if (this.colReorder == null) {
            this.colReorder = new YadaDTColReorder(this);
        }
        return this.colReorder;
    }

    /**
     * @return The fixedColumns configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/fixedColumns">DataTables fixedColumns option</a>
     */
    public YadaDTFixedColumns dtFixedColumnsObj() {
        if (this.fixedColumns == null) {
            this.fixedColumns = new YadaDTFixedColumns(this);
        }
        return this.fixedColumns;
    }

    /**
     * @return The fixedHeader configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/fixedHeader">DataTables fixedHeader option</a>
     */
    public YadaDTFixedHeader dtFixedHeaderObj() {
        if (this.fixedHeader == null) {
            this.fixedHeader = new YadaDTFixedHeader(this);
        }
        return this.fixedHeader;
    }

    /**
     * @return The keys configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/keys">DataTables keys option</a>
     */
    public YadaDTKeys dtKeysObj() {
        if (this.keys == null) {
            this.keys = new YadaDTKeys(this);
        }
        return this.keys;
    }

//    /**
//     * @return The language configuration for DataTables.
//     * @see <a href="https://datatables.net/reference/option/language">DataTables language option</a>
//     */
//    public YadaDTLanguage dtLanguageObj() {
//        if (this.language == null) {
//            this.language = new YadaDTLanguage(this);
//        }
//        return this.language;
//    }

    /**
     * @return The rowGroup configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/rowGroup">DataTables rowGroup option</a>
     */
    public YadaDTRowGroup dtRowGroupObj() {
        if (this.rowGroup == null) {
            this.rowGroup = new YadaDTRowGroup(this);
        }
        return this.rowGroup;
    }

    /**
     * @return The rowReorder configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/rowReorder">DataTables rowReorder option</a>
     */
    public YadaDTRowReorder dtRowReorderObj() {
        if (this.rowReorder == null) {
            this.rowReorder = new YadaDTRowReorder(this);
        }
        return this.rowReorder;
    }

    /**
     * @return The scroller configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/scroller">DataTables scroller option</a>
     */
    public YadaDTScroller dtScrollerObj() {
        if (this.scroller == null) {
            this.scroller = new YadaDTScroller(this);
        }
        return this.scroller;
    }

    /**
     * @return The search configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/search">DataTables search option</a>
     */
    public YadaDTSearch dtSearchObj() {
        if (this.search == null) {
            this.search = new YadaDTSearch(this);
        }
        return this.search;
    }

    /**
     * @return The searchBuilder configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/searchBuilder">DataTables searchBuilder option</a>
     */
    public YadaDTSearchBuilder dtSearchBuilderObj() {
        if (this.searchBuilder == null) {
            this.searchBuilder = new YadaDTSearchBuilder(this);
        }
        return this.searchBuilder;
    }

    /**
     * @return The searchPanes configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/searchPanes">DataTables searchPanes option</a>
     */
    public YadaDTSearchPanes dtSearchPanesObj() {
        if (this.searchPanes == null) {
            this.searchPanes = new YadaDTSearchPanes(this);
        }
        return this.searchPanes;
    }

    /**
     * @return The select configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/select">DataTables select option</a>
     */
    public YadaDTSelect dtSelectObj() {
        if (this.select == null) {
            this.select = new YadaDTSelect(this);
        }
        return this.select;
    }

    /**
     * @return A new column definition for DataTables.
     * @see <a href="https://datatables.net/reference/option/columnDefs">DataTables columnDefs option</a>
     */
    public YadaDTColumnDef dtColumnDefsObj() {
        if (this.columnDefs == null) {
            this.columnDefs = new ArrayList<>();
        }
        YadaDTColumnDef newColumnDef = new YadaDTColumnDefProxy(this);
        this.columnDefs.add(newColumnDef);
        return newColumnDef;
    }

    /**
     * @return A new column configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/columns">DataTables columns option</a>
     */
    public YadaDTColumns dtColumnsObj() {
        if (this.columns == null) {
            this.columns = new ArrayList<>();
        }
        YadaDTColumns newColumn = new YadaDTColumnsProxy(this);
        this.columns.add(newColumn);
        return newColumn;
    }

    /**
     * @return A new search column configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/searchCols">DataTables searchCols option</a>
     */
    public YadaDTSearchCol dtSearchColsObj() {
        if (this.searchCols == null) {
            this.searchCols = new ArrayList<>();
        }
        YadaDTSearchCol newSearchCol = new YadaDTSearchCol(this);
        this.searchCols.add(newSearchCol);
        return newSearchCol;
    }
    
    /**
     * Sets the responsive option as a boolean, determining if the table should adapt for different screen sizes.
     * 
     * @param responsive Boolean flag to enable or disable responsive behavior
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/responsive">DataTables responsive Reference</a>
     */
    public YadaDTOptions dtResponsive(Boolean responsive) {
        this.responsive = responsive;
        return this;
    }

    /**
     * Provides access to the responsive configuration options for DataTables.
     * 
     * @return the instance of YadaDTResponsive for further configuration
     * @see <a href="https://datatables.net/reference/option/responsive">DataTables responsive Reference</a>
     */
    public YadaDTResponsive dtResponsiveObj() {
    	this.yadaDTResponsive = YadaUtil.lazyUnsafeInit(this.yadaDTResponsive, () -> new YadaDTResponsive(this));
        return this.yadaDTResponsive;
    }

}
