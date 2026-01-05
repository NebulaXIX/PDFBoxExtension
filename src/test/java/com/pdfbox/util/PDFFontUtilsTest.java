package com.pdfbox.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 * PDFFontUtils工具类测试类
 * 包含多个场景的测试用例
 *
 * @author PDFBox Extension
 * @version 1.0.0
 */
public class PDFFontUtilsTest {

    /**
     * PDF文档对象
     */
    private PDDocument document;

    /**
     * PDF字体工具实例
     */
    private PDFFontUtils fontUtils;

    /**
     * 测试前初始化
     * 创建新的PDF文档和字体工具实例
     */
    @Before
    public void setUp() {
        try {
            // 创建新的PDF文档
            document = new PDDocument();
            // 创建字体工具实例，使用默认字体大小12
            fontUtils = new PDFFontUtils(document, 12.0f);
        } catch (Exception e) {
            fail("初始化失败: " + e.getMessage());
        }
    }

    /**
     * 测试后清理
     * 关闭PDF文档
     */
    @After
    public void tearDown() {
        try {
            // 关闭PDF文档
            if (document != null) {
                document.close();
            }
        } catch (IOException e) {
            System.err.println("关闭文档失败: " + e.getMessage());
        }
    }

    /**
     * 测试场景1：根据路径加载单个字体文件
     * 注意：此测试需要实际的字体文件，如果不存在则跳过
     */
    @Test
    public void testLoadFontByPath_SingleFile() {
        // 尝试从Windows系统字体目录加载字体（如果存在）
        String fontPath = "C:\\Windows\\Fonts\\arial.ttf";
        File fontFile = new File(fontPath);

        if (fontFile.exists()) {
            // 如果字体文件存在，尝试加载
            List<PDFont> fonts = fontUtils.loadFontByPath(fontPath);
            // 验证至少加载了一个字体
            assertTrue("应该至少加载一个字体", fonts.size() >= 1);
            // 验证字体列表包含加载的字体
            assertTrue("字体列表应该包含加载的字体",
                    fontUtils.getFontList().size() >= fonts.size());
        } else {
            // 如果字体文件不存在，跳过测试
            System.out.println("跳过测试：字体文件不存在 - " + fontPath);
        }
    }

    /**
     * 测试场景5：根据路径加载目录下的所有字体
     * 注意：此测试需要实际的字体目录，如果不存在则跳过
     */
    @Test
    public void testLoadFontByPath_Directory() {
        // 尝试从Windows系统字体目录加载所有字体（如果存在）
        String fontDir = "C:\\Windows\\Fonts";
        File dir = new File(fontDir);

        if (dir.exists() && dir.isDirectory()) {
            // 如果目录存在，尝试加载
            List<PDFont> fonts = fontUtils.loadFontByPath(fontDir);
            // 验证加载了至少一个字体（如果目录中有字体文件）
            System.out.println("从目录加载了 " + fonts.size() + " 个字体");
            // 验证字体列表包含加载的字体
            assertTrue("字体列表应该包含加载的字体",
                    fontUtils.getFontList().size() >= fonts.size());
        } else {
            // 如果目录不存在，跳过测试
            System.out.println("跳过测试：字体目录不存在 - " + fontDir);
        }
    }

    /**
     * 测试场景6：加载不存在的路径
     * 测试加载不存在路径时的处理
     */
    @Test
    public void testLoadFontByPath_InvalidPath() {
        // 尝试加载不存在的路径
        List<PDFont> fonts = fontUtils.loadFontByPath("C:\\NonExistent\\Path\\font.ttf");
        // 验证返回空列表
        assertTrue("不存在的路径应该返回空列表", fonts.isEmpty());
    }

    /**
     * 测试场景7：计算纯英文文本宽度
     * 测试使用字体文件计算英文文本宽度
     */
    @Test
    public void testCalculateTextWidth_English() {
        // 尝试从Windows系统字体目录加载字体（如果存在）
        String fontPath = "C:\\Windows\\Fonts\\arial.ttf";
        File fontFile = new File(fontPath);

        if (fontFile.exists()) {
            // 如果字体文件存在，加载字体
            fontUtils.loadFontByPath(fontPath);

            // 计算英文文本宽度
            String text = "Hello World";
            float width = fontUtils.calculateTextWidth(text, 12.0f);

            // 验证宽度大于0
            assertTrue("英文文本宽度应该大于0", width > 0);
        } else {
            // 如果字体文件不存在，跳过测试
            System.out.println("跳过测试：字体文件不存在 - " + fontPath);
        }
    }

    /**
     * 测试场景8：计算中英文混合文本宽度
     * 测试混合文本的宽度计算（支持TTC格式）
     */
    @Test
    public void testCalculateTextWidth_Mixed() {
        // 尝试从Windows系统字体目录加载支持中文的字体（包括TTC格式）
        String[] fontPaths = {
                "C:\\Windows\\Fonts\\msyh.ttf",      // 微软雅黑
                "C:\\Windows\\Fonts\\simhei.ttf",    // 黑体
                "C:\\Windows\\Fonts\\simsun.ttf",    // 宋体（TTF格式）
                "C:\\Windows\\Fonts\\simsun.ttc"     // 宋体（TTC格式，现在支持）
        };

        boolean fontLoaded = false;
        for (String fontPath : fontPaths) {
            File fontFile = new File(fontPath);
            if (fontFile.exists()) {
                // 尝试加载字体（现在支持TTC格式）
                fontUtils.loadFontByPath(fontPath);

                // 检查字体是否成功加载（字体列表不为空）
                if (!fontUtils.getFontList().isEmpty()) {
                    fontLoaded = true;
                    // 计算中英文混合文本宽度
                    String text = "Hello 世界";
                    float width = fontUtils.calculateTextWidth(text, 12.0f);

                    // 验证宽度大于0
                    assertTrue("混合文本宽度应该大于0", width > 0);
                    break;
                }
            }
        }

        if (!fontLoaded) {
            System.out.println("跳过测试：没有找到可用的中文字体文件");
        }
    }

