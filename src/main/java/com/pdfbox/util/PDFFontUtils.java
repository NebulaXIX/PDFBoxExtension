package com.pdfbox.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.fontbox.ttf.NamingTable;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

/**
 * PDF字体工具类
 * 提供字体懒加载、字符匹配和文本插入功能
 * 
 * @author PDFBox Extension
 * @version 1.0.0
 */
public class PDFFontUtils {
    
    /**
     * 静态初始化块：配置日志级别以抑制FontBox的警告信息
     */
    static {
        try {
            // 抑制 FontBox TTFParser 的警告日志
            Logger fontBoxLogger = Logger.getLogger("org.apache.fontbox.ttf.TTFParser");
            fontBoxLogger.setLevel(Level.SEVERE);
            // 禁用所有处理器，避免输出警告
            fontBoxLogger.setUseParentHandlers(false);
            
            // 同时抑制父级 logger 的警告
            Logger fontBoxParentLogger = Logger.getLogger("org.apache.fontbox");
            fontBoxParentLogger.setLevel(Level.SEVERE);
            fontBoxParentLogger.setUseParentHandlers(false);
            
            // 抑制所有 FontBox 相关的 logger
            Logger fontBoxRootLogger = Logger.getLogger("org.apache.fontbox.ttf");
            fontBoxRootLogger.setLevel(Level.SEVERE);
            fontBoxRootLogger.setUseParentHandlers(false);
        } catch (Exception e) {
            // 如果配置日志失败，忽略异常，不影响正常功能
        }
    }
    
    /**
     * 字体信息内部类
     * 存储字体文件路径和TTC索引（如果是TTC文件）
     */
    private static class FontInfo {
        /**
         * 字体文件路径
         */
        String filePath;
        
        /**
         * TTC文件中的字体索引，如果不是TTC文件则为-1
         */
        int ttcIndex;
        
        /**
         * 构造函数
         * 
         * @param filePath 字体文件路径
         * @param ttcIndex TTC索引，如果不是TTC文件则为-1
         */
        FontInfo(String filePath, int ttcIndex) {
            this.filePath = filePath;
            this.ttcIndex = ttcIndex;
        }
    }
    
    /**
     * 字体列表，按优先级排序
     * key: 字体的cosname
     * value: 字体信息（文件路径和TTC索引）
     */
    private static LinkedHashMap<String, FontInfo> fontList = new LinkedHashMap<>();

    /**
     * 当前 PDF 文档，由调用方在每一个 PDF 更换时通过 setCurrentDocument 设置。
     * 加载字体、插入文本、计算宽度等均使用此 document，不再通过方法参数传入。
     */
    private static PDDocument currentDocument;

    /**
     * 当前 PDF 生成过程中已加载的字体缓存（cosname -> PDFont），切换 document 时清空，避免复用上一份 PDF 的字体。
     */
    private static Map<String, PDFont> fontCache = new HashMap<>();

    /**
     * 默认字体大小
     */
    private static float defaultFontSize = 12.0f;

    /**
     * 字符间距（character spacing），单位：点（point）
     * 字符间距会在每个字符之间添加额外的空间
     * 默认值为0，表示无额外间距
     */
    private static float characterSpacing = 0.0f;
    
    /**
     * 私有构造函数，防止实例化
     */
    private PDFFontUtils() {
        // 静态工具类，不允许实例化
    }

    /**
     * 设置当前 PDF 文档。每个 PDF 生成前调用一次；切换时清空 fontCache，避免复用上一份 PDF 的字体。
     *
     * @param document 当前要操作的 PDDocument，可为 null
     */
    public static void setCurrentDocument(PDDocument document) {
        currentDocument = document;
        fontCache.clear();
    }

    /**
     * 获取当前 PDF 文档。
     *
     * @return 当前 document，可能为 null
     */
    public static PDDocument getCurrentDocument() {
        return currentDocument;
    }

