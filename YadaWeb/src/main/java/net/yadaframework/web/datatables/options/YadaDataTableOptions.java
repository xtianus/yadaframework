package net.yadaframework.web.datatables.options;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing options for configuring DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/">DataTables Reference</a>
 */
public class YadaDataTableOptions {
    private String dataTableExtErrMode;
    private YadaDTAjax ajax;
    private YadaDTAutoFill autoFill;
    private Boolean autoWidth;
    private String buttonSearchBuilder;
    private String buttonSearchPanes;
    private YadaDTButtons buttons;
    private String caption;
    private YadaDTColReorder colReorder;
    private List<YadaDTColumnDef> columnDefs;
    private List<YadaDTColumn> columns;
    private String createdRow;
    private Object data;
    private Boolean deferLoading;
    private Boolean deferRender;
    private Boolean destroy;
    private String detectType;
    private Integer displayStart;
    private String dom;
    private String drawCallback;
    private YadaDTFixedColumns fixedColumns;
    private YadaDTFixedHeader fixedHeader;
    private String footerCallback;
    private String formatNumber;
    private String headerCallback;
    private Boolean info;
    private String infoCallback;
    private String initComplete;
    private YadaDTKeys keys;
    private YadaDTLanguage language;
    private String layout;
    private Boolean lengthChange;
    private List<Object> lengthMenu;
    private List<Object> order;
    private Boolean orderCellsTop;
    private Boolean orderClasses;
    private Boolean orderDescReverse;
    private Object orderFixed;
    private Boolean orderMulti;
    private Boolean ordering;
    private Integer pageLength;
    private Boolean paging;
    private String pagingType;
    private String preDrawCallback;
    private Boolean processing;
    private String renderer;
    private Boolean responsive;
    private YadaDTResponsiveDetails responsiveDetails;
    private Boolean retrieve;
    private String rowCallback;
    private YadaDTRowGroup rowGroup;
    private String rowId;
    private YadaDTRowReorder rowReorder;
    private Boolean scrollCollapse;
    private Boolean scrollX;
    private Boolean scrollY;
    private YadaDTScroller scroller;
    private YadaDTSearch search;
    private YadaDTSearchBuilder searchBuilder;
    private List<YadaDTSearchCol> searchCols;
    private Integer searchDelay;
    private YadaDTSearchPanes searchPanes;
    private Boolean searching;
    private YadaDTSelect select;
    private Boolean serverSide;
    private Integer stateDuration;
    private String stateLoadCallback;
    private String stateLoadParams;
    private String stateLoaded;
    private Boolean stateSave;
    private String stateSaveCallback;
    private String stateSaveParams;
    private Integer tabIndex;

    /**
     * @return The DataTables error mode.
     * @see <a href="https://datatables.net/reference/option/">DataTables Reference</a>
     */
    public String getDataTableExtErrMode() {
        return dataTableExtErrMode;
    }

    /**
     * @return Whether the table should automatically adjust column widths.
     * @see <a href="https://datatables.net/reference/option/autoWidth">DataTables autoWidth option</a>
     */
    public Boolean getAutoWidth() {
        return autoWidth;
    }

    /**
     * @return The configuration for SearchBuilder button.
     * @see <a href="https://datatables.net/reference/option/buttons">DataTables buttons option</a>
     */
    public String getButtonSearchBuilder() {
        return buttonSearchBuilder;
    }

    /**
     * @return The configuration for SearchPanes button.
     * @see <a href="https://datatables.net/reference/option/buttons">DataTables buttons option</a>
     */
    public String getButtonSearchPanes() {
        return buttonSearchPanes;
    }

    /**
     * @return The caption text for the table.
     * @see <a href="https://datatables.net/reference/option/caption">DataTables caption option</a>
     */
    public String getCaption() {
        return caption;
    }

    /**
     * @return Callback that can be used to manipulate the created row.
     * @see <a href="https://datatables.net/reference/option/createdRow">DataTables createdRow option</a>
     */
    public String getCreatedRow() {
        return createdRow;
    }

