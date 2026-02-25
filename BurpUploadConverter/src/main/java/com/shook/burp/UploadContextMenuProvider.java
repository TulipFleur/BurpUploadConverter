package com.shook.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.http.message.HttpRequestResponse;

import javax.swing.JMenuItem;
import javax.swing.JMenu;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

public class UploadContextMenuProvider implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final UploadConverter converter;

    public UploadContextMenuProvider(MontoyaApi api) {
        this.api = api;
        this.converter = new UploadConverter(api);
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        // Only show if we have a request selected in an editor or list
        if (event.messageEditorRequestResponse().isPresent() || !event.selectedRequestResponses().isEmpty()) {
            List<Component> menuList = new ArrayList<>();
            
            JMenu uploadMenu = new JMenu("Convert to Upload");
            
            String[] types = {"JSP", "PHP", "ASPX", "HTML", "JPG", "PNG", "GIF", "ZIP"};
            
            for (String type : types) {
                JMenuItem item = new JMenuItem(type);
                item.addActionListener(e -> converter.handleConversion(event, type));
                uploadMenu.add(item);
            }

            menuList.add(uploadMenu);
            return menuList;
        }
        return null;
    }
}
