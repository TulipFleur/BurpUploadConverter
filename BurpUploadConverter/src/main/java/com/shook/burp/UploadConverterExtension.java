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
        
        String banner = "\n" +
            "========================================================================\n" +
            "Upload Converter extension loaded successfully.\n" +
            "Author: TulipFleur\n" +
            "GitHub: https://github.com/TulipFleur/BurpUploadConverter.git\n" +
            "========================================================================\n";
            
        api.logging().logToOutput(banner);
    }
}