    /**
     * @return Data to be displayed in the table.
     * @see <a href="https://datatables.net/reference/option/data">DataTables data option</a>
     */
    public Object getData() {
        return data;
    }

    /**
     * @return Whether deferred loading of data is enabled.
     * @see <a href="https://datatables.net/reference/option/deferLoading">DataTables deferLoading option</a>
     */
    public Boolean getDeferLoading() {
        return deferLoading;
    }

    /**
     * @return Whether deferred rendering is enabled.
     * @see <a href="https://datatables.net/reference/option/deferRender">DataTables deferRender option</a>
     */
    public Boolean getDeferRender() {
        return deferRender;
    }

    /**
     * @return Whether the table should be destroyed when reinitialized.
     * @see <a href="https://datatables.net/reference/option/destroy">DataTables destroy option</a>
     */
    public Boolean getDestroy() {
        return destroy;
    }

    /**
     * @return The type detection options.
     * @see <a href="https://datatables.net/reference/option/detectType">DataTables detectType option</a>
     */
    public String getDetectType() {
        return detectType;
    }

    /**
     * @return The display start point of the table.
     * @see <a href="https://datatables.net/reference/option/displayStart">DataTables displayStart option</a>
     */
    public Integer getDisplayStart() {
        return displayStart;
    }

    /**
     * @return The DataTables DOM positioning control.
     * @see <a href="https://datatables.net/reference/option/dom">DataTables dom option</a>
     */
    public String getDom() {
        return dom;
    }

    /**
     * @return Callback function that is called every time DataTables performs a draw.
     * @see <a href="https://datatables.net/reference/option/drawCallback">DataTables drawCallback option</a>
     */
    public String getDrawCallback() {
        return drawCallback;
    }

    /**
     * @return Callback function for when the footer is drawn.
     * @see <a href="https://datatables.net/reference/option/footerCallback">DataTables footerCallback option</a>
     */
    public String getFooterCallback() {
        return footerCallback;
    }

    /**
     * @return Number formatting string.
     * @see <a href="https://datatables.net/reference/option/formatNumber">DataTables formatNumber option</a>
     */
    public String getFormatNumber() {
        return formatNumber;
    }

    /**
     * @return Callback function for when the header is drawn.
     * @see <a href="https://datatables.net/reference/option/headerCallback">DataTables headerCallback option</a>
     */
    public String getHeaderCallback() {
        return headerCallback;
    }

    /**
     * @return Whether the table information summary is shown.
     * @see <a href="https://datatables.net/reference/option/info">DataTables info option</a>
     */
    public Boolean getInfo() {
        return info;
    }

    /**
     * @return Callback function for modifying the information summary.
     * @see <a href="https://datatables.net/reference/option/infoCallback">DataTables infoCallback option</a>
     */
    public String getInfoCallback() {
        return infoCallback;
    }

    /**
     * @return Callback function that is called when initialisation is complete.
     * @see <a href="https://datatables.net/reference/option/initComplete">DataTables initComplete option</a>
     */
    public String getInitComplete() {
        return initComplete;
    }

    /**
     * @return Layout control for DataTables elements.
     * @see <a href="https://datatables.net/reference/option/layout">DataTables layout option</a>
     */
    public String getLayout() {
        return layout;
    }

    /**
     * @return Whether the length change control is enabled.
     * @see <a href="https://datatables.net/reference/option/lengthChange">DataTables lengthChange option</a>
     */
    public Boolean getLengthChange() {
        return lengthChange;
    }

    /**
     * @return Length menu options.
     * @see <a href="https://datatables.net/reference/option/lengthMenu">DataTables lengthMenu option</a>
     */
    public List<Object> getLengthMenu() {
        return lengthMenu;
    }

    /**
     * @return Ordering options for the table.
     * @see <a href="https://datatables.net/reference/option/order">DataTables order option</a>
     */
    public List<Object> getOrder() {
        return order;
    }

