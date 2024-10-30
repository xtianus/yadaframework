
package net.yadaframework.web.datatables.options;

/**
 * DataTables ordering object.
 * 
 * @see <a href="https://datatables.net/reference/type/DataTables.Order">Order</a>
 */
public class YadaDTOrder {
    private int idx;
    private String dir;

    YadaDTOrder(int idx, String dir) {
    	this.idx = idx;
    	this.dir = dir;
    }

	public int getIdx() {
		return idx;
	}

	public String getDir() {
		return dir;
	}
	
}
