package com.udacity.catpoint.image;

import java.awt.image.BufferedImage;

/**
 * Interface that describes the necessary behaviors of the Image Recognition Service
 * for detecting cats in camera images.
 */
public interface ImageService {
    boolean imageContainsCat(BufferedImage image, float confidenceThreshold);
}
