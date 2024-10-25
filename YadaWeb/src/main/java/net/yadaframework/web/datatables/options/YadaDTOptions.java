package net.yadaframework.web.datatables.options;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaFluentBase;
import net.yadaframework.web.datatables.YadaDataTable;

/**
 * Class representing options for configuring DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/">DataTables Reference</a>
 */
@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class YadaDTOptions extends YadaFluentBase<YadaDataTable> {
    protected String dataTableExtErrMode;
    protected YadaDTAjax ajax;
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
    protected String drawCallback;
    protected YadaDTFixedColumns fixedColumns;
    protected YadaDTFixedHeader fixedHeader;
    protected String footerCallback;
    protected String formatNumber;
    protected String headerCallback;
    protected Boolean info;
    protected String infoCallback;
    protected String initComplete;
    protected YadaDTKeys keys;
    protected YadaDTLanguage language;
    protected String layout;
    protected Boolean lengthChange;
    protected List<Object> lengthMenu;
    protected List<Object> order;
    protected Boolean orderCellsTop;
    protected Boolean orderClasses;
    protected Boolean orderDescReverse;
    protected Object orderFixed;
    protected Boolean orderMulti;
    protected String ordering;
    protected Integer pageLength;
    protected Boolean paging;
    protected String pagingType;
    protected String preDrawCallback;
    protected Boolean processing;
    protected String renderer;
    // can be either a boolean or an object
    protected Boolean responsive;
    protected YadaDTResponsive yadaDTResponsive;
    //
    protected Boolean retrieve;
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
    protected Boolean serverSide;
    protected Integer stateDuration;
    protected String stateLoadCallback;
    protected String stateLoadParams;
    protected String stateLoaded;
    protected Boolean stateSave;
    protected String stateSaveCallback;
    protected String stateSaveParams;
    protected Integer tabIndex;

    public YadaDTOptions(YadaDataTable parent) {
 		super(parent);
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
     * @param ordering define the initial sorting of the table
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/ordering">ordering</a>
     */
    public YadaDTOptions dtOrdering(String ordering) {
        this.ordering = ordering;
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
    
// Getters removed to simplify fluent interface during autocompletion
    
//    /**
//     * @return The DataTables error mode.
//     * @see <a href="https://datatables.net/reference/option/">DataTables Reference</a>
//     */
//    public String getDataTableExtErrMode() {
//        return dataTableExtErrMode;
//    }
//
//    /**
//     * @return Whether the table should automatically adjust column widths.
//     * @see <a href="https://datatables.net/reference/option/autoWidth">DataTables autoWidth option</a>
//     */
//    public Boolean getAutoWidth() {
//        return autoWidth;
//    }
//
//    /**
//     * @return The configuration for SearchBuilder button.
//     * @see <a href="https://datatables.net/reference/option/buttons">DataTables buttons option</a>
//     */
//    public String getButtonSearchBuilder() {
//        return buttonSearchBuilder;
//    }
//
//    /**
//     * @return The configuration for SearchPanes button.
//     * @see <a href="https://datatables.net/reference/option/buttons">DataTables buttons option</a>
//     */
//    public String getButtonSearchPanes() {
//        return buttonSearchPanes;
//    }
//
//    /**
//     * @return The caption text for the table.
//     * @see <a href="https://datatables.net/reference/option/caption">DataTables caption option</a>
//     */
//    public String getCaption() {
//        return caption;
//    }
//
//    /**
//     * @return Callback that can be used to manipulate the created row.
//     * @see <a href="https://datatables.net/reference/option/createdRow">DataTables createdRow option</a>
//     */
//    public String getCreatedRow() {
//        return createdRow;
//    }
//
//    /**
//     * @return Data to be displayed in the table.
//     * @see <a href="https://datatables.net/reference/option/data">DataTables data option</a>
//     */
//    public Object getData() {
//        return data;
//    }
//
//    /**
//     * @return Whether deferred loading of data is enabled.
//     * @see <a href="https://datatables.net/reference/option/deferLoading">DataTables deferLoading option</a>
//     */
//    public Boolean getDeferLoading() {
//        return deferLoading;
//    }
//
//    /**
//     * @return Whether deferred rendering is enabled.
//     * @see <a href="https://datatables.net/reference/option/deferRender">DataTables deferRender option</a>
//     */
//    public Boolean getDeferRender() {
//        return deferRender;
//    }
//
//    /**
//     * @return Whether the table should be destroyed when reinitialized.
//     * @see <a href="https://datatables.net/reference/option/destroy">DataTables destroy option</a>
//     */
//    public Boolean getDestroy() {
//        return destroy;
//    }
//
//    /**
//     * @return The type detection options.
//     * @see <a href="https://datatables.net/reference/option/detectType">DataTables detectType option</a>
//     */
//    public String getDetectType() {
//        return detectType;
//    }
//
//    /**
//     * @return The display start point of the table.
//     * @see <a href="https://datatables.net/reference/option/displayStart">DataTables displayStart option</a>
//     */
//    public Integer getDisplayStart() {
//        return displayStart;
//    }
//
//    /**
//     * @return The DataTables DOM positioning control.
//     * @see <a href="https://datatables.net/reference/option/dom">DataTables dom option</a>
//     */
//    public String getDom() {
//        return dom;
//    }
//
//    /**
//     * @return Callback function that is called every time DataTables performs a draw.
//     * @see <a href="https://datatables.net/reference/option/drawCallback">DataTables drawCallback option</a>
//     */
//    public String getDrawCallback() {
//        return drawCallback;
//    }
//
//    /**
//     * @return Callback function for when the footer is drawn.
//     * @see <a href="https://datatables.net/reference/option/footerCallback">DataTables footerCallback option</a>
//     */
//    public String getFooterCallback() {
//        return footerCallback;
//    }
//
//    /**
//     * @return Number formatting string.
//     * @see <a href="https://datatables.net/reference/option/formatNumber">DataTables formatNumber option</a>
//     */
//    public String getFormatNumber() {
//        return formatNumber;
//    }
//
//    /**
//     * @return Callback function for when the header is drawn.
//     * @see <a href="https://datatables.net/reference/option/headerCallback">DataTables headerCallback option</a>
//     */
//    public String getHeaderCallback() {
//        return headerCallback;
//    }
//
//    /**
//     * @return Whether the table information summary is shown.
//     * @see <a href="https://datatables.net/reference/option/info">DataTables info option</a>
//     */
//    public Boolean getInfo() {
//        return info;
//    }
//
//    /**
//     * @return Callback function for modifying the information summary.
//     * @see <a href="https://datatables.net/reference/option/infoCallback">DataTables infoCallback option</a>
//     */
//    public String getInfoCallback() {
//        return infoCallback;
//    }
//
//    /**
//     * @return Callback function that is called when initialisation is complete.
//     * @see <a href="https://datatables.net/reference/option/initComplete">DataTables initComplete option</a>
//     */
//    public String getInitComplete() {
//        return initComplete;
//    }
//
//    /**
//     * @return Layout control for DataTables elements.
//     * @see <a href="https://datatables.net/reference/option/layout">DataTables layout option</a>
//     */
//    public String getLayout() {
//        return layout;
//    }
//
//    /**
//     * @return Whether the length change control is enabled.
//     * @see <a href="https://datatables.net/reference/option/lengthChange">DataTables lengthChange option</a>
//     */
//    public Boolean getLengthChange() {
//        return lengthChange;
//    }
//
//    /**
//     * @return Length menu options.
//     * @see <a href="https://datatables.net/reference/option/lengthMenu">DataTables lengthMenu option</a>
//     */
//    public List<Object> getLengthMenu() {
//        return lengthMenu;
//    }
//
//    /**
//     * @return Ordering options for the table.
//     * @see <a href="https://datatables.net/reference/option/order">DataTables order option</a>
//     */
//    public List<Object> getOrder() {
//        return order;
//    }
//
//    /**
//     * @return Whether the cells in the header should be ordered top to bottom.
//     * @see <a href="https://datatables.net/reference/option/orderCellsTop">DataTables orderCellsTop option</a>
//     */
//    public Boolean getOrderCellsTop() {
//        return orderCellsTop;
//    }
//
//    /**
//     * @return Whether classes should be added to columns during ordering.
//     * @see <a href="https://datatables.net/reference/option/orderClasses">DataTables orderClasses option</a>
//     */
//    public Boolean getOrderClasses() {
//        return orderClasses;
//    }
//
//    /**
//     * @return Whether ordering direction is reversed for full numbers.
//     * @see <a href="https://datatables.net/reference/option/orderDesc">DataTables orderDescReverse option</a>
//     */
//    public Boolean getOrderDescReverse() {
//        return orderDescReverse;
//    }
//
//    /**
//     * @return Fixed ordering definition that is applied.
//     * @see <a href="https://datatables.net/reference/option/orderFixed">DataTables orderFixed option</a>
//     */
//    public Object getOrderFixed() {
//        return orderFixed;
//    }
//
//    /**
//     * @return Whether multiple column ordering is enabled.
//     * @see <a href="https://datatables.net/reference/option/orderMulti">DataTables orderMulti option</a>
//     */
//    public Boolean getOrderMulti() {
//        return orderMulti;
//    }
//
//    /**
//     * @return the ordering option.
//     * @see <a href="https://datatables.net/reference/option/ordering">DataTables ordering option</a>
//     */
//    public String getOrdering() {
//        return ordering;
//    }
//
//    /**
//     * @return Number of rows to display on a single page.
//     * @see <a href="https://datatables.net/reference/option/pageLength">DataTables pageLength option</a>
//     */
//    public Integer getPageLength() {
//        return pageLength;
//    }
//
//    /**
//     * @return Whether paging is enabled for the table.
//     * @see <a href="https://datatables.net/reference/option/paging">DataTables paging option</a>
//     */
//    public Boolean getPaging() {
//        return paging;
//    }
//
//    /**
//     * @return The type of pagination control to be used.
//     * @see <a href="https://datatables.net/reference/option/pagingType">DataTables pagingType option</a>
//     */
//    public String getPagingType() {
//        return pagingType;
//    }
//
//    /**
//     * @return Callback function that is called just before the table is drawn.
//     * @see <a href="https://datatables.net/reference/option/preDrawCallback">DataTables preDrawCallback option</a>
//     */
//    public String getPreDrawCallback() {
//        return preDrawCallback;
//    }
//
//    /**
//     * @return Whether processing indicator should be shown.
//     * @see <a href="https://datatables.net/reference/option/processing">DataTables processing option</a>
//     */
//    public Boolean getProcessing() {
//        return processing;
//    }
//
//    /**
//     * @return Renderer to use for the table.
//     * @see <a href="https://datatables.net/reference/option/renderer">DataTables renderer option</a>
//     */
//    public String getRenderer() {
//        return renderer;
//    }
//
//    /**
//     * @return Whether the table data should be retrieved.
//     * @see <a href="https://datatables.net/reference/option/retrieve">DataTables retrieve option</a>
//     */
//    public Boolean getRetrieve() {
//        return retrieve;
//    }
//
//    /**
//     * @return Callback function for each row that is created.
//     * @see <a href="https://datatables.net/reference/option/rowCallback">DataTables rowCallback option</a>
//     */
//    public String getRowCallback() {
//        return rowCallback;
//    }
//
//    /**
//     * @return Id to be assigned to each row.
//     * @see <a href="https://datatables.net/reference/option/rowId">DataTables rowId option</a>
//     */
//    public String getRowId() {
//        return rowId;
//    }
//
//    /**
//     * @return Whether collapsing of the table when scrolling is enabled.
//     * @see <a href="https://datatables.net/reference/option/scrollCollapse">DataTables scrollCollapse option</a>
//     */
//    public Boolean getScrollCollapse() {
//        return scrollCollapse;
//    }
//
//    /**
//     * @return Whether horizontal scrolling is enabled.
//     * @see <a href="https://datatables.net/reference/option/scrollX">DataTables scrollX option</a>
//     */
//    public Boolean getScrollX() {
//        return scrollX;
//    }
//
//    /**
//     * @return Whether vertical scrolling is enabled.
//     * @see <a href="https://datatables.net/reference/option/scrollY">DataTables scrollY option</a>
//     */
//    public Boolean getScrollY() {
//        return scrollY;
//    }
//
//    /**
//     * @return Delay between keypress and search action in milliseconds.
//     * @see <a href="https://datatables.net/reference/option/searchDelay">DataTables searchDelay option</a>
//     */
//    public Integer getSearchDelay() {
//        return searchDelay;
//    }
//
//    /**
//     * @return Whether the table should be searchable.
//     * @see <a href="https://datatables.net/reference/option/searching">DataTables searching option</a>
//     */
//    public Boolean getSearching() {
//        return searching;
//    }
//
//    /**
//     * @return Whether server-side processing is enabled.
//     * @see <a href="https://datatables.net/reference/option/serverSide">DataTables serverSide option</a>
//     */
//    public Boolean getServerSide() {
//        return serverSide;
//    }
//
//    /**
//     * @return Duration for which the state should be saved in local storage.
//     * @see <a href="https://datatables.net/reference/option/stateDuration">DataTables stateDuration option</a>
//     */
//    public Integer getStateDuration() {
//        return stateDuration;
//    }
//
//    /**
//     * @return Callback function for loading the state.
//     * @see <a href="https://datatables.net/reference/option/stateLoadCallback">DataTables stateLoadCallback option</a>
//     */
//    public String getStateLoadCallback() {
//        return stateLoadCallback;
//    }
//
//    /**
//     * @return Parameters to be loaded for state saving.
//     * @see <a href="https://datatables.net/reference/option/stateLoadParams">DataTables stateLoadParams option</a>
//     */
//    public String getStateLoadParams() {
//        return stateLoadParams;
//    }
//
//    /**
//     * @return Callback function called after the state is loaded.
//     * @see <a href="https://datatables.net/reference/option/stateLoaded">DataTables stateLoaded option</a>
//     */
//    public String getStateLoaded() {
//        return stateLoaded;
//    }
//
//    /**
//     * @return Whether the table state should be saved.
//     * @see <a href="https://datatables.net/reference/option/stateSave">DataTables stateSave option</a>
//     */
//    public Boolean getStateSave() {
//        return stateSave;
//    }
//
//    /**
//     * @return Callback function for saving the state.
//     * @see <a href="https://datatables.net/reference/option/stateSaveCallback">DataTables stateSaveCallback option</a>
//     */
//    public String getStateSaveCallback() {
//        return stateSaveCallback;
//    }
//
//    /**
//     * @return Parameters to be saved for state saving.
//     * @see <a href="https://datatables.net/reference/option/stateSaveParams">DataTables stateSaveParams option</a>
//     */
//    public String getStateSaveParams() {
//        return stateSaveParams;
//    }
//
//    /**
//     * @return Tab index for navigation.
//     * @see <a href="https://datatables.net/reference/option/tabIndex">DataTables tabIndex option</a>
//     */
//    public Integer getTabIndex() {
//        return tabIndex;
//    }

    // Fluent methods for nested options
    /**
     * @return The ajax configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/ajax">DataTables ajax option</a>
     */
    public YadaDTAjax dtAjaxObj() {
        if (this.ajax == null) {
            this.ajax = new YadaDTAjax(this);
        }
        return this.ajax;
    }

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

    /**
     * @return The language configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/language">DataTables language option</a>
     */
    public YadaDTLanguage dtLanguageObj() {
        if (this.language == null) {
            this.language = new YadaDTLanguage(this);
        }
        return this.language;
    }

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
        YadaDTColumnDef newColumnDef = new YadaDTColumnDef(this);
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
        YadaDTColumns newColumn = new YadaDTColumns(this);
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