    /**
     * @return Whether the cells in the header should be ordered top to bottom.
     * @see <a href="https://datatables.net/reference/option/orderCellsTop">DataTables orderCellsTop option</a>
     */
    public Boolean getOrderCellsTop() {
        return orderCellsTop;
    }

    /**
     * @return Whether classes should be added to columns during ordering.
     * @see <a href="https://datatables.net/reference/option/orderClasses">DataTables orderClasses option</a>
     */
    public Boolean getOrderClasses() {
        return orderClasses;
    }

    /**
     * @return Whether ordering direction is reversed for full numbers.
     * @see <a href="https://datatables.net/reference/option/orderDesc">DataTables orderDescReverse option</a>
     */
    public Boolean getOrderDescReverse() {
        return orderDescReverse;
    }

    /**
     * @return Fixed ordering definition that is applied.
     * @see <a href="https://datatables.net/reference/option/orderFixed">DataTables orderFixed option</a>
     */
    public Object getOrderFixed() {
        return orderFixed;
    }

    /**
     * @return Whether multiple column ordering is enabled.
     * @see <a href="https://datatables.net/reference/option/orderMulti">DataTables orderMulti option</a>
     */
    public Boolean getOrderMulti() {
        return orderMulti;
    }

    /**
     * @return Whether ordering is enabled.
     * @see <a href="https://datatables.net/reference/option/ordering">DataTables ordering option</a>
     */
    public Boolean getOrdering() {
        return ordering;
    }

    /**
     * @return Number of rows to display on a single page.
     * @see <a href="https://datatables.net/reference/option/pageLength">DataTables pageLength option</a>
     */
    public Integer getPageLength() {
        return pageLength;
    }

    /**
     * @return Whether paging is enabled for the table.
     * @see <a href="https://datatables.net/reference/option/paging">DataTables paging option</a>
     */
    public Boolean getPaging() {
        return paging;
    }

    /**
     * @return The type of pagination control to be used.
     * @see <a href="https://datatables.net/reference/option/pagingType">DataTables pagingType option</a>
     */
    public String getPagingType() {
        return pagingType;
    }

    /**
     * @return Callback function that is called just before the table is drawn.
     * @see <a href="https://datatables.net/reference/option/preDrawCallback">DataTables preDrawCallback option</a>
     */
    public String getPreDrawCallback() {
        return preDrawCallback;
    }

    /**
     * @return Whether processing indicator should be shown.
     * @see <a href="https://datatables.net/reference/option/processing">DataTables processing option</a>
     */
    public Boolean getProcessing() {
        return processing;
    }

    /**
     * @return Renderer to use for the table.
     * @see <a href="https://datatables.net/reference/option/renderer">DataTables renderer option</a>
     */
    public String getRenderer() {
        return renderer;
    }

    /**
     * @return Whether responsive behavior is enabled.
     * @see <a href="https://datatables.net/reference/option/responsive">DataTables responsive option</a>
     */
    public Boolean getResponsive() {
        return responsive;
    }

    /**
     * @return Whether the table data should be retrieved.
     * @see <a href="https://datatables.net/reference/option/retrieve">DataTables retrieve option</a>
     */
    public Boolean getRetrieve() {
        return retrieve;
    }

    /**
     * @return Callback function for each row that is created.
     * @see <a href="https://datatables.net/reference/option/rowCallback">DataTables rowCallback option</a>
     */
    public String getRowCallback() {
        return rowCallback;
    }

    /**
     * @return Id to be assigned to each row.
     * @see <a href="https://datatables.net/reference/option/rowId">DataTables rowId option</a>
     */
    public String getRowId() {
        return rowId;
    }

    /**
     * @return Whether collapsing of the table when scrolling is enabled.
     * @see <a href="https://datatables.net/reference/option/scrollCollapse">DataTables scrollCollapse option</a>
     */
    public Boolean getScrollCollapse() {
        return scrollCollapse;
    }

    /**
     * @return Whether horizontal scrolling is enabled.
     * @see <a href="https://datatables.net/reference/option/scrollX">DataTables scrollX option</a>
     */
    public Boolean getScrollX() {
        return scrollX;
    }

