package com.shook.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.ContentType;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author TulipFleur
 */
public class UploadConverter {
    private final MontoyaApi api;

    public UploadConverter(MontoyaApi api) {
        this.api = api;
    }

    public void handleConversion(ContextMenuEvent event, String fileType) {
        Optional<MessageEditorHttpRequestResponse> editorRequestResponse = event.messageEditorRequestResponse();

        if (editorRequestResponse.isPresent()) {
            MessageEditorHttpRequestResponse editor = editorRequestResponse.get();
            HttpRequest originalRequest = editor.requestResponse().request();
            
            HttpRequest newRequest = convertToMultipart(originalRequest, fileType);
            
            editor.setRequest(newRequest);
        }
    }

    private HttpRequest convertToMultipart(HttpRequest request, String fileType) {
        String boundary;
        String filename = "shell." + fileType.toLowerCase();
        
        String contentType = "application/octet-stream";
        byte[] fileContent = "test".getBytes(StandardCharsets.UTF_8);

        switch (fileType) {
            case "JSP":
                contentType = "application/octet-stream";
                fileContent = "<% out.println(\"test\"); %>".getBytes(StandardCharsets.UTF_8);
                break;
            case "PHP":
                contentType = "application/x-php";
                fileContent = "<?php echo 'test'; ?>".getBytes(StandardCharsets.UTF_8);
                break;
            case "ASPX":
                contentType = "application/xml";
                fileContent = "<%@ Page Language=\"C#\" %> <% Response.Write(\"test\"); %>".getBytes(StandardCharsets.UTF_8);
                break;
            case "HTML":
                contentType = "text/html";
                fileContent = "<h1>test</h1>".getBytes(StandardCharsets.UTF_8);
                break;
            case "JPG":
                contentType = "image/jpeg";
                fileContent = getMagicBytes("JPG");
                break;
            case "PNG":
                contentType = "image/png";
                fileContent = getMagicBytes("PNG");
                break;
            case "GIF":
                contentType = "image/gif";
                fileContent = getMagicBytes("GIF");
                break;
            case "ZIP":
                contentType = "application/zip";
                fileContent = getMagicBytes("ZIP");
                break;
        }

        // Check if request is already multipart
        if (request.contentType() == ContentType.MULTIPART) {
            return updateMultipartRequest(request, fileType, filename, contentType, fileContent);
        }

        boundary = "---------------------------" + System.currentTimeMillis() + new Random().nextInt(1000);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // 1. Add existing parameters
            boolean hasParams = false;
            boolean hasFileParam = false;
            List<ParsedHttpParameter> parameters = request.parameters();
            
            for (ParsedHttpParameter param : parameters) {
                if (param.type() == HttpParameterType.URL || param.type() == HttpParameterType.BODY) {
                    hasParams = true;
                    outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                    
                    String paramName = param.name();
                    if (isUploadParameter(paramName) && param.value().isEmpty()) {
                         hasFileParam = true;
                         outputStream.write(("Content-Disposition: form-data; name=\"" + paramName + "\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                         outputStream.write(("Content-Type: " + contentType + "\r\n").getBytes(StandardCharsets.UTF_8));
                         outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
                         outputStream.write(fileContent);
                    } else {
                        outputStream.write(("Content-Disposition: form-data; name=\"" + paramName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                        outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
                        outputStream.write(param.value().getBytes(StandardCharsets.UTF_8));
                    }
                    outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
                }
            }

            // 2. Add file part (Only if no file parameters exist)
            if (!hasFileParam) {
                outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                outputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                outputStream.write(("Content-Type: " + contentType + "\r\n").getBytes(StandardCharsets.UTF_8));
                outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
                outputStream.write(fileContent);
                outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            
            outputStream.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            // 3. Remove query string from path
            String newPath = request.path();
            int queryIndex = newPath.indexOf('?');
            if (queryIndex != -1) {
                newPath = newPath.substring(0, queryIndex);
            }

            return request
                    .withMethod("POST")
                    .withPath(newPath)
                    .withRemovedHeader("Content-Type")
                    .withHeader("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .withBody(ByteArray.byteArray(outputStream.toByteArray()));
        } catch (IOException e) {
            api.logging().logToError("Error constructing multipart body: " + e.getMessage());
            return request;
        }
    }

    private HttpRequest updateMultipartRequest(HttpRequest request, String fileType, String filename, String contentType, byte[] fileContent) {
        String body = request.bodyToString();
        String boundary = getBoundaryFromHeader(request);
        
        if (boundary == null) {
            // Fallback if boundary parsing fails
            return request; 
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // Split body by boundary
            // Note: This is a simplified parsing approach. For complex cases, a proper parser is needed.
            // We assume standard multipart format.
            String[] parts = body.split("--" + Pattern.quote(boundary));
            
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part.trim().isEmpty() || part.trim().equals("--")) continue;

                outputStream.write(("--" + boundary).getBytes(StandardCharsets.UTF_8));
                
                // Check if this part is a file upload part
                if (isFilePart(part)) {
                    // Reconstruct this part with new content
                    String headerSection = getHeaderSection(part);
                    String name = getNameFromContentDisposition(headerSection);
                    
                    outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
                    outputStream.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                    outputStream.write(("Content-Type: " + contentType + "\r\n").getBytes(StandardCharsets.UTF_8));
                    outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
                    outputStream.write(fileContent);
                    outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
                } else {
                    // Keep original part
                    outputStream.write(part.getBytes(StandardCharsets.UTF_8));
                }
            }
            outputStream.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            
            return request.withBody(ByteArray.byteArray(outputStream.toByteArray()));

        } catch (IOException e) {
             api.logging().logToError("Error updating multipart body: " + e.getMessage());
             return request;
        }
    }

    private String getBoundaryFromHeader(HttpRequest request) {
        String contentType = request.headerValue("Content-Type");
        if (contentType != null && contentType.contains("boundary=")) {
            return contentType.substring(contentType.indexOf("boundary=") + 9);
        }
        return null;
    }

    private boolean isFilePart(String part) {
        return part.contains("filename=\"") || (part.contains("Content-Disposition") && isUploadParameter(getNameFromContentDisposition(part)));
    }

    private String getHeaderSection(String part) {
        int emptyLineIndex = part.indexOf("\r\n\r\n");
        if (emptyLineIndex != -1) {
            return part.substring(0, emptyLineIndex);
        }
        return part;
    }

    private String getNameFromContentDisposition(String header) {
        Pattern pattern = Pattern.compile("name=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(header);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "file";
    }

    private boolean isUploadParameter(String paramName) {
        String name = paramName.toLowerCase();
        return name.contains("file") || name.contains("upload") || name.equals("f");
    }

    private byte[] getMagicBytes(String type) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            switch (type) {
                case "JPG":
                    // Header: FF D8 FF E0
                    bos.write(new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0});
                    bos.write("\n".getBytes(StandardCharsets.UTF_8));
                    // Random content
                    bos.write("......JFIF......".getBytes(StandardCharsets.UTF_8));
                    bos.write("\n".getBytes(StandardCharsets.UTF_8));
                    // Footer: FF D9
                    bos.write(new byte[]{(byte)0xFF, (byte)0xD9});
                    break;
                case "PNG":
                    // Header: 89 50 4E 47 0D 0A 1A 0A
                    bos.write(new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
                    bos.write("\n".getBytes(StandardCharsets.UTF_8));
                    // Random content
                    bos.write("......IHDR......".getBytes(StandardCharsets.UTF_8));
                    bos.write("\n".getBytes(StandardCharsets.UTF_8));
                    // Footer: 49 45 4E 44 AE 42 60 82
                    bos.write(new byte[]{0x49, 0x45, 0x4E, 0x44, (byte)0xAE, 0x42, 0x60, (byte)0x82});
                    break;
                case "GIF":
                    // Header: GIF89a (47 49 46 38 39 61)
                    bos.write("GIF89a".getBytes(StandardCharsets.UTF_8));
                    bos.write("\n".getBytes(StandardCharsets.UTF_8));
                    // Random content
                    bos.write("......".getBytes(StandardCharsets.UTF_8));
                    bos.write("\n".getBytes(StandardCharsets.UTF_8));
                    // Footer: 3B
                    bos.write(new byte[]{0x3B});
                    break;
                case "ZIP":
                    // Header: 50 4B 03 04
                    bos.write(new byte[]{0x50, 0x4B, 0x03, 0x04});
                    bos.write("\n".getBytes(StandardCharsets.UTF_8));
                    // Random content
                    bos.write("......".getBytes(StandardCharsets.UTF_8));
                    bos.write("\n".getBytes(StandardCharsets.UTF_8));
                    // Footer: 50 4B 05 06
                    bos.write(new byte[]{0x50, 0x4B, 0x05, 0x06});
                    break;
            }
        } catch (IOException e) {
            // Should not happen with ByteArrayOutputStream
        }
        return bos.toByteArray();
    }
}
