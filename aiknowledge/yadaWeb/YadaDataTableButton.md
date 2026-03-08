# `net.yadaframework.web.datatables.config.YadaDataTableButton`

| Method | Description |
|---|---|
| `dtConfirmDialogObj` | Enable javascript-side confirmation dialog for button action. |
| `dtMultiRow` | Enable the toolbar button when one or many rows are selected. |
| `dtToolbarCssClass` | CSS class to be applied to the button in the toolbar, for example 'btn-primary'. |
| `dtIcon` | HTML content for the button's icon. |
| `dtUrlProvider` | javascript function to be called when the button is clicked in order to compute the target URL for the button action. |
| `dtUrl` | URL to be called when the button is clicked, it can be a thymeleaf expression and will be inserted in a @{ when missing |
| `dtGlobal` | Sets for a button that is always enabled regardless of row selection, for example an Add button. |
| `dtIdName` | Name of the ID request parameter, default is "id". |
| `dtNoAjax` | Indicate that the button should use a normal request |
| `dtHidePageLoader` | Do not show the page loader when the button is clicked |
| `dtElementLoader` | Show the loader on the selected element |
| `dtWindowTarget` | Name of the window for opening the URL in a new window. |
| `dtWindowFeatures` | Features of the window when opened, such as its size, scrollbars, and whether it is resizable. |
| `dtShowCommandIcon` | Javascript function that determines whether to show the button icon for each row. |
| `dtRole` | Role that can access the button. |
| `back` | Sets back. |
