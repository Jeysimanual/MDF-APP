package com.capstone.mdfeventmanagementsystem.Student;

public class Certificate {

    private String previewImageUrl;
    private String templateName;
    private String receivedDate;
    private String certificateKey; // 🔑 Firebase key

    public Certificate() {
        // Default constructor required for Firebase
    }

    public Certificate(String previewImageUrl, String templateName, String receivedDate) {
        this.previewImageUrl = previewImageUrl;
        this.templateName = templateName;
        this.receivedDate = receivedDate;
    }

    public String getPreviewImageUrl() {
        return previewImageUrl;
    }

    public void setPreviewImageUrl(String previewImageUrl) {
        this.previewImageUrl = previewImageUrl;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(String receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getCertificateKey() {
        return certificateKey;
    }

    public void setCertificateKey(String certificateKey) {
        this.certificateKey = certificateKey;
    }
}
