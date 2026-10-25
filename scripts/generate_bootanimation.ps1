$code = @'
using System;
using System.IO;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;

public class FullAnimationGenerator {
    private const int WIDTH = 1280;
    private const int HEIGHT = 720;

    public static void GenerateAll(string baseDir) {
        string part0Dir = Path.Combine(baseDir, "part0");
        string part1Dir = Path.Combine(baseDir, "part1");
        Directory.CreateDirectory(part0Dir);
        Directory.CreateDirectory(part1Dir);

        // Part 0: Intro (75 frames, 0 to 74)
        for (int i = 0; i < 75; i++) {
            string file = Path.Combine(part0Dir, string.Format("{0:D3}.png", i));
            RenderFrame(file, i, 75, false);
        }

        // Part 1: Loop (25 frames, 0 to 24)
        for (int i = 0; i < 25; i++) {
            string file = Path.Combine(part1Dir, string.Format("{0:D3}.png", i));
            RenderFrame(file, i, 25, true);
        }
    }

    private static void RenderFrame(string outputPath, int frame, int total, bool isLoop) {
        using (Bitmap bmp = new Bitmap(WIDTH, HEIGHT, PixelFormat.Format32bppArgb))
        using (Graphics g = Graphics.FromImage(bmp)) {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.InterpolationMode = InterpolationMode.HighQualityBicubic;
            g.TextRenderingHint = System.Drawing.Text.TextRenderingHint.AntiAliasGridFit;

            float p = isLoop ? 1.0f : ((float)frame / (total - 1));
            float loopPhase = isLoop ? (float)Math.Sin((float)frame / total * Math.PI * 2) : 0f;

            // 1. Sky Gradient
            Color skyTop = Color.FromArgb(4, 7, 16);
            float dawnFactor = isLoop ? (0.85f + 0.15f * loopPhase) : (float)Math.Pow(p, 1.4);
            int rH = (int)(14 + 88 * dawnFactor);
            int gH = (int)(20 + 58 * dawnFactor);
            int bH = (int)(36 + 28 * dawnFactor);
            Color skyBottom = Color.FromArgb(Math.Min(255, rH), Math.Min(255, gH), Math.Min(255, bH));

            using (LinearGradientBrush skyBrush = new LinearGradientBrush(
                new Point(0, 0), new Point(0, HEIGHT), skyTop, skyBottom)) {
                g.FillRectangle(skyBrush, 0, 0, WIDTH, HEIGHT);
            }

            // 2. Sunrise Glow Bloom
            float glowAlpha = 0f;
            if (isLoop) {
                glowAlpha = 0.85f + 0.15f * loopPhase;
            } else if (p > 0.15f) {
                glowAlpha = Math.Min(1.0f, (p - 0.15f) / 0.40f);
            }

            if (glowAlpha > 0.01f) {
                int glowCenterX = WIDTH / 2;
                int glowCenterY = 415;
                int glowRadiusX = (int)(320 + 80 * glowAlpha);
                int glowRadiusY = (int)(180 + 60 * glowAlpha);

                using (GraphicsPath glowPath = new GraphicsPath()) {
                    glowPath.AddEllipse(glowCenterX - glowRadiusX, glowCenterY - glowRadiusY, glowRadiusX * 2, glowRadiusY * 2);
                    using (PathGradientBrush pgb = new PathGradientBrush(glowPath)) {
                        pgb.CenterColor = Color.FromArgb((int)(160 * glowAlpha), 245, 178, 70);
                        pgb.SurroundColors = new Color[] { Color.FromArgb(0, 245, 178, 70) };
                        g.FillPath(pgb, glowPath);
                    }
                }
            }

            // 3. Mountain Ridges with subtle rim lighting
            DrawMountainRidge(g, WIDTH, HEIGHT, 410, 0.35f, Color.FromArgb(14, 20, 32), glowAlpha * 0.5f);
            DrawMountainRidge(g, WIDTH, HEIGHT, 465, 0.65f, Color.FromArgb(9, 13, 22), glowAlpha * 0.3f);
            DrawMountainRidge(g, WIDTH, HEIGHT, 545, 1.0f, Color.FromArgb(5, 7, 13), 0f);

            // 4. Logo & Emblem
            float logoAlpha = isLoop ? 1.0f : (p > 0.35f ? Math.Min(1.0f, (p - 0.35f) / 0.25f) : 0f);

            if (logoAlpha > 0.01f) {
                int logoY = 240;

                // Crescent & Star
                DrawAlgerianEmblem(g, WIDTH / 2, logoY - 50, 24, logoAlpha);

                using (Font mainFont = new Font("Segoe UI", 48, FontStyle.Bold, GraphicsUnit.Pixel))
                using (Font subFont = new Font("Segoe UI", 16, FontStyle.Regular, GraphicsUnit.Pixel)) {
                    string title = "AURÃˆS TV";
                    SizeF titleSize = g.MeasureString(title, mainFont);
                    float titleX = (WIDTH - titleSize.Width) / 2;
                    float titleY = logoY;

                    // Drop Shadow
                    using (SolidBrush shadowBrush = new SolidBrush(Color.FromArgb((int)(180 * logoAlpha), 0, 0, 0))) {
                        g.DrawString(title, mainFont, shadowBrush, titleX + 2, titleY + 3);
                    }

                    // Gold gradient wordmark
                    using (LinearGradientBrush textBrush = new LinearGradientBrush(
                        new PointF(titleX, titleY), new PointF(titleX, titleY + titleSize.Height),
                        Color.FromArgb((int)(255 * logoAlpha), 255, 250, 238),
                        Color.FromArgb((int)(255 * logoAlpha), 224, 182, 98))) {
                        g.DrawString(title, mainFont, textBrush, titleX, titleY);
                    }

                    // Golden Specular Sweep (Part 0: frames 50 to 74)
                    if (!isLoop && p > 0.62f) {
                        float sweepP = Math.Min(1.0f, (p - 0.62f) / 0.32f);
                        float sweepX = titleX - 80 + sweepP * (titleSize.Width + 160);

                        using (GraphicsPath textPath = new GraphicsPath()) {
                            StringFormat sf = new StringFormat();
                            textPath.AddString(title, mainFont.FontFamily, (int)mainFont.Style, mainFont.Size, new PointF(titleX, titleY), sf);
                            g.SetClip(textPath);

                            using (LinearGradientBrush sweepBrush = new LinearGradientBrush(
                                new PointF(sweepX - 45, titleY), new PointF(sweepX + 45, titleY),
                                Color.FromArgb(0, 255, 255, 255),
                                Color.FromArgb(220, 255, 248, 195))) {
                                sweepBrush.SetBlendTriangularShape(0.5f);
                                g.FillRectangle(sweepBrush, sweepX - 45, titleY, 90, titleSize.Height + 10);
                            }
                            g.ResetClip();
                        }
                    }

                    // Subtitle
                    string sub = "Ø§Ù„Ø¬Ø²Ø§Ø¦Ø± Ø¯Ø§Ø¦Ù…Ø§Ù‹ ÙˆØ£Ø¨Ø¯Ø§Ù‹  â€¢  TANIX TX1";
                    SizeF subSize = g.MeasureString(sub, subFont);
                    float subX = (WIDTH - subSize.Width) / 2;
                    using (SolidBrush subBrush = new SolidBrush(Color.FromArgb((int)(175 * logoAlpha), 185, 200, 220))) {
                        g.DrawString(sub, subFont, subBrush, subX, titleY + titleSize.Height + 6);
                    }
                }
            }

            bmp.Save(outputPath, ImageFormat.Png);
        }
    }

