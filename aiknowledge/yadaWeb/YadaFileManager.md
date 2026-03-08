# `net.yadaframework.components.YadaFileManager`

| Method | Description |
|---|---|
| `moveToTemp` | Move the file to the public temp folder for later processing. |
| `delete` | Remove a managed file from disk and database |
| `getAbsoluteFile` | Returns the absolute path of a managed file |
| `duplicateFiles` | You don't usually need to call this method but YadaUtil#copyEntity(net.yadaframework.core.CloneableFiltered) instead. |
| `getAbsoluteMobileFile` | Returns the absolute path of the mobile file |
| `getAbsoluteDesktopFile` | Returns the absolute path of the desktop file |
| `getAbsolutePdfFile` | Returns the absolute path of the pdf file |
| `deleteFileAttachment` | Deletes from the filesystem all files related to the attachment |
| `getMobileImageUrl` | Returns the (relative) url of the mobile image if any, or null |
| `getDesktopImageUrl` | Returns the (relative) url of the desktop image. |
| `getPdfImageUrl` | Returns the (relative) url of the pdf image if any, or null. |
| `getFileUrl` | Returns the (relative) url of the file, or null. |
| `uploadFile` | Copies a received file to the upload folder. |
| `manageFile` | Copies a received file to the upload folder. |
| `attachReplace` | Replace the file associated with the current attachment The multipartFile is moved to the destination when config.isFileManagerDeletingUploads() is true, otherwise the original is copied and left unchanged. |
| `attachNew` | Copies an uploaded file to the destination folder, creating a database association to assign to an Entity. |
| `attach` | Performs file copy and (for images) resize to different versions. |
