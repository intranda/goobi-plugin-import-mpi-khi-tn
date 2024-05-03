package de.intranda.goobi.plugins;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RectWithDist {
    private int x, y;
    private int width, height;
    private double distance;
}