    /**
     * 测试场景9：计算空字符串宽度
     * 测试空字符串的宽度计算
     */
    @Test
    public void testCalculateTextWidth_Empty() {
        // 尝试从Windows系统字体目录加载字体（如果存在）
        String fontPath = "C:\\Windows\\Fonts\\arial.ttf";
        File fontFile = new File(fontPath);

        if (fontFile.exists()) {
            // 如果字体文件存在，加载字体
            fontUtils.loadFontByPath(fontPath);

            // 计算空字符串宽度
            String text = "";
            float width = fontUtils.calculateTextWidth(text, 12.0f);

            // 验证宽度为0
            assertEquals("空字符串宽度应该为0", 0.0f, width, 0.001f);
        } else {
            // 如果字体文件不存在，跳过测试
            System.out.println("跳过测试：字体文件不存在 - " + fontPath);
        }
    }

    /**
     * 测试场景10：插入纯英文文本到PDF
     * 测试英文文本的插入
     */
    @Test
    public void testInsertTextWithFontMatching_English() {
        try {
            // 尝试从Windows系统字体目录加载字体（如果存在）
            String fontPath = "C:\\Windows\\Fonts\\arial.ttf";
            File fontFile = new File(fontPath);

            if (fontFile.exists()) {
                // 创建PDF页面
                PDPage page = new PDPage();
                document.addPage(page);

                // 加载字体
                fontUtils.loadFontByPath(fontPath);

                // 创建内容流
                PDPageContentStream contentStream = new PDPageContentStream(document, page);

                // 插入文本
                fontUtils.insertTextWithFontMatching(contentStream, "Hello World", 50, 700, 12.0f);

                // 关闭内容流
                contentStream.close();
            } else {
                // 如果字体文件不存在，跳过测试
                System.out.println("跳过测试：字体文件不存在 - " + fontPath);
            }
        } catch (IOException e) {
            fail("插入文本失败: " + e.getMessage());
        }
    }

    /**
     * 测试场景12：插入中英文混合文本到PDF
     * 测试混合文本的插入（支持TTC格式）
     */
    @Test
    public void testInsertTextWithFontMatching_Mixed() {
        try {
            // 尝试从Windows系统字体目录加载支持中文的字体（包括TTC格式）
            String[] fontPaths = {
                    "C:\\Windows\\Fonts\\msyh.ttf",      // 微软雅黑
                    "C:\\Windows\\Fonts\\simhei.ttf",    // 黑体
                    "C:\\Windows\\Fonts\\simsun.ttf",    // 宋体（TTF格式）
                    "C:\\Windows\\Fonts\\simsun.ttc"     // 宋体（TTC格式，现在支持）
            };

            boolean fontLoaded = false;
            for (String fontPath : fontPaths) {
                File fontFile = new File(fontPath);
                if (fontFile.exists()) {
                    // 尝试加载字体（现在支持TTC格式）
                    fontUtils.loadFontByPath(fontPath);

                    // 检查字体是否成功加载（字体列表不为空）
                    if (!fontUtils.getFontList().isEmpty()) {
                        fontLoaded = true;
                        // 创建PDF页面
                        PDPage page = new PDPage();
                        document.addPage(page);

                        // 创建内容流
                        PDPageContentStream contentStream = new PDPageContentStream(document, page);

                        // 插入中英文混合文本
                        fontUtils.insertTextWithFontMatching(contentStream, "Hello 世界", 50, 700, 12.0f);

                        // 关闭内容流
                        contentStream.close();
                        break;
                    }
                }
            }

            if (!fontLoaded) {
                System.out.println("跳过测试：没有找到可用的中文字体文件");
            }
        } catch (IOException e) {
            fail("插入文本失败: " + e.getMessage());
        }
    }

    /**
     * 测试场景13：清空字体列表
     * 测试清空字体列表和缓存的功能
     */
    @Test
    public void testClearFonts() {
        // 尝试从Windows系统字体目录加载字体（如果存在）
        String fontPath = "C:\\Windows\\Fonts\\arial.ttf";
        File fontFile = new File(fontPath);

        if (fontFile.exists()) {
            // 加载字体
            fontUtils.loadFontByPath(fontPath);

            // 验证字体列表不为空
            assertFalse("字体列表不应该为空", fontUtils.getFontList().isEmpty());

            // 清空字体
            fontUtils.clearFonts();

            // 验证字体列表为空
            assertTrue("字体列表应该为空", fontUtils.getFontList().isEmpty());
        } else {
            // 如果字体文件不存在，跳过测试
            System.out.println("跳过测试：字体文件不存在 - " + fontPath);
        }
    }

    /**
     * 测试场景14：测试字体优先级
     * 测试多个字体的加载顺序和优先级
     */
    @Test
    public void testFontPriority() {
        // 尝试从Windows系统字体目录加载多个字体（如果存在）
        String fontPath1 = "C:\\Windows\\Fonts\\arial.ttf";
        String fontPath2 = "C:\\Windows\\Fonts\\times.ttf";
        String fontPath3 = "C:\\Windows\\Fonts\\cour.ttf";

        File fontFile1 = new File(fontPath1);
        File fontFile2 = new File(fontPath2);
        File fontFile3 = new File(fontPath3);

        if (fontFile1.exists() && fontFile2.exists() && fontFile3.exists()) {
            // 按顺序加载多个字体
            fontUtils.loadFontByPath(fontPath1);
            fontUtils.loadFontByPath(fontPath2);
            fontUtils.loadFontByPath(fontPath3);

            // 获取字体列表
            List<PDFont> fontList = fontUtils.getFontList();

            // 验证字体列表包含至少3个字体
            assertTrue("字体列表应该包含至少3个字体", fontList.size() >= 3);

            // 验证字体顺序（第一个加载的字体应该在第一位）
            // 注意：由于字体匹配逻辑，第一个字体会被优先使用
        } else {
            // 如果字体文件不存在，跳过测试
            System.out.println("跳过测试：字体文件不存在");
        }
    }

