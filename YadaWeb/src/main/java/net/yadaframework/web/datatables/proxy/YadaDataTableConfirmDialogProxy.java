package net.yadaframework.web.datatables.proxy;

import net.yadaframework.web.datatables.config.YadaDataTableButton;
import net.yadaframework.web.datatables.config.YadaDataTableConfirmDialog;
import net.yadaframework.web.datatables.config.YadaDataTableHTML;

public class YadaDataTableConfirmDialogProxy extends YadaDataTableConfirmDialog {

	public YadaDataTableConfirmDialogProxy(YadaDataTableButton parent, YadaDataTableHTML yadaDataTableHTML) {
		super(parent, yadaDataTableHTML);
	}

	/**
	 * @return the confirmTitle
	 */
	public String getConfirmTitle() {
		return confirmTitle;
	}

	/**
	 * @return the confirmOneMessage
	 */
	public String getConfirmOneMessage() {
		return confirmOneMessage;
	}

	/**
	 * @return the confirmManyMessage
	 */
	public String getConfirmManyMessage() {
		return confirmManyMessage;
	}

	/**
	 * @return the confirmButtonText
	 */
	public String getConfirmButtonText() {
		return confirmButtonText;
	}

	/**
	 * @return the abortButtonText
	 */
	public String getAbortButtonText() {
		return abortButtonText;
	}

}