    private static void DrawMountainRidge(Graphics g, int w, int h, int baseY, float variance, Color color, float rimGlow) {
        using (GraphicsPath path = new GraphicsPath()) {
            path.StartFigure();
            path.AddLine(0, h, 0, baseY);

            int segments = 24;
            float step = (float)w / segments;
            PointF[] crest = new PointF[segments + 1];

            for (int i = 0; i <= segments; i++) {
                float x = i * step;
                double s1 = Math.Sin(i * 0.45) * 55 * variance;
                double s2 = Math.Cos(i * 1.1 + 0.3) * 35 * variance;
                double s3 = Math.Sin(i * 2.3) * 15 * variance;
                double centerBoost = Math.Exp(-Math.Pow((i - segments / 2.0) / 4.0, 2)) * 68 * variance;
                float y = (float)(baseY - s1 - s2 - s3 - centerBoost);
                crest[i] = new PointF(x, y);
                path.AddLine(x, y, x, y);
            }

            path.AddLine(w, baseY, w, h);
            path.CloseFigure();

            using (SolidBrush brush = new SolidBrush(color)) {
                g.FillPath(brush, path);
            }

            // Subtle gold rim lighting on crest
            if (rimGlow > 0.05f) {
                using (Pen rimPen = new Pen(Color.FromArgb((int)(110 * rimGlow), 245, 195, 105), 1.5f)) {
                    for (int i = 0; i < segments; i++) {
                        g.DrawLine(rimPen, crest[i], crest[i + 1]);
                    }
                }
            }
        }
    }

    private static void DrawAlgerianEmblem(Graphics g, float cx, float cy, float radius, float alpha) {
        // Outer disc / crescent
        using (GraphicsPath crescent = new GraphicsPath()) {
            crescent.AddEllipse(cx - radius, cy - radius, radius * 2f, radius * 2f);
            using (GraphicsPath cutout = new GraphicsPath()) {
                cutout.AddEllipse(cx - radius * 0.35f, cy - radius * 0.82f, radius * 1.65f, radius * 1.65f);
                crescent.AddPath(cutout, false);
            }
            using (SolidBrush goldBrush = new SolidBrush(Color.FromArgb((int)(245 * alpha), 235, 195, 105))) {
                g.FillPath(goldBrush, crescent);
            }
        }

        // 5-pointed Star inside crescent
        DrawStar(g, cx + radius * 0.38f, cy, radius * 0.40f, Color.FromArgb((int)(245 * alpha), 235, 195, 105));
    }

    private static void DrawStar(Graphics g, float cx, float cy, float r, Color color) {
        PointF[] pts = new PointF[10];
        double step = Math.PI / 5;
        for (int i = 0; i < 10; i++) {
            double angle = i * step - Math.PI / 2;
            float rad = (i % 2 == 0) ? r : r * 0.42f;
            pts[i] = new PointF(cx + (float)(Math.Cos(angle) * rad), cy + (float)(Math.Sin(angle) * rad));
        }
        using (SolidBrush b = new SolidBrush(color)) {
            g.FillPolygon(b, pts);
        }
    }
}
'@
Add-Type -TypeDefinition $code -ReferencedAssemblies "System.Drawing"
$rootDir = Split-Path -Parent $PSScriptRoot; if (!$rootDir) { $rootDir = Get-Location }; $outDir = Join-Path $rootDir "assets\boot-animation"
if (Test-Path $outDir) { Remove-Item -Recurse -Force $outDir }
New-Item -ItemType Directory -Path $outDir -Force
[FullAnimationGenerator]::GenerateAll($outDir)
Write-Output "Rendered all frames into $outDir"