    /**
     * @return Whether vertical scrolling is enabled.
     * @see <a href="https://datatables.net/reference/option/scrollY">DataTables scrollY option</a>
     */
    public Boolean getScrollY() {
        return scrollY;
    }

    /**
     * @return Delay between keypress and search action in milliseconds.
     * @see <a href="https://datatables.net/reference/option/searchDelay">DataTables searchDelay option</a>
     */
    public Integer getSearchDelay() {
        return searchDelay;
    }

    /**
     * @return Whether the table should be searchable.
     * @see <a href="https://datatables.net/reference/option/searching">DataTables searching option</a>
     */
    public Boolean getSearching() {
        return searching;
    }

    /**
     * @return Whether server-side processing is enabled.
     * @see <a href="https://datatables.net/reference/option/serverSide">DataTables serverSide option</a>
     */
    public Boolean getServerSide() {
        return serverSide;
    }

    /**
     * @return Duration for which the state should be saved in local storage.
     * @see <a href="https://datatables.net/reference/option/stateDuration">DataTables stateDuration option</a>
     */
    public Integer getStateDuration() {
        return stateDuration;
    }

    /**
     * @return Callback function for loading the state.
     * @see <a href="https://datatables.net/reference/option/stateLoadCallback">DataTables stateLoadCallback option</a>
     */
    public String getStateLoadCallback() {
        return stateLoadCallback;
    }

    /**
     * @return Parameters to be loaded for state saving.
     * @see <a href="https://datatables.net/reference/option/stateLoadParams">DataTables stateLoadParams option</a>
     */
    public String getStateLoadParams() {
        return stateLoadParams;
    }

    /**
     * @return Callback function called after the state is loaded.
     * @see <a href="https://datatables.net/reference/option/stateLoaded">DataTables stateLoaded option</a>
     */
    public String getStateLoaded() {
        return stateLoaded;
    }

    /**
     * @return Whether the table state should be saved.
     * @see <a href="https://datatables.net/reference/option/stateSave">DataTables stateSave option</a>
     */
    public Boolean getStateSave() {
        return stateSave;
    }

    /**
     * @return Callback function for saving the state.
     * @see <a href="https://datatables.net/reference/option/stateSaveCallback">DataTables stateSaveCallback option</a>
     */
    public String getStateSaveCallback() {
        return stateSaveCallback;
    }

    /**
     * @return Parameters to be saved for state saving.
     * @see <a href="https://datatables.net/reference/option/stateSaveParams">DataTables stateSaveParams option</a>
     */
    public String getStateSaveParams() {
        return stateSaveParams;
    }

    /**
     * @return Tab index for navigation.
     * @see <a href="https://datatables.net/reference/option/tabIndex">DataTables tabIndex option</a>
     */
    public Integer getTabIndex() {
        return tabIndex;
    }

    // Fluent methods for nested options
    /**
     * @return The ajax configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/ajax">DataTables ajax option</a>
     */
    public YadaDTAjax ajax() {
        if (this.ajax == null) {
            this.ajax = new YadaDTAjax(this);
        }
        return this.ajax;
    }

    /**
     * @return The autoFill configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/autoFill">DataTables autoFill option</a>
     */
    public YadaDTAutoFill autoFill() {
        if (this.autoFill == null) {
            this.autoFill = new YadaDTAutoFill(this);
        }
        return this.autoFill;
    }

    /**
     * @return The buttons configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/buttons">DataTables buttons option</a>
     */
    public YadaDTButtons buttons() {
        if (this.buttons == null) {
            this.buttons = new YadaDTButtons(this);
        }
        return this.buttons;
    }

    /**
     * @return The colReorder configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/colReorder">DataTables colReorder option</a>
     */
    public YadaDTColReorder colReorder() {
        if (this.colReorder == null) {
            this.colReorder = new YadaDTColReorder(this);
        }
        return this.colReorder;
    }

    /**
     * @return The fixedColumns configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/fixedColumns">DataTables fixedColumns option</a>
     */
    public YadaDTFixedColumns fixedColumns() {
        if (this.fixedColumns == null) {
            this.fixedColumns = new YadaDTFixedColumns(this);
        }
        return this.fixedColumns;
    }

