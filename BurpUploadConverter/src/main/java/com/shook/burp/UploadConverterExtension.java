package com.shook.burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

/**
 * @author TulipFleur
 */
public class UploadConverterExtension implements BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("Upload Converter");
        api.userInterface().registerContextMenuItemsProvider(new UploadContextMenuProvider(api));
        api.logging().logToOutput("Upload Converter extension loaded successfully. Author: TulipFleur");
    }
}
