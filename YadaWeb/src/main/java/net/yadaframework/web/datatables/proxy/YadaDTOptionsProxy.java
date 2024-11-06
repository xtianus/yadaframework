package net.yadaframework.web.datatables.proxy;

import java.util.ArrayList;
import java.util.List;

import net.yadaframework.web.datatables.YadaDataTable;
import net.yadaframework.web.datatables.options.YadaDTAutoFill;
import net.yadaframework.web.datatables.options.YadaDTButtons;
import net.yadaframework.web.datatables.options.YadaDTColReorder;
import net.yadaframework.web.datatables.options.YadaDTColumnDef;
import net.yadaframework.web.datatables.options.YadaDTColumns;
import net.yadaframework.web.datatables.options.YadaDTFixedColumns;
import net.yadaframework.web.datatables.options.YadaDTFixedHeader;
import net.yadaframework.web.datatables.options.YadaDTKeys;
import net.yadaframework.web.datatables.options.YadaDTLanguage;
import net.yadaframework.web.datatables.options.YadaDTOptions;
import net.yadaframework.web.datatables.options.YadaDTOrder;
import net.yadaframework.web.datatables.options.YadaDTResponsive;
import net.yadaframework.web.datatables.options.YadaDTRowGroup;
import net.yadaframework.web.datatables.options.YadaDTRowReorder;
import net.yadaframework.web.datatables.options.YadaDTScroller;
import net.yadaframework.web.datatables.options.YadaDTSearch;
import net.yadaframework.web.datatables.options.YadaDTSearchBuilder;
import net.yadaframework.web.datatables.options.YadaDTSearchCol;
import net.yadaframework.web.datatables.options.YadaDTSearchPanes;
import net.yadaframework.web.datatables.options.YadaDTSelect;

/**
 * This class implements the methods needed for <b>internal use</b> 
 * so that they don't pollute the fluent interface.
 */
public class YadaDTOptionsProxy extends YadaDTOptions {

	public YadaDTOptionsProxy(YadaDataTable parent) {
		super(parent);
	}
	
	/**
	 * Add a new column at the specified position.
	 * @param pos the position to add the new column
	 * @return the new column
	 */
	public YadaDTColumns addNewColumn(int pos) {
        if (columns == null) {
            columns = new ArrayList<>();
        }
		YadaDTColumns newColumn = new YadaDTColumnsProxy(this);
		columns.add(pos, newColumn);
		return newColumn;
	}
	
	/**
	 * @return either a boolean or a full object.
	 * @see <a href="https://datatables.net/reference/option/responsive">DataTables responsive option</a>
	 */
	public Object getResponsive() {
		return yadaDTResponsive != null ? yadaDTResponsive : responsive;
	}
	
	
	public String getDataTableExtErrMode() {
		return dataTableExtErrMode;
	}

	public YadaDTAutoFill getAutoFill() {
		return autoFill;
	}

	public Boolean getAutoWidth() {
		return autoWidth;
	}

	public String getButtonSearchBuilder() {
		return buttonSearchBuilder;
	}

	public String getButtonSearchPanes() {
		return buttonSearchPanes;
	}

	public YadaDTButtons getButtons() {
		return buttons;
	}

	public String getCaption() {
		return caption;
	}

	public YadaDTColReorder getColReorder() {
		return colReorder;
	}

	public List<YadaDTColumnDef> getColumnDefs() {
		return columnDefs;
	}

	public List<YadaDTColumns> getColumns() {
		return columns;
	}

	public String getCreatedRow() {
		return createdRow;
	}

	public Object getData() {
		return data;
	}

	public Boolean getDeferLoading() {
		return deferLoading;
	}

	public Boolean getDeferRender() {
		return deferRender;
	}

	public Boolean getDestroy() {
		return destroy;
	}

	public String getDetectType() {
		return detectType;
	}

	public Integer getDisplayStart() {
		return displayStart;
	}

	public String getDom() {
		return dom;
	}

	public String getDrawCallback() {
		return drawCallback;
	}

	public YadaDTFixedColumns getFixedColumns() {
		return fixedColumns;
	}

	public YadaDTFixedHeader getFixedHeader() {
		return fixedHeader;
	}

	public String getFooterCallback() {
		return footerCallback;
	}

	public String getFormatNumber() {
		return formatNumber;
	}

	public String getHeaderCallback() {
		return headerCallback;
	}

	public Boolean getInfo() {
		return info;
	}

	public String getInfoCallback() {
		return infoCallback;
	}

	public String getInitComplete() {
		return initComplete;
	}

	public YadaDTKeys getKeys() {
		return keys;
	}

//	public YadaDTLanguage getLanguage() {
//		return language;
//	}

	public String getLayout() {
		return layout;
	}

	public Boolean getLengthChange() {
		return lengthChange;
	}

	public List<Object> getLengthMenu() {
		return lengthMenu;
	}

	public List<YadaDTOrder> getOrder() {
		return order;
	}

	public Boolean getOrderCellsTop() {
		return orderCellsTop;
	}

	public Boolean getOrderClasses() {
		return orderClasses;
	}

	public Boolean getOrderDescReverse() {
		return orderDescReverse;
	}

	public Object getOrderFixed() {
		return orderFixed;
	}

	public Boolean getOrderMulti() {
		return orderMulti;
	}

	public Boolean getOrdering() {
		return ordering;
	}

	public Integer getPageLength() {
		return pageLength;
	}

	public Boolean getPaging() {
		return paging;
	}

	public String getPagingType() {
		return pagingType;
	}

	public String getPreDrawCallback() {
		return preDrawCallback;
	}

	public Boolean getProcessing() {
		return processing;
	}

	public String getRenderer() {
		return renderer;
	}

	public YadaDTResponsive getYadaDTResponsive() {
		return yadaDTResponsive;
	}

	public Boolean getRetrieve() {
		return retrieve;
	}

	public String getRowCallback() {
		return rowCallback;
	}

	public YadaDTRowGroup getRowGroup() {
		return rowGroup;
	}

	public String getRowId() {
		return rowId;
	}

	public YadaDTRowReorder getRowReorder() {
		return rowReorder;
	}

	public Boolean getScrollCollapse() {
		return scrollCollapse;
	}

	public Boolean getScrollX() {
		return scrollX;
	}

	public Boolean getScrollY() {
		return scrollY;
	}

	public YadaDTScroller getScroller() {
		return scroller;
	}

	public YadaDTSearch getSearch() {
		return search;
	}

	public YadaDTSearchBuilder getSearchBuilder() {
		return searchBuilder;
	}

	public List<YadaDTSearchCol> getSearchCols() {
		return searchCols;
	}

	public Integer getSearchDelay() {
		return searchDelay;
	}

	public YadaDTSearchPanes getSearchPanes() {
		return searchPanes;
	}

	public Boolean getSearching() {
		return searching;
	}

	public YadaDTSelect getSelect() {
		return select;
	}

	public Boolean getServerSide() {
		return serverSide;
	}

	public Integer getStateDuration() {
		return stateDuration;
	}

	public String getStateLoadCallback() {
		return stateLoadCallback;
	}

	public String getStateLoadParams() {
		return stateLoadParams;
	}

	public String getStateLoaded() {
		return stateLoaded;
	}

	public Boolean getStateSave() {
		return stateSave;
	}

	public String getStateSaveCallback() {
		return stateSaveCallback;
	}

	public String getStateSaveParams() {
		return stateSaveParams;
	}

	public Integer getTabIndex() {
		return tabIndex;
	}
	

}
