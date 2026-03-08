package fr.smolder.mirage.core.upload;

import fr.smolder.mirage.core.model.SkinData;

import java.awt.image.BufferedImage;
import java.io.IOException;

public interface MineskinClient {
    SkinData upload(String tileHash, BufferedImage skinImage) throws IOException, InterruptedException;
}