    /**
     * 测试场景17：保存PDF文档
     * 测试创建和保存PDF文档
     */
    @Test
    public void testSavePDF() {
        try {
            // 尝试从Windows系统字体目录加载字体（如果存在）
            String fontPath = "C:\\Windows\\Fonts";
            File fontFile = new File(fontPath);

            if (fontFile.exists()) {
                // 创建PDF页面
                PDPage page = new PDPage();
                document.addPage(page);

                // 加载字体
                fontUtils.loadFontByPath(fontPath);

                // 创建内容流
                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                System.out.println("混合长度:" + fontUtils.calculateTextWidth("Test PDF  ݢ ݣ"));
                // 插入文本
                fontUtils.insertTextWithFontMatching(contentStream, "Test PDF  ݢ ݣ", 50, 700, 12.0f);

                // 关闭内容流
                contentStream.close();

                // 保存PDF到临时文件
                File tempFile = new File("test.pdf");
                tempFile.createNewFile();
                document.save(tempFile);

                // 验证文件存在
                assertTrue("PDF文件应该被创建", tempFile.exists());
                // 验证文件大小大于0
                assertTrue("PDF文件大小应该大于0", tempFile.length() > 0);

                // 清理临时文件
                // tempFile.delete();
            } else {
                // 如果字体文件不存在，跳过测试
                System.out.println("跳过测试：字体文件不存在 - " + fontPath);
            }
        } catch (IOException e) {
            fail("保存PDF失败: " + e.getMessage());
        }
    }

    /**
     * 测试场景18：精确验证单个字符宽度计算
     * 通过直接使用PDFont计算宽度，与工具类计算结果对比
     */
    @Test
    public void testCalculateTextWidth_SingleCharacter_Precise() {
        // 尝试从Windows系统字体目录加载字体（如果存在）
        String fontPath = "C:\\Windows\\Fonts\\arial.ttf";
        File fontFile = new File(fontPath);

        if (fontFile.exists()) {
            try {
                // 加载字体
                List<PDFont> fonts = fontUtils.loadFontByPath(fontPath);
                if (fonts.isEmpty()) {
                    System.out.println("跳过测试：字体加载失败");
                    return;
                }

                PDFont font = fonts.get(0);
                float fontSize = 12.0f;

                // 测试单个字符
                String[] testChars = {"A", "B", "C", "a", "b", "c", "1", "2", "3", " "};

                for (String testChar : testChars) {
                    // 使用工具类计算宽度
                    float calculatedWidth = fontUtils.calculateTextWidth(testChar, fontSize);

                    // 使用PDFont直接计算宽度（标准方法）
                    float expectedWidth = font.getStringWidth(testChar) / 1000.0f * fontSize;

                    // 验证计算结果（允许0.001的误差）
                    assertEquals("字符 '" + testChar + "' 的宽度计算不正确",
                            expectedWidth, calculatedWidth, 0.001f);
                }
            } catch (IOException e) {
                fail("计算宽度失败: " + e.getMessage());
            }
        } else {
            System.out.println("跳过测试：字体文件不存在 - " + fontPath);
        }
    }

    /**
     * 测试场景19：精确验证多个字符宽度累加
     * 验证多个字符的总宽度等于各字符宽度之和
     */
    @Test
    public void testCalculateTextWidth_MultipleCharacters_Precise() {
        // 尝试从Windows系统字体目录加载字体（如果存在）
        String fontPath = "C:\\Windows\\Fonts\\arial.ttf";
        File fontFile = new File(fontPath);

        if (fontFile.exists()) {
            try {
                // 加载字体
                List<PDFont> fonts = fontUtils.loadFontByPath(fontPath);
                if (fonts.isEmpty()) {
                    System.out.println("跳过测试：字体加载失败");
                    return;
                }

                float fontSize = 12.0f;

                // 测试字符串
                String[] testStrings = {"ABC", "Hello", "12345", "A B C", "Test String"};

                for (String testString : testStrings) {
                    // 使用工具类计算总宽度
                    float totalWidth = fontUtils.calculateTextWidth(testString, fontSize);

                    // 手动累加每个字符的宽度
                    float manualSum = 0.0f;
                    for (int i = 0; i < testString.length(); i++) {
                        char ch = testString.charAt(i);
                        PDFont charFont = fontUtils.getFontList().get(0);
                        boolean exists = false;
                        try {
                            charFont.getStringWidth(String.valueOf(ch));
                            exists = true;
                        } catch (Exception e) {
                            // 字符不存在
                        }
                        String charToMeasure = exists ? String.valueOf(ch) : " ";
                        manualSum += charFont.getStringWidth(charToMeasure) / 1000.0f * fontSize;
                    }

                    // 验证总宽度等于各字符宽度之和（允许0.01的误差，因为可能有字体匹配差异）
                    assertEquals("字符串 '" + testString + "' 的宽度计算不正确",
                            manualSum, totalWidth, 0.01f);
                }
            } catch (IOException e) {
                fail("计算宽度失败: " + e.getMessage());
            }
        } else {
            System.out.println("跳过测试：字体文件不存在 - " + fontPath);
        }
    }

