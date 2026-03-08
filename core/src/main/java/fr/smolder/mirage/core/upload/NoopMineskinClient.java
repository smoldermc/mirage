package fr.smolder.mirage.core.upload;

import fr.smolder.mirage.core.model.SkinData;

import java.awt.image.BufferedImage;
import java.io.IOException;

public final class NoopMineskinClient implements MineskinClient {
    @Override
    public SkinData upload(String tileHash, BufferedImage skinImage) throws IOException {
        throw new UnsupportedOperationException("Mineskin upload is not implemented in the scaffold yet.");
    }
}
