package de.intranda.goobi.plugins;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import org.apache.commons.lang.mutable.MutableInt;

public class FindSubImage {

    public static void main(String[] args) throws IOException {
        List<Path> masterPaths = new ArrayList<>();
        List<Path> objPaths = new ArrayList<>();

        Map<Path, MutableInt> objImageToMasterCount = new ConcurrentHashMap<>();

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get("/home/robert/13506/images/master_khi_tn_b181034r_media"))) {
            for (Path p : dirStream) {
                if (p.getFileName().toString().startsWith("obj_")) {
                    objPaths.add(p);
                } else {
                    masterPaths.add(p);
                }
            }
        }

        Path dest = Paths.get("/home/robert/13506/images/match");

        masterPaths.parallelStream().forEach(p -> {
            try {
                String filename = p.getFileName().toString();
                Path destFolder = dest.resolve(filename.substring(0, filename.lastIndexOf('.')));

                for (Path objPath : objPaths) {

                    Optional<LocationWithDist> foundPosition = findSubImage(p, objPath);
                    if (foundPosition.isPresent()) {
                        objImageToMasterCount.get(objPath).increment();
                        Files.createDirectories(destFolder);
                        Files.copy(objPath, destFolder.resolve(objPath.getFileName()));
                    }
                }
                if (Files.exists(destFolder)) {
                    Files.copy(p, destFolder.resolve(p.getFileName()));
                }
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        });

        for (Entry<Path, MutableInt> numberEntry : objImageToMasterCount.entrySet()) {
            System.out.println(numberEntry);
        }

    }

    public static Optional<LocationWithDist> findSubImage(Path pathToBigImage, Path pathToSmallImage) throws IOException {

        BufferedImage bigImage = readImage(pathToBigImage);
        BufferedImage smallImage = readImage(pathToSmallImage);

        List<LocationWithDist> locs = new ArrayList<>();
        IntStream.range(0, bigImage.getHeight() - smallImage.getHeight() + 1).forEach(y -> {
            for (int x = 0; x < bigImage.getWidth() - smallImage.getWidth() + 1; x++) {
                double distance = imageDistance(bigImage, x, y, smallImage);
                if (distance < 0.08) {
                    locs.add(new LocationWithDist(x, y, distance));
                }
            }
        });

        return locs.stream().sorted(Comparator.comparing(LocationWithDist::getDistance)).findFirst();
    }

    private static BufferedImage readImage(Path pathToBigImage) throws IOException {
        BufferedImage bigImage = null;
        try (InputStream in = Files.newInputStream(pathToBigImage)) {
            BufferedImage desktopIn = ImageIO.read(in);
            bigImage = scaleImage(desktopIn);
        }
        return bigImage;
    }

    private static BufferedImage scaleImage(BufferedImage desktop) {
        Image toolkitImage = desktop.getScaledInstance(desktop.getWidth() / 5, desktop.getHeight() / 5, Image.SCALE_FAST);
        int width = toolkitImage.getWidth(null);
        int height = toolkitImage.getHeight(null);

        // width and height are of the toolkit image
        desktop = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = desktop.getGraphics();
        g.drawImage(toolkitImage, 0, 0, null);
        g.dispose();
        return desktop;
    }

    // computes the root mean squared error between a rectangular window in
    // bigImg and target.
    private static double imageDistance(BufferedImage bigImg, int bx, int by, BufferedImage target) {
        double dist = 0;
        for (int y = 0; y < target.getHeight(); y += 1) {
            for (int x = 0; x < target.getWidth(); x += 1) {
                // assume RGB images...
                int targetRGB = target.getRGB(x, y);
                int bigRGB = bigImg.getRGB(bx + x, by + y);
                for (int colorChannel = 0; colorChannel < 3; colorChannel++) {
                    int targetColor = (targetRGB >> (8 * colorChannel)) & 0xFF;
                    int bigColor = (bigRGB >> (8 * colorChannel)) & 0xFF;
                    dist += Math.pow(targetColor - bigColor, 2);
                }
            }
        }
        return Math.sqrt(dist) / target.getWidth() / target.getHeight();
    }
}