    /**
     * 测试场景20：精确验证不同字体大小的宽度计算
     * 验证宽度与字体大小成正比
     */
    @Test
    public void testCalculateTextWidth_DifferentFontSizes_Precise() {
        // 尝试从Windows系统字体目录加载字体（如果存在）
        String fontPath = "C:\\Windows\\Fonts\\arial.ttf";
        File fontFile = new File(fontPath);

        if (fontFile.exists()) {
            try {
                // 加载字体
                List<PDFont> fonts = fontUtils.loadFontByPath(fontPath);
                if (fonts.isEmpty()) {
                    System.out.println("跳过测试：字体加载失败");
                    return;
                }

                PDFont font = fonts.get(0);
                String testText = "Hello World";

                // 测试不同的字体大小
                float[] fontSizes = {10.0f, 12.0f, 14.0f, 16.0f, 20.0f, 24.0f};

                for (float fontSize : fontSizes) {
                    // 使用工具类计算宽度
                    float calculatedWidth = fontUtils.calculateTextWidth(testText, fontSize);

                    // 使用PDFont直接计算宽度（标准方法）
                    // 注意：由于工具类可能使用字体匹配，我们需要手动计算每个字符
                    float expectedWidth = 0.0f;
                    for (int i = 0; i < testText.length(); i++) {
                        char ch = testText.charAt(i);
                        boolean exists = false;
                        try {
                            font.getStringWidth(String.valueOf(ch));
                            exists = true;
                        } catch (Exception e) {
                            // 字符不存在
                        }
                        String charToMeasure = exists ? String.valueOf(ch) : " ";
                        expectedWidth += font.getStringWidth(charToMeasure) / 1000.0f * fontSize;
                    }

                    // 验证计算结果（允许0.01的误差）
                    assertEquals("字体大小 " + fontSize + " 时的宽度计算不正确",
                            expectedWidth, calculatedWidth, 0.01f);
                }

                // 验证宽度与字体大小成正比（12pt应该是10pt的1.2倍）
                float width10 = fontUtils.calculateTextWidth(testText, 10.0f);
                float width12 = fontUtils.calculateTextWidth(testText, 12.0f);
                float width20 = fontUtils.calculateTextWidth(testText, 20.0f);

                // 验证比例关系（允许0.01的误差）
                assertEquals("宽度比例不正确（12pt应该是10pt的1.2倍）",
                        width10 * 1.2f, width12, 0.01f);
                assertEquals("宽度比例不正确（20pt应该是10pt的2倍）",
                        width10 * 2.0f, width20, 0.01f);
            } catch (IOException e) {
                fail("计算宽度失败: " + e.getMessage());
            }
        } else {
            System.out.println("跳过测试：字体文件不存在 - " + fontPath);
        }
    }

    /**
     * 测试场景21：精确验证中英文混合文本宽度计算
     * 验证混合文本中每个字符使用正确字体计算宽度
     */
    @Test
    public void testCalculateTextWidth_MixedText_Precise() {
        // 尝试从Windows系统字体目录加载支持中文的字体（包括TTC格式）
        String[] fontPaths = {
                "C:\\Windows\\Fonts\\msyh.ttf",      // 微软雅黑
                "C:\\Windows\\Fonts\\simhei.ttf",    // 黑体
                "C:\\Windows\\Fonts\\simsun.ttf",    // 宋体（TTF格式）
                "C:\\Windows\\Fonts\\simsun.ttc"     // 宋体（TTC格式）
        };

        boolean fontLoaded = false;
        for (String fontPath : fontPaths) {
            File fontFile = new File(fontPath);
            if (fontFile.exists()) {
                try {
                    // 加载字体
                    fontUtils.loadFontByPath(fontPath);

                    if (!fontUtils.getFontList().isEmpty()) {
                        fontLoaded = true;
                        float fontSize = 12.0f;

                        // 测试混合文本
                        String[] testStrings = {
                                "Hello世界",
                                "测试Test",
                                "ABC中文123",
                                "Hello 世界 Test 测试"
                        };

                        for (String testString : testStrings) {
                            // 使用工具类计算宽度
                            float calculatedWidth = fontUtils.calculateTextWidth(testString, fontSize);

                            // 手动计算：遍历每个字符，找到匹配的字体并累加宽度
                            float manualSum = 0.0f;
                            List<PDFont> fontList = fontUtils.getFontList();

                            for (int i = 0; i < testString.length(); i++) {
                                char ch = testString.charAt(i);

                                // 查找包含该字符的字体
                                PDFont matchedFont = null;
                                for (PDFont font : fontList) {
                                    try {
                                        font.getStringWidth(String.valueOf(ch));
                                        matchedFont = font;
                                        break;
                                    } catch (Exception e) {
                                        // 字符不存在于该字体
                                    }
                                }

                                // 如果找不到匹配字体，使用第一个字体和空格
                                if (matchedFont == null) {
                                    matchedFont = fontList.get(0);
                                }

                                // 检查字符是否存在
                                boolean exists = false;
                                try {
                                    matchedFont.getStringWidth(String.valueOf(ch));
                                    exists = true;
                                } catch (Exception e) {
                                    // 字符不存在
                                }

                                String charToMeasure = exists ? String.valueOf(ch) : " ";
                                manualSum += matchedFont.getStringWidth(charToMeasure) / 1000.0f * fontSize;
                            }

                            // 验证计算结果（允许0.02的误差，因为字体匹配可能有差异）
                            assertEquals("混合文本 '" + testString + "' 的宽度计算不正确",
                                    manualSum, calculatedWidth, 0.02f);
                        }

                        break;
                    }
                } catch (IOException e) {
                    System.err.println("计算宽度失败: " + e.getMessage());
                }
            }
        }

        if (!fontLoaded) {
            System.out.println("跳过测试：没有找到可用的中文字体文件");
        }
    }

    /**
     * 测试场景22：精确验证宽度计算公式
     * 验证宽度 = font.getStringWidth(text) / 1000 * fontSize
     */
    @Test
    public void testCalculateTextWidth_Formula_Precise() {
        // 尝试从Windows系统字体目录加载字体（如果存在）
        String fontPath = "C:\\Windows\\Fonts\\arial.ttf";
        File fontFile = new File(fontPath);

        if (fontFile.exists()) {
            try {
                // 加载字体
                List<PDFont> fonts = fontUtils.loadFontByPath(fontPath);
                if (fonts.isEmpty()) {
                    System.out.println("跳过测试：字体加载失败");
                    return;
                }

                PDFont font = fonts.get(0);
                String testText = "Hello";
                float fontSize = 12.0f;

                // 使用PDFont直接计算（标准方法）
                float fontWidthInUnits = font.getStringWidth(testText);
                float expectedWidth = fontWidthInUnits / 1000.0f * fontSize;

                // 使用工具类计算
                float calculatedWidth = fontUtils.calculateTextWidth(testText, fontSize);

                // 验证公式：宽度 = font.getStringWidth(text) / 1000 * fontSize
                // 注意：工具类可能使用字体匹配，所以可能不完全相等
                // 但应该非常接近（允许0.01的误差）
                assertEquals("宽度计算公式验证失败",
                        expectedWidth, calculatedWidth, 0.01f);

                // 打印详细信息用于调试
                System.out.println("测试文本: " + testText);
                System.out.println("字体大小: " + fontSize + "pt");
                System.out.println("字体单位宽度: " + fontWidthInUnits);
                System.out.println("预期宽度: " + expectedWidth + "pt");
                System.out.println("计算宽度: " + calculatedWidth + "pt");
                System.out.println("差异: " + Math.abs(expectedWidth - calculatedWidth) + "pt");

            } catch (IOException e) {
                fail("计算宽度失败: " + e.getMessage());
            }
        } else {
            System.out.println("跳过测试：字体文件不存在 - " + fontPath);
        }
    }

