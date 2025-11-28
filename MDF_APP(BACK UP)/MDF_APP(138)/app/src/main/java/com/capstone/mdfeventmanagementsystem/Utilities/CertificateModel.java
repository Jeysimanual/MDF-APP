package com.capstone.mdfeventmanagementsystem.Utilities;

import java.util.Map;

public class CertificateModel {
    private String templateName;
    private String templateData;

    public CertificateModel() {}

    public CertificateModel(String templateName, String templateData) {
        this.templateName = templateName;
        this.templateData = templateData;
    }

    public String getTemplateName() { return templateName; }
    public String getTemplateData() { return templateData; }
}

