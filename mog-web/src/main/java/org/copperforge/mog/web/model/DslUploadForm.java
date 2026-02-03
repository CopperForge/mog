package org.copperforge.mog.web.model;

import org.springframework.web.multipart.MultipartFile;

public class DslUploadForm {

    private String jsonText;
    private MultipartFile file;

    public String getJsonText() {
        return jsonText;
    }

    public void setJsonText(String jsonText) {
        this.jsonText = jsonText;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