    /**
     * 测试场景23：验证空字符串和空格字符串的宽度
     * 空字符串宽度应为0，空格字符串宽度应大于0
     */
    @Test
    public void testCalculateTextWidth_EmptyAndSpace_Precise() {
        // 尝试从Windows系统字体目录加载字体（如果存在）
        String fontPath = "C:\\Windows\\Fonts\\arial.ttf";
        File fontFile = new File(fontPath);

        if (fontFile.exists()) {
            try {
                // 加载字体
                List<PDFont> fonts = fontUtils.loadFontByPath(fontPath);
                if (fonts.isEmpty()) {
                    System.out.println("跳过测试：字体加载失败");
                    return;
                }

                PDFont font = fonts.get(0);
                float fontSize = 12.0f;

                // 测试空字符串
                float emptyWidth = fontUtils.calculateTextWidth("", fontSize);
                assertEquals("空字符串宽度应该为0", 0.0f, emptyWidth, 0.001f);

                // 测试单个空格
                float spaceWidth = fontUtils.calculateTextWidth(" ", fontSize);
                float expectedSpaceWidth = font.getStringWidth(" ") / 1000.0f * fontSize;
                assertEquals("空格宽度计算不正确", expectedSpaceWidth, spaceWidth, 0.001f);
                assertTrue("空格宽度应该大于0", spaceWidth > 0);

                // 测试多个空格
                float multipleSpacesWidth = fontUtils.calculateTextWidth("   ", fontSize);
                float expectedMultipleSpacesWidth = font.getStringWidth("   ") / 1000.0f * fontSize;
                assertEquals("多个空格宽度计算不正确",
                        expectedMultipleSpacesWidth, multipleSpacesWidth, 0.001f);
                assertEquals("多个空格宽度应该是单个空格的3倍",
                        spaceWidth * 3, multipleSpacesWidth, 0.01f);

            } catch (IOException e) {
                fail("计算宽度失败: " + e.getMessage());
            }
        } else {
            System.out.println("跳过测试：字体文件不存在 - " + fontPath);
        }
    }

    /**
     * 测试场景24：多字体混合长度校验
     * 验证当加载多个字体时，文本中不同字符使用不同字体计算宽度，总宽度计算是否正确
     */
    @Test
    public void testCalculateTextWidth_MultipleFonts_Precise() {
        // 尝试从Windows系统字体目录加载多个字体（如果存在）
        String[] fontPaths = {
                "C:\\Windows\\Fonts\\arial.ttf",      // Arial - 支持英文
                "C:\\Windows\\Fonts\\times.ttf",      // Times New Roman - 支持英文
                "C:\\Windows\\Fonts\\cour.ttf",       // Courier New - 支持英文
                "C:\\Windows\\Fonts\\msyh.ttf",       // 微软雅黑 - 支持中文
                "C:\\Windows\\Fonts\\simhei.ttf",     // 黑体 - 支持中文
                "C:\\Windows\\Fonts\\simsun.ttc"      // 宋体 - 支持中文（TTC格式）
        };

        // 检查哪些字体文件存在
        List<String> availableFonts = new ArrayList<>();
        for (String fontPath : fontPaths) {
            File fontFile = new File(fontPath);
            if (fontFile.exists()) {
                availableFonts.add(fontPath);
            }
        }

        if (availableFonts.size() < 2) {
            System.out.println("跳过测试：需要至少2个字体文件，但只找到 " + availableFonts.size() + " 个");
            return;
        }

        try {
            // 清空之前的字体
            fontUtils.clearFonts();

            // 加载多个字体
            int loadedCount = 0;
            for (String fontPath : availableFonts) {
                List<PDFont> fonts = fontUtils.loadFontByPath(fontPath);
                if (!fonts.isEmpty()) {
                    loadedCount++;
                }
            }

            if (loadedCount < 2) {
                System.out.println("跳过测试：成功加载的字体少于2个");
                return;
            }

            List<PDFont> fontList = fontUtils.getFontList();
            System.out.println("成功加载了 " + fontList.size() + " 个字体");

            float fontSize = 12.0f;

            // 测试混合文本（包含英文、中文、数字等，会匹配到不同字体）
            String[] testStrings = {
                    "Hello World 123",           // 英文和数字
                    "测试 Test 123",              // 中文、英文、数字
                    "ABC 中文 123 XYZ",          // 混合文本
                    "Hello 世界 Test 测试 123"    // 复杂混合文本
            };

            for (String testString : testStrings) {
                // 使用工具类计算总宽度
                float calculatedWidth = fontUtils.calculateTextWidth(testString, fontSize);

                // 手动计算：遍历每个字符，找到匹配的字体并累加宽度
                float manualSum = 0.0f;

                for (int i = 0; i < testString.length(); i++) {
                    char ch = testString.charAt(i);

                    // 查找包含该字符的字体（按字体列表顺序）
                    PDFont matchedFont = null;
                    for (PDFont font : fontList) {
                        try {
                            font.getStringWidth(String.valueOf(ch));
                            matchedFont = font;
                            break; // 找到第一个包含该字符的字体
                        } catch (IllegalArgumentException | IOException e) {
                            // 字符不存在于该字体，继续查找下一个字体
                        }
                    }

                    // 如果找不到匹配字体，使用第一个字体和空格
                    if (matchedFont == null) {
                        matchedFont = fontList.get(0);
                    }

                    // 检查字符是否存在于匹配的字体中
                    boolean exists = false;
                    try {
                        matchedFont.getStringWidth(String.valueOf(ch));
                        exists = true;
                    } catch (IllegalArgumentException | IOException e) {
                        // 字符不存在
                    }

                    // 计算字符宽度
                    String charToMeasure = exists ? String.valueOf(ch) : " ";
                    float charWidth = matchedFont.getStringWidth(charToMeasure) / 1000.0f * fontSize;
                    manualSum += charWidth;
                }

                // 验证计算结果（允许0.02的误差，因为字体匹配可能有细微差异）
                assertEquals("多字体混合文本 '" + testString + "' 的宽度计算不正确",
                        manualSum, calculatedWidth, 0.02f);

                // 打印详细信息用于调试
                System.out.println("测试文本: " + testString);
                System.out.println("  工具类计算宽度: " + calculatedWidth + "pt");
                System.out.println("  手动计算宽度: " + manualSum + "pt");
                System.out.println("  差异: " + Math.abs(manualSum - calculatedWidth) + "pt");
            }

            // 额外验证：测试单个字符在不同字体中的宽度
            // 如果多个字体都包含同一个字符，应该使用第一个匹配的字体
            if (fontList.size() >= 2) {
                char testChar = 'A';
                PDFont firstFont = fontList.get(0);
                PDFont secondFont = fontList.get(1);

                boolean firstHasChar = false;
                boolean secondHasChar = false;

                try {
                    firstFont.getStringWidth(String.valueOf(testChar));
                    firstHasChar = true;
                } catch (Exception e) {
                    // 字符不存在
                }

                try {
                    secondFont.getStringWidth(String.valueOf(testChar));
                    secondHasChar = true;
                } catch (Exception e) {
                    // 字符不存在
                }

                // 如果两个字体都包含该字符，验证使用第一个字体
                if (firstHasChar && secondHasChar) {
                    float singleCharWidth = fontUtils.calculateTextWidth(String.valueOf(testChar), fontSize);
                    float expectedWidth = firstFont.getStringWidth(String.valueOf(testChar)) / 1000.0f * fontSize;

                    assertEquals("单个字符 '" + testChar + "' 应该使用第一个匹配的字体",
                            expectedWidth, singleCharWidth, 0.001f);

                    System.out.println("验证：字符 '" + testChar + "' 使用第一个匹配字体，宽度: " + singleCharWidth + "pt");
                }
            }

        } catch (IOException e) {
            fail("多字体混合长度校验失败: " + e.getMessage());
        }
    }

