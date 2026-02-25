# BurpUploadConverter

**BurpUploadConverter** 是一款基于 Burp Suite Montoya API 开发的高效插件，专注于文件上传漏洞的辅助测试。它能够一键将任意 HTTP 请求（GET/POST）转换为标准的 `multipart/form-data` 文件上传请求，并自动适配常见的文件类型 Payload。

## ✨ 核心功能 (Features)

*   **一键转换**：在 Repeater 或 Proxy 中右键即可将当前请求转换为文件上传格式。
*   **多类型支持**：内置多种常见文件类型的 Payload 模板：
    *   **WebShell**: JSP, PHP, ASPX
    *   **Web**: HTML
    *   **图片 (带魔数)**: JPG, PNG, GIF (包含标准的十六进制文件头/尾)
    *   **压缩包**: ZIP
*   **智能参数处理**：
    *   **参数保留**：自动解析并保留原请求中的 URL 参数和 Body 参数，将其迁移至 Multipart 表单中。
    *   **自动识别**：智能检测空的文件参数（如 `upload_file=`），自动填充文件名和 Payload，避免重复添加。
    *   **增量更新**：如果请求已经是 Multipart 格式，插件仅修改文件部分的内容和类型，保留其他参数不变。
*   **真实二进制生成**：图片和压缩包类型不再是简单的字符串模拟，而是生成包含正确 Magic Bytes 的真实二进制流，可绕过基于文件头的文件类型检测。

## 🚀 安装 (Installation)

1.  下载本项目生成的 JAR 包：`target/Upload_Converter-jar-with-dependencies.jar`。
2.  打开 Burp Suite，进入 **Extensions** -> **Installed**。
3.  点击 **Add**。
4.  选择 **Extension type** 为 **Java**。
5.  选择下载的 JAR 文件进行安装。
6.  安装成功后 Output 窗口将显示：`Upload Converter extension loaded successfully. Author: TulipFleur`。

## 📖 使用方法 (Usage)

1.  **基础转换**：
    *   在 Burp Suite 的 **Repeater** 或 **Proxy** (Intercept) 界面中。
    *   右键点击请求区域。
    *   选择 **Extensions** -> **Convert to Upload**。
    *   选择你想要转换的文件类型（例如 `PHP`）。
    *   插件会自动将请求方法改为 `POST`，`Content-Type` 改为 `multipart/form-data`，并填充 Payload。

2.  **针对已有参数的测试**：
    *   如果原请求是 `GET /index.php?id=1&upload_file=`。
    *   右键转换后，插件会自动保留 `id=1`，并将 `upload_file` 字段填充为对应的文件内容（如 PHP 一句话木马）。

3.  **快速切换 Payload**：
    *   在已经是 Multipart 的上传请求中，再次右键选择不同的类型（例如从 `PHP` 切换为 `JPG`）。
    *   插件会直接替换文件部分的 Content-Type 和内容，保持其他参数不变，极大提高测试效率。

## 🛠️ 编译构建 (Build)

如果你需要自行编译项目：

```bash
# 克隆项目
git clone https://github.com/YourUsername/BurpUploadConverter.git

# 进入目录
cd BurpUploadConverter

# 使用 Maven 打包
mvn package
```

编译完成后，JAR 包位于 `target/` 目录下。

## ⚠️ 免责声明 (Disclaimer)

本工具仅用于合法的安全性测试和教育目的。请勿用于未授权的渗透测试。开发者不对因使用本工具造成的任何非法后果负责。

---
**Author**: TulipFleur
