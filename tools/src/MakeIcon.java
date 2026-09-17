import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

/**
 * Desktop only: draws the 48x48 application icon so the repository carries no
 * foreign art. Run with: java -cp build/tools MakeIcon res/icon.png
 */
public final class MakeIcon {

    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : "res/icon.png";
        int s = 48;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                img.setRGB(x, y, 0);
            }
        }
        // rounded plate with a lit rim
        for (int y = 1; y < s - 1; y++) {
            for (int x = 1; x < s - 1; x++) {
                int dx = Math.max(0, Math.abs(x - s / 2) - 16);
                int dy = Math.max(0, Math.abs(y - s / 2) - 16);
                if (dx * dx + dy * dy > 36) {
                    continue;
                }
                int shade = 0x16 + (y * 0x18) / s;
                int c = 0xFF000000 | (shade << 16) | ((shade + 6) << 8) | (shade + 20);
                img.setRGB(x, y, c);
            }
        }
        // visor
        for (int y = 14; y < 24; y++) {
            for (int x = 8; x < s - 8; x++) {
                int edge = (y == 14 || y == 23) ? 1 : 0;
                int c = edge == 1 ? 0xFF1C5A6E : 0xFF35D2E8;
                if (y > 18) {
                    c = edge == 1 ? 0xFF14404E : 0xFF1F93A8;
                }
                img.setRGB(x, y, c);
            }
        }
        // three scan slots in the visor
        for (int k = 0; k < 3; k++) {
            for (int y = 16; y < 22; y++) {
                int x = 14 + k * 10;
                img.setRGB(x, y, 0xFF0B1A20);
                img.setRGB(x + 1, y, 0xFF0B1A20);
            }
        }
        // crosshair below the visor
        int cx = s / 2;
        int cy = 34;
        for (int i = -8; i <= 8; i++) {
            if (Math.abs(i) > 2) {
                img.setRGB(cx + i, cy, 0xFFE8F4FF);
                img.setRGB(cx, cy + i, 0xFFE8F4FF);
            }
        }
        for (int i = -1; i <= 1; i++) {
            img.setRGB(cx + i, cy, 0xFFFF5040);
            img.setRGB(cx, cy + i, 0xFFFF5040);
        }
        ImageIO.write(img, "png", new File(path));
        System.out.println("wrote " + path);
    }

    private MakeIcon() {
    }
}