    /**
     * 判断 TrueType 字体是否包含 Unicode cmap 表。
     * 保存 PDF 时 PDFBox 会对字体做子集化，需要 cmap；无 cmap 的字体（如部分符号字体）会抛出 IOException。
     *
     * @param ttf TrueTypeFont 对象
     * @return 若包含可用的 Unicode cmap 返回 true，否则 false
     */
    private static boolean hasUnicodeCmap(TrueTypeFont ttf) {
        try {
            ttf.getUnicodeCmapLookup();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 从TrueTypeFont获取字体的cosname
     * 
     * @param ttf TrueTypeFont对象
     * @return 字体的cosname，如果获取失败返回null
     */
    private static String getFontCosname(TrueTypeFont ttf) {
        try {
            NamingTable namingTable = ttf.getNaming();
            if (namingTable != null) {
                // 获取PostScript名称（通常用作cosname）
                String psName = namingTable.getPostScriptName();
                if (psName != null && !psName.isEmpty()) {
                    return psName;
                }
                // 如果没有PostScript名称，尝试使用字体族名称
                String familyName = namingTable.getFontFamily();
                if (familyName != null && !familyName.isEmpty()) {
                    return familyName;
                }
            }
        } catch (Exception e) {
            // 获取失败，返回null
        }
        return null;
    }
    
    /**
     * 根据路径解析字体文件或路径下的所有字体信息（不实际加载字体）
     * 如果路径是文件，则解析该字体文件
     * 如果路径是目录，则解析目录下所有支持的字体文件（.ttf, .otf, .ttc）
     * 
     * @param fontPath 字体文件路径或目录路径
     * @return 解析成功的字体数量
     */
    public static int loadFontByPath(String fontPath) {
        int count = 0;
        
        try {
            // 将字符串路径转换为Path对象
            Path path = Paths.get(fontPath);
            
            // 检查路径是否存在
            if (!Files.exists(path)) {
                System.err.println("路径不存在: " + fontPath);
                return count;
            }
            
            // 判断是文件还是目录
            if (Files.isRegularFile(path)) {
                // 如果是文件，直接解析该字体文件
                File fontFile = path.toFile();
                String fileName = fontFile.getName().toLowerCase();
                
                // 如果是TTC文件，需要解析所有字体
                if (fileName.endsWith(".ttc")) {
                    count += parseTTCFileInfo(fontFile);
                } else {
                    // TTF或OTF文件，解析单个字体
                    if (parseFontFileInfo(fontFile)) {
                        count++;
                    }
                }
            } else if (Files.isDirectory(path)) {
                // 如果是目录，遍历目录下的所有文件
                File dir = path.toFile();
                File[] files = dir.listFiles();
                
                if (files != null) {
                    // 遍历目录中的每个文件
                    for (File file : files) {
                        // 检查文件扩展名是否为支持的字体格式
                        String fileName = file.getName().toLowerCase();
                        if (fileName.endsWith(".ttf") || 
                            fileName.endsWith(".otf") || 
                            fileName.endsWith(".ttc")) {
                            // 如果是TTC文件，解析所有字体
                            if (fileName.endsWith(".ttc")) {
                                count += parseTTCFileInfo(file);
                            } else {
                                // TTF或OTF文件，解析单个字体
                                if (parseFontFileInfo(file)) {
                                    count++;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 解析失败时打印错误信息
            System.err.println("从路径解析字体失败: " + fontPath + ", 错误: " + e.getMessage());
        }
        
        return count;
    }
    
    /**
     * 解析字体文件信息（不实际加载字体）。
     * 仅当字体包含 Unicode cmap 表时才加入列表，避免保存时子集化报错。
     *
     * @param fontFile 字体文件
     * @return 如果解析成功且字体可用则返回 true，否则返回 false
     */
    private static boolean parseFontFileInfo(File fontFile) {
        Logger fontBoxLogger = Logger.getLogger("org.apache.fontbox.ttf.TTFParser");
        Level originalLevel = fontBoxLogger.getLevel();
        try {
            fontBoxLogger.setLevel(Level.SEVERE);
            fontBoxLogger.setUseParentHandlers(false);
            TrueTypeFont ttf = new TTFParser().parse(fontFile);
            if (!hasUnicodeCmap(ttf)) {
                return false;
            }
            String cosname = getFontCosname(ttf);
            if (cosname != null && !cosname.isEmpty()) {
                if (!fontList.containsKey(cosname)) {
                    fontList.put(cosname, new FontInfo(fontFile.getAbsolutePath(), -1));
                    return true;
                }
            } else {
                String fallbackKey = fontFile.getAbsolutePath();
                if (!fontList.containsKey(fallbackKey)) {
                    fontList.put(fallbackKey, new FontInfo(fontFile.getAbsolutePath(), -1));
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("解析字体文件信息失败: " + fontFile.getAbsolutePath() + ", 错误: " + e.getMessage());
        } finally {
            if (originalLevel != null) {
                fontBoxLogger.setLevel(originalLevel);
            }
        }
        return false;
    }
    
    /**
     * 解析TTC文件信息，获取所有字体的cosname（不实际加载字体）
     * 
     * @param ttcFile TTC字体文件
     * @return 解析成功的字体数量
     */
    private static int parseTTCFileInfo(File ttcFile) {
        int count = 0;
        TrueTypeCollection ttc = null;
        
        // 临时设置日志级别以抑制警告
        Logger fontBoxLogger = Logger.getLogger("org.apache.fontbox");
        Level originalLevel = fontBoxLogger.getLevel();
        try {
            // 设置日志级别为 SEVERE，只显示严重错误
            fontBoxLogger.setLevel(Level.SEVERE);
            fontBoxLogger.setUseParentHandlers(false);
            
            // 使用TrueTypeCollection来解析TTC文件
            ttc = new TrueTypeCollection(ttcFile);
            final int[] index = {0}; // 使用数组来在lambda中修改值
            
            final int[] addedCount = {0};
            // 使用processAllFonts方法遍历TTC文件中的每一个字体
            ttc.processAllFonts((TrueTypeFont ttf) -> {
                try {
                    if (!hasUnicodeCmap(ttf)) {
                        index[0]++;
                        return;
                    }
                    String cosname = getFontCosname(ttf);
                    String filePath = ttcFile.getAbsolutePath();
                    int currentIndex = index[0];

                    if (cosname != null && !cosname.isEmpty()) {
                        if (!fontList.containsKey(cosname)) {
                            fontList.put(cosname, new FontInfo(filePath, currentIndex));
                            addedCount[0]++;
                        }
                    } else {
                        String fallbackKey = filePath + "#" + currentIndex;
                        if (!fontList.containsKey(fallbackKey)) {
                            fontList.put(fallbackKey, new FontInfo(filePath, currentIndex));
                            addedCount[0]++;
                        }
                    }
                    index[0]++;
                } catch (Exception e) {
                    System.err.println("解析TTC文件中的字体失败: " + e.getMessage());
                    index[0]++;
                }
            });

            count = addedCount[0];
            
            if (count > 0) {
                System.out.println("从TTC文件解析了 " + count + " 个字体信息: " + ttcFile.getName());
            } else {
                System.err.println("TTC文件中没有成功解析任何字体: " + ttcFile.getAbsolutePath());
            }
        } catch (IOException e) {
            // 解析失败时打印错误信息
            System.err.println("解析TTC文件失败: " + ttcFile.getAbsolutePath() + ", 错误: " + e.getMessage());
        } finally {
            // 恢复原始日志级别
            if (originalLevel != null) {
                fontBoxLogger.setLevel(originalLevel);
            }
            fontBoxLogger.setUseParentHandlers(true);
            
            // 关闭TrueTypeCollection资源
            if (ttc != null) {
                try {
                    ttc.close();
                } catch (IOException e) {
                    System.err.println("关闭TTC文件失败: " + e.getMessage());
                }
            }
        }
        
        return count;
    }
    
    /**
     * 根据 cosname 加载字体，使用 currentDocument；单次 PDF 生成过程中已加载的字体放入 fontCache 复用。
     *
     * @param cosname 字体的 cosname
     * @return 加载的 PDFont，失败或 currentDocument 为 null 时返回 null
     */
    private static PDFont loadFontByCosname(String cosname) {
        if (currentDocument == null) {
            return null;
        }
        if (fontCache.containsKey(cosname)) {
            return fontCache.get(cosname);
        }
        FontInfo fontInfo = fontList.get(cosname);
        if (fontInfo == null) {
            return null;
        }
        try {
            File fontFile = new File(fontInfo.filePath);
            PDFont font;
            if (fontInfo.ttcIndex >= 0) {
                font = loadTTCFontByIndex(fontFile, fontInfo.ttcIndex);
            } else {
                try {
                    TrueTypeFont ttf = new TTFParser().parse(fontFile);
                    if (!hasUnicodeCmap(ttf)) {
                        font = null;
                    } else {
                        font = PDType0Font.load(currentDocument, fontFile);
                    }
                } catch (IOException e) {
                    font = null;
                }
            }
            if (font != null) {
                fontCache.put(cosname, font);
            }
            return font;
        } catch (Exception e) {
            System.err.println("延迟加载字体失败: " + cosname + ", 文件: " + fontInfo.filePath + ", 错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从TTC文件中加载指定索引的字体，使用 currentDocument。
     *
     * @param ttcFile TTC字体文件
     * @param index 字体索引
     * @return 加载的PDFont对象，如果加载失败返回null
     */
    private static PDFont loadTTCFontByIndex(File ttcFile, int index) {
        TrueTypeCollection ttc = null;
        final PDFont[] result = {null}; // 使用数组来在lambda中修改值
        final int[] currentIndex = {0}; // 使用数组来在lambda中修改值
        try {
            // 使用TrueTypeCollection来解析TTC文件
            ttc = new TrueTypeCollection(ttcFile);
            
            // 使用processAllFonts方法遍历TTC文件中的每一个字体
            ttc.processAllFonts((TrueTypeFont ttf) -> {
                try {
                    if (currentIndex[0] == index) {
                        // 加载前再次确认含 Unicode cmap，避免 save 时子集化报错
                        if (!hasUnicodeCmap(ttf)) {
                            currentIndex[0]++;
                            return;
                        }
                        PDFont font = PDType0Font.load(currentDocument, ttf, true);
                        if (font != null) {
                            result[0] = font;
                        }
                    }
                    currentIndex[0]++;
                } catch (IOException | IllegalArgumentException e) {
                    // 字体加载失败，打印错误信息但继续处理
                    if (currentIndex[0] == index) {
                        System.err.println("加载TTC文件中索引为 " + index + " 的字体失败: " + e.getMessage());
                    }
                    currentIndex[0]++;
                }
            });
        } catch (IOException e) {
            // 加载失败时打印错误信息
            System.err.println("加载TTC文件失败: " + ttcFile.getAbsolutePath() + ", 错误: " + e.getMessage());
        } finally {
            // 仅当未成功加载字体时关闭 TTC；若已返回字体，PDType0Font 仍持有其底层流，关闭会导致 save 时 subset 出现 raf 为 null 的 NPE
            if (ttc != null && result[0] == null) {
                try {
                    ttc.close();
                } catch (IOException e) {
                    System.err.println("关闭TTC文件失败: " + e.getMessage());
                }
            }
        }
        
        return result[0];
    }
    
    /**
     * 检查字符是否在字体中存在
     * 
     * @param font 字体对象
     * @param character 要检查的字符
     * @return 如果字符存在返回true，否则返回false
     */
    private static boolean isCharacterInFont(PDFont font, char character) {
        try {
            // 尝试获取字符的宽度，如果字符不存在会抛出异常
            font.getStringWidth(String.valueOf(character));
            return true;
        } catch (IllegalArgumentException | IOException e) {
            // 字符不存在于字体中或IO异常，认为字符不存在
            return false;
        }
    }
    
    /**
     * 为字符查找合适的字体
     * 按照字体列表顺序查找，找到第一个包含该字符的字体
     * 如果都不包含，返回第一个字体（用于替换为空格）
     * 按 fontList 顺序加载字体（不缓存）
     * 
     * @param character 要查找字体的字符
     * @return 合适的字体对象，如果字体列表为空返回null
     */
    private static PDFont findFontForCharacter(char character) {
        if (fontList.isEmpty()) {
            return null;
        }
        PDFont firstFont = null;
        for (Map.Entry<String, FontInfo> entry : fontList.entrySet()) {
            String cosname = entry.getKey();
            PDFont font = loadFontByCosname(cosname);

            // 如果字体加载失败，跳过
            if (font == null) {
                continue;
            }

            // 保存第一个成功加载的字体
            if (firstFont == null) {
                firstFont = font;
            }

            // 检查字符是否在该字体中存在
            if (isCharacterInFont(font, character)) {
                // 找到包含该字符的字体，返回该字体
                return font;
            }
        }

        // 如果所有字体都不包含该字符，返回第一个字体（用于替换为空格）
        return firstFont;
    }

    /**
     * 为字符查找合适的字体，支持优先字体COSName
     * 如果preferredFontCosName不为null且对应的字体存在，则优先尝试该字体
     * 如果字符在该字体中不存在，则按照字体列表顺序尝试其他字体
     * 如果preferredFontCosName为null或对应字体不存在，则使用原有逻辑
     *
     * @param character 要查找字体的字符
     * @param preferredFontCosName 首选字体的COSName，可以为null
     * @return 合适的字体对象，如果字体列表为空返回null
     */
    private static PDFont findFontForCharacterWithPreference(char character, String preferredFontCosName) {
        if (fontList.isEmpty()) {
            return null;
        }
        if (preferredFontCosName != null) {
            PDFont preferredFont = loadFontByCosname(preferredFontCosName);
            if (preferredFont != null && isCharacterInFont(preferredFont, character)) {
                return preferredFont;
            }
        }
        return findFontForCharacter(character);
    }
    
    /**
     * 给定字符串，逐个字符按照字体列表判断当前字符是否在字体中存在
     * 如果存在则使用当前字体，不存在则继续查看下一个字体
     * 都不存在就使用第一个字体中的空格字符替代
     * 最后将这个字符串按照各字符对应的字体插入PDF中
     * 
     * 单位说明：
     * - x, y 坐标单位：点（point, pt），1点 = 1/72英寸
     * - fontSize 字体大小单位：点（point, pt）
     * - 字符宽度计算：字体单位 / 1000 * 字体大小(点)
     * - 字符间距单位：点（point, pt）
     * 
     * @param contentStream PDF内容流
     * @param text 要插入的文本
     * @param x 起始X坐标，单位：点（point, pt）
     * @param y 起始Y坐标，单位：点（point, pt）
     * @param fontSize 字体大小，单位：点（point, pt）
     * @throws IOException IO异常
     */
    public static void insertTextWithFontMatching(PDPageContentStream contentStream,
                                           String text,
                                           float x,
                                           float y,
                                           float fontSize) throws IOException {
        // 如果字体列表为空，抛出异常
        if (fontList.isEmpty()) {
            throw new IllegalStateException("字体列表为空，请先加载字体");
        }
        
        // 当前文本位置
        float currentX = x;
        
        // 遍历文本中的每个字符
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            
            // 查找适合该字符的字体
            PDFont font = findFontForCharacter(character);
            
            // 如果找不到字体，跳过该字符
            if (font == null) {
                continue;
            }
            
            // 检查字符是否在所有字体中都不存在
            boolean characterExists = isCharacterInFont(font, character);
            
            // 如果字符不存在，使用空格替代
            String charToDraw = characterExists ? String.valueOf(character) : " ";
            
            // 设置当前字体和字体大小
            contentStream.setFont(font, fontSize);
            
            // 计算字符宽度（字体单位转换为点单位）
            // font.getStringWidth() 返回字体单位（1000单位制）
            // 转换为点单位：字符宽度(点) = 字体单位 / 1000 * 字体大小(点)
            float charWidth = font.getStringWidth(charToDraw) / 1000.0f * fontSize;
            
            // 绘制字符
            contentStream.beginText();
            contentStream.newLineAtOffset(currentX, y);
            contentStream.showText(charToDraw);
            contentStream.endText();
            
            // 更新X坐标，移动到下一个字符位置（单位：点）
            currentX += charWidth;
            
            // 添加字符间距（最后一个字符后不需要间距）
            // 注意：由于我们逐个字符绘制，需要在字符之间手动添加间距（单位：点）
            if (i < text.length() - 1 && characterSpacing > 0) {
                currentX += characterSpacing;
            }
        }
    }
    
    /**
     * 使用默认字体大小插入文本
     *
     * @param contentStream PDF内容流
     * @param text 要插入的文本
     * @param x 起始X坐标
     * @param y 起始Y坐标
     * @throws IOException IO异常
     */
    public static void insertTextWithFontMatching(PDPageContentStream contentStream,
                                           String text,
                                           float x,
                                           float y) throws IOException {
        insertTextWithFontMatching(contentStream, text, x, y, defaultFontSize);
    }

    /**
     * 使用指定字体COSName插入文本，支持字体回退机制
     * 先从fontList中查找对应的字体，如果存在就加载使用
     * 如果字符在该字体中不存在，则按照字体列表顺序尝试其他字体
     * 如果指定字体COSName不存在，则按照原有的字体匹配逻辑处理
     *
     * 单位说明：
     * - x, y 坐标单位：点（point, pt），1点 = 1/72英寸
     * - fontSize 字体大小单位：点（point, pt）
     * - 字符宽度计算：字体单位 / 1000 * 字体大小(点)
     * - 字符间距单位：点（point, pt）
     *
     * @param contentStream PDF内容流
     * @param text 要插入的文本
     * @param x 起始X坐标，单位：点（point, pt）
     * @param y 起始Y坐标，单位：点（point, pt）
     * @param fontSize 字体大小，单位：点（point, pt）
     * @param preferredFontCosName 首选字体的COSName，如果为null则使用原有逻辑
     * @throws IOException IO异常
     */
    public static void insertTextWithPreferredFont(PDPageContentStream contentStream,
                                           String text,
                                           float x,
                                           float y,
                                           float fontSize,
                                           String preferredFontCosName) throws IOException {
        // 如果字体列表为空，抛出异常
        if (fontList.isEmpty()) {
            throw new IllegalStateException("字体列表为空，请先加载字体");
        }

        // 当前文本位置
        float currentX = x;

        // 遍历文本中的每个字符
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);

            // 查找适合该字符的字体
            PDFont font = findFontForCharacterWithPreference(character, preferredFontCosName);

            // 如果找不到字体，跳过该字符
            if (font == null) {
                continue;
            }

            // 检查字符是否在找到的字体中存在
            boolean characterExists = isCharacterInFont(font, character);

            // 如果字符不存在，使用空格替代
            String charToDraw = characterExists ? String.valueOf(character) : " ";

            // 设置当前字体和字体大小
            contentStream.setFont(font, fontSize);

            // 计算字符宽度（字体单位转换为点单位）
            // font.getStringWidth() 返回字体单位（1000单位制）
            // 转换为点单位：字符宽度(点) = 字体单位 / 1000 * 字体大小(点)
            float charWidth = font.getStringWidth(charToDraw) / 1000.0f * fontSize;

            // 绘制字符
            contentStream.beginText();
            contentStream.newLineAtOffset(currentX, y);
            contentStream.showText(charToDraw);
            contentStream.endText();

            // 更新X坐标，移动到下一个字符位置（单位：点）
            currentX += charWidth;

            // 添加字符间距（最后一个字符后不需要间距）
            // 注意：由于我们逐个字符绘制，需要在字符之间手动添加间距（单位：点）
            if (i < text.length() - 1 && characterSpacing > 0) {
                currentX += characterSpacing;
            }
        }
    }

    /**
     * 使用指定字体COSName和默认字体大小插入文本
     *
     * @param contentStream PDF内容流
     * @param text 要插入的文本
     * @param x 起始X坐标
     * @param y 起始Y坐标
     * @param preferredFontCosName 首选字体的COSName，如果为null则使用原有逻辑
     * @throws IOException IO异常
     */
    public static void insertTextWithPreferredFont(PDPageContentStream contentStream,
                                           String text,
                                           float x,
                                           float y,
                                           String preferredFontCosName) throws IOException {
        insertTextWithPreferredFont(contentStream, text, x, y, defaultFontSize, preferredFontCosName);
    }
    
    /**
     * 根据给定字符串按照字体列表顺序计算出该字符串在PDF中的长度
     * 每个字符使用其匹配的字体计算宽度
     * 考虑字符间距（character spacing）的影响
     *
     * 单位说明：
     * - 字体内部单位：PDFont.getStringWidth() 返回的是字体单位（font units），通常为1000单位制
     * - 转换公式：字符宽度(点) = font.getStringWidth(字符) / 1000 * fontSize(点)
     * - 最终返回单位：点（point, pt），1点 = 1/72英寸 ≈ 0.3528毫米
     * - 字符间距单位：点（point, pt）
     *
     * @param text 要计算的文本
     * @param fontSize 字体大小，单位：点（point, pt）
     * @return 文本在PDF中的总宽度，单位：点（point, pt），包括字符间距
     */
    public static float calculateTextWidth(String text, float fontSize) {
        return calculateTextWidth(text, fontSize, null);
    }

    /**
     * 根据给定字符串在指定字体下计算出该字符串在PDF中的长度，使用 currentDocument。
     *
     * @param text      要计算的文本
     * @param fontSize  字体大小，单位：点（point, pt）
     * @param fontName  首选字体的COSName，如果为null则使用原有逻辑
     * @return 文本在PDF中的总宽度，单位：点（point, pt），包括字符间距；currentDocument 为 null 时返回 0
     */
    public static float calculateTextWidth(String text, float fontSize, String fontName) {
        if (fontList.isEmpty() || currentDocument == null) {
            return 0.0f;
        }
        float totalWidth = 0.0f;
        int validCharCount = 0;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            PDFont font = findFontForCharacterWithPreference(character, fontName);
            if (font == null) {
                continue;
            }
            boolean characterExists = isCharacterInFont(font, character);
            String charToMeasure = characterExists ? String.valueOf(character) : " ";
            try {
                float charWidth = font.getStringWidth(charToMeasure) / 1000.0f * fontSize;
                totalWidth += charWidth;
                validCharCount++;
            } catch (IOException e) {
                System.err.println("计算字符宽度失败: " + character + ", 错误: " + e.getMessage());
            }
        }
        if (validCharCount > 1 && characterSpacing > 0) {
            totalWidth += characterSpacing * (validCharCount - 1);
        }
        return totalWidth;
    }

    /**
     * 使用默认字体大小计算文本宽度
     * 
     * @param text 要计算的文本
     * @return 文本在PDF中的总宽度，单位：点（point, pt）
     */
    public static float calculateTextWidth(String text) {
        // 调用带字体大小参数的方法，使用默认字体大小
        return calculateTextWidth(text, defaultFontSize);
    }
    
    /**
     * 获取字体列表（cosname 列表，来自 fontList，不缓存已加载的 PDFont）
     *
     * @return 已注册的字体 cosname 列表
     */
    public static List<String> getFontList() {
        return new ArrayList<>(fontList.keySet());
    }

    /**
     * 为 currentDocument 预加载至少一种字体（仅触发加载，不缓存）。
     */
    public static void ensureAtLeastOneFontLoaded() {
        if (currentDocument == null || fontList.isEmpty()) {
            return;
        }
        String firstCosname = fontList.keySet().iterator().next();
        loadFontByCosname(firstCosname);
    }

    /**
     * 清空字体列表与当前 PDF 的字体缓存。
     */
    public static void clearFonts() {
        fontList.clear();
        fontCache.clear();
    }
    
    /**
     * 设置默认字体大小
     * 
     * @param fontSize 字体大小，单位：点（point, pt），1点 = 1/72英寸
     */
    public static void setDefaultFontSize(float fontSize) {
        defaultFontSize = fontSize;
    }
    
    /**
     * 获取默认字体大小
     * 
     * @return 默认字体大小，单位：点（point, pt）
     */
    public static float getDefaultFontSize() {
        return defaultFontSize;
    }
    
    /**
     * 设置字符间距（character spacing）
     * 字符间距会在每个字符之间添加额外的空间
     * 
     * @param characterSpacing 字符间距，单位：点（point, pt），1点 = 1/72英寸，默认值为0
     */
    public static void setCharacterSpacing(float characterSpacing) {
        PDFFontUtils.characterSpacing = characterSpacing;
    }
    
    /**
     * 获取字符间距（character spacing）
     * 
     * @return 字符间距，单位：点（point, pt）
     */
    public static float getCharacterSpacing() {
        return characterSpacing;
    }

}

