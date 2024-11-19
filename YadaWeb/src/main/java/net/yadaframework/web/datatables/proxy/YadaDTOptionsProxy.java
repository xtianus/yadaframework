package net.yadaframework.web.datatables.proxy;

import java.util.ArrayList;
import java.util.List;

import net.yadaframework.web.datatables.YadaDataTable;
import net.yadaframework.web.datatables.options.YadaDTColumnDef;
import net.yadaframework.web.datatables.options.YadaDTColumns;
import net.yadaframework.web.datatables.options.YadaDTOptions;
import net.yadaframework.web.datatables.options.YadaDTOrder;

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

	public Boolean getAutoWidth() {
		return autoWidth;
	}

	public String getCaption() {
		return caption;
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

	public Object getDeferLoading() {
		return deferLoading;
	}

	public Boolean getDeferRender() {
		return deferRender;
	}

	public Boolean getDestroy() {
		return destroy;
	}

	public Boolean getTypeDetect() {
		return typeDetect;
	}

	public Integer getDisplayStart() {
		return displayStart;
	}

	public String getDrawCallback() {
		return drawCallback;
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

	public Object getLayout() {
		return layout;
	}

	public Boolean getLengthChange() {
		return lengthChange;
	}

	public int[] getLengthMenu() {
		return lengthMenu;
	}

	public List<YadaDTOrder> getOrder() {
		return order;
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

	public String getPreDrawCallback() {
		return preDrawCallback;
	}

	public Boolean getProcessing() {
		return processing;
	}

	public String getRenderer() {
		return renderer;
	}

//	public YadaDTResponsive getYadaDTResponsive() {
//		return yadaDTResponsive;
//	}

	public String getRowCallback() {
		return rowCallback;
	}

	public String getRowId() {
		return rowId;
	}

	public Boolean getScrollCollapse() {
		return scrollCollapse;
	}

	public Boolean getScrollX() {
		return scrollX;
	}

	public Integer getSearchDelay() {
		return searchDelay;
	}

	public Boolean getSearching() {
		return searching;
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

    /**
     * Enable/disable multiple column ordering ability
     * This has been added for internal use only.
     * @see <a href="https://datatables.net/reference/option/orderMulti">orderMulti</a>
     */
	public void setOrderMulti(boolean multi) {
		orderMulti = multi;
	}
	

}
