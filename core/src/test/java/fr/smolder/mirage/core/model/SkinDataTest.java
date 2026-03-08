package fr.smolder.mirage.core.model;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkinDataTest {
    @Test
    void normalizesVerboseTexturePayloadToMinifiedTexturesOnlyJson() {
        String verbosePayload = """
                {
                  "timestamp" : 1772947964051,
                  "profileId" : "7060094982d74173a3cf85f756470a9b",
                  "profileName" : "inexactostentat",
                  "signatureRequired" : true,
                  "textures" : {
                    "SKIN" : {
                      "url" : "http://textures.minecraft.net/texture/ee5cc759a1854963cfa6569b02875f76e5b804b3280bc6e6b441585f621b64f9",
                      "metadata" : {
                        "model" : "slim"
                      }
                    }
                  }
                }
                """;
        String encoded = Base64.getEncoder().encodeToString(verbosePayload.getBytes(StandardCharsets.UTF_8));

        SkinData skinData = new SkinData(encoded, null);

        assertEquals(
                "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/ee5cc759a1854963cfa6569b02875f76e5b804b3280bc6e6b441585f621b64f9\",\"metadata\":{\"model\":\"slim\"}}}}",
                new String(Base64.getDecoder().decode(skinData.textureBase64()), StandardCharsets.UTF_8)
        );
        assertEquals("", skinData.signature());
    }
}
