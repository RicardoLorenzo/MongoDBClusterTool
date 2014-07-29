package views.data;

/**
 * Created by ricardolorenzo on 29/07/2014.
 */
public class ClusterEditionForm {
    private String fileContent;
    private String fileName;

    public ClusterEditionForm() {
    }

    public ClusterEditionForm(String fileConetent, String fileName) {
        this.fileContent = fileConetent;
        this.fileName = fileName;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
