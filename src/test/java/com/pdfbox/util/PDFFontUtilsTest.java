package com.pdfbox.util;
import java.lang.reflect.Field;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.junit.After;
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
     * 测试前初始化
     * 创建新的PDF文档和字体工具实例
     */
    @Before
    public void setUp() {
        try {
            document = new PDDocument();
            PDFFontUtils.setCurrentDocument(document);
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
            PDFFontUtils.setCurrentDocument(null);
            if (document != null) {
                document.close();
            }
        } catch (IOException e) {
            System.err.println("关闭文档失败: " + e.getMessage());
        }
    }

    /**
     * 测试场景17：保存PDF文档
     * 测试创建和保存PDF文档
     */
    @Test
    public void testWidth() {
        try {
            // 尝试从Windows系统字体目录加载字体（如果存在）
            String fontPath = "C:\\Windows\\Fonts";
            File fontFile = new File(fontPath);
            printMemoryUsage("init:");
            if (fontFile.exists()) {
                // 创建PDF页面
                PDPage page = new PDPage();
                document.addPage(page);

                // 加载字体
                PDFFontUtils.loadFontByPath(fontPath);
                printMemoryUsage("After Load:");
                // 创建内容流
                PDPageContentStream contentStream = new PDPageContentStream(document, page);

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

                for (int i = 1; i <= testStrings.length; i++) {
                    PDFFontUtils.insertTextWithFontMatching(contentStream, testStrings[i - 1], 50, i * 100, 12.0f);
                    printMemoryUsage("插入文本: " + testStrings[i - 1]);
                    PDFFontUtils.insertTextWithFontMatching(contentStream, "|", 50, i * 100, 12.0f);
                    printMemoryUsage("插入标记: |");
                    PDFFontUtils.insertTextWithFontMatching(contentStream, "|", 50 + PDFFontUtils.calculateTextWidth(testStrings[i - 1], 12.0f), i * 100, 12.0f);
                    printMemoryUsage("插入结束标记: |");
                }
                // 插入文本

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

    /** 多语种测试数据：中、英、法、俄、高棉等混合，用于多语言/多字体测试。 */
    private static final String[] MULTILANG_TEST_STRINGS = {
            "Hello 你好",
            "Bonjour 你好 Привет",
            "Hello Bonjour Привет 你好",
            "Hello 你好 Bonjour Привет 123",
            "សួស្តី Hello 你好 Bonjour Привет",
            "测试 Test Épreuve Тест"
    };

    /**
     * 多种字体混合插入PDF：加载整目录字体，按字符自动匹配多字体，通过起始/结束 "|" 验证位置计算。
     * 验证 calculateTextWidth 与 insertTextWithFontMatching 在混合字体场景下一致。
     */
    @Test
    public void testMultiLanguageMultiFont_InsertAndVerifyPosition() throws IOException {
        String fontPath = "C:\\Windows\\Fonts";
        File fontDir = new File(fontPath);
        if (!fontDir.exists() || !fontDir.isDirectory()) {
            System.out.println("跳过测试：字体目录不存在 - " + fontPath);
            return;
        }

        PDFFontUtils.loadFontByPath(fontPath);
        PDPage page = new PDPage();
        document.addPage(page);

        float startX = 50f;
        float fontSize = 12.0f;

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            for (int i = 0; i < MULTILANG_TEST_STRINGS.length; i++) {
                float y = (i + 1) * 100f;
                String text = MULTILANG_TEST_STRINGS[i];
                insertTextWithPositionMarkers(contentStream, text, startX, y, fontSize, null);
            }
        }

        File tempFile = new File("test_multilang_mixed.pdf");
        document.save(tempFile);
        assertTrue("PDF文件应该被创建", tempFile.exists());
        assertTrue("PDF文件大小应该大于0", tempFile.length() > 0);
    }

    /**
     * 指定字体插入PDF：仅使用一个首选字体（含回退），通过起始/结束 "|" 验证位置计算。
     * 验证 insertTextWithPreferredFont 与 calculateTextWidth(text, fontSize, preferredFontCosName) 一致。
     */
    @Test
    public void testMultiLanguage_SpecifiedFont_InsertAndVerifyPosition() throws IOException {
        String fontPath = "C:\\Windows\\Fonts";
        File fontDir = new File(fontPath);
        if (!fontDir.exists() || !fontDir.isDirectory()) {
            System.out.println("跳过测试：字体目录不存在 - " + fontPath);
            return;
        }

        PDFFontUtils.loadFontByPath(fontPath);
        String preferredCosName = "Cambria";
        if (preferredCosName == null) {
            System.out.println("跳过测试：未加载到任何字体");
            return;
        }

        PDPage page = new PDPage();
        document.addPage(page);
        float startX = 50f;
        float fontSize = 12.0f;

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            for (int i = 0; i < MULTILANG_TEST_STRINGS.length; i++) {
                float y = (i + 1) * 100f;
                String text = MULTILANG_TEST_STRINGS[i];
                PDFFontUtils.insertTextWithPreferredFont(contentStream, text, startX, y, fontSize, preferredCosName);
                PDFFontUtils.insertTextWithPreferredFont(contentStream, "|", startX, y, fontSize, preferredCosName);
                float endX = startX + PDFFontUtils.calculateTextWidth(text, fontSize, preferredCosName);
                PDFFontUtils.insertTextWithPreferredFont(contentStream, "|", endX, y, fontSize, preferredCosName);
            }
        }

        File tempFile = new File("test_multilang_specified_font.pdf");
        document.save(tempFile);
        assertTrue("PDF文件应该被创建", tempFile.exists());
        assertTrue("PDF文件大小应该大于0", tempFile.length() > 0);
    }

    /**
     * 多种字体混合（仅加载少量字体）：只加载英文+中文等少量字体，强制多字体按字符匹配，验证位置计算。
     */
    @Test
    public void testMultiLanguage_MixedFontsOnly_InsertAndVerifyPosition() throws IOException {
        String[] fontPaths = {
                "C:\\Windows\\Fonts\\simsun.ttc",
                "C:\\Windows\\Fonts\\simsunb.ttf"
        };
        java.util.List<String> available = new java.util.ArrayList<>();
        for (String p : fontPaths) {
            if (new File(p).exists()) available.add(p);
        }
        if (available.size() < 2) {
            System.out.println("跳过测试：需要至少2个字体文件，当前可用: " + available.size());
            return;
        }

        PDFFontUtils.clearFonts();
        for (String p : available) {
            PDFFontUtils.loadFontByPath(p);
        }
        // fontCache 需用“真实” document 预加载至少一种字体，避免 calculateTextWidth 内部临时 document 关闭导致保存时报 Scratch file already closed
        float fontSize = 12.0f;
        PDFFontUtils.ensureAtLeastOneFontLoaded();
        assertFalse("应至少加载一种字体", PDFFontUtils.getFontList().isEmpty());

        PDPage page = new PDPage();
        document.addPage(page);
        float startX = 50f;

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            for (int i = 0; i < MULTILANG_TEST_STRINGS.length; i++) {
                float y = (i + 1) * 100f;
                String text = MULTILANG_TEST_STRINGS[i];
                insertTextWithPositionMarkers(contentStream, text, startX, y, fontSize, null);
            }
        }

        File tempFile = new File("test_multilang_mixed_few_fonts.pdf");
        document.save(tempFile);
        assertTrue("PDF文件应该被创建", tempFile.exists());
        assertTrue("PDF文件大小应该大于0", tempFile.length() > 0);
    }

    /**
     * 指定字体与混合字体对比：同一多语种文本分别用“指定字体”和“字体匹配”插入，均做位置验证。
     */
    @Test
    public void testMultiLanguage_SpecifiedVsMixed_InsertAndVerifyPosition() throws IOException {
        String fontPath = "C:\\Windows\\Fonts";
        File fontDir = new File(fontPath);
        if (!fontDir.exists() || !fontDir.isDirectory()) {
            System.out.println("跳过测试：字体目录不存在 - " + fontPath);
            return;
        }

        PDFFontUtils.loadFontByPath(fontPath);
        String preferredCosName = getFirstFontCosName();
        if (preferredCosName == null) {
            System.out.println("跳过测试：未加载到任何字体");
            return;
        }

        PDPage page = new PDPage();
        document.addPage(page);
        float startX = 50f;
        float fontSize = 12.0f;

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            // 上半部分：指定字体插入 + 位置标记
            for (int i = 0; i < MULTILANG_TEST_STRINGS.length; i++) {
                float y = 550 - i * 80;
                String text = MULTILANG_TEST_STRINGS[i];
                PDFFontUtils.insertTextWithPreferredFont(contentStream, text, startX, y, fontSize, preferredCosName);
                PDFFontUtils.insertTextWithPreferredFont(contentStream, "|", startX, y, fontSize, preferredCosName);
                float endX = startX + PDFFontUtils.calculateTextWidth(text, fontSize, preferredCosName);
                PDFFontUtils.insertTextWithPreferredFont(contentStream, "|", endX, y, fontSize, preferredCosName);
            }
            // 下半部分：多字体匹配插入 + 位置标记
            for (int i = 0; i < MULTILANG_TEST_STRINGS.length; i++) {
                float y = 80 + i * 80;
                String text = MULTILANG_TEST_STRINGS[i];
                insertTextWithPositionMarkers(contentStream, text, startX, y, fontSize, null);
            }
        }

        File tempFile = new File("test_multilang_specified_vs_mixed.pdf");
        document.save(tempFile);
        assertTrue("PDF文件应该被创建", tempFile.exists());
        assertTrue("PDF文件大小应该大于0", tempFile.length() > 0);
    }

    /**
     * 连续生成两个 PDF：使用相同字体、不同内容。
     * 第一个 PDF 保存并关闭后清空字体缓存并重新加载，再生成第二个 PDF，避免复用已关闭 document 的字体。
     */
    @Test
    public void testTwoPdfsSameFontDifferentContent() throws IOException {
        String fontPath = "C:\\Windows\\Fonts";
        File fontDir = new File(fontPath);
        if (!fontDir.exists() || !fontDir.isDirectory()) {
            System.out.println("跳过测试：字体目录不存在 - " + fontPath);
            return;
        }

        printMemoryUsage("testTwoPdfsSameFontDifferentContent-开始");

        String[] content1 = { "第一份文档", "Title A", "Content line 1", "Content line 2" };
        String[] content2 = { "第二份文档", "Title B", "Different line 1", "Different line 2" };

        float startX = 50f;
        float fontSize = 12.0f;

        // 第一份 PDF
        PDFFontUtils.clearFonts();
        PDFFontUtils.loadFontByPath(fontPath);
        PDDocument doc1 = new PDDocument();
        PDFFontUtils.setCurrentDocument(doc1);
        PDFFontUtils.ensureAtLeastOneFontLoaded();
        assertFalse("应至少加载一种字体", PDFFontUtils.getFontList().isEmpty());
        printMemoryUsage("第一份PDF-字体加载完成");

        PDPage page1 = new PDPage();
        doc1.addPage(page1);
        try (PDPageContentStream cs1 = new PDPageContentStream(doc1, page1)) {
            for (int i = 0; i < content1.length; i++) {
                float y = 700 - i * 80;
                PDFFontUtils.insertTextWithFontMatching(cs1, content1[i], startX, y, fontSize);
            }
        }
        File file1 = new File("test_two_pdfs_1.pdf");
        doc1.save(file1);
        doc1.close();
        assertTrue("第一个PDF应被创建", file1.exists());
        assertTrue("第一个PDF大小应大于0", file1.length() > 0);
        printMemoryUsage("第一份PDF-保存并关闭后");

        // 清空字体缓存并重新加载，再生成第二份 PDF（避免使用已关闭 doc1 的字体）
        PDFFontUtils.clearFonts();
        PDFFontUtils.loadFontByPath(fontPath);
        PDDocument doc2 = new PDDocument();
        PDFFontUtils.setCurrentDocument(doc2);
        PDFFontUtils.ensureAtLeastOneFontLoaded();
        printMemoryUsage("第二份PDF-字体加载完成");

        PDPage page2 = new PDPage();
        doc2.addPage(page2);
        try (PDPageContentStream cs2 = new PDPageContentStream(doc2, page2)) {
            for (int i = 0; i < content2.length; i++) {
                float y = 700 - i * 80;
                PDFFontUtils.insertTextWithFontMatching(cs2, content2[i], startX, y, fontSize);
            }
        }
        File file2 = new File("test_two_pdfs_2.pdf");
        doc2.save(file2);
        doc2.close();
        assertTrue("第二个PDF应被创建", file2.exists());
        assertTrue("第二个PDF大小应大于0", file2.length() > 0);
        printMemoryUsage("testTwoPdfsSameFontDifferentContent-结束");
    }

    /** 批量生成 PDF 的数量。 */
    private static final int BATCH_PDF_COUNT = 500;
    /** 每份 PDF 的行数（中英混合）。 */
    private static final int LINES_PER_PDF = 50;
    /** 每多少份打印一次内存与速度。 */
    private static final int BATCH_REPORT_INTERVAL = 50;

    /** 用于生成每行中英混合内容的模板（交替使用）。 */
    private static final String[] LINE_TEMPLATES = {
            "PDF 第 %d 份 / 第 %d 行",
            "Line %d - 行 %d",
            "Content 内容 %d %d",
            "测试 Test No.%d Row %d",
            "数据 Data %d 行 %d",
            "文档 Document 第 %d 行 %d",
            "编号 No.%d 行号 %d",
            "Text 文本 %d %d"
    };

    /**
     * 循环创建 500 份不同内容、不同字体选用的 PDF，并监控内存与执行速度。
     * 每份的内存与执行时间写入日志文件（每份一行）。
     */
    @Test
    public void testBatch500PdfsWithMemoryAndSpeedMonitor() throws IOException {
        String fontPath = "C:\\Windows\\Fonts";
        File fontDir = new File(fontPath);
        if (!fontDir.exists() || !fontDir.isDirectory()) {
            System.out.println("跳过测试：字体目录不存在 - " + fontPath);
            return;
        }

        File outDir = new File("test_batch_500_output");
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        File logFile = new File(outDir, "batch_500_log.csv");

        float fontSize = 12.0f;
        long batchStartMs = System.currentTimeMillis();
        printMemoryUsage("batch500-开始");

        // 日志表头
        String header = "序号,已用MB,空闲MB,总计MB,最大MB,本份耗时ms,累计耗时ms\n";
        Files.write(logFile.toPath(), header.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        for (int n = 1; n <= BATCH_PDF_COUNT; n++) {
            long iterStartMs = System.currentTimeMillis();

            PDFFontUtils.clearFonts();
            PDFFontUtils.loadFontByPath(fontPath);
            try (PDDocument doc = new PDDocument()) {
                PDFFontUtils.setCurrentDocument(doc);
                PDFFontUtils.ensureAtLeastOneFontLoaded();
                if (PDFFontUtils.getFontList().isEmpty()) {
                    fail("第 " + n + " 份：未加载到任何字体");
                }

                // 每份 50 行，中英混合
                String[] lines = new String[LINES_PER_PDF];
                for (int i = 0; i < LINES_PER_PDF; i++) {
                    String tpl = LINE_TEMPLATES[i % LINE_TEMPLATES.length];
                    lines[i] = String.format(tpl, n, i + 1);
                }

                PDPage page = new PDPage();
                doc.addPage(page);
                float startX = 50f;
                float lineHeight = 14f;
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    for (int i = 0; i < lines.length; i++) {
                        float y = 700 - i * lineHeight;
                        PDFFontUtils.insertTextWithFontMatching(cs, lines[i], startX, y, fontSize);
                        PDFFontUtils.calculateTextWidth(lines[i],fontSize);
                    }
                }

                String fileName = String.format("pdf_%03d.pdf", n);
                File file = new File(outDir, fileName);
                doc.save(file);
            }

            long iterEndMs = System.currentTimeMillis();
            long thisPdfMs = iterEndMs - iterStartMs;
            long cumulativeMs = iterEndMs - batchStartMs;
            double[] mem = getMemoryMB();
            String line = String.format("%d,%.2f,%.2f,%.2f,%.2f,%d,%d\n",
                    n, mem[0], mem[1], mem[2], mem[3], thisPdfMs, cumulativeMs);
            Files.write(logFile.toPath(), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            if (n % BATCH_REPORT_INTERVAL == 0 || n == BATCH_PDF_COUNT) {
                double elapsedSec = cumulativeMs / 1000.0;
                double rate = n / elapsedSec;
                System.out.println(String.format("[批量] 已生成 %d 份, 耗时 %.2f s, 速度 %.1f 份/s, 日志: %s", n, elapsedSec, rate, logFile.getAbsolutePath()));
                printMemoryUsage("batch500-第" + n + "份后");
            }
        }

        long totalMs = System.currentTimeMillis() - batchStartMs;
        double totalSec = totalMs / 1000.0;
        double avgMsPerPdf = (double) totalMs / BATCH_PDF_COUNT;
        System.out.println(String.format("[批量] 完成 %d 份, 总耗时 %.2f s, 平均 %.0f ms/份, 日志: %s", BATCH_PDF_COUNT, totalSec, avgMsPerPdf, logFile.getAbsolutePath()));
        printMemoryUsage("batch500-结束");
    }

    /** 返回当前内存 [已用MB, 空闲MB, 总计MB, 最大MB]。 */
    private static double[] getMemoryMB() {
        Runtime rt = Runtime.getRuntime();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long used = total - free;
        long max = rt.maxMemory();
        return new double[]{
                used / (1024.0 * 1024.0),
                free / (1024.0 * 1024.0),
                total / (1024.0 * 1024.0),
                max / (1024.0 * 1024.0)
        };
    }

    /**
     * 使用字体匹配插入文本，并在起始/结束位置插入 "|" 以验证 calculateTextWidth 与插入一致。
     * 使用当前测试的 document。
     */
    private void insertTextWithPositionMarkers(PDPageContentStream contentStream, String text,
                                               float startX, float y, float fontSize, String preferredFontCosName) throws IOException {
        insertTextWithPositionMarkers(document, contentStream, text, startX, y, fontSize, preferredFontCosName);
    }

    /**
     * 使用指定 PDDocument 插入文本并在起始/结束位置插入 "|" 以验证位置计算。
     */
    private void insertTextWithPositionMarkers(PDDocument document, PDPageContentStream contentStream, String text,
                                               float startX, float y, float fontSize, String preferredFontCosName) throws IOException {
        PDFFontUtils.setCurrentDocument(document);
        if (preferredFontCosName != null) {
            PDFFontUtils.insertTextWithPreferredFont(contentStream, text, startX, y, fontSize, preferredFontCosName);
            PDFFontUtils.insertTextWithPreferredFont(contentStream, "|", startX, y, fontSize, preferredFontCosName);
            float endX = startX + PDFFontUtils.calculateTextWidth(text, fontSize, preferredFontCosName);
            PDFFontUtils.insertTextWithPreferredFont(contentStream, "|", endX, y, fontSize, preferredFontCosName);
        } else {
            PDFFontUtils.insertTextWithFontMatching(contentStream, text, startX, y, fontSize);
            PDFFontUtils.insertTextWithFontMatching(contentStream, "|", startX, y, fontSize);
            float endX = startX + PDFFontUtils.calculateTextWidth(text, fontSize);
            PDFFontUtils.insertTextWithFontMatching(contentStream, "|", endX, y, fontSize);
        }
    }

    /** 获取字体列表中第一个字体的 COS 名称，用于“指定字体”测试。 */
    private static String getFirstFontCosName() {
        return PDFFontUtils.getFontList().isEmpty() ? null : PDFFontUtils.getFontList().get(0);
    }

    /**
     * 打印当前程序内存占用
     *
     * @param operation 操作描述
     */
    private void printMemoryUsage(String operation) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        // 转换为MB
        double totalMemoryMB = totalMemory / (1024.0 * 1024.0);
        double freeMemoryMB = freeMemory / (1024.0 * 1024.0);
        double usedMemoryMB = usedMemory / (1024.0 * 1024.0);
        double maxMemoryMB = maxMemory / (1024.0 * 1024.0);

        System.out.println(String.format("[内存占用] %s - 已用: %.2f MB, 空闲: %.2f MB, 总计: %.2f MB, 最大: %.2f MB",
                operation, usedMemoryMB, freeMemoryMB, totalMemoryMB, maxMemoryMB));
    }

}