    /**
     * @return The fixedHeader configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/fixedHeader">DataTables fixedHeader option</a>
     */
    public YadaDTFixedHeader fixedHeader() {
        if (this.fixedHeader == null) {
            this.fixedHeader = new YadaDTFixedHeader(this);
        }
        return this.fixedHeader;
    }

    /**
     * @return The keys configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/keys">DataTables keys option</a>
     */
    public YadaDTKeys keys() {
        if (this.keys == null) {
            this.keys = new YadaDTKeys(this);
        }
        return this.keys;
    }

    /**
     * @return The language configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/language">DataTables language option</a>
     */
    public YadaDTLanguage language() {
        if (this.language == null) {
            this.language = new YadaDTLanguage(this);
        }
        return this.language;
    }

    /**
     * @return The responsiveDetails configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/responsive">DataTables responsive option</a>
     */
    public YadaDTResponsiveDetails responsiveDetails() {
        if (this.responsiveDetails == null) {
            this.responsiveDetails = new YadaDTResponsiveDetails(this);
        }
        return this.responsiveDetails;
    }

    /**
     * @return The rowGroup configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/rowGroup">DataTables rowGroup option</a>
     */
    public YadaDTRowGroup rowGroup() {
        if (this.rowGroup == null) {
            this.rowGroup = new YadaDTRowGroup(this);
        }
        return this.rowGroup;
    }

    /**
     * @return The rowReorder configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/rowReorder">DataTables rowReorder option</a>
     */
    public YadaDTRowReorder rowReorder() {
        if (this.rowReorder == null) {
            this.rowReorder = new YadaDTRowReorder(this);
        }
        return this.rowReorder;
    }

    /**
     * @return The scroller configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/scroller">DataTables scroller option</a>
     */
    public YadaDTScroller scroller() {
        if (this.scroller == null) {
            this.scroller = new YadaDTScroller(this);
        }
        return this.scroller;
    }

    /**
     * @return The search configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/search">DataTables search option</a>
     */
    public YadaDTSearch search() {
        if (this.search == null) {
            this.search = new YadaDTSearch(this);
        }
        return this.search;
    }

    /**
     * @return The searchBuilder configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/searchBuilder">DataTables searchBuilder option</a>
     */
    public YadaDTSearchBuilder searchBuilder() {
        if (this.searchBuilder == null) {
            this.searchBuilder = new YadaDTSearchBuilder(this);
        }
        return this.searchBuilder;
    }

    /**
     * @return The searchPanes configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/searchPanes">DataTables searchPanes option</a>
     */
    public YadaDTSearchPanes searchPanes() {
        if (this.searchPanes == null) {
            this.searchPanes = new YadaDTSearchPanes(this);
        }
        return this.searchPanes;
    }

    /**
     * @return The select configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/select">DataTables select option</a>
     */
    public YadaDTSelect select() {
        if (this.select == null) {
            this.select = new YadaDTSelect(this);
        }
        return this.select;
    }

    /**
     * @return A new column definition for DataTables.
     * @see <a href="https://datatables.net/reference/option/columnDefs">DataTables columnDefs option</a>
     */
    public YadaDTColumnDef columnDefs() {
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
    public YadaDTColumn columns() {
        if (this.columns == null) {
            this.columns = new ArrayList<>();
        }
        YadaDTColumn newColumn = new YadaDTColumn(this);
        this.columns.add(newColumn);
        return newColumn;
    }

    /**
     * @return A new search column configuration for DataTables.
     * @see <a href="https://datatables.net/reference/option/searchCols">DataTables searchCols option</a>
     */
    public YadaDTSearchCol searchCols() {
        if (this.searchCols == null) {
            this.searchCols = new ArrayList<>();
        }
        YadaDTSearchCol newSearchCol = new YadaDTSearchCol(this);
        this.searchCols.add(newSearchCol);
        return newSearchCol;
    }
}
