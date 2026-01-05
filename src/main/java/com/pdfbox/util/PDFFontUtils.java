package com.pdfbox.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * 字体列表，按优先级排序
     */
    private List<PDFont> fontList;
    
    /**
     * 字体名称到PDFont的映射，用于缓存已加载的字体
     */
    private Map<String, PDFont> fontCache;
    
    /**
     * 当前PDDocument实例
     */
    private PDDocument document;
    
    /**
     * 默认字体大小
     */
    private float defaultFontSize;
    
    /**
     * 字符间距（character spacing），单位：点（point）
     * 字符间距会在每个字符之间添加额外的空间
     * 默认值为0，表示无额外间距
     */
    private float characterSpacing;
    
    /**
     * 构造函数
     * 
     * @param document PDF文档对象
     * @param defaultFontSize 默认字体大小
     */
    public PDFFontUtils(PDDocument document, float defaultFontSize) {
        // 初始化字体列表
        this.fontList = new ArrayList<>();
        // 初始化字体缓存
        this.fontCache = new HashMap<>();
        // 设置PDF文档对象
        this.document = document;
        // 设置默认字体大小
        this.defaultFontSize = defaultFontSize;
        // 初始化字符间距为0（无额外间距）
        this.characterSpacing = 0.0f;
    }
    
    /**
     * 构造函数，使用默认字体大小12
     * 
     * @param document PDF文档对象
     */
    public PDFFontUtils(PDDocument document) {
        this(document, 12.0f);
    }
    
    /**
     * 根据路径懒加载字体文件或路径下的所有字体
     * 如果路径是文件，则加载该字体文件
     * 如果路径是目录，则加载目录下所有支持的字体文件（.ttf, .otf, .ttc）
     * 
     * @param fontPath 字体文件路径或目录路径
     * @return 加载成功的字体列表
     */
    public List<PDFont> loadFontByPath(String fontPath) {
        // 存储加载成功的字体列表
        List<PDFont> loadedFonts = new ArrayList<>();
        
        try {
            // 将字符串路径转换为Path对象
            Path path = Paths.get(fontPath);
            
            // 检查路径是否存在
            if (!Files.exists(path)) {
                System.err.println("路径不存在: " + fontPath);
                return loadedFonts;
            }
            
            // 判断是文件还是目录
            if (Files.isRegularFile(path)) {
                // 如果是文件，直接加载该字体文件
                File fontFile = path.toFile();
                String fileName = fontFile.getName().toLowerCase();
                
                // 如果是TTC文件，需要加载所有字体
                if (fileName.endsWith(".ttc")) {
                    // TTC文件包含多个字体，加载所有字体
                    List<PDFont> ttcFonts = loadTTCFileAll(fontFile);
                    loadedFonts.addAll(ttcFonts);
                } else {
                    // TTF或OTF文件，加载单个字体
                    PDFont font = loadFontFile(fontFile);
                    if (font != null) {
                        loadedFonts.add(font);
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
                            // 如果是TTC文件，加载所有字体
                            if (fileName.endsWith(".ttc")) {
                                List<PDFont> ttcFonts = loadTTCFileAll(file);
                                loadedFonts.addAll(ttcFonts);
                            } else {
                                // TTF或OTF文件，加载单个字体
                                PDFont font = loadFontFile(file);
                                if (font != null) {
                                    loadedFonts.add(font);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 加载失败时打印错误信息
            System.err.println("从路径加载字体失败: " + fontPath + ", 错误: " + e.getMessage());
        }
        
        return loadedFonts;
    }
    
    /**
     * 加载单个字体文件
     * 支持TTF、OTF和TTC格式
     * 
     * @param fontFile 字体文件
     * @return 加载的PDFont对象，如果加载失败返回null（TTC文件返回第一个字体）
     */
    private PDFont loadFontFile(File fontFile) {
        try {
            // 检查缓存中是否已存在该字体
            String fontKey = fontFile.getAbsolutePath();
            if (fontCache.containsKey(fontKey)) {
                PDFont cachedFont = fontCache.get(fontKey);
                // 如果字体列表中不包含该字体，则添加到列表
                if (!fontList.contains(cachedFont)) {
                    fontList.add(cachedFont);
                }
                return cachedFont;
            }
            
            // 检查文件扩展名，判断是否为TTC文件
            String fileName = fontFile.getName().toLowerCase();
            PDFont font;
            
            if (fileName.endsWith(".ttc")) {
                // TTC文件（TrueType Collection），包含多个字体
                // 使用TrueTypeCollection来解析TTC文件
                font = loadTTCFile(fontFile);
            } else {
                // TTF或OTF文件，直接加载
                font = PDType0Font.load(document, fontFile);
            }
            
            // 如果加载成功，添加到缓存和列表
            if (font != null) {
                fontCache.put(fontKey, font);
                // 如果字体列表中不包含该字体，则添加到列表
                if (!fontList.contains(font)) {
                    fontList.add(font);
                }
            }
            
            return font;
        } catch (IOException e) {
            // 加载失败时打印错误信息
            System.err.println("加载字体文件失败: " + fontFile.getAbsolutePath() + ", 错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 加载TTC文件（TrueType Collection）
     * TTC文件包含多个字体，此方法会遍历并加载TTC文件中的每一个字体
     * 所有加载的字体都会被添加到字体列表中
     * 
     * @param ttcFile TTC字体文件
     * @return 加载的PDFont对象（第一个字体），如果加载失败返回null
     */
    private PDFont loadTTCFile(File ttcFile) {
        TrueTypeCollection ttc = null;
        // 存储所有成功加载的字体
        List<PDFont> loadedFonts = new ArrayList<>();
        
        try {
            // 使用TrueTypeCollection来解析TTC文件
            ttc = new TrueTypeCollection(ttcFile);
            
            // 使用processAllFonts方法遍历TTC文件中的每一个字体
            ttc.processAllFonts((TrueTypeFont ttf) -> {
                try {
                    // 将TrueTypeFont转换为PDFont
                    // 使用PDType0Font.load的重载方法，传入TrueTypeFont和是否嵌入标志
                    PDFont font = PDType0Font.load(document, ttf, true);
                    
                    if (font != null) {
                        // 添加到成功加载的字体列表
                        loadedFonts.add(font);
                        
                        // 添加到字体列表（如果列表中不包含该字体）
                        if (!fontList.contains(font)) {
                            fontList.add(font);
                        }
                    }
                } catch (IOException | IllegalArgumentException e) {
                    // 某个字体加载失败，打印错误信息但继续处理其他字体
                    System.err.println("加载TTC文件中的字体失败: " + e.getMessage());
                }
            });
            
            // 检查是否有字体成功加载
            if (loadedFonts.isEmpty()) {
                System.err.println("TTC文件中没有成功加载任何字体: " + ttcFile.getAbsolutePath());
                return null;
            }
            
            // 打印加载信息
            System.out.println("成功从TTC文件加载了 " + loadedFonts.size() + " 个字体: " + ttcFile.getName());
            
            // 返回第一个字体（保持向后兼容）
            return loadedFonts.get(0);
            
        } catch (IOException e) {
            // 加载失败时打印错误信息
            System.err.println("加载TTC文件失败: " + ttcFile.getAbsolutePath() + ", 错误: " + e.getMessage());
            return null;
        } finally {
            // 关闭TrueTypeCollection资源
            if (ttc != null) {
                try {
                    ttc.close();
                } catch (IOException e) {
                    System.err.println("关闭TTC文件失败: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 从TTC文件中加载所有字体
     * 使用processAllFonts方法遍历TTC文件中的每一个字体，不使用字体名称匹配
     * 所有加载的字体都会被添加到字体列表中
     * 
     * @param ttcFile TTC字体文件
     * @return 加载成功的字体列表
     */
    public List<PDFont> loadTTCFileAll(File ttcFile) {
        List<PDFont> fonts = new ArrayList<>();
        TrueTypeCollection ttc = null;
        
        try {
            // 使用TrueTypeCollection来解析TTC文件
            ttc = new TrueTypeCollection(ttcFile);
            
            // 使用processAllFonts方法遍历TTC文件中的每一个字体
            ttc.processAllFonts((TrueTypeFont ttf) -> {
                try {
                    // 将TrueTypeFont转换为PDFont
                    // 使用PDType0Font.load的重载方法，传入TrueTypeFont和是否嵌入标志
                    PDFont font = PDType0Font.load(document, ttf, true);
                    
                    if (font != null) {
                        // 添加到返回的字体列表
                        fonts.add(font);
                        
                        // 添加到字体列表（如果列表中不包含该字体）
                        if (!fontList.contains(font)) {
                            fontList.add(font);
                        }
                    }
                } catch (IOException | IllegalArgumentException e) {
                    // 某个字体加载失败，打印错误信息但继续处理其他字体
                    System.err.println("加载TTC文件中的字体失败: " + e.getMessage());
                }
            });
            
            if (fonts.isEmpty()) {
                System.err.println("TTC文件中没有成功加载任何字体: " + ttcFile.getAbsolutePath());
            } else {
                System.out.println("从TTC文件加载了 " + fonts.size() + " 个字体: " + ttcFile.getName());
            }
        } catch (IOException e) {
            // 加载失败时打印错误信息
            System.err.println("加载TTC文件失败: " + ttcFile.getAbsolutePath() + ", 错误: " + e.getMessage());
        } finally {
            // 关闭TrueTypeCollection资源
            if (ttc != null) {
                try {
                    ttc.close();
                } catch (IOException e) {
                    System.err.println("关闭TTC文件失败: " + e.getMessage());
                }
            }
        }
        
        return fonts;
    }
    
    /**
     * 检查字符是否在字体中存在
     * 
     * @param font 字体对象
     * @param character 要检查的字符
     * @return 如果字符存在返回true，否则返回false
     */
    private boolean isCharacterInFont(PDFont font, char character) {
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
     * 
     * @param character 要查找字体的字符
     * @return 合适的字体对象，如果字体列表为空返回null
     */
    private PDFont findFontForCharacter(char character) {
        // 如果字体列表为空，返回null
        if (fontList.isEmpty()) {
            return null;
        }
        
        // 遍历字体列表，查找包含该字符的字体
        for (PDFont font : fontList) {
            if (isCharacterInFont(font, character)) {
                // 找到包含该字符的字体，返回该字体
                return font;
            }
        }
        
        // 如果所有字体都不包含该字符，返回第一个字体（用于替换为空格）
        return fontList.get(0);
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
    public void insertTextWithFontMatching(PDPageContentStream contentStream, 
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
    public void insertTextWithFontMatching(PDPageContentStream contentStream, 
                                           String text, 
                                           float x, 
                                           float y) throws IOException {
        // 调用带字体大小参数的方法，使用默认字体大小
        insertTextWithFontMatching(contentStream, text, x, y, defaultFontSize);
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
    public float calculateTextWidth(String text, float fontSize) {
        // 如果字体列表为空，返回0
        if (fontList.isEmpty()) {
            return 0.0f;
        }
        
        // 总宽度
        float totalWidth = 0.0f;
        
        // 有效字符计数（用于计算字符间距）
        int validCharCount = 0;
        
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
            String charToMeasure = characterExists ? String.valueOf(character) : " ";
            
            try {
                // 计算字符宽度（字体单位转换为点单位）
                // font.getStringWidth() 返回字体单位（1000单位制）
                // 转换为点单位：字符宽度(点) = 字体单位 / 1000 * 字体大小(点)
                float charWidth = font.getStringWidth(charToMeasure) / 1000.0f * fontSize;
                // 累加到总宽度（单位：点）
                totalWidth += charWidth;
                // 增加有效字符计数
                validCharCount++;
            } catch (IOException e) {
                // IO异常时，字符宽度为0，不计入有效字符
                System.err.println("计算字符宽度失败: " + character + ", 错误: " + e.getMessage());
            }
        }
        
        // 添加字符间距：如果有n个有效字符，则有(n-1)个字符间距
        // 字符间距在每两个字符之间添加额外空间（单位：点）
        if (validCharCount > 1 && characterSpacing > 0) {
            totalWidth += characterSpacing * (validCharCount - 1);
        }
        
        // 返回总宽度，单位：点（point, pt）
        return totalWidth;
    }
    
    /**
     * 使用默认字体大小计算文本宽度
     * 
     * @param text 要计算的文本
     * @return 文本在PDF中的总宽度，单位：点（point, pt）
     */
    public float calculateTextWidth(String text) {
        // 调用带字体大小参数的方法，使用默认字体大小
        return calculateTextWidth(text, defaultFontSize);
    }
    
    /**
     * 获取字体列表
     * 
     * @return 字体列表
     */
    public List<PDFont> getFontList() {
        return new ArrayList<>(fontList);
    }
    
    /**
     * 清空字体列表和缓存
     */
    public void clearFonts() {
        fontList.clear();
        fontCache.clear();
    }
    
    /**
     * 设置默认字体大小
     * 
     * @param fontSize 字体大小，单位：点（point, pt），1点 = 1/72英寸
     */
    public void setDefaultFontSize(float fontSize) {
        this.defaultFontSize = fontSize;
    }
    
    /**
     * 获取默认字体大小
     * 
     * @return 默认字体大小，单位：点（point, pt）
     */
    public float getDefaultFontSize() {
        return defaultFontSize;
    }
    
    /**
     * 设置字符间距（character spacing）
     * 字符间距会在每个字符之间添加额外的空间
     * 
     * @param characterSpacing 字符间距，单位：点（point, pt），1点 = 1/72英寸，默认值为0
     */
    public void setCharacterSpacing(float characterSpacing) {
        this.characterSpacing = characterSpacing;
    }
    
    /**
     * 获取字符间距（character spacing）
     * 
     * @return 字符间距，单位：点（point, pt）
     */
    public float getCharacterSpacing() {
        return characterSpacing;
    }
}