    /**
     * 测试场景25：中文、柬埔寨语、英文、法文、俄文混合多字体测试
     * 验证多种语言字符混合时，每个字符能正确匹配到支持该字符的字体，宽度计算正确
     */
    @Test
    public void testCalculateTextWidth_MultiLanguage_Precise() {
        // 尝试加载支持多种语言的字体
        // 注意：Windows系统可能没有专门的柬埔寨语字体，但某些Unicode字体可能支持
        String[] fontPaths = {
                "C:\\Windows\\Fonts\\arial.ttf",       // Arial - 支持英文、法文、俄文
                "C:\\Windows\\Fonts\\times.ttf",       // Times New Roman - 支持英文、法文、俄文
                "C:\\Windows\\Fonts\\msyh.ttf",        // 微软雅黑 - 支持中文、英文
                "C:\\Windows\\Fonts\\simhei.ttf",      // 黑体 - 支持中文
                "C:\\Windows\\Fonts\\simsun.ttc",      // 宋体 - 支持中文（TTC格式）
                "C:\\Windows\\Fonts\\calibri.ttf",     // Calibri - 支持多种语言
                "C:\\Windows\\Fonts\\tahoma.ttf",      // Tahoma - 支持多种语言
                "C:\\Windows\\Fonts\\verdana.ttf"      // Verdana - 支持多种语言
        };

        // 检查哪些字体文件存在
        List<String> availableFonts = new ArrayList<>();
        for (String fontPath : fontPaths) {
            File fontFile = new File(fontPath);
            if (fontFile.exists()) {
                availableFonts.add(fontPath);
            }
        }

        if (availableFonts.isEmpty()) {
            System.out.println("跳过测试：没有找到可用的字体文件");
            return;
        }

        try {
            // 清空之前的字体
            fontUtils.clearFonts();

            // 加载多个字体
            int loadedCount = 0;
            for (String fontPath : availableFonts) {
                List<PDFont> fonts = fontUtils.loadFontByPath(fontPath);
                if (!fonts.isEmpty()) {
                    loadedCount++;
                }
            }

            if (loadedCount == 0) {
                System.out.println("跳过测试：没有成功加载任何字体");
                return;
            }

            List<PDFont> fontList = fontUtils.getFontList();
            System.out.println("成功加载了 " + fontList.size() + " 个字体用于多语言测试");

            float fontSize = 12.0f;

            // 创建包含多种语言的测试字符串
            // 中文：你好世界
            // 柬埔寨语（高棉语）：សួស្តី（你好）
            // 英文：Hello
            // 法文：Bonjour（带重音字符）
            // 俄文：Привет（你好）
            String[] testStrings = {
                    "Hello 你好",                                    // 英文 + 中文
                    "Bonjour 你好 Привет",                           // 法文 + 中文 + 俄文
                    "Hello Bonjour Привет 你好",                     // 英文 + 法文 + 俄文 + 中文
                    "Hello 你好 Bonjour Привет 123",                // 英文 + 中文 + 法文 + 俄文 + 数字
                    "សួស្តី Hello 你好 Bonjour Привет",              // 柬埔寨语 + 英文 + 中文 + 法文 + 俄文
                    "测试 Test Épreuve Тест"                        // 中文 + 英文 + 法文（带重音）+ 俄文
            };

                        for (String testString : testStrings) {
                            // 使用工具类计算总宽度
                            float calculatedWidth = fontUtils.calculateTextWidth(testString, fontSize);

                            // 手动计算：遍历每个字符，找到匹配的字体并累加宽度
                            float manualSum = 0.0f;
                            int matchedCount = 0;
                            int unmatchedCount = 0;

                            for (int i = 0; i < testString.length(); i++) {
                                char ch = testString.charAt(i);

                                // 查找包含该字符的字体（按字体列表顺序）
                                PDFont matchedFont = null;
                                for (PDFont font : fontList) {
                                    try {
                                        font.getStringWidth(String.valueOf(ch));
                                        matchedFont = font;
                                        matchedCount++;
                                        break; // 找到第一个包含该字符的字体
                                    } catch (IllegalArgumentException | IOException e) {
                                        // 字符不存在于该字体，继续查找下一个字体
                                    }
                                }

                                // 如果找不到匹配字体，使用第一个字体和空格
                                if (matchedFont == null) {
                                    matchedFont = fontList.get(0);
                                    unmatchedCount++;
                                }

                                // 检查字符是否存在于匹配的字体中
                                boolean exists = false;
                                try {
                                    matchedFont.getStringWidth(String.valueOf(ch));
                                    exists = true;
                                } catch (IllegalArgumentException | IOException e) {
                                    // 字符不存在
                                }

                                // 计算字符宽度
                                String charToMeasure = exists ? String.valueOf(ch) : " ";
                                float charWidth = matchedFont.getStringWidth(charToMeasure) / 1000.0f * fontSize;
                                manualSum += charWidth;
                            }

                            // 验证计算结果（允许0.03的误差，因为多语言字符匹配可能有更多差异）
                            assertEquals("多语言混合文本 '" + testString + "' 的宽度计算不正确",
                                    manualSum, calculatedWidth, 0.03f);

                            // 打印详细信息用于调试
                            System.out.println("测试文本: " + testString);
                            System.out.println("  工具类计算宽度: " + calculatedWidth + "pt");
                            System.out.println("  手动计算宽度: " + manualSum + "pt");
                            System.out.println("  差异: " + Math.abs(manualSum - calculatedWidth) + "pt");
                            System.out.println("  匹配字符数: " + matchedCount + ", 未匹配字符数: " + unmatchedCount);

                            // 验证宽度大于0（除非是空字符串）
                            if (!testString.isEmpty()) {
                                assertTrue("多语言混合文本宽度应该大于0", calculatedWidth > 0);
                            }
                        }

            // 额外验证：测试各种语言的单个字符
            System.out.println("\n=== 单字符验证 ===");

            // 测试中文字符
            char chineseChar = '你';
            if (fontUtils.getFontList().size() > 0) {
                try {
                    float chineseWidth = fontUtils.calculateTextWidth(String.valueOf(chineseChar), fontSize);
                    System.out.println("中文字符 '" + chineseChar + "' 宽度: " + chineseWidth + "pt");
                    assertTrue("中文字符宽度应该大于0", chineseWidth > 0);
                } catch (Exception e) {
                    System.out.println("中文字符 '" + chineseChar + "' 无法计算宽度: " + e.getMessage());
                }
            }

            // 测试俄文字符
            char russianChar = 'П';
            if (fontUtils.getFontList().size() > 0) {
                try {
                    float russianWidth = fontUtils.calculateTextWidth(String.valueOf(russianChar), fontSize);
                    System.out.println("俄文字符 '" + russianChar + "' 宽度: " + russianWidth + "pt");
                    // 某些字体可能不支持俄文，所以不强制要求大于0
                } catch (Exception e) {
                    System.out.println("俄文字符 '" + russianChar + "' 无法计算宽度: " + e.getMessage());
                }
            }

            // 测试法文重音字符
            char frenchChar = 'é';
            if (fontUtils.getFontList().size() > 0) {
                try {
                    float frenchWidth = fontUtils.calculateTextWidth(String.valueOf(frenchChar), fontSize);
                    System.out.println("法文字符 '" + frenchChar + "' 宽度: " + frenchWidth + "pt");
                    assertTrue("法文字符宽度应该大于0", frenchWidth > 0);
                } catch (Exception e) {
                    System.out.println("法文字符 '" + frenchChar + "' 无法计算宽度: " + e.getMessage());
                }
            }

            // 测试英文字符（作为基准）
            char englishChar = 'A';
            if (fontUtils.getFontList().size() > 0) {
                try {
                    float englishWidth = fontUtils.calculateTextWidth(String.valueOf(englishChar), fontSize);
                    System.out.println("英文字符 '" + englishChar + "' 宽度: " + englishWidth + "pt");
                    assertTrue("英文字符宽度应该大于0", englishWidth > 0);
                } catch (Exception e) {
                    System.out.println("英文字符 '" + englishChar + "' 无法计算宽度: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            fail("多语言混合长度校验失败: " + e.getMessage());
        }
    }

    /**
     * 测试场景26：字符间距（Character Spacing）验证
     * 验证字符间距对文本宽度计算的影响
     */
    @Test
    public void testCalculateTextWidth_CharacterSpacing_Precise() {
        // 尝试从Windows系统字体目录加载字体（如果存在）
        String fontPath = "C:\\Windows\\Fonts\\arial.ttf";
        File fontFile = new File(fontPath);

        if (fontFile.exists()) {
            try {
                // 加载字体
                List<PDFont> fonts = fontUtils.loadFontByPath(fontPath);
                if (fonts.isEmpty()) {
                    System.out.println("跳过测试：字体加载失败");
                    return;
                }

                float fontSize = 12.0f;

                // 测试不同的字符间距值
                float[] spacingValues = {0.0f, 1.0f, 2.0f, 3.0f, 5.0f, 10.0f};

                for (float spacing : spacingValues) {
                    // 设置字符间距
                    fontUtils.setCharacterSpacing(spacing);

                    // 验证字符间距设置成功
                    assertEquals("字符间距设置失败", spacing, fontUtils.getCharacterSpacing(), 0.001f);

                    // 测试不同长度的字符串
                    String[] testStrings = {"AB", "ABC", "Hello", "Hello World", "测试 Test"};

                    for (String testString : testStrings) {
                        // 使用工具类计算宽度（包含字符间距）
                        float calculatedWidth = fontUtils.calculateTextWidth(testString, fontSize);

                        // 手动计算：字符宽度 + 字符间距
                        float manualSum = 0.0f;
                        int validCharCount = 0;

                        for (int i = 0; i < testString.length(); i++) {
                            char ch = testString.charAt(i);
                            PDFont charFont = fontUtils.getFontList().get(0);
                            boolean exists = false;
                            try {
                                charFont.getStringWidth(String.valueOf(ch));
                                exists = true;
                            } catch (Exception e) {
                                // 字符不存在
                            }
                            String charToMeasure = exists ? String.valueOf(ch) : " ";
                            manualSum += charFont.getStringWidth(charToMeasure) / 1000.0f * fontSize;
                            if (exists || charToMeasure.equals(" ")) {
                                validCharCount++;
                            }
                        }

                        // 添加字符间距：n个字符有(n-1)个间距
                        if (validCharCount > 1 && spacing > 0) {
                            manualSum += spacing * (validCharCount - 1);
                        }

                        // 验证计算结果（允许0.01的误差）
                        assertEquals("字符间距 " + spacing + "pt 时，文本 '" + testString + "' 的宽度计算不正确",
                                manualSum, calculatedWidth, 0.01f);

                        // 打印详细信息
                        System.out.println("字符间距: " + spacing + "pt, 文本: '" + testString + "'");
                        System.out.println("  有效字符数: " + validCharCount);
                        System.out.println("  字符间距总数: " + (validCharCount > 1 ? (validCharCount - 1) : 0));
                        System.out.println("  工具类计算宽度: " + calculatedWidth + "pt");
                        System.out.println("  手动计算宽度: " + manualSum + "pt");
                    }
                }

                // 验证字符间距为0时，宽度应该等于无间距时的宽度
                fontUtils.setCharacterSpacing(0.0f);
                float widthWithoutSpacing = fontUtils.calculateTextWidth("Hello", fontSize);

                fontUtils.setCharacterSpacing(5.0f);
                float widthWithSpacing = fontUtils.calculateTextWidth("Hello", fontSize);

                // 验证有间距的宽度应该大于无间距的宽度
                assertTrue("有字符间距的宽度应该大于无间距的宽度",
                        widthWithSpacing > widthWithoutSpacing);

                // 验证差异应该等于字符间距 * (字符数 - 1)
                // "Hello"有5个字符，应该有4个间距
                float expectedDifference = 5.0f * 4; // spacing * (5 - 1)
                float actualDifference = widthWithSpacing - widthWithoutSpacing;
                assertEquals("字符间距差异计算不正确",
                        expectedDifference, actualDifference, 0.01f);

                System.out.println("\n验证：字符间距影响");
                System.out.println("  无间距宽度: " + widthWithoutSpacing + "pt");
                System.out.println("  有间距宽度: " + widthWithSpacing + "pt");
                System.out.println("  预期差异: " + expectedDifference + "pt");
                System.out.println("  实际差异: " + actualDifference + "pt");

            } catch (IOException e) {
                fail("字符间距验证失败: " + e.getMessage());
            }
        } else {
            System.out.println("跳过测试：字体文件不存在 - " + fontPath);
        }
    }

    /**
     * 测试场景27：字符间距与多字体混合验证
     * 验证在多字体混合场景下，字符间距计算是否正确
     */
    @Test
    public void testCalculateTextWidth_CharacterSpacing_MultiFont() {
        // 尝试加载多个字体
        String[] fontPaths = {
                "C:\\Windows\\Fonts\\arial.ttf",
                "C:\\Windows\\Fonts\\msyh.ttf",
                "C:\\Windows\\Fonts\\simhei.ttf"
        };

        List<String> availableFonts = new ArrayList<>();
        for (String fontPath : fontPaths) {
            File fontFile = new File(fontPath);
            if (fontFile.exists()) {
                availableFonts.add(fontPath);
            }
        }

        if (availableFonts.size() < 2) {
            System.out.println("跳过测试：需要至少2个字体文件");
            return;
        }

        try {
            // 清空之前的字体
            fontUtils.clearFonts();

            // 加载多个字体
            for (String fontPath : availableFonts) {
                fontUtils.loadFontByPath(fontPath);
            }

            if (fontUtils.getFontList().isEmpty()) {
                System.out.println("跳过测试：没有成功加载任何字体");
                return;
            }

            float fontSize = 12.0f;
            float spacing = 2.0f;
            fontUtils.setCharacterSpacing(spacing);

            // 测试混合文本
            String[] testStrings = {
                    "Hello 你好",
                    "Test 测试 123",
                    "ABC 中文 XYZ"
            };

            for (String testString : testStrings) {
                // 使用工具类计算宽度（包含字符间距）
                float calculatedWidth = fontUtils.calculateTextWidth(testString, fontSize);

                // 手动计算：字符宽度 + 字符间距
                float manualSum = 0.0f;
                int validCharCount = 0;
                List<PDFont> fontList = fontUtils.getFontList();

                for (int i = 0; i < testString.length(); i++) {
                    char ch = testString.charAt(i);

                    // 查找匹配的字体
                    PDFont matchedFont = null;
                    for (PDFont font : fontList) {
                        try {
                            font.getStringWidth(String.valueOf(ch));
                            matchedFont = font;
                            break;
                        } catch (Exception e) {
                            // 字符不存在
                        }
                    }

                    if (matchedFont == null) {
                        matchedFont = fontList.get(0);
                    }

                    boolean exists = false;
                    try {
                        matchedFont.getStringWidth(String.valueOf(ch));
                        exists = true;
                    } catch (Exception e) {
                        // 字符不存在
                    }

                    String charToMeasure = exists ? String.valueOf(ch) : " ";
                    manualSum += matchedFont.getStringWidth(charToMeasure) / 1000.0f * fontSize;
                    validCharCount++;
                }

                // 添加字符间距
                if (validCharCount > 1) {
                    manualSum += spacing * (validCharCount - 1);
                }

                // 验证计算结果（允许0.02的误差）
                assertEquals("多字体混合文本 '" + testString + "' 在字符间距 " + spacing + "pt 时的宽度计算不正确",
                        manualSum, calculatedWidth, 0.02f);

                System.out.println("多字体混合 + 字符间距测试:");
                System.out.println("  文本: '" + testString + "'");
                System.out.println("  字符间距: " + spacing + "pt");
                System.out.println("  有效字符数: " + validCharCount);
                System.out.println("  工具类计算宽度: " + calculatedWidth + "pt");
                System.out.println("  手动计算宽度: " + manualSum + "pt");
            }

        } catch (IOException e) {
            fail("字符间距与多字体混合验证失败: " + e.getMessage());
        }
    }
}

